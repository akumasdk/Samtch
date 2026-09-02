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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.akumasdk.samtch.ui.MainScreen
import com.akumasdk.samtch.ui.screens.browser.TwitchBrowser
import com.akumasdk.samtch.ui.screens.login.LoginActivity
import com.akumasdk.samtch.ui.screens.player.TwitchPlayer
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
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

import com.akumasdk.samtch.data.badge.BadgeRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var emoteRepository: EmoteRepository
    @Inject lateinit var badgeRepository: BadgeRepository
    @Inject lateinit var gqlService: TwitchGqlService

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
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !viewModel.isAppLoaded }

        orientationManager = DeviceOrientationManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            ScriptLoader.initialize(this@MainActivity)
            
            val lastVersion = settingsManager.getLastVersionCode().first()
            if ((lastVersion != -1) && (lastVersion != BuildConfig.VERSION_CODE)) {
                settingsManager.clear()
            }
            settingsManager.setLastVersionCode(BuildConfig.VERSION_CODE)
            
            // Warm up Twitch GQL cache
            gqlService.getPlaybackAccessToken("twitch")
        }

        handleIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction(Constants.Actions.REFRESH)
            addAction(Constants.Actions.STOP_PLAYER)
        }
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val themeMode by settingsManager.getThemeMode().collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
            val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            
            val darkTheme = when (themeMode) {
                SettingsManager.ThemeMode.DARK -> true
                SettingsManager.ThemeMode.LIGHT -> false
                SettingsManager.ThemeMode.SYSTEM -> isSystemInDarkTheme
            }
            
            key(BuildConfig.VERSION_CODE) {
                SamtchTheme(darkTheme = darkTheme) {
                    val isPipEnabled by settingsManager.isPipEnabled().collectAsState(initial = true)
                    val isAutoRotateEnabled by SystemSettingsUtil.observeAutoRotate(this@MainActivity).collectAsState(initial = false)

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

                    LaunchedEffect(viewModel.selectedChannel, viewModel.isInPipMode, isPipEnabled, viewModel.isMinimized, viewModel.isSettingsOpen) {
                        updatePipParams(isPipEnabled)
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

                    MainScreen(
                        mainViewModel = viewModel,
                        playerViewModel = playerViewModel,
                        orientationManager = orientationManager,
                        darkTheme = darkTheme,
                        isAutoRotateEnabled = isAutoRotateEnabled,
                        onToggleFullscreen = { isFullscreen: Boolean ->
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
                                if (isAutoRotateEnabled) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        onLogout = {
                            viewModel.updateChannel(null)
                            playerViewModel.updateChannel(null)
                            try {
                                val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply { action = "STOP" }
                                stopService(stopIntent)
                            } catch (_: Exception) {}

                            emoteRepository.clearCache()
                            badgeRepository.clearCache()

                            android.webkit.CookieManager.getInstance().removeAllCookies { 
                                lifecycleScope.launch(Dispatchers.Main) {
                                    settingsManager.setAuthData(
                                        token = null,
                                        clientId = null,
                                        userName = null,
                                        userId = null,
                                        isLoggedIn = false
                                    )
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
            val audioOnlyBackgroundEnabled = settingsManager.isAudioOnlyBackgroundEnabled().first()
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
            val audioOnlyEnabled = settingsManager.isAudioOnlyBackgroundEnabled().first()
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
