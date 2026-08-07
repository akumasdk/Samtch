package com.akumasdk.samtch.ui.screens.player

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import com.akumasdk.samtch.ui.components.chat.TwitchChat
import com.akumasdk.samtch.ui.components.playerComponents.MiniPlayerContainer
import com.akumasdk.samtch.ui.components.playerComponents.MiniPlayerOverlay
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.components.playerComponents.PlayerLoadingScreen
import com.akumasdk.samtch.ui.components.playerComponents.TapTooltip
import com.akumasdk.samtch.ui.components.playerComponents.createTwitchPlayerUrl
import com.akumasdk.samtch.ui.screens.player.components.AudioOnlyPlayer
import com.akumasdk.samtch.ui.screens.player.components.AudioServiceEffects
import com.akumasdk.samtch.ui.screens.player.components.FullscreenChatToggle
import com.akumasdk.samtch.ui.screens.player.components.PlayerGestureIndicators
import com.akumasdk.samtch.ui.screens.player.components.PlayerLifecycleEffects
import com.akumasdk.samtch.ui.screens.player.components.PlayerOverlay
import com.akumasdk.samtch.ui.screens.player.components.PlayerWebView
import com.akumasdk.samtch.ui.screens.player.components.playerGestureHandler
import com.akumasdk.samtch.ui.screens.player.components.playerInputHandler
import com.akumasdk.samtch.ui.screens.player.components.rememberPlayerLayoutDimensions
import com.akumasdk.samtch.ui.screens.player.models.ChatContentConfig
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.screens.player.util.unloadWebView
import com.akumasdk.samtch.ui.screens.player.viewmodel.PlayerViewModel
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun TwitchPlayer(
    channel: String = "forsen",
    isFullscreen: Boolean = false,
    isPip: Boolean = false,
    isMinimized: Boolean = false,
    refreshTrigger: Int = 0,
    playerViewModel: PlayerViewModel = viewModel(),
    onToggleFullscreen: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onClose: () -> Unit = {},
    onExpand: () -> Unit = {},
    onMetadataUpdated: (String?, String?) -> Unit = { _, _ -> },
    onLoginRequested: () -> Unit = {},
    onAudioOnlyModeChanged: (Boolean) -> Unit = {},
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val context = LocalContext.current
        
        var isAudioOnly by playerViewModel::isAudioOnly
            var portraitMode by playerViewModel::portraitMode
        
        val streamMetadata = playerViewModel.streamMetadata
        val avatarUrl = playerViewModel.avatarUrl
        val streamSubtitle = playerViewModel.streamSubtitle

        // Gesture Overlay State
        var isDraggingVolume by remember { mutableStateOf(false) }
        var showVolumeOverlay by remember { mutableStateOf(false) }
        var volumeProgress by remember { mutableFloatStateOf(0f) }

        var isDraggingBrightness by remember { mutableStateOf(false) }
        var showBrightnessOverlay by remember { mutableStateOf(false) }
        var brightnessProgress by remember { mutableFloatStateOf(0.5f) }
        var originalBrightness by remember { mutableFloatStateOf(-100f) }

        val activity = context as? android.app.Activity

        // Handle Brightness Lifecycle & System Sync
        var lastKnownSystemBrightness by remember { mutableFloatStateOf(-1f) }
        var hasExplicitBrightness by remember { mutableStateOf(false) }
        val currentOriginalBrightness by rememberUpdatedState(originalBrightness)

        LaunchedEffect(isFullscreen, isPip) {
            if (isFullscreen && !isPip) {
                // Initialize
                val systemB = com.akumasdk.samtch.util.SystemSettingsUtil.getSystemBrightness(context)
                lastKnownSystemBrightness = systemB
                brightnessProgress = systemB
                hasExplicitBrightness = false
                
                activity?.let {
                    val lp = it.window.attributes
                    originalBrightness = lp.screenBrightness
                    // Start with system control
                    lp.screenBrightness = -1f
                    it.window.attributes = lp
                }
            } else {
                // Restore when NOT in fullscreen OR when in PiP
                if (originalBrightness != -100f) {
                    activity?.let {
                        val lp = it.window.attributes
                        lp.screenBrightness = originalBrightness
                        it.window.attributes = lp
                    }
                }
            }
        }

        // Observe system brightness changes to keep our control in sync
        LaunchedEffect(isFullscreen, isPip, isDraggingBrightness) {
            if (isFullscreen && !isPip && !isDraggingBrightness) {
                com.akumasdk.samtch.util.SystemSettingsUtil.observeSystemBrightness(context).collect { systemB ->
                    // Only sync if the SYSTEM value actually changed relative to itself.
                    if (lastKnownSystemBrightness != -1f && kotlin.math.abs(systemB - lastKnownSystemBrightness) > 0.02f) {
                        brightnessProgress = systemB
                        hasExplicitBrightness = false
                    }
                    lastKnownSystemBrightness = systemB
                }
            }
        }

        // Ensure brightness is restored when the player is completely dismissed
        DisposableEffect(activity) {
            onDispose {
                if (currentOriginalBrightness != -100f) {
                    activity?.let {
                        val lp = it.window.attributes
                        lp.screenBrightness = currentOriginalBrightness
                        it.window.attributes = lp
                    }
                }
            }
        }

        // Apply brightness changes while in fullscreen (but not in PiP)
        LaunchedEffect(brightnessProgress, isFullscreen, isPip, isDraggingBrightness, hasExplicitBrightness) {
            if (isFullscreen && !isPip) {
                activity?.let {
                    val lp = it.window.attributes
                    val currentSystemB = com.akumasdk.samtch.util.SystemSettingsUtil.getSystemBrightness(context)
                    
                    val isSameAsSystem = kotlin.math.abs(brightnessProgress - currentSystemB) < 0.01f
                    
                    if (!isDraggingBrightness && (!hasExplicitBrightness || isSameAsSystem)) {
                        lp.screenBrightness = -1f // Hand back control to system
                    } else {
                        lp.screenBrightness = brightnessProgress.coerceIn(0.01f, 1f)
                    }
                    
                    it.window.attributes = lp
                }
            }
        }

        LaunchedEffect(isDraggingVolume) {
            if (isDraggingVolume) {
                showVolumeOverlay = true
            } else {
                delay(2.seconds)
                showVolumeOverlay = false
            }
        }

        LaunchedEffect(isDraggingBrightness) {
            if (isDraggingBrightness) {
                showBrightnessOverlay = true
            } else {
                delay(2.seconds)
                showBrightnessOverlay = false
            }
        }

        var isUiLoading by remember { mutableStateOf(true) }
        val defaultLoadingMessage = stringResource(R.string.loading_stream)
        var loadingMessage by remember(defaultLoadingMessage) { mutableStateOf(defaultLoadingMessage) }
        var adblockText by remember { mutableStateOf("") }

        var showFullscreenControls by remember(isFullscreen) { mutableStateOf(!isFullscreen) }

        val isAudioOnlyBackgroundEnabled by SettingsManager.isAudioOnlyBackgroundEnabled(context).collectAsState(initial = false)

        val chatViewModel: ChatViewModel = viewModel()
        val isEmoteMenuVisible by chatViewModel.isEmoteMenuVisible.collectAsState()
        val isImeVisible = WindowInsets.isImeVisible
        val forceSlimMetadata = isEmoteMenuVisible || isImeVisible

        val scope = rememberCoroutineScope()
        var currentLoadingSession by remember { mutableLongStateOf(0L) }

        val hintShown by SettingsManager.isMiniPlayerHintShown(context).collectAsState(initial = true)
        val tooltipShowCount by SettingsManager.getPlayerTooltipShowCount(context).collectAsState(initial = 0)

        var isChatVisible by remember { mutableStateOf(true) }
        var metadataExpandTrigger by remember { mutableIntStateOf(0) }

        val lifecycleOwner = LocalLifecycleOwner.current
        val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
        val chatLoadingText = stringResource(R.string.chat_connecting)
        val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
        val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

        // Nudge animation for first-time users
        val nudgeOffset = remember { Animatable(0f) }
        LaunchedEffect(isMinimized, hintShown) {
            if (isMinimized && !hintShown) {
                delay(1000.milliseconds)
                nudgeOffset.animateTo(targetValue = 40f, animationSpec = SamtchAnimation.springBouncy())
                nudgeOffset.animateTo(targetValue = 0f, animationSpec = SamtchAnimation.springInteractive())
                SettingsManager.setMiniPlayerHintShown(context, true)
            }
        }

        // Toggle chat off by default when entering fullscreen and delay controls
        LaunchedEffect(isFullscreen) {
            if (isFullscreen) {
                isChatVisible = false
                delay(1.seconds) 
                showFullscreenControls = true
                if (tooltipShowCount < 2) {
                    SettingsManager.incrementPlayerTooltipShowCount(context)
                }
            }
        }

        var lastProcessedRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }

        // Logic for using the background audio service
        // Strictly tied to explicit Audio Only mode. Chat Only mode remains silent when minimized.
        val shouldUseAudioService = isAudioOnly

        // Extracted Effects
        PlayerLifecycleEffects(
            channel = channel,
            isPip = isPip,
            lifecycleState = lifecycleState,
            portraitMode = portraitMode,
            chatViewModel = chatViewModel,
            chatLoadingText = chatLoadingText,
            chatWelcomeTemplate = chatWelcomeTemplate,
            chatLoginTemplate = chatLoginTemplate,
            isUiLoading = isUiLoading,
            onLoadingTimeout = { isUiLoading = false }
        )

        AudioServiceEffects(
            channel = channel,
            shouldUseAudioService = shouldUseAudioService,
            isAudioOnlyBackgroundEnabled = isAudioOnlyBackgroundEnabled,
            playerViewModel = playerViewModel,
            context = context
        )

        // Consolidated loading and metadata logic
        LaunchedEffect(channel, refreshTrigger) {
            val isManualRefresh = refreshTrigger > lastProcessedRefreshTrigger
            lastProcessedRefreshTrigger = refreshTrigger
            playerViewModel.updateChannel(channel, forceRefresh = isManualRefresh)
        }

        // Keep parent activity in sync
        LaunchedEffect(avatarUrl, streamSubtitle) {
            onMetadataUpdated(avatarUrl, streamSubtitle)
        }

        LaunchedEffect(shouldUseAudioService) {
            onAudioOnlyModeChanged(shouldUseAudioService)
        }

        val state = rememberSaveableWebViewState("")
        val navigator = rememberWebViewNavigator()

        Log.d("TwitchPlayer", "Creating player for channel: $channel (isPip: $isPip, isMinimized: $isMinimized)")

        // Handle back button behavior:
        // 1. If in fullscreen, return to portrait
        // 2. Otherwise, minimize (return to browser)
        if (!isPip && !isMinimized) {
            BackHandler {
                Log.d("TwitchPlayer", "BackHandler triggered for $channel. isFullscreen=$isFullscreen")
                if (isFullscreen) {
                    onToggleFullscreen()
                } else {
                    onBack?.invoke()
                }
            }
        }

        val isVideoRequired = remember(isAudioOnly, portraitMode, isFullscreen, isMinimized) {
            // Video is NOT required if:
            // 1. Explicitly in Audio Only mode
            // 2. In Chat Only mode (always unload video)
            !isAudioOnly && portraitMode != PortraitMode.CHAT_ONLY && (isFullscreen || portraitMode == PortraitMode.VIDEO_AND_CHAT)
        }

        // Handle URL loading and refresh logic
        LaunchedEffect(channel, refreshTrigger, isVideoRequired) {
            if (!isVideoRequired) {
                Log.d("TwitchPlayer", "Video not required (audio=$isAudioOnly, mode=$portraitMode). Unloading.")
                // Invalidate current loading session and stop any active load immediately
                currentLoadingSession = System.currentTimeMillis()
                isUiLoading = false
                unloadWebView(state, navigator)
                return@LaunchedEffect
            }
            
            // Resume WebView if it was paused
            try {
                state.nativeWebView.apply {
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
                playerViewModel.disconnectMediaController()
                chatViewModel.disconnect()
                // Clean up WebView resources aggressively
                try {
                    state.nativeWebView.apply {
                        unloadWebView(state, navigator)
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
            movableContentOf { config: ChatContentConfig, pMode: PortraitMode?, onToggle: (() -> Unit)?, modifier: Modifier ->
                val liveMetadata = playerViewModel.streamMetadata
                val livePreviewImageUrl = liveMetadata?.user?.stream?.previewImageUrl

                TwitchChat(
                    channel = channel,
                    isCompact = config.isCompact,
                    showInput = config.showInput,
                    refreshTrigger = config.refreshTrigger,
                    viewModel = chatViewModel,
                    portraitMode = pMode,
                    onToggleMode = onToggle,
                    onLoginRequested = onLoginRequested,
                    modifier = modifier
                )
            }
        }

        // Stable WebView content that won't be recreated when moving in the tree,
        // but WILL be wiped out when entering Chat Only mode to save resources and ensure it stops playing.
        val playerContent = remember(channel) {
            movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
                Box(modifier = modifier) {
                    // Read metadata and state inside the lambda to ensure reactivity 
                    // even if the lambda is remembered and moved in the tree.
                    val liveMetadata = playerViewModel.streamMetadata
                    val liveAvatarUrl = playerViewModel.avatarUrl
                    val liveSubtitle = playerViewModel.streamSubtitle
                    val liveIsPlaying = playerViewModel.isPlaying
                    
                    // Mode logic
                    val isAudioOrChatMode = playerViewModel.isAudioOnly || playerViewModel.portraitMode == PortraitMode.CHAT_ONLY
                    val previewImageUrl = liveMetadata?.user?.stream?.previewImageUrl

                    if (isMinimized && isAudioOrChatMode) {
                        // Use the dedicated overlay for minimized non-video modes
                        MiniPlayerOverlay(
                            channel = channel,
                            avatarUrl = liveAvatarUrl,
                            previewImageUrl = previewImageUrl,
                            badgeText = if (playerViewModel.portraitMode == PortraitMode.CHAT_ONLY) "CHAT ONLY" else "AUDIO ONLY",
                            usePreview = playerViewModel.portraitMode == PortraitMode.CHAT_ONLY,
                            showLoading = isUiLoading && !playerViewModel.isAudioOnly
                        )
                    } else if (isAudioOrChatMode) {
                        // Expanded non-video modes: AudioOnlyPlayer (with its own background preview)
                        AudioOnlyPlayer(
                            channel = channel,
                            avatarUrl = liveAvatarUrl,
                            subtitle = liveSubtitle,
                            displayName = liveMetadata?.user?.displayName,
                            streamTitle = liveMetadata?.user?.stream?.title,
                            gameName = liveMetadata?.user?.stream?.game?.name,
                            viewersCount = liveMetadata?.user?.stream?.viewersCount ?: 0,
                            isPlaying = liveIsPlaying,
                            onTogglePlayback = {
                                playerViewModel.togglePlayback()
                            },
                            onCloseAudioOnly = {
                                isAudioOnly = false
                                portraitMode = PortraitMode.VIDEO_AND_CHAT
                                playerViewModel.disconnectMediaController()
                            },
                            onRefresh = {
                                playerViewModel.updateMediaItem(channel)
                            },
                            previewImageUrl = previewImageUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Video mode (Minimized or Expanded): Actual WebView Player
                        PlayerBackground(
                            channel = channel,
                            previewUrl = previewImageUrl,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PlayerWebView(
                                state = state,
                                navigator = navigator,
                                channel = channel,
                                isMinimized = isMinimized,
                                onToggleFullscreen = onToggleFullscreen,
                                onToggleChat = onToggleChat,
                                onToggleAudioOnly = {
                                    if (isFullscreen) {
                                        onToggleFullscreen()
                                    }
                                    isAudioOnly = true
                                    portraitMode = PortraitMode.AUDIO_AND_CHAT
                                },
                                onPlaybackStarted = {
                                    val session = currentLoadingSession
                                    scope.launch {
                                        delay(300.milliseconds)
                                        if (session == currentLoadingSession) {
                                            isUiLoading = false
                                        }
                                    }
                                },
                                onLoadingStatus = { loadingMessage = it },
                                onAdblocked = { text ->
                                    adblockText = text
                                    if (text.isNotEmpty() && isUiLoading) isUiLoading = false
                                },
                                onVideoBoundsChanged = onVideoBoundsChanged
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

        val bannerText = when {
            isAudioOnly -> stringResource(R.string.status_audio_only)
            portraitMode == PortraitMode.CHAT_ONLY -> stringResource(R.string.status_chat_only)
            else -> adblockText
        }

        // --- STABLE ANIMATION SYSTEM ---
        val viewConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current

        val layout = rememberPlayerLayoutDimensions(
            isMinimized = isMinimized,
            isAudioOnly = isAudioOnly,
            isFullscreen = isFullscreen,
            portraitMode = portraitMode,
            isPip = isPip,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            isChatVisible = isChatVisible
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
                PlayerOverlay(
                    isMinimized = isMinimized,
                    isFullscreen = isFullscreen,
                    channel = channel,
                    streamMetadata = streamMetadata,
                    avatarUrl = avatarUrl,
                    isAudioOnly = isAudioOnly,
                    adblockText = bannerText,
                    portraitMode = portraitMode,
                    metadataExpandTrigger = metadataExpandTrigger,
                    isPip = isPip,
                    isChatVisible = isChatVisible,
                    refreshTrigger = refreshTrigger,
                    forceSlimMetadata = forceSlimMetadata,
                    onToggleChat = { 
                        isChatVisible = !isChatVisible
                        if (isFullscreen) showFullscreenControls = true
                    },
                    onToggleMode = {
                        if (portraitMode == PortraitMode.CHAT_ONLY) {
                            portraitMode = PortraitMode.VIDEO_AND_CHAT
                            isAudioOnly = false
                        } else {
                            portraitMode = PortraitMode.CHAT_ONLY
                        }
                        isChatVisible = true
                    },
                    chatContent = { config, pMode, onToggle, modifier ->
                        chatContent(config, pMode, onToggle, modifier)
                    }
                )

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

                MiniPlayerContainer(
                    visible = isMinimized,
                    channel = channel,
                    displayName = streamMetadata?.user?.displayName,
                    streamTitle = streamMetadata?.user?.stream?.title,
                    elevation = layout.elevation.value,
                    nudgeOffset = nudgeOffset.value,
                    dismissState = dismissState,
                    onExpand = onExpand,
                    onClose = onClose,
                    content = {
                        // This placeholder box will be filled by Point 3 (the shared player)
                    }
                )

                // 3. THE STABLE PLAYER (Stable during layout changes)
                // We always render the player if a channel is selected to preserve the WebView 
                // instance and allow fast switching between modes.
                val shouldRenderPlayer = true
                
                if (shouldRenderPlayer) {
                    key(channel) { 
                        Box(
                            modifier = if (isPip) {
                                Modifier.fillMaxSize()
                            } else if (isMinimized) {
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .navigationBarsPadding()
                                    .padding(bottom = layout.paddingBottom.value)
                                    .padding(start = layout.paddingStart.value)
                                    .offset { IntOffset(nudgeOffset.value.roundToInt(), 0) } // Follow the nudge!
                                    .offset { 
                                        // Follow the swipe to dismiss offset
                                        IntOffset(dismissState.requireOffset().roundToInt(), 0) 
                                    }
                                    .size(layout.width.value, layout.height.value)
                                    .clip(RoundedCornerShape(layout.cornerRadius.value))
                            } else if (isFullscreen && !isAudioOnly) {
                            Modifier
                                .align(Alignment.TopStart)
                                .width(layout.width.value)
                                .fillMaxHeight()
                                .clip(RectangleShape)
                        } else {
                                Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .fillMaxWidth()
                                    .height(layout.height.value)
                                    .clip(RectangleShape)
                            }
                            .onSizeChanged { stablePlayerSize = it }
                            .playerGestureHandler(
                                isFullscreen = isFullscreen && !isAudioOnly,
                                onBrightnessChange = { 
                                    brightnessProgress = it
                                    hasExplicitBrightness = true
                                },
                                onVolumeChange = { volumeProgress = it },
                                onVolumeDragging = { isDraggingVolume = it },
                                onBrightnessDragging = { isDraggingBrightness = it }
                            )
                            .playerInputHandler(
                                size = stablePlayerSize,
                                isFullscreen = isFullscreen,
                                isMinimized = isMinimized,
                                doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis,
                                onDoubleTapCenter = {
                                    if (isFullscreen && !isAudioOnly) {
                                        isChatVisible = !isChatVisible
                                    } else {
                                        onToggleFullscreen()
                                    }
                                },
                                onSingleTap = {
                                    if (isFullscreen && !isAudioOnly) {
                                        showFullscreenControls = !showFullscreenControls
                                    } else {
                                        metadataExpandTrigger++
                                        if (portraitMode == PortraitMode.CHAT_ONLY) {
                                            portraitMode = PortraitMode.VIDEO_AND_CHAT
                                        }
                                    }
                                }
                            )
                    ) {
                        if (isPip && portraitMode == PortraitMode.CHAT_ONLY) {
                            chatContent(ChatContentConfig(true, false, refreshTrigger), null, null, Modifier.fillMaxSize())
                        } else {
                            playerContent(Modifier.fillMaxSize()) {
                                Log.d("TwitchPlayer", "Toggle chat requested via bridge. isFullscreen: $isFullscreen")
                                if (isFullscreen) {
                                    isChatVisible = !isChatVisible
                                    showFullscreenControls = true
                                } else {
                                    // Cycle modes in portrait
                                    if (portraitMode == PortraitMode.CHAT_ONLY) {
                                        portraitMode = PortraitMode.VIDEO_AND_CHAT
                                        isAudioOnly = false
                                    } else {
                                        portraitMode = PortraitMode.CHAT_ONLY
                                    }
                                    isChatVisible = true
                                }
                            }
                        }

                        // Overlays on top of the player
                        if (!isMinimized && !isPip) {
                            if (isFullscreen && !isAudioOnly) {
                                // Visual indicators for gestures
                                PlayerGestureIndicators(
                                    showVolume = showVolumeOverlay,
                                    volumeProgress = volumeProgress,
                                    showBrightness = showBrightnessOverlay,
                                    brightnessProgress = brightnessProgress
                                )

                                TapTooltip(
                                    visible = showFullscreenControls && tooltipShowCount < 2,
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                // Toggle Chat Tab Button
                                FullscreenChatToggle(
                                    visible = showFullscreenControls,
                                    isChatVisible = isChatVisible,
                                    onClick = {
                                        isChatVisible = !isChatVisible
                                        showFullscreenControls = true
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
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
}
