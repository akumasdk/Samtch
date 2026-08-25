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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
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
import com.akumasdk.samtch.ui.screens.player.components.BrightnessManager
import com.akumasdk.samtch.ui.screens.player.components.FullscreenChatToggle
import com.akumasdk.samtch.ui.screens.player.components.NativePlayer
import com.akumasdk.samtch.ui.screens.player.components.NativePlayerControls
import com.akumasdk.samtch.ui.screens.player.components.PlayerGestureIndicators
import com.akumasdk.samtch.ui.screens.player.components.PlayerGestureOverlay
import com.akumasdk.samtch.ui.screens.player.components.QualitySelectorDialog
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
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.theme.StreamPreviewInfo
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
    onSettingsClick: () -> Unit = {},
    onAudioOnlyModeChanged: (Boolean) -> Unit = {},
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val context = LocalContext.current
        val view = LocalView.current
        
        var isAudioOnly by playerViewModel::isAudioOnly
        var portraitMode by playerViewModel::portraitMode
        
        // Keep screen on while player is active and not in audio-only mode
        DisposableEffect(isAudioOnly) {
            if (!isAudioOnly) {
                view.keepScreenOn = true
            }
            onDispose {
                view.keepScreenOn = false
            }
        }
        
        val isImmersiveEnabled by SettingsManager.isImmersiveBackgroundEnabled(context).collectAsState(initial = true)

        val streamMetadata = playerViewModel.streamMetadata
        val avatarUrl = playerViewModel.avatarUrl
        val streamSubtitle = playerViewModel.streamSubtitle

        // Gesture Overlay State
        var isDraggingVolume by remember { mutableStateOf(false) }
        var volumeProgress by remember { mutableFloatStateOf(0f) }

        var isDraggingBrightness by remember { mutableStateOf(false) }
        var brightnessProgress by remember { mutableFloatStateOf(0.5f) }
        var hasExplicitBrightness by remember { mutableStateOf(false) }

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

        var currentLoadingSession by remember { mutableLongStateOf(0L) }

        val hintShown by SettingsManager.isMiniPlayerHintShown(context).collectAsState(initial = true)
        val tooltipShowCount by SettingsManager.getPlayerTooltipShowCount(context).collectAsState(initial = 0)

        var isChatVisible by remember { mutableStateOf(true) }
        var showNativeControls by remember { mutableStateOf(false) }
        var showQualityMenu by remember { mutableStateOf(false) }
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

        // Logic for using the native player and background service
        // We now use the native player for both Video and Audio Only modes.
        val isVideoRequired = remember(isAudioOnly, portraitMode, isFullscreen, isMinimized) {
            (!isAudioOnly) && (portraitMode != PortraitMode.CHAT_ONLY) && (isFullscreen || portraitMode == PortraitMode.VIDEO_AND_CHAT)
        }
        val shouldUseNativePlayer = isAudioOnly || isVideoRequired

        // Extracted Effects
        PlayerLifecycleEffects(
            channel = channel,
            isPip = isPip,
            refreshTrigger = refreshTrigger,
            lifecycleState = lifecycleState,
            portraitMode = portraitMode,
            chatViewModel = chatViewModel,
            chatLoadingText = chatLoadingText,
            chatWelcomeTemplate = chatWelcomeTemplate,
            chatLoginTemplate = chatLoginTemplate,
            isUiLoading = isUiLoading,
        ) { isUiLoading = false }

        AudioServiceEffects(
            channel = channel,
            shouldUseAudioService = shouldUseNativePlayer,
            isAudioOnlyBackgroundEnabled = isAudioOnlyBackgroundEnabled,
            playerViewModel = playerViewModel,
            context = context,
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

        LaunchedEffect(isAudioOnly) {
            onAudioOnlyModeChanged(isAudioOnly)
            // Re-evaluate stream quality when switching to/from audio only if we have the master URL
            playerViewModel.masterStreamUrl?.let { masterUrl ->
                playerViewModel.onStreamUrlFound(masterUrl, source = "mode_change")
            }
        }

        val state = rememberSaveableWebViewState("")
        val navigator = rememberWebViewNavigator()

        Log.d("TwitchPlayer", "Creating player for channel: $channel (isPip: $isPip, isMinimized: $isMinimized)")

        // Handle back button behavior:
        // 1. If emote menu is open, close it
        // 2. If in fullscreen, return to portrait
        // 3. Otherwise, minimize (return to browser)
        if (!isPip && !isMinimized) {
            BackHandler {
                Log.d("TwitchPlayer", "BackHandler triggered for $channel. isFullscreen=$isFullscreen. isEmoteMenuVisible=$isEmoteMenuVisible")
                if (isEmoteMenuVisible) {
                    chatViewModel.setEmoteMenuVisible(false)
                } else if (isFullscreen) {
                    onToggleFullscreen()
                } else {
                    onBack?.invoke()
                }
            }
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
                TwitchChat(
                    channel = channel,
                    isCompact = config.isCompact,
                    showInput = config.showInput,
                    refreshTrigger = config.refreshTrigger,
                    viewModel = chatViewModel,
                    portraitMode = pMode,
                    onToggleMode = onToggle,
                    onLoginRequested = onLoginRequested,
                    onSettingsClick = onSettingsClick,
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
                        // Video mode: Native Player + Orchestrator WebView
                        PlayerBackground(
                            channel = channel,
                            previewUrl = previewImageUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        ) {
                            // Native Player
                            Box(modifier = Modifier.fillMaxSize()) {
                                NativePlayer(
                                    player = playerViewModel.mediaController,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Custom Native Controls
                                NativePlayerControls(
                                    isVisible = showNativeControls && !isMinimized && !isPip,
                                    isPlaying = liveIsPlaying,
                                    onTogglePlayback = { playerViewModel.togglePlayback() },
                                    onToggleFullscreen = onToggleFullscreen,
                                    onToggleChat = onToggleChat,
                                    onRefresh = { 
                                        playerViewModel.updateMediaItem(channel)
                                        lastProcessedRefreshTrigger-- // Force refresh
                                    },
                                    onSettingsClick = { showQualityMenu = true },
                                    isSettingsEnabled = playerViewModel.availableQualities.isNotEmpty()
                                )
                            }

                            // Orchestrator WebView (Invisible)
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
                                onLoadingStatus = { loadingMessage = it },
                                onAdblocked = { text ->
                                    adblockText = text
                                    playerViewModel.onAdblocked(text)
                                    // Don't auto-dismiss loader if we are holding it for autoplay ads
                                    if (text.isNotEmpty() && isUiLoading && !playerViewModel.isHoldingLoader) {
                                        isUiLoading = false
                                    }
                                },
                                onVideoBoundsChanged = onVideoBoundsChanged,
                                onStreamUrlFound = { url, validated, source ->
                                    playerViewModel.onStreamUrlFound(url, validated, source)
                                },
                                onAdStatusChanged = { isAd, msg ->
                                    playerViewModel.onAdStatusChanged(isAd, msg)
                                },
                                modifier = Modifier
                                    .size(320.dp, 180.dp) // Maintain size for logic but hide
                                    .alpha(0f) 
                                    .offset(x = (-2000).dp)
                            )
                            
                            // Native player drives the loading screen now
                            LaunchedEffect(playerViewModel.isPlaying, playerViewModel.isHoldingLoader) {
                                if (playerViewModel.isPlaying && !playerViewModel.isHoldingLoader) {
                                    isUiLoading = false
                                }
                            }
                            
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

        BrightnessManager(
            isFullscreen = isFullscreen,
            isPip = isPip,
            isDraggingBrightness = isDraggingBrightness,
            brightnessProgress = brightnessProgress,
            onBrightnessProgressChanged = { brightnessProgress = it },
            hasExplicitBrightness = hasExplicitBrightness,
            onHasExplicitBrightnessChanged = { hasExplicitBrightness = it }
        )

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
                showNativeControls = false
            }
        }

        // Auto-hide native controls in portrait
        LaunchedEffect(showNativeControls, isFullscreen) {
            if (!isFullscreen && showNativeControls) {
                delay(5.seconds)
                showNativeControls = false
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

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val animatedTopPadding by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isFullscreen && !isAudioOnly) 0.dp else statusBarHeight,
            animationSpec = SamtchAnimation.DpSpring,
            label = "PlayerTopPadding"
        )

        // Root Container
        SharedTransitionLayout {
            var stablePlayerSize by remember {
                mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
            }

            // MINI PLAYER SHELL & DISMISS LOGIC
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

            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Fullscreen Background (Bottom-most)
                if (!isMinimized) {
                    val bgAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isFullscreen) 0.65f else 0.45f,
                        animationSpec = SamtchAnimation.StandardTween,
                        label = "BackgroundAlpha"
                    )
                    
                    PlayerBackground(
                        channel = channel,
                        previewUrl = streamMetadata?.user?.stream?.previewImageUrl,
                        refreshKey = playerViewModel.metadataRefreshTrigger,
                        modifier = Modifier.fillMaxSize(),
                        alpha = bgAlpha,
                        blurRadius = if (isFullscreen) 30.dp else 0.dp,
                        contentScale = ContentScale.FillBounds
                    ) {
                        // Immersive gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = if (isFullscreen) 0.8f else 0.4f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = if (isFullscreen) 0.9f else 0.6f)
                                        )
                                    )
                                )
                        )
                        
                        // Dynamic radial glow that moves or scales slightly in fullscreen
                        if (isFullscreen) {
                            val glowScale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isChatVisible) 1.2f else 1.0f,
                                animationSpec = SamtchAnimation.StandardTween,
                                label = "GlowScale"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                SamtchTheme.colors.twitchPurple.copy(alpha = 0.2f),
                                                Color.Transparent
                                            ),
                                            center = Offset(0f, 0f),
                                            radius = 800f * glowScale
                                        )
                                    )
                            )
                        }
                    }
                }

                // 2. THE STABLE PLAYER (Middle Layer)
                // Unified modifier system for perfectly smooth transitions
                key(channel) {
                    Box(
                        modifier = if (isPip) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .align(if (isMinimized) Alignment.BottomStart else Alignment.TopStart)
                                .then(if (isMinimized) Modifier.navigationBarsPadding() else Modifier)
                                .padding(
                                    start = layout.paddingStart.value.let { if (it.isSpecified) it.coerceAtLeast(0.dp) else 0.dp },
                                    bottom = layout.paddingBottom.value.let { if (it.isSpecified) it.coerceAtLeast(0.dp) else 0.dp },
                                    top = (if (!isMinimized && !isFullscreen) animatedTopPadding else 0.dp).let { if (it.isSpecified) it.coerceAtLeast(0.dp) else 0.dp }
                                )
                                .offset {
                                    if (isMinimized) {
                                        IntOffset(
                                            (nudgeOffset.value + dismissState.requireOffset()).roundToInt(),
                                            0
                                        )
                                    } else {
                                        IntOffset.Zero
                                    }
                                }
                                .size(
                                    width = layout.width.value.let { if (it.isSpecified) it.coerceAtLeast(0.dp) else 0.dp },
                                    height = layout.height.value.let { if (it.isSpecified) it.coerceAtLeast(0.dp) else 0.dp }
                                )
                                .clip(RoundedCornerShape(layout.cornerRadius.value))
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
                                        showNativeControls = showFullscreenControls
                                    } else {
                                        showNativeControls = !showNativeControls
                                        metadataExpandTrigger++
                                        if (portraitMode == PortraitMode.CHAT_ONLY) {
                                            portraitMode = PortraitMode.VIDEO_AND_CHAT
                                        }
                                    }
                                }
                            )
                    ) {
                        if (isPip && portraitMode == PortraitMode.CHAT_ONLY) {
                            val bgAlpha = if (isImmersiveEnabled) 0.3f else 0f
                            val bgBlur = if (isImmersiveEnabled) 60.dp else 0.dp
                            val surfaceAlpha = if (isImmersiveEnabled) {
                                if (SamtchTheme.colors.dialogBackground.luminance() > 0.5f) 0.94f else 0.82f
                            } else 1.0f

                            PlayerBackground(
                                channel = channel,
                                previewUrl = streamMetadata?.user?.stream?.previewImageUrl,
                                modifier = Modifier.fillMaxSize(),
                                alpha = bgAlpha,
                                blurRadius = bgBlur,
                                contentScale = ContentScale.FillBounds
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            SamtchTheme.colors.chatBackground.copy(
                                                alpha = surfaceAlpha
                                            )
                                        )
                                ) {
                                    chatContent(
                                        ChatContentConfig(
                                            true,
                                            false,
                                            refreshTrigger,
                                            isFullscreen = isFullscreen
                                        ),
                                        null,
                                        null,
                                        Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            playerContent(Modifier.fillMaxSize()) {
                                Log.d(
                                    "TwitchPlayer",
                                    "Toggle chat requested via bridge. isFullscreen: $isFullscreen"
                                )
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
                                PlayerGestureOverlay(
                                    isDraggingVolume = isDraggingVolume,
                                    isDraggingBrightness = isDraggingBrightness,
                                    volumeProgress = volumeProgress,
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
                    }
                }

                // 3. FULL PLAYER OVERLAY (Chat, Metadata) - Top-most layer
                CompositionLocalProvider(
                    LocalStreamPreview provides StreamPreviewInfo(
                        channel = channel,
                        previewUrl = streamMetadata?.user?.stream?.previewImageUrl,
                        refreshKey = playerViewModel.metadataRefreshTrigger
                    )
                ) {
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
                        isImmersiveEnabled = isImmersiveEnabled,
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
                }

                // 4. MINI PLAYER SHELL & DISMISS LOGIC
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
                        // This placeholder box will be filled by Point 2 (the shared player)
                    }
                )
            }

            if (showQualityMenu) {
                QualitySelectorDialog(
                    availableQualities = playerViewModel.availableQualities,
                    selectedQuality = playerViewModel.selectedQuality,
                    onQualitySelected = { playerViewModel.selectQuality(it) },
                    onDismiss = { showQualityMenu = false }
                )
            }
        }
    }
}
