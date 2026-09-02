package com.akumasdk.samtch.ui

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.service.PlaybackService
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.screens.browser.TwitchBrowser
import com.akumasdk.samtch.ui.screens.login.LoginActivity
import com.akumasdk.samtch.ui.screens.player.TwitchPlayer
import com.akumasdk.samtch.ui.screens.player.viewmodel.PlayerViewModel
import com.akumasdk.samtch.ui.screens.settings.SettingsScreen
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.DeviceOrientationManager
import com.akumasdk.samtch.util.PhysicalOrientation
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    orientationManager: DeviceOrientationManager,
    darkTheme: Boolean,
    isAutoRotateEnabled: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    
    val browserBottomPadding by animateDpAsState(
        targetValue = if (mainViewModel.isMinimized && mainViewModel.selectedChannel != null) 104.dp else 0.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "BrowserPaddingAnimation"
    )

    var isFullscreen by rememberSaveable { 
        mutableStateOf(orientationManager.orientation.value == PhysicalOrientation.LANDSCAPE) 
    }

    val physicalOrientation by orientationManager.orientation.collectAsState()
    
    LaunchedEffect(physicalOrientation, isAutoRotateEnabled) {
        if (isAutoRotateEnabled && mainViewModel.selectedChannel != null && !mainViewModel.isMinimized && !mainViewModel.isAudioOnlyMode) {
            when (physicalOrientation) {
                PhysicalOrientation.LANDSCAPE -> isFullscreen = true
                PhysicalOrientation.PORTRAIT -> isFullscreen = false
                else -> {}
            }
        }
    }

    LaunchedEffect(isFullscreen) {
        onToggleFullscreen(isFullscreen)
    }

    val browserState = rememberSaveableWebViewState(Constants.Twitch.MOBILE_URL)
    val browserNavigator = rememberWebViewNavigator()

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            mainViewModel.incrementRefreshTrigger()
        }
    }

    var displayedChannel by remember { mutableStateOf<String?>(null) }
    if (mainViewModel.selectedChannel != null) {
        displayedChannel = mainViewModel.selectedChannel
    }

    val isPlayerActive = remember(mainViewModel.selectedChannel, mainViewModel.isMinimized) {
        mainViewModel.selectedChannel != null && !mainViewModel.isMinimized
    }

    Box(modifier = Modifier.fillMaxSize().background(SamtchTheme.colors.rootBackground)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = browserBottomPadding.coerceAtLeast(0.dp))
        ) {
            TwitchBrowser(
                state = browserState,
                navigator = browserNavigator,
                isPlayerActive = isPlayerActive,
                refreshTrigger = mainViewModel.refreshTrigger,
                hasBackgroundReloaded = playerViewModel.hasBackgroundReloaded,
                onBackgroundReloadFinished = { playerViewModel.hasBackgroundReloaded = true },
                onChannelSelected = { channel ->
                    val isSameChannel = mainViewModel.selectedChannel == channel
                    mainViewModel.updateChannel(channel)
                    if (!isSameChannel) {
                        playerViewModel.updateChannel(channel)
                    } else {
                        playerViewModel.hasBackgroundReloaded = false
                    }
                },
                onSettingsClick = { mainViewModel.isSettingsOpen = true },
                onLoginRequested = {
                    val intent = Intent(context, LoginActivity::class.java)
                    loginLauncher.launch(intent)
                },
                onRefreshRequested = {
                    mainViewModel.incrementRefreshTrigger()
                },
            ) { mainViewModel.isAppLoaded = true }
        }

        val dimAlpha: Float by animateFloatAsState(
            targetValue = if (isPlayerActive) 0.5f else 0f,
            animationSpec = tween<Float>(durationMillis = 500, easing = SamtchAnimation.EmphasizedEasing),
            label = "PlayerBackdropDim"
        )
        
        if (dimAlpha > 0f || (mainViewModel.selectedChannel != null && !mainViewModel.isMinimized)) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (mainViewModel.selectedChannel != null && !mainViewModel.isMinimized) {
                    PlayerBackground(
                        channel = mainViewModel.selectedChannel!!,
                        previewUrl = playerViewModel.streamMetadata?.user?.stream?.previewImageUrl,
                        refreshKey = playerViewModel.metadataRefreshTrigger,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                        alpha = if (darkTheme) 0.5f else 0.12f,
                        blurRadius = 100.dp,
                        containerColor = SamtchTheme.colors.rootBackground
                    )
                }
                
                if (dimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAlpha))
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = mainViewModel.selectedChannel != null,
            enter = SamtchAnimation.PlayerEnterTransition,
            exit = SamtchAnimation.PlayerExitTransition,
            modifier = Modifier.fillMaxSize()
        ) {
            displayedChannel?.let { channel ->
                key(channel) {
                    TwitchPlayer(
                        channel = channel,
                        isFullscreen = isFullscreen,
                        isPip = mainViewModel.isInPipMode,
                        isMinimized = mainViewModel.isMinimized,
                        refreshTrigger = mainViewModel.refreshTrigger,
                        playerViewModel = playerViewModel,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        onBack = {
                            mainViewModel.isMinimized = true
                            isFullscreen = false
                        },
                        onExpand = { mainViewModel.isMinimized = false },
                        onClose = {
                            mainViewModel.updateChannel(null)
                            playerViewModel.updateChannel(null)
                            val stopIntent = Intent(context, PlaybackService::class.java)
                            context.stopService(stopIntent)
                        },
                        onMetadataUpdated = { avatar, subtitle ->
                            mainViewModel.lastAvatarUrl = avatar
                            mainViewModel.lastSubtitle = subtitle
                        },
                        onLoginRequested = {
                            val intent = Intent(context, LoginActivity::class.java)
                            loginLauncher.launch(intent)
                        },
                        onSettingsClick = { mainViewModel.isSettingsOpen = true },
                        onAudioOnlyModeChanged = { isAudioOnly ->
                            mainViewModel.isAudioOnlyMode = isAudioOnly
                        },
                        onVideoBoundsChanged = { rect -> mainViewModel.pipRect = rect }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = mainViewModel.isSettingsOpen,
            enter = SamtchAnimation.ScreenEnterTransition,
            exit = SamtchAnimation.ScreenExitTransition
        ) {
            SettingsScreen(
                onBack = { mainViewModel.isSettingsOpen = false },
                onLogout = onLogout
            )
        }
    }
}
