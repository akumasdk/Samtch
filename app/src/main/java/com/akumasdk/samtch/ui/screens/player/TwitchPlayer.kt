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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.akumasdk.samtch.ui.MainViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.akumasdk.samtch.R
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
import com.akumasdk.samtch.ui.screens.player.components.PlayerGestureOverlay
import com.akumasdk.samtch.ui.screens.player.components.PlayerLifecycleEffects
import com.akumasdk.samtch.ui.screens.player.components.PlayerOverlay
import com.akumasdk.samtch.ui.screens.player.components.PlayerWebView
import com.akumasdk.samtch.ui.screens.player.components.playerGestureHandler
import com.akumasdk.samtch.ui.screens.player.components.playerInputHandler
import com.akumasdk.samtch.ui.screens.player.components.rememberPlayerLayoutDimensions
import com.akumasdk.samtch.ui.screens.player.components.PlayerLayoutDimensions
import com.akumasdk.samtch.ui.screens.player.models.ChatContentConfig
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.screens.player.util.unloadWebView
import com.akumasdk.samtch.ui.screens.player.viewmodel.PlayerViewModel
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.theme.StreamPreviewInfo
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.compose.material3.SwipeToDismissBoxState

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun TwitchPlayer(
    channel: String = "forsen",
    isFullscreen: Boolean = false,
    isPip: Boolean = false,
    isMinimized: Boolean = false,
    refreshTrigger: Int = 0,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val context = LocalContext.current
        
        var isAudioOnly by playerViewModel::isAudioOnly
        var portraitMode by playerViewModel::portraitMode
        
        val isImmersiveEnabled by mainViewModel.settingsManager.isImmersiveBackgroundEnabled().collectAsState(initial = true)

        val streamMetadata = playerViewModel.streamMetadata
        val avatarUrl = playerViewModel.avatarUrl
        val streamSubtitle = playerViewModel.streamSubtitle

        if (!isMinimized) {
            PlayerBackground(
                channel = channel,
                previewUrl = streamMetadata?.user?.stream?.previewImageUrl,
                refreshKey = playerViewModel.metadataRefreshTrigger,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                alpha = if (SamtchTheme.colors.dialogBackground.luminance() < 0.5f) 0.6f else 0.15f,
                blurRadius = 120.dp,
                containerColor = SamtchTheme.colors.rootBackground
            )
        }

        val defaultLoadingMessage = stringResource(R.string.loading_stream)
        val isAudioOnlyBackgroundEnabled by mainViewModel.settingsManager.isAudioOnlyBackgroundEnabled().collectAsState(initial = false)
        val chatRatioPercent by mainViewModel.settingsManager.getFullscreenChatRatio().collectAsState(initial = 0)
        
        val chatViewModel: ChatViewModel = hiltViewModel()
        val isEmoteMenuVisible by chatViewModel.isEmoteMenuVisible.collectAsState()
        val isInputFocused by chatViewModel.isInputFocused.collectAsState()

        LaunchedEffect(playerViewModel.chatInteractionTrigger, isInputFocused, isEmoteMenuVisible) {
            if (playerViewModel.chatInteractionTrigger > 0 || isInputFocused || isEmoteMenuVisible) {
                playerViewModel.isChatTemporarilyExpanded = true
                if (!isInputFocused && !isEmoteMenuVisible) {
                    delay(5.seconds)
                    playerViewModel.isChatTemporarilyExpanded = false
                    chatViewModel.setEmoteMenuVisible(false)
                }
            } else {
                playerViewModel.isChatTemporarilyExpanded = false
                chatViewModel.setEmoteMenuVisible(false)
            }
        }

        val chatRatioFloat = playerViewModel.getChatRatio(
            chatRatioPercent = chatRatioPercent,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            isFullscreen = isFullscreen
        )

        val isEmoteMenuVisibleInPlace = isEmoteMenuVisible
        val isImeVisible = WindowInsets.isImeVisible
        val forceSlimMetadata = isEmoteMenuVisibleInPlace || isImeVisible

        val scope = rememberCoroutineScope()
        var currentLoadingSession by remember { mutableLongStateOf(0L) }

        val hintShown by mainViewModel.settingsManager.isMiniPlayerHintShown().collectAsState(initial = true)
        val tooltipShowCount by mainViewModel.settingsManager.getPlayerTooltipShowCount().collectAsState(initial = 0)

        var metadataExpandTrigger by remember { mutableIntStateOf(0) }

        val lifecycleOwner = LocalLifecycleOwner.current
        val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
        val chatLoadingText = stringResource(R.string.chat_connecting)
        val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
        val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

        val nudgeOffset = remember { Animatable(0f) }
        LaunchedEffect(isMinimized, hintShown) {
            if (isMinimized && !hintShown) {
                delay(1000.milliseconds)
                nudgeOffset.animateTo(targetValue = 40f, animationSpec = SamtchAnimation.springBouncy())
                nudgeOffset.animateTo(targetValue = 0f, animationSpec = SamtchAnimation.springInteractive())
                mainViewModel.settingsManager.setMiniPlayerHintShown(true)
            }
        }

        LaunchedEffect(isFullscreen) {
            if (isFullscreen) {
                playerViewModel.isChatVisible = false
                delay(1.seconds) 
                playerViewModel.showFullscreenControls = true
                if (tooltipShowCount < 2) {
                    mainViewModel.settingsManager.incrementPlayerTooltipShowCount()
                }
            }
        }

        var lastProcessedRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }
        val shouldUseAudioService = isAudioOnly

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
            isUiLoading = playerViewModel.isUiLoading,
        ) { playerViewModel.isUiLoading = false }

        AudioServiceEffects(
            channel = channel,
            shouldUseAudioService = shouldUseAudioService,
            isAudioOnlyBackgroundEnabled = isAudioOnlyBackgroundEnabled,
            playerViewModel = playerViewModel,
            context = context,
        )

        LaunchedEffect(channel, refreshTrigger) {
            val isManualRefresh = refreshTrigger > lastProcessedRefreshTrigger
            lastProcessedRefreshTrigger = refreshTrigger
            playerViewModel.updateChannel(channel, forceRefresh = isManualRefresh)
        }

        LaunchedEffect(avatarUrl, streamSubtitle) {
            onMetadataUpdated(avatarUrl, streamSubtitle)
        }

        LaunchedEffect(shouldUseAudioService) {
            onAudioOnlyModeChanged(shouldUseAudioService)
        }

        val state = rememberSaveableWebViewState("")
        val navigator = rememberWebViewNavigator()

        LaunchedEffect(playerViewModel.metadataRefreshTrigger) {
            if (playerViewModel.metadataRefreshTrigger > 0) {
                navigator.evaluateJavaScript("if (window.refreshSamtchBackground) { window.refreshSamtchBackground(${playerViewModel.metadataRefreshTrigger}); }")
            }
        }

        if (!isPip && !isMinimized) {
            BackHandler {
                if (isEmoteMenuVisible) {
                    chatViewModel.setEmoteMenuVisible(false)
                } else if (isFullscreen) {
                    onToggleFullscreen()
                } else {
                    onBack?.invoke()
                }
            }
        }

        val isVideoRequired = playerViewModel.isVideoRequired(isFullscreen)

        LaunchedEffect(channel, refreshTrigger, isVideoRequired) {
            if (!isVideoRequired) {
                currentLoadingSession = System.currentTimeMillis()
                playerViewModel.isUiLoading = false
                unloadWebView(state, navigator)
                return@LaunchedEffect
            }

            try { state.nativeWebView.apply { onResume() } } catch (_: Exception) {}

            currentLoadingSession = System.currentTimeMillis()
            playerViewModel.isUiLoading = true
            playerViewModel.loadingMessage = defaultLoadingMessage

            val baseUrl = createTwitchPlayerUrl(channel)
            val finalUrl = if (refreshTrigger > 0) "$baseUrl&refresh=$refreshTrigger" else baseUrl
            navigator.loadUrl(finalUrl)
        }

        DisposableEffect(channel) {
            onDispose {
                playerViewModel.disconnectMediaController()
                chatViewModel.disconnect()
                try {
                    state.nativeWebView.apply {
                        unloadWebView(state, navigator)
                        clearCache(true)
                        clearHistory()
                        clearFormData()
                        removeAllViews()
                    }
                } catch (e: Exception) {}
            }
        }

        val chatContent = remember(channel) {
            movableContentOf { config: ChatContentConfig, modifier: Modifier ->
                PlayerChatContent(
                    channel = channel,
                    chatViewModel = chatViewModel,
                    config = config,
                    onLoginRequested = onLoginRequested,
                    onSettingsClick = onSettingsClick,
                    modifier = modifier
                )
            }
        }

        val playerContent = remember(channel) {
            movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
                PlayerVideoContent(
                    channel = channel,
                    playerViewModel = playerViewModel,
                    state = state,
                    navigator = navigator,
                    isMinimized = isMinimized,
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onToggleChat = onToggleChat,
                    onPlaybackStarted = {
                        val session = currentLoadingSession
                        scope.launch {
                            delay(300.milliseconds)
                            if (session == currentLoadingSession) {
                                playerViewModel.isUiLoading = false
                            }
                        }
                    },
                    onVideoBoundsChanged = onVideoBoundsChanged,
                    modifier = modifier
                )
            }
        }

        LaunchedEffect(playerViewModel.showFullscreenControls, isFullscreen) {
            if (isFullscreen && playerViewModel.showFullscreenControls) {
                delay(5.seconds)
                playerViewModel.showFullscreenControls = false
            }
        }

        val bannerText = when {
            isAudioOnly -> stringResource(R.string.status_audio_only)
            portraitMode == PortraitMode.CHAT_ONLY -> stringResource(R.string.status_chat_only)
            else -> playerViewModel.adblockText
        }

        val playerLayout = remember(isMinimized, isPip, isFullscreen) {
            when {
                isPip -> PlayerLayoutType.PIP
                isMinimized -> PlayerLayoutType.MINIMIZED
                isFullscreen -> PlayerLayoutType.FULLSCREEN
                else -> PlayerLayoutType.PORTRAIT
            }
        }

        TwitchPlayerOrchestrator(
            layoutType = playerLayout,
            channel = channel,
            playerViewModel = playerViewModel,
            playerContent = playerContent,
            chatContent = chatContent,
            nudgeOffset = nudgeOffset.value,
            onToggleFullscreen = onToggleFullscreen,
            onClose = onClose,
            onExpand = onExpand,
            refreshTrigger = refreshTrigger,
            chatRatioFloat = chatRatioFloat,
            isImmersiveEnabled = isImmersiveEnabled,
            forceSlimMetadata = forceSlimMetadata,
            metadataExpandTrigger = metadataExpandTrigger,
            onMetadataExpandTriggered = { metadataExpandTrigger++ },
            tooltipShowCount = tooltipShowCount,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bannerText = bannerText
        )
    }
}

