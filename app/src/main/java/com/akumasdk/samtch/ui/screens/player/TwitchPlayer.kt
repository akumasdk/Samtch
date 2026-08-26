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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.media3.common.Player
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
import com.akumasdk.samtch.ui.screens.player.components.AudioOnlyPlayer
import com.akumasdk.samtch.ui.screens.player.components.AudioServiceEffects
import com.akumasdk.samtch.ui.screens.player.components.BrightnessManager
import com.akumasdk.samtch.ui.screens.player.components.FullscreenChatToggle
import com.akumasdk.samtch.ui.screens.player.components.NativePlayer
import com.akumasdk.samtch.ui.screens.player.components.NativePlayerControls
import com.akumasdk.samtch.ui.screens.player.components.PlayerGestureOverlay
import com.akumasdk.samtch.ui.screens.player.components.QualitySelectorDialog
import com.akumasdk.samtch.ui.screens.player.components.PlayerLifecycleEffects
import com.akumasdk.samtch.ui.screens.player.components.PlayerOverlay
import com.akumasdk.samtch.ui.screens.player.components.playerGestureHandler
import com.akumasdk.samtch.ui.screens.player.components.playerInputHandler
import com.akumasdk.samtch.ui.screens.player.components.rememberPlayerLayoutDimensions
import com.akumasdk.samtch.ui.screens.player.models.ChatContentConfig
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.screens.player.viewmodel.PlayerViewModel
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.theme.StreamPreviewInfo
import kotlinx.coroutines.delay
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
        val isBuffering = playerViewModel.playbackState == Player.STATE_BUFFERING

        // Gesture Overlay State
        var isDraggingVolume by remember { mutableStateOf(false) }
        var volumeProgress by remember { mutableFloatStateOf(0f) }

        var isDraggingBrightness by remember { mutableStateOf(false) }
        var brightnessProgress by remember { mutableFloatStateOf(0.5f) }
        var hasExplicitBrightness by remember { mutableStateOf(false) }

        var isUiLoading by remember { mutableStateOf(true) }
        val defaultLoadingMessage = stringResource(R.string.loading_stream)
        var loadingMessage by remember(defaultLoadingMessage) { mutableStateOf(defaultLoadingMessage) }

        var showFullscreenControls by remember(isFullscreen) { mutableStateOf(false) }

        val isAudioOnlyBackgroundEnabled by SettingsManager.isAudioOnlyBackgroundEnabled(context).collectAsState(initial = false)

        val chatViewModel: ChatViewModel = viewModel()
        val isEmoteMenuVisible by chatViewModel.isEmoteMenuVisible.collectAsState()
        val isImeVisible = WindowInsets.isImeVisible
        val forceSlimMetadata = isEmoteMenuVisible || isImeVisible

        val hintShown by SettingsManager.isMiniPlayerHintShown(context).collectAsState(initial = true)
        val tooltipShowCount by SettingsManager.getPlayerTooltipShowCount(context).collectAsState(initial = 0)

        var isChatVisible by remember { mutableStateOf(true) }
        var showNativeControls by remember { mutableStateOf(false) }
        var controlsInteractionTrigger by remember { mutableIntStateOf(0) }

        // Force show controls when buffering starts (but not on initial stream load)
        LaunchedEffect(isBuffering) {
            if (isBuffering && !isUiLoading) {
                showNativeControls = true
                if (isFullscreen) showFullscreenControls = true
            }
        }

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

        // Toggle chat off by default when entering fullscreen
        LaunchedEffect(isFullscreen) {
            if (isFullscreen) {
                isChatVisible = false
                if (tooltipShowCount < 2) {
                    delay(1.seconds)
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
        }

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

        DisposableEffect(channel) {
            onDispose {
                Log.d("TwitchPlayer", "Disposing player for channel: $channel")
                playerViewModel.disconnectMediaController()
                chatViewModel.disconnect()
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

        // Stable content that won't be recreated when moving in the tree
        val playerContent = remember(channel) {
            movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
                Box(modifier = modifier) {
                    val liveMetadata = playerViewModel.streamMetadata
                    val liveAvatarUrl = playerViewModel.avatarUrl
                    val liveSubtitle = playerViewModel.streamSubtitle
                    val liveIsPlaying = playerViewModel.isPlaying
                    
                    // Mode logic
                    val isAudioOnlyMode = playerViewModel.isAudioOnly
                    val isChatOnlyMode = playerViewModel.portraitMode == PortraitMode.CHAT_ONLY
                    val previewImageUrl = liveMetadata?.user?.stream?.previewImageUrl

                    // 1. BASE LAYER: Native Video Player
                    if (playerViewModel.mediaController != null) {
                        NativePlayer(
                            player = playerViewModel.mediaController,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 2. LOADING LAYER
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

                    // 3. OVERLAY LAYER (Audio Only or Chat Only)
                    if (isMinimized && (isAudioOnlyMode || isChatOnlyMode)) {
                        MiniPlayerOverlay(
                            channel = channel,
                            avatarUrl = liveAvatarUrl,
                            previewImageUrl = previewImageUrl,
                            badgeText = if (isChatOnlyMode) "CHAT ONLY" else "AUDIO ONLY",
                            usePreview = isChatOnlyMode,
                            showLoading = isUiLoading && !isAudioOnlyMode
                        )
                    } else if (isAudioOnlyMode || isChatOnlyMode) {
                        AudioOnlyPlayer(
                            channel = channel,
                            avatarUrl = liveAvatarUrl,
                            subtitle = liveSubtitle,
                            displayName = liveMetadata?.user?.displayName,
                            streamTitle = liveMetadata?.user?.stream?.title,
                            gameName = liveMetadata?.user?.stream?.game?.name,
                            viewersCount = liveMetadata?.user?.stream?.viewersCount ?: 0,
                            isPlaying = liveIsPlaying,
                            onTogglePlayback = { playerViewModel.togglePlayback() },
                            onCloseAudioOnly = { playerViewModel.toggleAudioOnly() },
                            onRefresh = { 
                                playerViewModel.updateMediaItem(channel, force = true)
                                playerViewModel.updateChannel(channel, forceRefresh = true)
                            },
                            previewImageUrl = previewImageUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!isMinimized && !isPip) {
                        // 4. VIDEO CONTROLS LAYER
                        NativePlayerControls(
                            isVisible = showNativeControls,
                            isPlaying = liveIsPlaying,
                            isBuffering = isBuffering,
                            isAudioOnly = isAudioOnlyMode,
                            isFullscreen = isFullscreen,
                            onTogglePlayback = { 
                                controlsInteractionTrigger++
                                playerViewModel.togglePlayback() 
                            },
                            onToggleFullscreen = {
                                controlsInteractionTrigger++
                                onToggleFullscreen()
                            },
                            onToggleChat = {
                                controlsInteractionTrigger++
                                onToggleChat()
                            },
                            onToggleAudioOnly = { 
                                controlsInteractionTrigger++
                                playerViewModel.toggleAudioOnly() 
                            },
                            onRefresh = { 
                                controlsInteractionTrigger++
                                playerViewModel.updateMediaItem(channel, force = true)
                                playerViewModel.updateChannel(channel, forceRefresh = true)
                            },
                            onSettingsClick = { 
                                controlsInteractionTrigger++
                                showQualityMenu = true 
                            },
                            isSettingsEnabled = playerViewModel.availableQualities.isNotEmpty() && !playerViewModel.isAdActive,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Update loading state
                    LaunchedEffect(liveIsPlaying, playerViewModel.isHoldingLoader) {
                        if (liveIsPlaying && !playerViewModel.isHoldingLoader) {
                            isUiLoading = false
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
        LaunchedEffect(showFullscreenControls, isFullscreen, isBuffering, controlsInteractionTrigger) {
            if (isFullscreen && showFullscreenControls && !isBuffering) {
                delay(2.seconds)
                showFullscreenControls = false
                showNativeControls = false
            }
        }

        // Auto-hide native controls in portrait
        LaunchedEffect(showNativeControls, isFullscreen, isBuffering, controlsInteractionTrigger) {
            if (!isFullscreen && showNativeControls && !isBuffering) {
                delay(2.seconds)
                showNativeControls = false
            }
        }

        val bannerText = when {
            isAudioOnly -> stringResource(R.string.status_audio_only)
            portraitMode == PortraitMode.CHAT_ONLY -> stringResource(R.string.status_chat_only)
            else -> playerViewModel.adblockMessage
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
                        Log.d("TwitchPlayer", "Miniplayer dismissed by swipe. Stopping playback.")
                        playerViewModel.stopAndDisconnect()
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
                        blurRadius = 150.dp,
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

                // 2. MINI PLAYER SHELL & DISMISS LOGIC
                // Rendered below the stable player so the shared video appears on top of the shell's placeholder
                MiniPlayerContainer(
                    visible = isMinimized,
                    channel = channel,
                    displayName = streamMetadata?.user?.displayName,
                    streamTitle = streamMetadata?.user?.stream?.title,
                    elevation = layout.elevation.value,
                    nudgeOffset = nudgeOffset.value,
                    dismissState = dismissState,
                    onExpand = onExpand,
                    onClose = {
                        Log.d("TwitchPlayer", "Miniplayer closed by button. Stopping playback.")
                        playerViewModel.stopAndDisconnect()
                        onClose()
                    },
                    content = {
                        // This placeholder box will be filled by Point 3 (the shared player)
                    }
                )

                // 3. THE STABLE PLAYER (Middle Layer)
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
                                .clip(RoundedCornerShape(layout.cornerRadius.value.let { if (it.isSpecified) it.coerceAtLeast(0.dp) else 0.dp }))
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
                                    Log.d("TwitchPlayer", "Single tap detected. isMinimized: $isMinimized")
                                    if (isMinimized) {
                                        onExpand()
                                    } else if (isFullscreen && !isAudioOnly) {
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
                            val bgBlur = if (isImmersiveEnabled) 150.dp else 0.dp
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
                                    "Toggle chat requested. isFullscreen: $isFullscreen"
                                )
                                if (isFullscreen) {
                                    isChatVisible = !isChatVisible
                                    showFullscreenControls = true
                                } else {
                                    // Cycle modes in portrait
                                    if (portraitMode == PortraitMode.CHAT_ONLY) {
                                        portraitMode = PortraitMode.VIDEO_AND_CHAT
                                        isAudioOnly = false
                                        // Refresh stream when returning to video mode
                                        playerViewModel.updateMediaItem(channel, force = true)
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

                // 4. FULL PLAYER OVERLAY (Chat, Metadata) - Top-most layer
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
                                // Refresh stream when returning to video mode
                                playerViewModel.updateMediaItem(channel, force = true)
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
