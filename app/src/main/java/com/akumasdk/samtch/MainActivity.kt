package com.akumasdk.samtch

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.akumasdk.samtch.ui.screens.TvPlayer
import com.akumasdk.samtch.ui.screens.TwitchBrowser
import com.akumasdk.samtch.ui.screens.TwitchChannelSelector
import com.akumasdk.samtch.ui.screens.TwitchPlayer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var selectedChannelState = mutableStateOf<String?>(null)
    private var isInPipModeState = mutableStateOf(false)
    private var pipRectState = mutableStateOf<Rect?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val isTv = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

        if (isTv) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // Always allow content to fit system windows to handle it in Compose (fullscreen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (!isTv) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedChannel by selectedChannelState
                    var isInPipMode by isInPipModeState
                    var isFullscreen by remember { mutableStateOf(false) }
                    var isPlayerReady by remember { mutableStateOf(false) }

                    // Update PiP params when channel or rect changes
                    LaunchedEffect(selectedChannel, pipRectState.value) {
                        if (!isTv) {
                            updatePipParams()
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
                        if (isTv) {
                            isPlayerReady = selectedChannel != null
                            return@LaunchedEffect
                        }

                        if (isInPipMode) return@LaunchedEffect

                        if (selectedChannel != null) {
                            if (isFullscreen) {
                                // Fullscreen Landscape
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                            } else {
                                // Portrait for default player view
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                            }
                            // Small delay to ensure previous player is destroyed
                            isPlayerReady = false
                            delay(200)
                            isPlayerReady = true
                        } else {
                            // Browser: force portrait
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                            isPlayerReady = false
                            isFullscreen = false // Reset fullscreen state
                        }
                    }

                    if (selectedChannel != null) {
                        if (isTv) {
                            key(selectedChannel) {
                                TvPlayer(
                                    channel = selectedChannel!!,
                                    onBack = { selectedChannel = null }
                                )
                            }
                        } else if (isPlayerReady) {
                            // Mobile Player
                            key(selectedChannel) {
                                TwitchPlayer(
                                    channel = selectedChannel!!,
                                    isFullscreen = isFullscreen,
                                    isPip = isInPipMode,
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
                        }
                    } else {
                        // No channel selected
                        if (isTv) {
                            // Fullscreen TV Selector
                            TwitchChannelSelector(
                                onChannelSelected = { channel ->
                                    selectedChannel = channel
                                }
                            )
                        } else {
                            // Mobile Browser
                            TwitchBrowser(
                                onChannelSelected = { channel ->
                                    selectedChannel = channel
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updatePipParams() {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(selectedChannelState.value != null)
        
        pipRectState.value?.let {
            builder.setSourceRectHint(it)
        }
        
        setPictureInPictureParams(builder.build())
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
        val regex = """(?:www\.|m\.)?twitch\.tv/([^/\?]+)""".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}
