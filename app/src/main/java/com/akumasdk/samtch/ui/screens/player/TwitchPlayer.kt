package com.akumasdk.samtch.ui.screens.player

import android.content.ComponentName
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.service.PlaybackService
import com.akumasdk.samtch.ui.components.WebViewContainer
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import com.akumasdk.samtch.ui.components.createTwitchPlayerUrl
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun TwitchPlayer(
    channel: String = "forsen",
    isFullscreen: Boolean = false,
    isPip: Boolean = false,
    isMinimized: Boolean = false,
    refreshTrigger: Int = 0,
    onToggleFullscreen: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onClose: () -> Unit = {},
    onExpand: () -> Unit = {},
    onRefreshRequested: () -> Unit = {},
    onMetadataUpdated: (String?, String?) -> Unit = { _, _ -> },
    onAudioOnlyModeChanged: (Boolean) -> Unit = {},
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {}
) {
    val context = LocalContext.current
    var isAudioOnly by remember { mutableStateOf(false) }
    var isUiLoading by remember { mutableStateOf(true) }
    val defaultLoadingMessage = stringResource(R.string.loading_stream)
    var loadingMessage by remember(defaultLoadingMessage) { mutableStateOf(defaultLoadingMessage) }
    var adblockText by remember { mutableStateOf("") }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var streamSubtitle by remember { mutableStateOf<String?>(null) }
    var streamMetadata by remember { mutableStateOf<TwitchStreamMetadata?>(null) }

    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentAvatarUrl by rememberUpdatedState(avatarUrl)

    val isAudioOnlyBackgroundEnabled by SettingsManager.isAudioOnlyBackgroundEnabled(context).collectAsState(initial = false)

    val chatViewModel: ChatViewModel = viewModel()

    val hintShown by SettingsManager.isMiniPlayerHintShown(context).collectAsState(initial = true)

    var isChatVisible by remember { mutableStateOf(true) }

    // Nudge animation for first-time users
    val nudgeOffset = remember { Animatable(0f) }
    LaunchedEffect(isMinimized, hintShown) {
        if (isMinimized && !hintShown) {
            delay(1000.milliseconds)
            // Nudge right
            nudgeOffset.animateTo(
                targetValue = 40f,
                animationSpec = SamtchAnimation.springBouncy()
            )
            // Back to center
            nudgeOffset.animateTo(
                targetValue = 0f,
                animationSpec = SamtchAnimation.springInteractive()
            )
            SettingsManager.setMiniPlayerHintShown(context, true)
        }
    }

    // Manage chat connection lifecycle based on player state and app background status
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val chatLoadingText = stringResource(R.string.chat_connecting)
    val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
    val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

    LaunchedEffect(channel, isPip, lifecycleState) {
        // Only stay connected if NOT in PiP and the app is in the foreground (at least STARTED)
        val shouldBeConnected = !isPip && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        
        if (shouldBeConnected) {
            chatViewModel.connect(channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate)
        } else {
            // Disconnect when entering PiP or going to background (Background Audio)
            chatViewModel.disconnect()
        }
    }

    // Safety timeout for loading screen
    LaunchedEffect(isUiLoading) {
        if (isUiLoading) {
            delay(12.seconds) // 12 second absolute maximum for loading screen
            if (isUiLoading) {
                Log.w("TwitchPlayer", "Loading timeout reached for $channel - forcing removal")
                isUiLoading = false
            }
        }
    }

    LaunchedEffect(channel, refreshTrigger) {
        isUiLoading = true
        loadingMessage = defaultLoadingMessage
        while (true) {
            // Fetch detailed metadata via GraphQL
            Log.d("TwitchPlayer", "Fetching periodic metadata for $channel")
            val metadata = TwitchGqlService.getStreamMetadata(channel)
            
            // Append timestamp to preview image URL to force refresh in UI and MediaController
            val timestampedMetadata = metadata?.copy(
                user = metadata.user?.copy(
                    stream = metadata.user.stream?.let { stream ->
                        stream.copy(
                            previewImageUrl = stream.previewImageUrl?.let { url ->
                                val separator = if (url.contains("?")) "&" else "?"
                                "$url${separator}t=${System.currentTimeMillis()}"
                            }
                        )
                    }
                )
            )
            streamMetadata = timestampedMetadata
            
            // Update UI-facing metadata
            timestampedMetadata?.user?.let { user ->
                avatarUrl = user.profileImageUrl
                streamSubtitle = user.stream?.title
                onMetadataUpdated(user.profileImageUrl, user.stream?.title)
            }
            
            delay(1.minutes)
        }
    }

    LaunchedEffect(isAudioOnly) {
        onAudioOnlyModeChanged(isAudioOnly)
    }

    // Connect to service only when Audio Only mode is manually enabled
    LaunchedEffect(isAudioOnly) {
        if (!isAudioOnly) {
            mediaController?.release()
            mediaController = null
            // If the background setting is also off, ensure the service is killed
            if (!isAudioOnlyBackgroundEnabled) {
                context.stopService(android.content.Intent(context, PlaybackService::class.java))
            }
            return@LaunchedEffect
        }

        if (mediaController == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener({
                val controller = controllerFuture.get()
                mediaController = controller
                isPlaying = controller.isPlaying
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
            }, MoreExecutors.directExecutor())
        }
    }

    // Handle initial playback when switching to Audio Only mode
    LaunchedEffect(mediaController, isAudioOnly) {
        if (isAudioOnly && mediaController != null && !isPlaying) {
            val previewUri = streamMetadata?.user?.stream?.previewImageUrl?.toUri()
            val metadata = MediaMetadata.Builder()
                .setTitle(streamMetadata?.user?.stream?.title ?: channel)
                .setArtist(streamMetadata?.user?.displayName ?: channel)
                .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
                .setArtworkUri(previewUri)
                .build()
            
            mediaController?.setMediaItem(
                MediaItem.Builder()
                    .setMediaId(channel)
                    .setMediaMetadata(metadata)
                    .build()
            )
            mediaController?.prepare()
            mediaController?.play()
        }
    }

    // Periodically update controller metadata if it's connected
    LaunchedEffect(streamMetadata, mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        val stream = streamMetadata?.user?.stream ?: return@LaunchedEffect
        
        Log.d("TwitchPlayer", "Updating controller metadata for $channel")
        val metadata = MediaMetadata.Builder()
            .setTitle(stream.title)
            .setArtist(streamMetadata?.user?.displayName ?: channel)
            .setAlbumTitle(stream.game?.name)
            .setArtworkUri(stream.previewImageUrl?.toUri())
            .build()
            
        // Use replaceMediaItem to update metadata without disrupting the stream
        controller.replaceMediaItem(
            0,
            MediaItem.Builder()
                .setMediaId(channel)
                .setMediaMetadata(metadata)
                .build()
        )
    }

    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()

    Log.d("TwitchPlayer", "Creating player for channel: $channel (isPip: $isPip, isMinimized: $isMinimized)")

    // Handle back button to return to browser (minimize)
    if (!isPip && !isMinimized) {
        androidx.activity.compose.BackHandler {
            Log.d("TwitchPlayer", "BackHandler triggered for $channel")
            onBack?.invoke()
        }
    }

    // Handle URL loading and refresh logic
    LaunchedEffect(channel, refreshTrigger) {
        val baseUrl = createTwitchPlayerUrl(channel)
        val finalUrl = if (refreshTrigger > 0) {
            "$baseUrl&refresh=$refreshTrigger"
        } else {
            baseUrl
        }
        Log.d("TwitchPlayer", "Loading URL: $finalUrl (trigger: $refreshTrigger)")
        navigator.loadUrl(finalUrl)
    }

    DisposableEffect(channel) {
        onDispose {
            Log.d("TwitchPlayer", "Disposing player for channel: $channel")
            mediaController?.release()
            chatViewModel.disconnect()
            // Clean up WebView resources aggressively
            try {
                state.nativeWebView.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearCache(true)
                    clearHistory()
                    clearFormData()
                    // Remove all views to prevent overlap
                    removeAllViews()
                }
            } catch (e: Exception) {
                Log.e("TwitchPlayer", "Error disposing WebView", e)
            }
        }
    }

    // Stable WebView content that won't be recreated when moving in the tree
    val playerContent = remember(channel, isAudioOnly) {
        movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
            if (isAudioOnly) {
                AudioOnlyPlayer(
                    channel = channel,
                    avatarUrl = avatarUrl,
                    subtitle = streamSubtitle,
                    displayName = streamMetadata?.user?.displayName,
                    streamTitle = streamMetadata?.user?.stream?.title,
                    gameName = streamMetadata?.user?.stream?.game?.name,
                    viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                    isPlaying = isPlaying,
                    onTogglePlayback = {
                        if (currentIsPlaying) mediaController?.pause() else mediaController?.play()
                    },
                    onCloseAudioOnly = {
                        isAudioOnly = false
                        mediaController?.stop()
                        onRefreshRequested()
                    },
                    onRefresh = {
                        mediaController?.stop()
                        val avatarUri = currentAvatarUrl?.toUri()
                        mediaController?.setMediaItem(
                            MediaItem.Builder()
                                .setMediaId(channel)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(streamMetadata?.user?.stream?.title ?: channel)
                                        .setArtist(streamMetadata?.user?.displayName ?: channel)
                                        .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
                                        .setArtworkUri(avatarUri)
                                        .build()
                                )
                                .build()
                        )
                        mediaController?.prepare()
                        mediaController?.play()
                    },
                    previewImageUrl = streamMetadata?.user?.stream?.previewImageUrl,
                    modifier = modifier
                )
            } else {
                Box(modifier = modifier.background(Color.Black)) {
                    WebViewContainer(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { layoutCoordinates ->
                                if (!isMinimized) {
                                    val rect = layoutCoordinates.boundsInWindow()
                                    onVideoBoundsChanged(
                                        android.graphics.Rect(
                                            rect.left.toInt(),
                                            rect.top.toInt(),
                                            rect.right.toInt(),
                                            rect.bottom.toInt()
                                        )
                                    )
                                }
                            },
                        state = state,
                        navigator = navigator,
                        channel = channel,
                        onToggleFullscreen = onToggleFullscreen,
                        onToggleChat = onToggleChat,
                        onToggleAudioOnly = { isAudioOnly = true },
                        onPlaybackStarted = { isUiLoading = false },
                        onLoadingStatus = { message -> loadingMessage = message },
                        onAdblocked = { text ->
                            adblockText = text
                            if (text.isNotEmpty() && isUiLoading) isUiLoading = false
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = isUiLoading,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                        modifier = Modifier.matchParentSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            val previewUrl = streamMetadata?.user?.stream?.previewImageUrl
                                ?: "https://static-cdn.jtvnw.net/previews-ttv/live_user_${channel.lowercase()}-853x480.jpg"

                            AsyncImage(
                                model = previewUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f))
                            )
                            CircularProgressIndicator(
                                color = Color(0xFF9146FF),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = loadingMessage,
                                color = Color.White,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    shadow = Shadow(color = Color.Black, blurRadius = 8f)
                                ),
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .align(Alignment.Center)
                                    .offset(y = 40.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isMinimized) {
        if (isMinimized && !hintShown) {
            SettingsManager.setMiniPlayerHintShown(context, true)
        }
    }

    // --- STABLE ANIMATION SYSTEM ---
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Size & Position animations
    val playerHeight by animateDpAsState(
        targetValue = if (isMinimized) 64.dp else if (isAudioOnly) 240.dp else (screenWidth * 9 / 16),
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerHeight"
    )

    val playerWidth by animateDpAsState(
        targetValue = if (isMinimized) 120.dp else screenWidth,
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerWidth"
    )

    val playerPaddingStart by animateDpAsState(
        targetValue = if (isMinimized) (8.dp + 16.dp) else 0.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerPaddingStart"
    )

    val playerPaddingBottom by animateDpAsState(
        targetValue = if (isMinimized) (12.dp + 8.dp) else 0.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerPaddingBottom"
    )

    val playerCornerRadius by animateDpAsState(
        targetValue = if (isMinimized) 20.dp else 0.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerCornerRadius"
    )

    val playerElevation by animateDpAsState(
        targetValue = if (isMinimized) 12.dp else 0.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerElevation"
    )

    // Root Container
    SharedTransitionLayout {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fullscreen Background
            if (!isMinimized) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

            // 1. FULL PLAYER OVERLAY (Chat, Metadata)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isMinimized,
                enter = fadeIn(animationSpec = SamtchAnimation.EmphasizedTween),
                exit = fadeOut(animationSpec = SamtchAnimation.FastTween)
            ) {
                if (isFullscreen) {
                    FullscreenPlayer(
                        channel = channel,
                        displayName = streamMetadata?.user?.displayName,
                        avatarUrl = avatarUrl,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        adblockText = adblockText,
                        streamStartedAt = streamMetadata?.user?.stream?.createdAt,
                        isChatVisible = isChatVisible,
                        onToggleChat = { isChatVisible = !isChatVisible },
                        chatViewModel = chatViewModel,
                        webView = { _, _ -> /* Stable player is shared */ }
                    )
                } else {
                    PortraitPlayer(
                        channel = channel,
                        displayName = streamMetadata?.user?.displayName,
                        avatarUrl = avatarUrl,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        isAudioOnly = isAudioOnly,
                        adblockText = adblockText,
                        streamStartedAt = streamMetadata?.user?.stream?.createdAt,
                        isChatVisible = isChatVisible,
                        onToggleChat = { isChatVisible = !isChatVisible },
                        onToggleFullscreen = onToggleFullscreen,
                        chatViewModel = chatViewModel,
                        webView = { _, _ -> /* Stable player is shared */ }
                    )
                }
            }

            // 2. MINI PLAYER SHELL & DISMISS LOGIC
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                        onClose()
                        true
                    } else {
                        false
                    }
                }
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isMinimized,
                enter = fadeIn(animationSpec = SamtchAnimation.StandardTween) + scaleIn(initialScale = 0.92f, animationSpec = SamtchAnimation.StandardTween),
                exit = fadeOut(animationSpec = SamtchAnimation.FastTween) + scaleOut(targetScale = 0.92f, animationSpec = SamtchAnimation.FastTween),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    .offset { IntOffset(nudgeOffset.value.roundToInt(), 0) }
            ) {
                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    backgroundContent = {
                        val direction = dismissState.dismissDirection
                        val isSwiping = direction != SwipeToDismissBoxValue.Settled
                        val progress = if (isSwiping) dismissState.progress else 0f
                        
                        val color by animateColorAsState(
                            if (isSwiping) Color.Red.copy(alpha = (0.1f + (0.3f * progress)).coerceIn(0f, 0.4f)) else Color.Transparent,
                            label = "DismissBackground"
                        )

                        val iconScale by animateFloatAsState(
                            if (isSwiping) 0.8f + (0.4f * progress) else 0.5f,
                            animationSpec = SamtchAnimation.springBouncy(),
                            label = "TrashIconScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(40.dp))
                                .background(color),
                            contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) 
                                Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            if (isSwiping) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = (0.2f + progress).coerceIn(0f, 1f)),
                                    modifier = Modifier
                                        .padding(horizontal = 28.dp)
                                        .size(28.dp)
                                        .scale(iconScale)
                                )
                            }
                        }
                    }
                ) {
                    // This mimics the MiniPlayer surface
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .shadow(playerElevation, RoundedCornerShape(40.dp))
                            .clip(RoundedCornerShape(40.dp))
                            .clickable(onClick = onExpand),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                        ) {
                            // Placeholder for the shared player
                            Box(modifier = Modifier.size(width = 120.dp, height = 64.dp))
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = streamMetadata?.user?.displayName ?: channel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = streamMetadata?.user?.stream?.title ?: "Live",
                                    color = Color(0xFFBF94FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. THE STABLE PLAYER (Stable during layout changes)
            Box(
                modifier = if (isPip) {
                    Modifier.fillMaxSize()
                } else if (isMinimized) {
                    Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(bottom = playerPaddingBottom)
                        .padding(start = playerPaddingStart)
                        .offset { IntOffset(nudgeOffset.value.roundToInt(), 0) } // Follow the nudge!
                        .offset { 
                            // Follow the swipe to dismiss offset
                            IntOffset(dismissState.requireOffset().roundToInt(), 0) 
                        }
                        .size(playerWidth, playerHeight)
                        .clip(RoundedCornerShape(playerCornerRadius))
                } else {
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(playerWidth, playerHeight)
                        .clip(RectangleShape)
                }
            ) {
                playerContent(Modifier.fillMaxSize()) {
                    isChatVisible = !isChatVisible
                }
                if (isMinimized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // No ripple here, the parent Surface will show it or we just want the action
                                onClick = onExpand
                            )
                    )
                }
            }
        }
    }
}
