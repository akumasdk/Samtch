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
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.ui.MainViewModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var backgroundController: MediaController? = null
    private lateinit var orientationManager: DeviceOrientationManager

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.Actions.REFRESH -> viewModel.incrementRefreshTrigger()
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
        splashScreen.setKeepOnScreenCondition { !viewModel.isAppLoaded }

        orientationManager = DeviceOrientationManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            ScriptLoader.initialize(this@MainActivity)
            
            val lastVersion = SettingsManager.getLastVersionCode(this@MainActivity).first()
            if ((lastVersion != -1) && (lastVersion != BuildConfig.VERSION_CODE)) {
                SettingsManager.clear(this@MainActivity)
            }
            SettingsManager.setLastVersionCode(this@MainActivity, BuildConfig.VERSION_CODE)
            
            // Warm up Twitch GQL cache
            TwitchGqlService.getPlaybackAccessToken("twitch")
        }

        handleIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
            
            key(BuildConfig.VERSION_CODE) {
                SamtchTheme(darkTheme = darkTheme) {
                    val playerViewModel: PlayerViewModel = viewModel()
                    val isPipEnabled by SettingsManager.isPipEnabled(this@MainActivity).collectAsState(initial = true)

                    LaunchedEffect(darkTheme, viewModel.selectedChannel, viewModel.isMinimized, viewModel.isSettingsOpen, viewModel.isInPipMode) {
                        // Sync status bar icons with the theme transition midpoint
                        if (darkTheme != (viewModel.lastDarkTheme ?: darkTheme)) {
                            delay(200) // Adjusted for 400ms total transition
                        }
                        viewModel.lastDarkTheme = darkTheme

                        val statusBarStyle = if (darkTheme) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        }

                        val navigationBarStyle = if (darkTheme) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        }

                        enableEdgeToEdge(
                            statusBarStyle = statusBarStyle,
                            navigationBarStyle = navigationBarStyle,
                        )
                    }

                    val physicalOrientation by orientationManager.orientation.collectAsState()
                    val isAutoRotateEnabled by SystemSettingsUtil.observeAutoRotate(this@MainActivity).collectAsState(initial = false)
                    
                    val browserBottomPadding by animateDpAsState(
                        targetValue = if (viewModel.isMinimized && viewModel.selectedChannel != null) 104.dp else 0.dp,
                        animationSpec = SamtchAnimation.DpSpring,
                        label = "BrowserPaddingAnimation"
                    )

                    var isFullscreen by rememberSaveable { 
                        mutableStateOf(orientationManager.orientation.value == PhysicalOrientation.LANDSCAPE) 
                    }

                    LaunchedEffect(physicalOrientation, isAutoRotateEnabled, viewModel.selectedChannel, viewModel.isMinimized) {
                        if (isAutoRotateEnabled && viewModel.selectedChannel != null && !viewModel.isMinimized && !viewModel.isAudioOnlyMode) {
                            when (physicalOrientation) {
                                PhysicalOrientation.LANDSCAPE -> isFullscreen = true
                                PhysicalOrientation.PORTRAIT -> isFullscreen = false
                                else -> {}
                            }
                        }
                    }

                    LaunchedEffect(viewModel.selectedChannel, viewModel.isMinimized, viewModel.isAudioOnlyMode, isFullscreen, viewModel.isInPipMode, playerViewModel.portraitMode) {
                        if (viewModel.isInPipMode) return@LaunchedEffect

                        val useImmersiveMode = isFullscreen && 
                                               viewModel.selectedChannel != null && 
                                               !viewModel.isMinimized && 
                                               !viewModel.isAudioOnlyMode && 
                                               playerViewModel.portraitMode != PortraitMode.CHAT_ONLY

                        if (useImmersiveMode) {
                            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        }
                        requestedOrientation = if (useImmersiveMode) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }

                    val browserState = rememberSaveableWebViewState(Constants.Twitch.MOBILE_URL)
                    val browserNavigator = rememberWebViewNavigator()

                    val loginLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            Log.d("MainActivity", "Login successful, triggering hard refresh.")
                            viewModel.incrementRefreshTrigger()
                        }
                    }

                    LaunchedEffect(intent) {
                        val newChannel = viewModel.handleIntent(intent)
                        if (newChannel != null) {
                            playerViewModel.updateChannel(newChannel)
                        } else if (intent.getStringExtra(Constants.Extras.ACTION) == Constants.Actions.STOP) {
                            playerViewModel.updateChannel(null)
                        }
                    }

                    LaunchedEffect(viewModel.pipRect) {
                        if ((viewModel.pipRect != null) && (viewModel.selectedChannel != null)) {
                            val builder = PictureInPictureParams.Builder()
                            viewModel.pipRect?.let { builder.setSourceRectHint(it) }
                            try {
                                setPictureInPictureParams(builder.build())
                            } catch (_: Exception) {}
                        }
                    }

                    LaunchedEffect(viewModel.selectedChannel, viewModel.isInPipMode, isPipEnabled, viewModel.isMinimized, viewModel.isSettingsOpen) {
                        updatePipParams(isPipEnabled)
                    }

                    var displayedChannel by remember { mutableStateOf<String?>(null) }
                    if (viewModel.selectedChannel != null) {
                        displayedChannel = viewModel.selectedChannel
                    }

                    val isPlayerActive = remember(viewModel.selectedChannel, viewModel.isMinimized) {
                        viewModel.selectedChannel != null && !viewModel.isMinimized
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
                                refreshTrigger = viewModel.refreshTrigger,
                                hasBackgroundReloaded = playerViewModel.hasBackgroundReloaded,
                                onBackgroundReloadFinished = { playerViewModel.hasBackgroundReloaded = true },
                                onChannelSelected = { channel ->
                                    val isSameChannel = viewModel.selectedChannel == channel
                                    viewModel.updateChannel(channel)
                                    if (!isSameChannel) {
                                        playerViewModel.updateChannel(channel)
                                    } else {
                                        playerViewModel.hasBackgroundReloaded = false
                                    }
                                },
                                onSettingsClick = { viewModel.isSettingsOpen = true },
                                onLoginRequested = {
                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    loginLauncher.launch(intent)
                                },
                                onRefreshRequested = {
                                    viewModel.incrementRefreshTrigger()
                                },
                            ) { viewModel.isAppLoaded = true }
                        }

                        AnimatedVisibility(
                            visible = viewModel.selectedChannel != null,
                            enter = SamtchAnimation.PlayerEnterTransition,
                            exit = SamtchAnimation.PlayerExitTransition,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            displayedChannel?.let { channel ->
                                key(channel) {
                                    TwitchPlayer(
                                        channel = channel,
                                        isFullscreen = isFullscreen,
                                        isPip = viewModel.isInPipMode,
                                        isMinimized = viewModel.isMinimized,
                                        refreshTrigger = viewModel.refreshTrigger,
                                        playerViewModel = playerViewModel,
                                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                                        onBack = {
                                            viewModel.isMinimized = true
                                            isFullscreen = false
                                        },
                                        onExpand = { viewModel.isMinimized = false },
                                        onClose = {
                                            viewModel.updateChannel(null)
                                            playerViewModel.updateChannel(null)
                                            val stopIntent = Intent(this@MainActivity, PlaybackService::class.java)
                                            stopService(stopIntent)
                                        },
                                        onMetadataUpdated = { avatar, subtitle ->
                                            viewModel.lastAvatarUrl = avatar
                                            viewModel.lastSubtitle = subtitle
                                        },
                                        onLoginRequested = {
                                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                            loginLauncher.launch(intent)
                                        },
                                        onSettingsClick = { viewModel.isSettingsOpen = true },
                                        onAudioOnlyModeChanged = { isAudioOnly ->
                                            viewModel.isAudioOnlyMode = isAudioOnly
                                            updatePipParams(isPipEnabled)
                                        },
                                        onVideoBoundsChanged = { rect -> viewModel.pipRect = rect }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = viewModel.isSettingsOpen,
                            enter = SamtchAnimation.ScreenEnterTransition,
                            exit = SamtchAnimation.ScreenExitTransition
                        ) {
                            SettingsScreen(
                                onBack = { viewModel.isSettingsOpen = false },
                                onLogout = {
                                    viewModel.updateChannel(null)
                                    playerViewModel.updateChannel(null)
                                    try {
                                        val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply { action = "STOP" }
                                        stopService(stopIntent)
                                    } catch (_: Exception) {}

                                    EmoteRepository.clearCache()
                                    com.akumasdk.samtch.data.badge.BadgeRepository.clearCache()

                                    android.webkit.CookieManager.getInstance().removeAllCookies { 
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            Log.d("MainActivity", "Logout: cookies cleared. Clearing OAuth data...")
                                            SettingsManager.setAuthData(
                                                context = this@MainActivity,
                                                token = null,
                                                clientId = null,
                                                userName = null,
                                                userId = null,
                                                isLoggedIn = false
                                            )
                                            Log.d("MainActivity", "OAuth data cleared. Triggering refresh.")
                                            viewModel.incrementRefreshTrigger()
                                            viewModel.isSettingsOpen = false
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
            currentChannel = viewModel.selectedChannel,
            isAudioOnly = viewModel.isAudioOnlyMode,
            isInPipMode = viewModel.isInPipMode
        )
        try {
            setPictureInPictureParams(params)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val audioOnlyBackgroundEnabled = SettingsManager.isAudioOnlyBackgroundEnabled(applicationContext).first()
            val isAudioOnlyPlayerActive = viewModel.isAudioOnlyMode
            
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
                
                if (viewModel.selectedChannel != null && !viewModel.wasInPip) {
                    viewModel.incrementRefreshTrigger()
                }
                viewModel.wasInPip = false
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
            val isAudioOnlyPlayerActive = viewModel.isAudioOnlyMode
            if (viewModel.selectedChannel != null && !viewModel.isInPipMode && audioOnlyEnabled) {
                if (!isAudioOnlyPlayerActive) {
                    val sessionToken = SessionToken(this@MainActivity, ComponentName(this@MainActivity, PlaybackService::class.java))
                    val controllerFuture = MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
                    controllerFuture.addListener({
                        val controller = controllerFuture.get()
                        backgroundController = controller
                        val metadata = MediaMetadata.Builder()
                            .setTitle(viewModel.selectedChannel)
                            .setArtist(viewModel.lastSubtitle)
                            .setArtworkUri(viewModel.lastAvatarUrl?.toUri())
                            .build()
                        controller.setMediaItem(MediaItem.Builder().setMediaId(viewModel.selectedChannel!!).setMediaMetadata(metadata).build())
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
        viewModel.isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            viewModel.wasInPip = true
            viewModel.isMinimized = false
            viewModel.isSettingsOpen = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val channelFromUrl = viewModel.handleIntent(intent)
        if (channelFromUrl != null) {
            val stopIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(Constants.Extras.CHANNEL, channelFromUrl)
            }
            startActivity(stopIntent)
        }
    }
}