enum class PlayerLayoutType { PORTRAIT, FULLSCREEN, MINIMIZED, PIP }

@Composable
private fun TwitchPlayerOrchestrator(
    layoutType: PlayerLayoutType,
    channel: String,
    playerViewModel: PlayerViewModel,
    playerContent: @Composable (Modifier, () -> Unit) -> Unit,
    chatContent: @Composable (ChatContentConfig, Modifier) -> Unit,
    nudgeOffset: Float,
    onToggleFullscreen: () -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
    refreshTrigger: Int,
    chatRatioFloat: Float,
    isImmersiveEnabled: Boolean,
    forceSlimMetadata: Boolean,
    metadataExpandTrigger: Int,
    onMetadataExpandTriggered: () -> Unit,
    tooltipShowCount: Int,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    bannerText: String
) {
    val layout = rememberPlayerLayoutDimensions(
        isMinimized = layoutType == PlayerLayoutType.MINIMIZED,
        isAudioOnly = playerViewModel.isAudioOnly,
        isFullscreen = layoutType == PlayerLayoutType.FULLSCREEN,
        portraitMode = playerViewModel.portraitMode,
        isPip = layoutType == PlayerLayoutType.PIP,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        isChatVisible = playerViewModel.isChatVisible,
        chatRatio = chatRatioFloat
    )

    SharedTransitionLayout {
        var stablePlayerSize by remember { mutableStateOf(IntSize.Zero) }

        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalStreamPreview provides StreamPreviewInfo(
                    channel = channel,
                    previewUrl = playerViewModel.streamMetadata?.user?.stream?.previewImageUrl,
                    refreshKey = playerViewModel.metadataRefreshTrigger
                )
            ) {
                PlayerOverlay(
                    isMinimized = layoutType == PlayerLayoutType.MINIMIZED,
                    isFullscreen = layoutType == PlayerLayoutType.FULLSCREEN,
                    channel = channel,
                    streamMetadata = playerViewModel.streamMetadata,
                    avatarUrl = playerViewModel.avatarUrl,
                    isAudioOnly = playerViewModel.isAudioOnly,
                    adblockText = bannerText,
                    portraitMode = playerViewModel.portraitMode,
                    metadataExpandTrigger = metadataExpandTrigger,
                    isPip = layoutType == PlayerLayoutType.PIP,
                    isChatVisible = playerViewModel.isChatVisible,
                    refreshTrigger = refreshTrigger,
                    chatRatio = chatRatioFloat,
                    forceSlimMetadata = forceSlimMetadata,
                    isImmersiveEnabled = isImmersiveEnabled,
                    onToggleChat = {
                        playerViewModel.toggleChat()
                        if (layoutType == PlayerLayoutType.FULLSCREEN) playerViewModel.showFullscreenControls = true
                    },
                    onToggleMode = {
                        if (playerViewModel.portraitMode == PortraitMode.CHAT_ONLY) {
                            playerViewModel.portraitMode = PortraitMode.VIDEO_AND_CHAT
                            playerViewModel.isAudioOnly = false
                        } else {
                            playerViewModel.portraitMode = PortraitMode.CHAT_ONLY
                        }
                        playerViewModel.isChatVisible = true
                    },
                    onChatInteraction = { if (layoutType == PlayerLayoutType.FULLSCREEN) playerViewModel.onChatInteraction() },
                    chatContent = { config, modifier -> chatContent(config, modifier) }
                )
            }

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

            if (layoutType == PlayerLayoutType.MINIMIZED) {
                MiniPlayerContainer(
                    visible = true,
                    channel = channel,
                    displayName = playerViewModel.streamMetadata?.user?.displayName,
                    streamTitle = playerViewModel.streamMetadata?.user?.stream?.title,
                    elevation = layout.elevation.value,
                    nudgeOffset = nudgeOffset,
                    dismissState = dismissState,
                    onExpand = onExpand,
                    onClose = onClose,
                    content = {}
                )
            }

            StablePlayerShell(
                layoutType = layoutType,
                channel = channel,
                playerViewModel = playerViewModel,
                layout = layout,
                nudgeOffset = nudgeOffset,
                dismissState = dismissState,
                playerContent = playerContent,
                chatContent = chatContent,
                onToggleFullscreen = onToggleFullscreen,
                onMetadataExpandTriggered = onMetadataExpandTriggered,
                isImmersiveEnabled = isImmersiveEnabled,
                tooltipShowCount = tooltipShowCount,
                refreshTrigger = refreshTrigger,
                onSizeChanged = { stablePlayerSize = it },
                stablePlayerSize = stablePlayerSize
            )
        }
    }
}

