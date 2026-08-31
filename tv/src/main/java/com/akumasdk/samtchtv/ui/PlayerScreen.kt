package com.akumasdk.samtchtv.ui

import android.util.Log
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import android.view.WindowManager
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ExtM3UParser
import com.akumasdk.samtch.util.ExtMediaEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.akumasdk.samtch.util.BufferingManager
import com.akumasdk.samtch.util.FpsMonitor
import com.akumasdk.samtch.util.PlaybackWatchdog
import com.akumasdk.samtch.util.StreamingPlayerFactory
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.service.TwitchChatClient
import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.data.emote.EmoteRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ChatMode {
    OFF, SIDE, OVERLAY
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(channel: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    
    // Completely hide and block the keyboard
    LaunchedEffect(Unit) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.ime())
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
    
    val exoPlayer = remember {
        val dataSourceFactory = StreamingPlayerFactory.getDataSourceFactory()
        val hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)

        StreamingPlayerFactory.createLowLatencyPlayerBuilder(context)
            .setMediaSourceFactory(hlsMediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    var showMenu by remember { mutableStateOf(false) }
    var chatMode by remember { mutableStateOf(ChatMode.OFF) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val chatClient = remember { TwitchChatClient(context) }
    val messages = remember { mutableStateListOf<IrcMessage>() }
    
    var qualities by remember { mutableStateOf<List<ExtMediaEntry>>(emptyList()) }
    var selectedQualityUrl by remember { mutableStateOf<String?>(null) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(channel, refreshTrigger) {
        val tokenPair = TwitchGqlService.getPlaybackAccessToken(channel)
        if (tokenPair != null) {
            val masterUrl = TwitchGqlService.buildHlsUrl(channel, tokenPair.first, tokenPair.second)
            
            // Fetch master manifest to get qualities
            withContext(Dispatchers.IO) {
                try {
                    val client = StreamingPlayerFactory.okHttpClient
                    val request = Request.Builder().url(masterUrl).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val parsed = ExtM3UParser().parse(body)
                        val filteredQualities = parsed.filter { !it.playlistUrl.isNullOrEmpty() }
                        qualities = filteredQualities
                        
                        // Set highest quality on start if not already selected
                        if (selectedQualityUrl == null && filteredQualities.isNotEmpty()) {
                            val highest = filteredQualities.maxByOrNull { it.bandwidth ?: 0L }
                            selectedQualityUrl = highest?.playlistUrl
                        }
                    }
                } catch (e: Exception) {
                    qualities = emptyList()
                }
            }

            val mediaItem = MediaItem.Builder()
                .setUri(selectedQualityUrl ?: masterUrl)
                .setLiveConfiguration(BufferingManager.getLiveConfiguration(true))
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }
    
    // Re-prepare when quality changes
    LaunchedEffect(selectedQualityUrl) {
        if (selectedQualityUrl != null) {
            val mediaItem = MediaItem.Builder()
                .setUri(selectedQualityUrl!!)
                .setLiveConfiguration(BufferingManager.getLiveConfiguration(true))
                .build()
            val currentPos = exoPlayer.currentPosition
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.seekTo(currentPos)
        }
    }

    LaunchedEffect(channel) {
        chatClient.connect(channel)
        chatClient.messages.collect { msg ->
            if (msg.command == "PRIVMSG") {
                messages.add(msg)
                if (messages.size > 20) {
                    messages.removeAt(0)
                }
            }
        }
    }

    LaunchedEffect(channel) {
        val metadata = TwitchGqlService.getStreamMetadata(channel)
        val rawUrl = metadata?.user?.stream?.previewImageUrl 
            ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())
        
        // Ensure high resolution for TV (replace placeholders or existing dimensions)
        previewUrl = rawUrl
            .replace("{width}", "1280").replace("{height}", "720")
            .replace("853x480", "1280x720")
            
        EmoteRepository.loadGlobalEmotes(context)
        EmoteRepository.loadChannelEmotes(context, channel)
    }
    
    LaunchedEffect(exoPlayer) {
        FpsMonitor.start(exoPlayer)
        PlaybackWatchdog.start(exoPlayer) {
            Log.e("PlayerScreen", "Watchdog triggered recovery!")
            refreshTrigger++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            chatClient.disconnect()
            FpsMonitor.stop()
            PlaybackWatchdog.stop()
            
            // Re-allow keyboard when leaving player
            (context as? Activity)?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED)
        }
    }

    // Auto-hide menu
    LaunchedEffect(showMenu) {
        if (showMenu) {
            delay(8000) // Longer delay for TV users
            showMenu = false
        }
    }

    // Handle Back button to close menu first
    BackHandler(enabled = showMenu || showQualityDialog) {
        if (showQualityDialog) {
            showQualityDialog = false
        } else if (showMenu) {
            showMenu = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                // Only intercept keys to show menu if it's hidden
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    if (!showMenu && !showQualityDialog) {
                        showMenu = true
                        return@onKeyEvent true
                    }
                }
                false
            }
            .clickable { 
                if (!showMenu) showMenu = true 
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(if (chatMode == ChatMode.SIDE) 0.8f else 1f)) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                if (chatMode == ChatMode.OVERLAY) {
                    ChatOverlay(
                        messages, 
                        channel, 
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(0.3f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.7f))
                    )
                }
            }

            if (chatMode == ChatMode.SIDE) {
                ChatOverlay(
                    messages, 
                    channel, 
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight()
                        .background(Color(0xFF121212))
                )
            }
        }
        
        // Loading Animation
        if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                previewUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.6f
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.Magenta,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (playbackState == Player.STATE_IDLE) "Preparing Stream..." else "Buffering...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerMenu(
                onBack = onBack,
                onRefresh = { refreshTrigger++ },
                onToggleChat = { 
                    chatMode = when(chatMode) {
                        ChatMode.OFF -> ChatMode.SIDE
                        ChatMode.SIDE -> ChatMode.OVERLAY
                        ChatMode.OVERLAY -> ChatMode.OFF
                    }
                },
                onSelectQuality = { showQualityDialog = true },
                chatMode = chatMode
            )
        }
        
        if (showQualityDialog) {
            QualityDialog(
                qualities = qualities,
                selectedUrl = selectedQualityUrl,
                onQualitySelected = { 
                    selectedQualityUrl = it
                    showQualityDialog = false
                    showMenu = false
                },
                onDismiss = { showQualityDialog = false }
            )
        }
    }
}

