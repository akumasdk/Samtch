package com.akumasdk.samtch

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.service.PlaybackService
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.ui.screens.browser.TwitchBrowser
import com.akumasdk.samtch.ui.screens.login.LoginActivity
import com.akumasdk.samtch.ui.screens.player.TwitchPlayer
import com.akumasdk.samtch.ui.screens.settings.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akumasdk.samtch.ui.screens.player.viewmodel.PlayerViewModel
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.DeviceOrientationManager
import com.akumasdk.samtch.util.PhysicalOrientation
import com.akumasdk.samtch.util.ScriptLoader
import com.akumasdk.samtch.util.SystemSettingsUtil
import com.akumasdk.samtch.util.PipManager
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isInPipModeState = mutableStateOf(false)
    private var wasInPip: Boolean = false
    private var pipRectState = mutableStateOf<Rect?>(null)
    private var refreshTriggerState = mutableIntStateOf(0)
    private var isAppLoadedState = mutableStateOf(false)
    private var isMinimizedState = mutableStateOf(false)
    private var currentChannel: String? = null
    private var isAudioOnlyModeState = mutableStateOf(false)
    private var lastAvatarUrl: String? = null
    private var lastSubtitle: String? = null
    private var backgroundController: MediaController? = null
    private lateinit var orientationManager: DeviceOrientationManager

    private var isSettingsOpenState = mutableStateOf(false)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.Actions.REFRESH -> refreshTriggerState.intValue += 1
                Constants.Actions.STOP_PLAYER -> {
                    val stopIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(Constants.Extras.ACTION, Constants.Actions.STOP)
                    }
                    startActivity(stopIntent)
                }
            }
        }
    }

    companion object {
        // No longer needed here as they are in Constants
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppLoadedState.value }

        ScriptLoader.initialize(this)
        orientationManager = DeviceOrientationManager(this)

        lifecycleScope.launch {
            val lastVersion = SettingsManager.getLastVersionCode(this@MainActivity).first()
            if (lastVersion != -1 && lastVersion != BuildConfig.VERSION_CODE) {
                // Version change detected, clear persistent settings
                SettingsManager.clear(this@MainActivity)
            }
            SettingsManager.setLastVersionCode(this@MainActivity, BuildConfig.VERSION_CODE)
        }

        handleIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            // Warm up Twitch GQL cache (Client ID and Integrity Token)
            TwitchGqlService.getPlaybackAccessToken("twitch")
        }

        val filter = IntentFilter().apply {
            addAction(Constants.Actions.REFRESH)
            addAction(Constants.Actions.STOP_PLAYER)
        }
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val themeMode by SettingsManager.getThemeMode(this@MainActivity).collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
            val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            
            val darkTheme = when (themeMode) {
                SettingsManager.ThemeMode.DARK -> true
                SettingsManager.ThemeMode.LIGHT -> false
                SettingsManager.ThemeMode.SYSTEM -> isSystemInDarkTheme
            }
            
            // Use VERSION_CODE as a key to force-reset all rememberSaveable state after an update
            key(BuildConfig.VERSION_CODE) {
                SamtchTheme(darkTheme = darkTheme) {
                    var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }
                var isInPipMode by isInPipModeState
                var refreshTrigger by refreshTriggerState
                var isMinimized by isMinimizedState
                var isSettingsOpen by isSettingsOpenState
                val isPipEnabled by SettingsManager.isPipEnabled(this@MainActivity).collectAsState(initial = true)

                val physicalOrientation by orientationManager.orientation.collectAsState()
                val isAutoRotateEnabled by SystemSettingsUtil.observeAutoRotate(this@MainActivity).collectAsState(initial = false)
                
                // Animated browser padding for smooth layout transitions
                val playerViewModel: PlayerViewModel = viewModel()

                val browserBottomPadding by animateDpAsState(
                    targetValue = if (isMinimized && selectedChannel != null) 104.dp else 0.dp,
                    animationSpec = SamtchAnimation.DpSpring,
                    label = "BrowserPaddingAnimation"
                )

                // Unified Fullscreen State
                var isFullscreen by rememberSaveable { 
                    mutableStateOf(orientationManager.orientation.value == PhysicalOrientation.LANDSCAPE) 
                }

                // 1. AUTO-ROTATE LOGIC: Sync isFullscreen with physical tilt ONLY when in player mode
                LaunchedEffect(physicalOrientation, isAutoRotateEnabled, selectedChannel, isMinimized) {
                    if (isAutoRotateEnabled && selectedChannel != null && !isMinimized && !isAudioOnlyModeState.value) {
                        when (physicalOrientation) {
                            PhysicalOrientation.LANDSCAPE -> isFullscreen = true
                            PhysicalOrientation.PORTRAIT -> isFullscreen = false
                            else -> {}
                        }
                    }
                }

                // 2. ORIENTATION & UI MODE ENFORCEMENT
                LaunchedEffect(selectedChannel, isMinimized, isAudioOnlyModeState.value, isFullscreen, isInPipMode, playerViewModel.portraitMode) {
                    if (isInPipMode) return@LaunchedEffect

                    // Immersive "Fullscreen" mode (hiding status/nav bars) is now EXCLUSIVELY for the landscape video player.
                    // Portrait video, Audio Only, and Chat Only modes will always show the status bar.
                    val useImmersiveMode = isFullscreen && 
                                           selectedChannel != null && 
                                           !isMinimized && 
                                           !isAudioOnlyModeState.value && 
                                           playerViewModel.portraitMode != PortraitMode.CHAT_ONLY

                    if (useImmersiveMode) {
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }

                // 3. SYSTEM BAR THEME CONSISTENCY
                // This ensures icons follow theme even when bars are re-shown after fullscreen
                LaunchedEffect(darkTheme, isFullscreen, isMinimized) {
                    windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                    windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
                }

                val browserState = rememberSaveableWebViewState(Constants.Twitch.MOBILE_URL)
                val browserNavigator = rememberWebViewNavigator()

                val loginLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        Log.d("MainActivity", "Login successful, triggering hard refresh for browser and player.")
                        refreshTriggerState.intValue += 1
                    }
                }

                LaunchedEffect(intent) {
                    val action = intent.getStringExtra(Constants.Extras.ACTION)
                    val newChannel = intent.getStringExtra(Constants.Extras.CHANNEL)
                    if (action == Constants.Actions.STOP) {
                        selectedChannel = null
                        isMinimized = false
                        playerViewModel.updateChannel(null)
                    } else if (newChannel != null) {
                        selectedChannel = newChannel
                        isMinimized = false
                        playerViewModel.updateChannel(newChannel)
                    }
                }

                LaunchedEffect(pipRectState.value) {
                    if ((pipRectState.value != null) && (selectedChannel != null)) {
                        val builder = PictureInPictureParams.Builder()
                        pipRectState.value?.let { builder.setSourceRectHint(it) }
                        try {
                            setPictureInPictureParams(builder.build())
                        } catch (_: Exception) {}
                    }
                }

                LaunchedEffect(selectedChannel, isInPipMode, isPipEnabled, isMinimized, isSettingsOpen) {
                    currentChannel = selectedChannel
                    updatePipParams(isPipEnabled)
                }

                var displayedChannel by remember { mutableStateOf<String?>(null) }
                if (selectedChannel != null) {
                    displayedChannel = selectedChannel
                }

                val isPlayerActive = remember(selectedChannel, isMinimized) {
                    selectedChannel != null && !isMinimized
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
                            refreshTrigger = refreshTrigger,
                            hasBackgroundReloaded = playerViewModel.hasBackgroundReloaded,
                            onBackgroundReloadFinished = { playerViewModel.hasBackgroundReloaded = true },
                            onChannelSelected = { channel ->
                                val isSameChannel = selectedChannel == channel
                                selectedChannel = channel
                                if (!isSameChannel) {
                                    playerViewModel.updateChannel(channel)
                                } else {
                                    // If same channel is re-selected, reset background reload flag
                                    // to ensure the browser performs a fresh background purge.
                                    playerViewModel.hasBackgroundReloaded = false
                                }
                                isMinimized = false
                            },
                            onSettingsClick = { isSettingsOpen = true },
                            onLoginRequested = {
                                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                loginLauncher.launch(intent)
                            },
                            onRefreshRequested = {
                                refreshTriggerState.intValue += 1
                            },
                            onLoaded = { isAppLoadedState.value = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedChannel != null,
                        enter = SamtchAnimation.PlayerEnterTransition,
                        exit = SamtchAnimation.PlayerExitTransition,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        displayedChannel?.let { channel ->
                            key(channel) {
                                TwitchPlayer(
                                    channel = channel,
                                    isFullscreen = isFullscreen,
                                    isPip = isInPipMode,
                                    isMinimized = isMinimized,
                                    refreshTrigger = refreshTrigger,
                                    playerViewModel = playerViewModel,
                                    onToggleFullscreen = { isFullscreen = !isFullscreen },
                                    onBack = {
                                        isMinimized = true
                                        isFullscreen = false
                                    },
                                    onExpand = { isMinimized = false },
                                    onClose = {
                                        selectedChannel = null
                                        playerViewModel.updateChannel(null)
                                        val stopIntent = Intent(this@MainActivity, PlaybackService::class.java)
                                        stopService(stopIntent)
                                    },
                                    onMetadataUpdated = { avatar, subtitle ->
                                        lastAvatarUrl = avatar
                                        lastSubtitle = subtitle
                                    },
                                    onLoginRequested = {
                                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                        loginLauncher.launch(intent)
                                    },
                                    onAudioOnlyModeChanged = { isAudioOnly ->
                                        isAudioOnlyModeState.value = isAudioOnly
                                        updatePipParams(isPipEnabled)
                                    },
                                    onVideoBoundsChanged = { rect -> pipRectState.value = rect }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSettingsOpen,
                        enter = SamtchAnimation.ScreenEnterTransition,
                        exit = SamtchAnimation.ScreenExitTransition
                    ) {
                        SettingsScreen(
                            onBack = { isSettingsOpen = false },
                            onLogout = {
                                // Clear all cookies and trigger hard refresh
                                android.webkit.CookieManager.getInstance().removeAllCookies { 
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        Log.d("MainActivity", "Logout: cookies cleared, triggering hard refresh for browser and player.")
                                        refreshTriggerState.intValue += 1
                                        isSettingsOpen = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updatePipParams(isPipEnabled: Boolean = true) {
        val params = PipManager.getPipParams(
            context = this,
            isPipEnabled = isPipEnabled,
            currentChannel = currentChannel,
            isAudioOnly = isAudioOnlyModeState.value,
            isInPipMode = isInPipModeState.value
        )
        try {
            setPictureInPictureParams(params)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val audioOnlyBackgroundEnabled = SettingsManager.isAudioOnlyBackgroundEnabled(applicationContext).first()
            val isAudioOnlyPlayerActive = isAudioOnlyModeState.value
            
            if (isAudioOnlyPlayerActive && audioOnlyBackgroundEnabled) {
                backgroundController?.release()
                backgroundController = null
            } else {
                backgroundController?.release()
                backgroundController = null
                try {
                    val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply { action = "STOP" }
                    stopService(stopIntent)
                } catch (_: Exception) {}
                
                if (currentChannel != null && !wasInPip) {
                    refreshTriggerState.intValue += 1
                }
                wasInPip = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        orientationManager.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationManager.disable()
        lifecycleScope.launch {
            val audioOnlyEnabled = SettingsManager.isAudioOnlyBackgroundEnabled(applicationContext).first()
            val isAudioOnlyPlayerActive = isAudioOnlyModeState.value
            if (currentChannel != null && !isInPipModeState.value && audioOnlyEnabled) {
                if (!isAudioOnlyPlayerActive) {
                    val sessionToken = SessionToken(this@MainActivity, ComponentName(this@MainActivity, PlaybackService::class.java))
                    val controllerFuture = MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
                    controllerFuture.addListener({
                        val controller = controllerFuture.get()
                        backgroundController = controller
                        val metadata = MediaMetadata.Builder()
                            .setTitle(currentChannel)
                            .setArtist(lastSubtitle)
                            .setArtworkUri(lastAvatarUrl?.toUri())
                            .build()
                        controller.setMediaItem(MediaItem.Builder().setMediaId(currentChannel!!).setMediaMetadata(metadata).build())
                        controller.prepare()
                        controller.play()
                    }, MoreExecutors.directExecutor())
                }
            } else {
                backgroundController?.release()
                backgroundController = null
                try {
                    val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply { action = "STOP" }
                    stopService(stopIntent)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundController?.release()
        backgroundController = null
        try {
            val stopIntent = Intent(this, PlaybackService::class.java).apply { action = "STOP" }
            stopService(stopIntent)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            wasInPip = true
            isMinimizedState.value = false
            isSettingsOpenState.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val intentUrl = intent?.data?.toString()
        val channelFromUrl = extractChannelFromUrl(intentUrl)
        if (channelFromUrl != null) {
            val stopIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(Constants.Extras.CHANNEL, channelFromUrl)
            }
            startActivity(stopIntent)
        }
    }

    private fun extractChannelFromUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val regex = """(?:www\.|m\.)?${Constants.Twitch.DOMAIN}/([^/?]+)""".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)?.trim()
    }
}