@Composable
private fun BoxScope.StablePlayerShell(
    layoutType: PlayerLayoutType,
    channel: String,
    playerViewModel: PlayerViewModel,
    layout: PlayerLayoutDimensions,
    nudgeOffset: Float,
    dismissState: SwipeToDismissBoxState,
    playerContent: @Composable (Modifier, () -> Unit) -> Unit,
    chatContent: @Composable (ChatContentConfig, Modifier) -> Unit,
    onToggleFullscreen: () -> Unit,
    onMetadataExpandTriggered: () -> Unit,
    isImmersiveEnabled: Boolean,
    tooltipShowCount: Int,
    refreshTrigger: Int,
    onSizeChanged: (IntSize) -> Unit,
    stablePlayerSize: IntSize
) {
    val viewConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current
    val isFullscreen = layoutType == PlayerLayoutType.FULLSCREEN
    val isMinimized = layoutType == PlayerLayoutType.MINIMIZED
    val isPip = layoutType == PlayerLayoutType.PIP

    var isDraggingVolume by remember { mutableStateOf(false) }
    var volumeProgress by remember { mutableFloatStateOf(0f) }
    var isDraggingBrightness by remember { mutableStateOf(false) }
    var brightnessProgress by remember { mutableFloatStateOf(0.5f) }

    key(channel) {
        Box(
            modifier = when (layoutType) {
                PlayerLayoutType.PIP -> Modifier.fillMaxSize()
                PlayerLayoutType.MINIMIZED -> Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(bottom = layout.paddingBottom.value.coerceAtLeast(0.dp))
                    .padding(start = layout.paddingStart.value.coerceAtLeast(0.dp))
                    .offset { IntOffset(nudgeOffset.roundToInt(), 0) }
                    .offset { IntOffset(dismissState.requireOffset().roundToInt(), 0) }
                    .size(layout.width.value.coerceAtLeast(1.dp), layout.height.value.coerceAtLeast(1.dp))
                    .clip(RoundedCornerShape(layout.cornerRadius.value.coerceAtLeast(0.dp)))
                PlayerLayoutType.FULLSCREEN -> if (!playerViewModel.isAudioOnly) {
                    Modifier
                        .align(Alignment.TopStart)
                        .width(layout.width.value.coerceAtLeast(1.dp))
                        .fillMaxHeight()
                        .clip(RectangleShape)
                } else {
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(layout.height.value.coerceAtLeast(1.dp))
                        .clip(RectangleShape)
                }
                PlayerLayoutType.PORTRAIT -> Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(layout.height.value.coerceAtLeast(1.dp))
                    .clip(RectangleShape)
            }
                .onSizeChanged(onSizeChanged)
                .playerGestureHandler(
                    isFullscreen = isFullscreen && !playerViewModel.isAudioOnly,
                    onBrightnessChange = { brightnessProgress = it },
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
                        if (isFullscreen && !playerViewModel.isAudioOnly) playerViewModel.toggleChat() else onToggleFullscreen()
                    },
                    onSingleTap = {
                        if (isFullscreen && !playerViewModel.isAudioOnly) {
                            playerViewModel.toggleFullscreenControls()
                        } else {
                            onMetadataExpandTriggered()
                            if (playerViewModel.portraitMode == PortraitMode.CHAT_ONLY) {
                                playerViewModel.portraitMode = PortraitMode.VIDEO_AND_CHAT
                            }
                        }
                    }
                )
        ) {
            if (isPip && playerViewModel.portraitMode == PortraitMode.CHAT_ONLY) {
                val surfaceAlpha = if (isImmersiveEnabled) {
                    if (SamtchTheme.colors.dialogBackground.luminance() > 0.5f) 0.94f else 0.82f
                } else 1.0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SamtchTheme.colors.chatBackground.copy(alpha = surfaceAlpha))
                ) {
                    chatContent(
                        ChatContentConfig(isCompact = true, showInput = false, refreshTrigger = refreshTrigger, isFullscreen = isFullscreen),
                        Modifier.fillMaxSize()
                    )
                }
            } else {
                playerContent(Modifier.fillMaxSize()) {
                    if (isFullscreen) {
                        playerViewModel.toggleChat()
                        playerViewModel.showFullscreenControls = true
                    } else {
                        if (playerViewModel.portraitMode == PortraitMode.CHAT_ONLY) {
                            playerViewModel.portraitMode = PortraitMode.VIDEO_AND_CHAT
                            playerViewModel.isAudioOnly = false
                        } else {
                            playerViewModel.portraitMode = PortraitMode.CHAT_ONLY
                        }
                        playerViewModel.isChatVisible = true
                    }
                }
            }

            if (!isMinimized && !isPip) {
                if (isFullscreen && !playerViewModel.isAudioOnly) {
                    PlayerGestureOverlay(
                        isDraggingVolume = isDraggingVolume,
                        isDraggingBrightness = isDraggingBrightness,
                        volumeProgress = volumeProgress,
                        brightnessProgress = brightnessProgress
                    )

                    TapTooltip(
                        visible = playerViewModel.showFullscreenControls && tooltipShowCount < 2,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    FullscreenChatToggle(
                        visible = playerViewModel.showFullscreenControls,
                        isChatVisible = playerViewModel.isChatVisible,
                        onClick = {
                            playerViewModel.toggleChat()
                            playerViewModel.showFullscreenControls = true
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerVideoContent(
    channel: String,
    playerViewModel: PlayerViewModel,
    state: WebViewState,
    navigator: WebViewNavigator,
    isMinimized: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit,
    onPlaybackStarted: () -> Unit,
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val liveMetadata = playerViewModel.streamMetadata
        val liveAvatarUrl = playerViewModel.avatarUrl
        val liveSubtitle = playerViewModel.streamSubtitle
        val liveIsPlaying = playerViewModel.isPlaying

        val isAudioOrChatMode = playerViewModel.isAudioOnly || playerViewModel.portraitMode == PortraitMode.CHAT_ONLY
        val previewImageUrl = liveMetadata?.user?.stream?.previewImageUrl

        if (isMinimized && isAudioOrChatMode) {
            MiniPlayerOverlay(
                channel = channel,
                avatarUrl = liveAvatarUrl,
                previewImageUrl = previewImageUrl,
                badgeText = if (playerViewModel.portraitMode == PortraitMode.CHAT_ONLY) "CHAT ONLY" else "AUDIO ONLY",
                usePreview = playerViewModel.portraitMode == PortraitMode.CHAT_ONLY,
                showLoading = playerViewModel.isUiLoading && !playerViewModel.isAudioOnly
            )
        } else if (isAudioOrChatMode) {
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
                onCloseAudioOnly = {
                    playerViewModel.isAudioOnly = false
                    playerViewModel.portraitMode = PortraitMode.VIDEO_AND_CHAT
                    playerViewModel.disconnectMediaController()
                },
                onRefresh = { playerViewModel.updateMediaItem(channel) },
                previewImageUrl = previewImageUrl,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlayerBackground(
                channel = channel,
                previewUrl = previewImageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            ) {
                PlayerWebView(
                    state = state,
                    navigator = navigator,
                    channel = channel,
                    isMinimized = isMinimized,
                    onToggleFullscreen = onToggleFullscreen,
                    onToggleChat = onToggleChat,
                    onToggleAudioOnly = {
                        if (isFullscreen) onToggleFullscreen()
                        playerViewModel.isAudioOnly = true
                        playerViewModel.portraitMode = PortraitMode.AUDIO_AND_CHAT
                    },
                    onPlaybackStarted = onPlaybackStarted,
                    onLoadingStatus = { playerViewModel.loadingMessage = it },
                    onAdblocked = { text ->
                        playerViewModel.adblockText = text
                        if (text.isNotEmpty() && playerViewModel.isUiLoading) playerViewModel.isUiLoading = false
                    },
                    onVideoBoundsChanged = onVideoBoundsChanged
                )

                AnimatedVisibility(
                    visible = playerViewModel.isUiLoading,
                    enter = fadeIn(animationSpec = SamtchAnimation.StandardTween),
                    exit = fadeOut(animationSpec = SamtchAnimation.StandardTween),
                    modifier = Modifier.matchParentSize()
                ) {
                    PlayerLoadingScreen(
                        channel = channel,
                        previewUrl = previewImageUrl,
                        loadingMessage = playerViewModel.loadingMessage,
                        refreshKey = playerViewModel.metadataRefreshTrigger
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerChatContent(
    channel: String,
    chatViewModel: ChatViewModel,
    config: ChatContentConfig,
    onLoginRequested: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TwitchChat(
        channel = channel,
        isCompact = config.isCompact,
        showInput = config.showInput,
        refreshTrigger = config.refreshTrigger,
        viewModel = chatViewModel,
        portraitMode = config.portraitMode,
        onToggleMode = config.onToggleMode,
        onInteraction = config.onInteraction ?: {},
        onLoginRequested = onLoginRequested,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}
