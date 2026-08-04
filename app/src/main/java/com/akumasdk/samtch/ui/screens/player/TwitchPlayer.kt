package com.akumasdk.samtch.ui.screens.player

import android.content.ComponentName
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
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
import com.akumasdk.samtch.ui.components.AdblockBanner
import com.akumasdk.samtch.ui.components.PlayerBackground
import com.akumasdk.samtch.ui.components.PlayerLoadingScreen
import com.akumasdk.samtch.ui.components.TapTooltip
import com.akumasdk.samtch.ui.components.TwitchChat
import com.akumasdk.samtch.ui.components.WebViewContainer
import com.akumasdk.samtch.ui.components.createTwitchPlayerUrl
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class PortraitMode {
    VIDEO_AND_CHAT,
    CHAT_ONLY
}

data class ChatContentConfig(
    val isCompact: Boolean,
    val showInput: Boolean,
    val refreshTrigger: Int
)

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

    var showFullscreenControls by remember(isFullscreen) { mutableStateOf(!isFullscreen) }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var streamSubtitle by remember { mutableStateOf<String?>(null) }
    var streamMetadata by remember { mutableStateOf<TwitchStreamMetadata?>(null) }

    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentAvatarUrl by rememberUpdatedState(avatarUrl)

    val isAudioOnlyBackgroundEnabled by SettingsManager.isAudioOnlyBackgroundEnabled(context).collectAsState(initial = false)

    val chatViewModel: ChatViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var currentLoadingSession by remember { mutableLongStateOf(0L) }

    val hintShown by SettingsManager.isMiniPlayerHintShown(context).collectAsState(initial = true)

    var isChatVisible by remember { mutableStateOf(true) }
    var portraitMode by remember { mutableStateOf(PortraitMode.VIDEO_AND_CHAT) }
    var metadataExpandTrigger by remember { mutableIntStateOf(0) }

    // Toggle chat off by default when entering fullscreen and delay controls
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            isChatVisible = false
            // Wait for rotation animation to fully end before showing any tooltips/controls
            delay(1.seconds) 
            showFullscreenControls = true
        }
    }

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

    LaunchedEffect(channel, isPip, lifecycleState, portraitMode) {
        // Stay connected if in foreground, OR if in PiP specifically in Chat Only mode
        val isForeground = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        val shouldBeConnected = isForeground && (!isPip || portraitMode == PortraitMode.CHAT_ONLY)
        
        if (shouldBeConnected) {
            chatViewModel.connect(channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate)
        } else {
            // Disconnect when entering PiP (if not in chat mode) or going to background
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

    // Consolidated loading and metadata logic
    LaunchedEffect(channel, refreshTrigger) {
        // Only the first metadata fetch and periodic ones
        // We don't set isUiLoading = true here anymore; the URL loader does it
        while (true) {
            Log.d("TwitchPlayer", "Fetching periodic metadata for $channel")
            val metadata = TwitchGqlService.getStreamMetadata(channel)
            
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

    val isVideoRequired = remember(isAudioOnly, isMinimized, isFullscreen, portraitMode) {
        !isAudioOnly && (isMinimized || isFullscreen || portraitMode == PortraitMode.VIDEO_AND_CHAT)
    }

    // Handle URL loading and refresh logic
    LaunchedEffect(channel, refreshTrigger, isVideoRequired) {
        if (!isVideoRequired) {
            Log.d("TwitchPlayer", "Video not required for current mode (isVideoRequired=false, audioOnly=$isAudioOnly). Unloading player.")
            // Invalidate current loading session and stop any active load immediately
            currentLoadingSession = System.currentTimeMillis()
            isUiLoading = false
            navigator.stopLoading()
            navigator.loadUrl(Constants.ABOUT_BLANK)
            
            // Aggressively silence the WebView if it still exists in the native layer
            try {
                state.nativeWebView.apply {
                    onPause() // Pause timers and JS
                    pauseTimers()
                    stopLoading()
                    loadUrl(Constants.ABOUT_BLANK)
                }
            } catch (_: Exception) {}
            return@LaunchedEffect
        }
        
        // Resume WebView if it was paused
        try {
            state.nativeWebView.apply {
                resumeTimers()
                onResume()
            }
        } catch (_: Exception) {}

        // If we were on about:blank and now need video, or if it's a refresh/channel change
        currentLoadingSession = System.currentTimeMillis()
        isUiLoading = true
        loadingMessage = defaultLoadingMessage

        val baseUrl = createTwitchPlayerUrl(channel)
        val finalUrl = if (refreshTrigger > 0) {
            "$baseUrl&refresh=$refreshTrigger"
        } else {
            baseUrl
        }
        Log.d("TwitchPlayer", "Loading URL: $finalUrl (session: $currentLoadingSession)")
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
                    loadUrl(Constants.ABOUT_BLANK)
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

    val chatContent = remember(channel) {
        movableContentOf { config: ChatContentConfig, modifier: Modifier ->
            TwitchChat(
                channel = channel,
                isCompact = config.isCompact,
                showInput = config.showInput,
                refreshTrigger = config.refreshTrigger,
                viewModel = chatViewModel,
                modifier = modifier
            )
        }
    }

    // Stable WebView content that won't be recreated when moving in the tree,
    // but WILL be wiped out when entering Chat Only mode to save resources and ensure it stops playing.
    val playerContent = remember(channel, isAudioOnly, portraitMode == PortraitMode.CHAT_ONLY) {
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
                val previewImageUrl = streamMetadata?.user?.stream?.previewImageUrl
                
                PlayerBackground(
                    channel = channel,
                    previewUrl = previewImageUrl,
                    modifier = modifier
                ) {
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
                        onPlaybackStarted = {
                            val session = currentLoadingSession
                            scope.launch {
                                // Add a small grace period (300ms) to ensure the WebView 
                                // has actually swapped buffers and rendered the first frame.
                                // Also verify we are still in the same loading session.
                                delay(300.milliseconds)
                                if (session == currentLoadingSession) {
                                    isUiLoading = false
                                }
                            }
                        },
                        onLoadingStatus = { message -> loadingMessage = message },
                        onAdblocked = { text ->
                            adblockText = text
                            if (text.isNotEmpty() && isUiLoading) isUiLoading = false
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = isUiLoading,
                        enter = fadeIn(animationSpec = SamtchAnimation.StandardTween),
                        exit = fadeOut(animationSpec = SamtchAnimation.StandardTween),
                        modifier = Modifier.matchParentSize()
                    ) {
                        PlayerLoadingScreen(
                            channel = channel,
                            previewUrl = previewImageUrl,
                            loadingMessage = loadingMessage
                        )
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

    // Auto-hide controls in fullscreen
    LaunchedEffect(showFullscreenControls, isFullscreen) {
        if (isFullscreen && showFullscreenControls) {
            delay(5.seconds)
            showFullscreenControls = false
        }
    }

    // --- STABLE ANIMATION SYSTEM ---
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Size & Position animations
    val playerHeight by animateDpAsState(
        targetValue = when {
            isMinimized -> 64.dp
            isAudioOnly -> 240.dp
            isFullscreen -> screenHeight
            portraitMode == PortraitMode.CHAT_ONLY && !isPip -> 0.dp
            else -> (screenWidth * 9 / 16)
        },
        animationSpec = SamtchAnimation.DpSpring,
        label = "StablePlayerHeight"
    )

    val playerWidth by animateDpAsState(
        targetValue = when {
            isMinimized -> 120.dp
            isFullscreen && isChatVisible -> (screenWidth - 300.dp)
            portraitMode == PortraitMode.CHAT_ONLY && !isPip -> 0.dp
            else -> screenWidth
        },
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
        var stablePlayerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

        Box(modifier = Modifier.fillMaxSize()) {
            // Fullscreen Background
            if (!isMinimized) {
                PlayerBackground(
                    channel = channel,
                    previewUrl = streamMetadata?.user?.stream?.previewImageUrl,
                    modifier = Modifier.fillMaxSize()
                )
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
                        refreshTrigger = refreshTrigger,
                        streamStartedAt = streamMetadata?.user?.stream?.createdAt,
                        previewImageUrl = streamMetadata?.user?.stream?.previewImageUrl,
                        isChatVisible = isChatVisible,
                        expandTrigger = metadataExpandTrigger,
                        onToggleChat = { 
                            isChatVisible = !isChatVisible
                            showFullscreenControls = true
                        },
                        chatContent = { isCompact, showInput, rTrigger, modifier -> 
                            chatContent(ChatContentConfig(isCompact, showInput, rTrigger), modifier)
                        },
                        webView = { modifier, _ -> 
                            Box(modifier = modifier)
                        }
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
                        previewImageUrl = streamMetadata?.user?.stream?.previewImageUrl,
                        portraitMode = portraitMode,
                        expandTrigger = metadataExpandTrigger,
                        onToggleMode = {
                            portraitMode = if (portraitMode == PortraitMode.VIDEO_AND_CHAT) 
                                PortraitMode.CHAT_ONLY else PortraitMode.VIDEO_AND_CHAT
                            isChatVisible = true
                        },
                        chatContent = { isCompact, showInput, modifier -> 
                            chatContent(ChatContentConfig(isCompact, showInput, refreshTrigger), modifier)
                        },
                        webView = { modifier, _ -> 
                            // Render a simple placeholder when Point 3 (the stable player) is active
                            // to avoid "tug-of-war" of the movable content.
                            Box(modifier = modifier)
                        }
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
                            if (isSwiping) SamtchTheme.colors.error.copy(alpha = (0.1f + (0.3f * progress)).coerceIn(0f, 0.4f)) else Color.Transparent,
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
                                    tint = SamtchTheme.colors.primaryText.copy(alpha = (0.2f + progress).coerceIn(0f, 1f)),
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
                        color = SamtchTheme.colors.miniPlayerBackground.copy(alpha = 0.98f),
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
                                    maxLines = 1,
                                    color = SamtchTheme.colors.miniPlayerTitle
                                )
                                Text(
                                    text = streamMetadata?.user?.stream?.title ?: "Live",
                                    color = SamtchTheme.colors.miniPlayerSubtitle,
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
                                    tint = SamtchTheme.colors.miniPlayerTitle.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. THE STABLE PLAYER (Stable during layout changes)
            val shouldRenderPlayer = portraitMode != PortraitMode.CHAT_ONLY || isMinimized || isFullscreen || isPip
            
            if (shouldRenderPlayer) {
                key(channel) { 
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
                                .align(Alignment.TopStart)
                                .size(playerWidth, playerHeight)
                                .clip(RectangleShape)
                        }
                        .onSizeChanged { stablePlayerSize = it }
                    .pointerInput(isFullscreen, isMinimized, portraitMode) {
                        var lastTapTime = 0L
                        awaitPointerEventScope {
                            while (true) {
                                // 1. Double tap detection on Initial pass (priority)
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val isPress = event.type == PointerEventType.Press
                                
                                if (isPress) {
                                    val currentTime = event.changes.first().uptimeMillis
                                    val isDoubleTap = (currentTime - lastTapTime) < viewConfiguration.doubleTapTimeoutMillis

                                    if (isDoubleTap) {
                                        val position = event.changes.first().position
                                        val centerX = stablePlayerSize.width / 2f
                                        val centerY = stablePlayerSize.height / 2f

                                        // Define central region (40% width and height from center)
                                        val radiusX = stablePlayerSize.width * 0.2f
                                        val radiusY = stablePlayerSize.height * 0.2f

                                        val isInCenterZone = kotlin.math.abs(position.x - centerX) <= radiusX &&
                                                kotlin.math.abs(position.y - centerY) <= radiusY

                                        if (isInCenterZone) {
                                            if (isFullscreen) {
                                                Log.d("TwitchPlayer", "Double tap: toggling chat")
                                                isChatVisible = !isChatVisible
                                            } else if (!isMinimized) {
                                                Log.d("TwitchPlayer", "Double tap: toggling fullscreen")
                                                onToggleFullscreen()
                                            }
                                            // Consume the second tap to prevent WebView from seeing it
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else {
                                        // Potential single tap - wait for Main pass to see if WebView consumes it
                                    }
                                    lastTapTime = currentTime
                                }
                            }
                        }
                    }
                    .pointerInput(isFullscreen, isMinimized) {
                        awaitPointerEventScope {
                            while (true) {
                                // 2. Single tap detection on Main pass (to avoid double-toggle with WebView controls)
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (event.type == PointerEventType.Press && !isMinimized) {
                                    if (isFullscreen) {
                                        showFullscreenControls = !showFullscreenControls
                                    } else {
                                        metadataExpandTrigger++
                                        // If we were in chat only, return to standard mode to show metadata
                                        if (portraitMode == PortraitMode.CHAT_ONLY) {
                                            portraitMode = PortraitMode.VIDEO_AND_CHAT
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    if (isPip && portraitMode == PortraitMode.CHAT_ONLY) {
                        chatContent(ChatContentConfig(true, false, refreshTrigger), Modifier.fillMaxSize())
                    } else {
                        playerContent(Modifier.fillMaxSize()) {
                            Log.d("TwitchPlayer", "Toggle chat requested via bridge. isFullscreen: $isFullscreen")
                            if (isFullscreen) {
                                isChatVisible = !isChatVisible
                                showFullscreenControls = true
                            } else {
                                // Cycle modes in portrait
                                portraitMode = when (portraitMode) {
                                    PortraitMode.VIDEO_AND_CHAT -> PortraitMode.CHAT_ONLY
                                    PortraitMode.CHAT_ONLY -> PortraitMode.VIDEO_AND_CHAT
                                }
                            }
                        }
                    }

                    // Overlays on top of the player
                    if (!isMinimized && !isPip) {
                        if (isFullscreen) {
                            TapTooltip(
                                visible = showFullscreenControls,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            // Toggle Chat Tab Button
                            AnimatedVisibility(
                                visible = showFullscreenControls,
                                enter = fadeIn() + slideInHorizontally { it / 2 },
                                exit = fadeOut() + slideOutHorizontally { it / 2 },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Surface(
                                    onClick = {
                                        isChatVisible = !isChatVisible
                                        showFullscreenControls = true
                                    },
                                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                                    color = SamtchTheme.colors.tabButtonBackground,
                                    contentColor = SamtchTheme.colors.primaryText,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.height(80.dp).width(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isChatVisible) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                                            contentDescription = "Toggle Chat",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
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
}
}
