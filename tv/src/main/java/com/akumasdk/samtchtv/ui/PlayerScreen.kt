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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.BufferingManager
import com.akumasdk.samtch.util.FpsMonitor
import com.akumasdk.samtch.util.PlaybackWatchdog
import com.akumasdk.samtch.util.StreamingPlayerFactory
import com.akumasdk.samtch.data.api.PreviewImageService
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.data.emote.EmoteType
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
                            .sortedByDescending { it.bandwidth ?: 0L }
                        qualities = filteredQualities
                        
                        // Default to Auto (null) instead of forcing highest bandwidth
                        // This allows ExoPlayer's ABR to handle initial startup
                        Log.d("PlayerScreen", "Found ${filteredQualities.size} qualities. Starting in Auto mode.")
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
        // Fetch metadata to get the most accurate preview URL
        val metadata = TwitchGqlService.getStreamMetadata(channel)
        previewUrl = PreviewImageService.getProcessedUrl(
            metadata?.user?.stream?.previewImageUrl,
            channel,
            width = 1280,
            height = 720
        )
            
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

    // Handle Back button: prioritize closing menus/dialogs, then exit to landing
    BackHandler {
        if (showQualityDialog) {
            showQualityDialog = false
        } else if (showMenu) {
            showMenu = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SamtchTheme.colors.rootBackground)
            .onKeyEvent { keyEvent ->
                // Only show menu on specific interaction keys (Center/OK, Directional keys)
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showMenu && !showQualityDialog) {
                                showMenu = true
                                return@onKeyEvent true
                            }
                        }
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
                            .background(SamtchTheme.colors.chatBackground.copy(alpha = 0.7f))
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
                        .background(SamtchTheme.colors.chatBackground)
                )
            }
        }
        
        // Loading Animation
        if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE) {
            Box(
                modifier = Modifier.fillMaxSize().background(SamtchTheme.colors.rootBackground),
                contentAlignment = Alignment.Center
            ) {
                previewUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = SamtchTheme.colors.accentColor,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (playbackState == Player.STATE_IDLE) "PREPARING STREAM" else "BUFFERING",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        letterSpacing = 2.sp
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
                .background(SamtchTheme.colors.miniPlayerBackground.copy(alpha = 0.85f), RoundedCornerShape(40.dp))
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
                containerColor = if (chatMode != ChatMode.OFF) SamtchTheme.colors.accentColor else SamtchTheme.colors.tabButtonBackground,
                contentColor = SamtchTheme.colors.primaryText
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
        title = { Text("Select Quality", color = SamtchTheme.colors.primaryText) },
        containerColor = SamtchTheme.colors.dialogBackground,
        text = {
            LazyColumn {
                item {
                    QualityItem(
                        label = "Auto (Adaptive)", 
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
        color = if (isSelected) SamtchTheme.colors.accentColor.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            color = if (isSelected) SamtchTheme.colors.accentColor else SamtchTheme.colors.primaryText,
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
    containerColor: Color = SamtchTheme.colors.tabButtonBackground,
    contentColor: Color = SamtchTheme.colors.primaryText
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
    val userColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { SamtchTheme.colors.defaultUserColor }

    val tokens = remember(msg) { parseMessageTokens(msg, channel) }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = displayName,
            color = userColor,
            style = MaterialTheme.typography.labelLarge
        )
        
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            tokens.forEach { token ->
                when (token) {
                    is MessageToken.Text -> {
                        Text(
                            text = token.text,
                            color = SamtchTheme.colors.primaryText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is MessageToken.EmoteToken -> {
                        EmoteImage(emote = token.emote, part = token.code)
                    }
                }
            }
        }
    }
}

sealed class MessageToken {
    data class Text(val text: String) : MessageToken()
    data class EmoteToken(val code: String, val emote: Emote) : MessageToken()
}

fun parseMessageTokens(msg: IrcMessage, channel: String): List<MessageToken> {
    val messageText = msg.message
    val occurrences = mutableListOf<Triple<IntRange, String, String>>() // Range, Code, URL

    // 1. Parse Twitch emotes from tags
    val twitchEmotesTag = msg.tags["emotes"]
    if (!twitchEmotesTag.isNullOrEmpty()) {
        twitchEmotesTag.split("/").forEach { emoteData ->
            val parts = emoteData.split(":")
            if (parts.size == 2) {
                val id = parts[0]
                val url = Constants.Twitch.Templates.EMOTE_CDN.format(id)
                parts[1].split(",").forEach { rangeStr ->
                    val rangeParts = rangeStr.split("-")
                    if (rangeParts.size == 2) {
                        val start = rangeParts[0].toIntOrNull() ?: 0
                        val end = rangeParts[1].toIntOrNull() ?: 0
                        try {
                            if (start < messageText.length && end < messageText.length) {
                                val code = messageText.substring(start, end + 1)
                                occurrences.add(Triple(start..end, code, url))
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // Sort by start position
    occurrences.sortBy { it.first.first }

    val tokens = mutableListOf<MessageToken>()
    var lastIdx = 0

    for (occ in occurrences) {
        val range = occ.first
        if (range.first > lastIdx) {
            val text = messageText.substring(lastIdx, range.first)
            // Process 3rd party emotes in the text segment
            tokens.addAll(processThirdPartyEmotes(text, channel))
        }
        
        tokens.add(MessageToken.EmoteToken(
            occ.second,
            Emote(
                id = "", 
                code = occ.second,
                url = occ.third,
                type = EmoteType.TWITCH
            )
        ))
        lastIdx = range.last + 1
    }

    if (lastIdx < messageText.length) {
        val text = messageText.substring(lastIdx)
        tokens.addAll(processThirdPartyEmotes(text, channel))
    }

    return if (tokens.isEmpty() && messageText.isNotEmpty()) {
        processThirdPartyEmotes(messageText, channel)
    } else tokens
}

fun processThirdPartyEmotes(text: String, channel: String): List<MessageToken> {
    if (text.isEmpty()) return emptyList()
    
    // Split by whitespace but keep the whitespace as separate parts to maintain spacing
    val tokens = mutableListOf<MessageToken>()
    val words = text.split(" ")
    
    words.forEachIndexed { index, word ->
        if (word.isNotEmpty()) {
            val thirdPartyEmote = EmoteRepository.getEmote(channel, word)
            if (thirdPartyEmote != null) {
                tokens.add(MessageToken.EmoteToken(word, thirdPartyEmote))
            } else {
                tokens.add(MessageToken.Text(word))
            }
        }
        // Add a space back if it's not the last word or if there was a trailing space
        if (index < words.size - 1) {
            tokens.add(MessageToken.Text(" "))
        }
    }
    return tokens
}

@Composable
fun EmoteImage(emote: Emote, part: String) {
    var aspectRatio by remember { mutableFloatStateOf(1f) }
    AsyncImage(
        model = emote.url,
        contentDescription = part,
        modifier = Modifier
            .height(28.dp)
            .aspectRatio(aspectRatio)
            .padding(horizontal = 2.dp),
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            val size = state.painter.intrinsicSize
            if (size.width > 0 && size.height > 0) {
                aspectRatio = size.width / size.height
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(modifier = modifier) {
        content()
    }
}
