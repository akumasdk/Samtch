package com.akumasdk.samtch

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
import android.util.Log
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.akumasdk.samtch.ui.screens.browser.TwitchBrowser
import com.akumasdk.samtch.ui.screens.player.TwitchPlayer
import com.akumasdk.samtch.ui.screens.settings.SettingsScreen
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.service.PlaybackService
import com.akumasdk.samtch.util.ScriptLoader
import com.akumasdk.samtch.data.settings.SettingsManager
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private var isInPipModeState = mutableStateOf(false)
    private var pipRectState = mutableStateOf<Rect?>(null)
    private var refreshTriggerState = mutableIntStateOf(0)
    private var isAppLoadedState = mutableStateOf(false)
    private var isMinimizedState = mutableStateOf(false)
    private var currentChannel: String? = null // For PiP and Service access
    private var isAudioOnlyModeState = mutableStateOf(false)
    private var lastAvatarUrl: String? = null
    private var lastSubtitle: String? = null
    private var backgroundController: MediaController? = null

    private var isSettingsOpenState = mutableStateOf(false)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_REFRESH -> refreshTriggerState.intValue += 1
                ACTION_STOP_PLAYER -> {
                    // This will be handled via intent or a global event if needed
                    // For now, we'll let handleIntent handle the stop if necessary
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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            !isAppLoadedState.value
        }

        // Preload all JS scripts into memory for faster access
        ScriptLoader.initialize(this)

        handleIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(ACTION_REFRESH)
            addAction(ACTION_STOP_PLAYER)
        }
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            SamtchTheme {
                var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }
                var isInPipMode by isInPipModeState
                var refreshTrigger by refreshTriggerState
                val configuration = LocalConfiguration.current
                var isFullscreen by rememberSaveable { 
                    mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 
                }
                var isMinimized by isMinimizedState
                var isSettingsOpen by isSettingsOpenState
                val isPipEnabled by SettingsManager.isPipEnabled(this@MainActivity).collectAsState(initial = true)

                // Handle updates from external intents
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

                val browserState = rememberSaveableWebViewState("https://m.twitch.tv/")
                val browserNavigator = rememberWebViewNavigator()

                // Auto-rotate handling: Always sync isFullscreen with physical orientation
                LaunchedEffect(configuration.orientation) {
                    if (!isMinimized && !isAudioOnlyModeState.value) {
                        isFullscreen = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    }
                }

                // Separately update source rect hint to avoid frequent heavy updates
                LaunchedEffect(pipRectState.value) {
                    if ((pipRectState.value != null) && (selectedChannel != null)) {
                        val builder = PictureInPictureParams.Builder()
                        pipRectState.value?.let { builder.setSourceRectHint(it) }
                        try {
                            setPictureInPictureParams(builder.build())
                        } catch (_: Exception) {
                            // Ignore if called too frequently or in wrong state
                        }
                    }
                }

                // Orientation and System Bars management
                LaunchedEffect(selectedChannel, isFullscreen, isInPipMode, isMinimized) {
                    if (isInPipMode) return@LaunchedEffect

                    if (selectedChannel != null && !isMinimized) {
                        if (isFullscreen) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        }
                    } else {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        
                        // Ensure white icons in browser (since Twitch is dark)
                        windowInsetsController.isAppearanceLightStatusBars = false
                        windowInsetsController.isAppearanceLightNavigationBars = false
                    }
                }

                // Sync with class property for PiP and Background Service
                LaunchedEffect(selectedChannel, isInPipMode, isPipEnabled, isMinimized, isSettingsOpen) {
                    currentChannel = selectedChannel
                    updatePipParams(isPipEnabled)
                }

                // Player state cleanup on exit
                LaunchedEffect(selectedChannel) {
                    if (selectedChannel == null) {
                        isFullscreen = false
                    }
                }

                // Track which channel to show in the player (even during exit animation)
                var displayedChannel by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(selectedChannel) {
                    if (selectedChannel != null) {
                        displayedChannel = selectedChannel
                    }
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
                            isVisible = true, // Always keep browser active to avoid state loss
                            onChannelSelected = { channel ->
                                if (selectedChannel != channel) {
                                    selectedChannel = channel
                                    isMinimized = false
                                }
                            },
                            onSettingsClick = {
                                isSettingsOpen = true
                            },
                            onLoaded = {
                                isAppLoadedState.value = true
                            }
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
                        // Use key(displayedChannel) to ensure the player state is tied to the current channel
                        // even while selectedChannel is null (during exit animation)
                        displayedChannel?.let { channel ->
                            key(channel) {
                                TwitchPlayer(
                                    channel = channel,
                                    isFullscreen = isFullscreen,
                                    isPip = isInPipMode,
                                    isMinimized = isMinimized,
                                    refreshTrigger = refreshTrigger,
                                    onToggleFullscreen = {
                                        if (isFullscreen) {
                                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                            isFullscreen = false
                                        } else {
                                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                            isFullscreen = true
                                        }
                                        // Release orientation lock after a delay to allow sensor to take over again
                                        lifecycleScope.launch {
                                            delay(1000.milliseconds)
                                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        }
                                    },
                                    onBack = {
                                        if (isFullscreen) {
                                            isFullscreen = false
                                        } else {
                                            isMinimized = true
                                            isFullscreen = false
                                        }
                                    },
                                    onExpand = {
                                        isMinimized = false
                                    },
                                    onRefreshRequested = {
                                        refreshTriggerState.intValue += 1
                                    },
                                    onClose = {
                                        selectedChannel = null
                                        // Stop the service explicitly to ensure notification and playback end
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
                                    onVideoBoundsChanged = { rect ->
                                        pipRectState.value = rect
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSettingsOpen,
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    ) {
                        SettingsScreen(
                            onBack = { isSettingsOpen = false }
                        )
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
                        this,
                        0,
                        Intent(ACTION_REFRESH).setPackage(packageName),
                        PendingIntent.FLAG_IMMUTABLE
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
        } catch (_: Exception) {
            // Activity might not be in a state to accept PiP params
        }
    }

    override fun onResume() {
        super.onResume()
        
        lifecycleScope.launch {
            val audioOnlyBackgroundEnabled = SettingsManager.isAudioOnlyBackgroundEnabled(applicationContext).first()
            val isAudioOnlyPlayerActive = isAudioOnlyModeState.value
            
            if (isAudioOnlyPlayerActive && audioOnlyBackgroundEnabled) {
                // If we are manually in Audio Only mode and backgrounding is enabled, 
                // DO NOT stop the service. We want the audio to continue seamlessly.
                Log.d("MainActivity", "Resuming while in Audio Only mode with background enabled. Keeping service alive.")
                backgroundController?.release()
                backgroundController = null
            } else {
                // Otherwise, stop background audio and refresh player to ensure video resumes
                backgroundController?.release()
                backgroundController = null
                
                val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply {
                    action = "STOP"
                }
                startService(stopIntent)
                
                if (currentChannel != null) {
                    refreshTriggerState.intValue += 1
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        
        lifecycleScope.launch {
            val audioOnlyEnabled = SettingsManager.isAudioOnlyBackgroundEnabled(applicationContext).first()
            val isAudioOnlyPlayerActive = isAudioOnlyModeState.value
            
            if (currentChannel != null && !isInPipModeState.value && audioOnlyEnabled) {
                if (isAudioOnlyPlayerActive) {
                    // If the user was already listening in Audio Only mode inside the app,
                    // the PlaybackService is already running and playing.
                    // Reusing the same playback instead of refreshing the stream.
                    Log.d("MainActivity", "Backgrounding from Audio Only mode. Reusing existing session.")
                } else {
                    // Transitioning from Video to Background Audio
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

                        controller.setMediaItem(
                            MediaItem.Builder()
                                .setMediaId(currentChannel!!)
                                .setMediaMetadata(metadata)
                                .build()
                        )
                        controller.prepare()
                        controller.play()
                    }, MoreExecutors.directExecutor())
                }
            } else {
                // If background play is disabled or we're in PiP, ensure the HLS service is stopped
                backgroundController?.release()
                backgroundController = null
                val stopIntent = Intent(this@MainActivity, PlaybackService::class.java).apply {
                    action = "STOP"
                }
                startService(stopIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final cleanup of the playback service when the app is closed
        backgroundController?.release()
        backgroundController = null
        val stopIntent = Intent(this, PlaybackService::class.java).apply {
            action = "STOP"
        }
        startService(stopIntent)
        
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {
            // Already unregistered or not registered
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Auto-enter handles this for Android 12+
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            isMinimizedState.value = false
            isSettingsOpenState.value = false // Ensure settings are closed in PiP
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

        val channel = channelFromUrl
        if (channel != null) {
            val stopIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("CHANNEL", channel)
            }
            startActivity(stopIntent)
        }
    }

    private fun extractChannelFromUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null

        // Extract channel from URLs like:
        // https://twitch.tv/channelname
        // https://www.twitch.tv/channelname
        // https://m.twitch.tv/channelname (mobile)
        val regex = """(?:www\.|m\.)?twitch\.tv/([^/?]+)""".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}
