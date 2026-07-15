package com.akumasdk.samtch

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.akumasdk.samtch.ui.screens.TwitchBrowser
import com.akumasdk.samtch.ui.screens.TwitchPlayer
import com.akumasdk.samtch.util.ScriptLoader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private var selectedChannelState = mutableStateOf<String?>(null)
    private var isInPipModeState = mutableStateOf(false)
    private var pipRectState = mutableStateOf<Rect?>(null)
    private var refreshTriggerState = mutableIntStateOf(0)
    private var isAppLoadedState = mutableStateOf(false)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_REFRESH) {
                refreshTriggerState.intValue += 1
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.akumasdk.samtch.REFRESH"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            !isAppLoadedState.value
        }

        // Preload all JS scripts into memory for faster access
        ScriptLoader.initialize(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(ACTION_REFRESH),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            var selectedChannel by selectedChannelState
            var isInPipMode by isInPipModeState
            var refreshTrigger by refreshTriggerState
            var isFullscreen by remember { mutableStateOf(false) }
            var isPlayerReady by remember { mutableStateOf(false) }

            // Update PiP params when channel or PiP mode changes
            LaunchedEffect(selectedChannel, isInPipMode) {
                updatePipParams()
            }

            // Separately update source rect hint to avoid frequent heavy updates
            LaunchedEffect(pipRectState.value) {
                if (pipRectState.value != null && selectedChannel != null) {
                    val builder = PictureInPictureParams.Builder()
                    pipRectState.value?.let { builder.setSourceRectHint(it) }
                    try {
                        setPictureInPictureParams(builder.build())
                    } catch (_: Exception) {
                        // Ignore if called too frequently or in wrong state
                    }
                }
            }

            // Check if app was opened with a Twitch URL
            val intentUrl = intent?.data?.toString()
            val channelFromIntent = extractChannelFromUrl(intentUrl)

            // If intent has a channel, use it; otherwise start with browser
            if (channelFromIntent != null && selectedChannel == null) {
                selectedChannel = channelFromIntent
            }

            // Change orientation and system bars based on current screen
            LaunchedEffect(selectedChannel, isFullscreen, isInPipMode) {
                if (isInPipMode) return@LaunchedEffect

                if (selectedChannel != null) {
                    if (isFullscreen) {
                        // Fullscreen Landscape
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        // Portrait for default player view
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    }
                    
                    // Only reset player if it wasn't already ready (e.g. channel changed)
                    if (!isPlayerReady) {
                        delay(200.milliseconds)
                        isPlayerReady = true
                        isAppLoadedState.value = true
                    }
                } else {
                    // Browser: force portrait
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    isPlayerReady = false
                    isFullscreen = false // Reset fullscreen state
                }
            }

            if (selectedChannel != null && isPlayerReady) {
                // Use key() to force complete recreation when channel changes
                key(selectedChannel) {
                    TwitchPlayer(
                        channel = selectedChannel!!,
                        isFullscreen = isFullscreen,
                        isPip = isInPipMode,
                        refreshTrigger = refreshTrigger,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        onBack = {
                            if (isFullscreen) {
                                isFullscreen = false
                            } else {
                                // Destroy player by setting channel to null
                                selectedChannel = null
                            }
                        },
                        onVideoBoundsChanged = { rect ->
                            pipRectState.value = rect
                        }
                    )
                }
            } else if (selectedChannel == null) {
                // Show browser to select a channel
                TwitchBrowser(
                    onChannelSelected = { channel ->
                        selectedChannel = channel
                    },
                    onLoaded = {
                        isAppLoadedState.value = true
                    }
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updatePipParams() {

        val actions = if (selectedChannelState.value != null && isInPipModeState.value) {
            listOf(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_refresh),
                    "Refresh",
                    "Refresh player",
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

        builder.setAutoEnterEnabled(selectedChannelState.value != null)

        try {
            setPictureInPictureParams(builder.build())
        } catch (_: Exception) {
            // Activity might not be in a state to accept PiP params
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
