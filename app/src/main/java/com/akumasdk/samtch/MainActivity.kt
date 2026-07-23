package com.akumasdk.samtch

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.akumasdk.samtch.ui.screens.browser.TwitchBrowser
import com.akumasdk.samtch.ui.screens.player.TwitchPlayer
import com.akumasdk.samtch.ui.screens.settings.SettingsScreen
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.DeviceOrientationManager
import com.akumasdk.samtch.util.PhysicalOrientation
import com.akumasdk.samtch.util.ScriptLoader
import com.akumasdk.samtch.util.SystemSettingsUtil
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isInPipModeState = mutableStateOf(false)
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
                ACTION_REFRESH -> refreshTriggerState.intValue += 1
                ACTION_STOP_PLAYER -> {
                    val stopIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("ACTION", "STOP")
                    }
                    startActivity(stopIntent)
                }
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.akumasdk.samtch.REFRESH"
        private const val ACTION_STOP_PLAYER = "com.akumasdk.samtch.STOP_PLAYER"
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppLoadedState.value }

        ScriptLoader.initialize(this)
        orientationManager = DeviceOrientationManager(this)

        handleIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(ACTION_REFRESH)
            addAction(ACTION_STOP_PLAYER)
        }
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            SamtchTheme {
                var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }
                var isInPipMode by isInPipModeState
                var refreshTrigger by refreshTriggerState
                var isMinimized by isMinimizedState
                var isSettingsOpen by isSettingsOpenState
                val isPipEnabled by SettingsManager.isPipEnabled(this@MainActivity).collectAsState(initial = true)

                val physicalOrientation by orientationManager.orientation.collectAsState()
                val isAutoRotateEnabled by SystemSettingsUtil.observeAutoRotate(this@MainActivity).collectAsState(initial = false)
                
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
                LaunchedEffect(selectedChannel, isMinimized, isAudioOnlyModeState.value, isFullscreen, isInPipMode) {
                    if (isInPipMode) return@LaunchedEffect

                    val isPlayerActive = selectedChannel != null && !isMinimized && !isAudioOnlyModeState.value

                    if (isPlayerActive) {
                        // Hide system bars for an immersive player experience (Portrait and Fullscreen)
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        
                        if (isFullscreen) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    } else {
                        // BROWSER LOCK: Always Portrait
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        isFullscreen = false
                    }

                    // Theme consistency
                    windowInsetsController.isAppearanceLightStatusBars = false
                    windowInsetsController.isAppearanceLightNavigationBars = false
                }

                val browserState = rememberSaveableWebViewState("https://m.twitch.tv/")
                val browserNavigator = rememberWebViewNavigator()

                LaunchedEffect(intent) {
                    val action = intent.getStringExtra("ACTION")
                    val newChannel = intent.getStringExtra("CHANNEL")
                    if (action == "STOP") {
                        selectedChannel = null
                        isMinimized = false
                    } else if (newChannel != null) {
                        selectedChannel = newChannel
                        isMinimized = false
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
                LaunchedEffect(selectedChannel) {
                    if (selectedChannel != null) displayedChannel = selectedChannel
                }

                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = if (isMinimized && selectedChannel != null) 104.dp else 0.dp)
                    ) {
                        TwitchBrowser(
                            state = browserState,
                            navigator = browserNavigator,
                            isVisible = true,
                            onChannelSelected = { channel ->
                                if (selectedChannel != channel) {
                                    selectedChannel = channel
                                    isMinimized = false
                                }
                            },
                            onSettingsClick = { isSettingsOpen = true },
                            onLoaded = { isAppLoadedState.value = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedChannel != null,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)
                        ) + fadeIn(),
                        exit = if (isMinimized) {
                            fadeOut(animationSpec = tween(durationMillis = 200))
                        } else {
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeOut()
                        },
                        modifier = if (isMinimized) {
                            Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp)
                                .wrapContentHeight()
                                .fillMaxWidth()
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        displayedChannel?.let { channel ->
                            key(channel) {
                                TwitchPlayer(
                                    channel = channel,
                                    isFullscreen = isFullscreen,
                                    isPip = isInPipMode,
                                    isMinimized = isMinimized,
                                    refreshTrigger = refreshTrigger,
                                    onToggleFullscreen = { isFullscreen = !isFullscreen },
                                    onBack = {
                                        isMinimized = true
                                        isFullscreen = false
                                    },
                                    onExpand = { isMinimized = false },
                                    onRefreshRequested = { refreshTriggerState.intValue += 1 },
                                    onClose = {
                                        selectedChannel = null
                                        val stopIntent = Intent(this@MainActivity, PlaybackService::class.java)
                                        stopService(stopIntent)
                                    },
                                    onMetadataUpdated = { avatar, subtitle ->
                                        lastAvatarUrl = avatar
                                        lastSubtitle = subtitle
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
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    ) {
                        SettingsScreen(onBack = { isSettingsOpen = false })
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updatePipParams(isPipEnabled: Boolean = true) {
        val actions = if (currentChannel != null && isInPipModeState.value) {
            listOf(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_refresh),
                    getString(R.string.pip_action_refresh),
                    getString(R.string.pip_action_refresh_description),
                    PendingIntent.getBroadcast(
                        this, 0, Intent(ACTION_REFRESH).setPackage(packageName), PendingIntent.FLAG_IMMUTABLE
                    )
                )
            )
        } else {
            emptyList()
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)

        builder.setAutoEnterEnabled(currentChannel != null && isPipEnabled && !isAudioOnlyModeState.value)

        try {
            setPictureInPictureParams(builder.build())
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
                val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply { action = "STOP" }
                startService(stopIntent)
                if (currentChannel != null) refreshTriggerState.intValue += 1
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
                val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply { action = "STOP" }
                startService(stopIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundController?.release()
        backgroundController = null
        val stopIntent = Intent(this, PlaybackService::class.java).apply { action = "STOP" }
        startService(stopIntent)
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
                putExtra("CHANNEL", channelFromUrl)
            }
            startActivity(stopIntent)
        }
    }

    private fun extractChannelFromUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val regex = """(?:www\.|m\.)?twitch\.tv/([^/?]+)""".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}