@Composable
fun PlayerMenu(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleChat: () -> Unit,
    onSelectQuality: () -> Unit,
    chatMode: ChatMode
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .padding(bottom = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(40.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuButton(
                onClick = onBack,
                icon = Icons.Default.ArrowBack,
                label = "Back"
            )
            MenuButton(
                onClick = onRefresh,
                icon = Icons.Default.Refresh,
                label = "Refresh",
                modifier = Modifier.focusRequester(focusRequester)
            )
            MenuButton(
                onClick = onSelectQuality,
                icon = Icons.Default.Settings,
                label = "Quality"
            )
            MenuButton(
                onClick = onToggleChat,
                icon = Icons.Default.Chat,
                label = when(chatMode) {
                    ChatMode.OFF -> "Show Chat"
                    ChatMode.SIDE -> "Side Chat"
                    ChatMode.OVERLAY -> "Overlay Chat"
                },
                containerColor = if (chatMode != ChatMode.OFF) Color.Magenta else Color(0xFF333333),
                contentColor = Color.White
            )
        }
    }
}

@Composable
fun QualityDialog(
    qualities: List<ExtMediaEntry>,
    selectedUrl: String?,
    onQualitySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Quality", color = Color.White) },
        containerColor = Color(0xFF1A1A1A),
        text = {
            LazyColumn {
                item {
                    QualityItem(
                        label = "Auto (Source)", 
                        isSelected = selectedUrl == null
                    ) {
                        onQualitySelected(null)
                    }
                }
                items(qualities) { quality ->
                    QualityItem(
                        label = quality.name ?: quality.resolution ?: "Unknown", 
                        isSelected = selectedUrl == quality.playlistUrl
                    ) {
                        onQualitySelected(quality.playlistUrl)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun QualityItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        color = if (isSelected) Color.Magenta.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            color = if (isSelected) Color.Magenta else Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun MenuButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF333333),
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp), // Taller buttons
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 28.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            focusedElevation = 12.dp
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun ChatOverlay(messages: List<IrcMessage>, channel: String, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        items(messages) { msg ->
            ChatMessageItem(msg, channel)
        }
    }
}

@Composable
fun ChatMessageItem(msg: IrcMessage, channel: String) {
    val displayName = msg.tags["display-name"] ?: msg.prefix?.split("!")?.get(0) ?: "Unknown"
    val colorHex = msg.tags["color"] ?: "#FFFFFF"
    val userColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { Color.White }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = displayName,
            color = userColor,
            style = MaterialTheme.typography.labelLarge
        )
        
        // Simple emote-aware rendering
        val parts = msg.message.split(" ")
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            parts.forEach { part ->
                val emote = EmoteRepository.getEmote(channel, part)
                if (emote != null) {
                    AsyncImage(
                        model = emote.url,
                        contentDescription = part,
                        modifier = Modifier.size(24.dp).padding(horizontal = 2.dp)
                    )
                } else {
                    Text(
                        text = "$part ",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(modifier = modifier) {
        content()
    }
}
