package com.akumasdk.samtch

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.akumasdk.samtch.ui.screens.TwitchBrowser
import com.akumasdk.samtch.ui.screens.TwitchPlayer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            var selectedChannel by remember { mutableStateOf<String?>(null) }
            var isFullscreen by remember { mutableStateOf(false) }
            var isPlayerReady by remember { mutableStateOf(false) }

            // Check if app was opened with a Twitch URL
            val intentUrl = intent?.data?.toString()
            val channelFromIntent = extractChannelFromUrl(intentUrl)

            // If intent has a channel, use it; otherwise start with browser
            if (channelFromIntent != null && selectedChannel == null) {
                selectedChannel = channelFromIntent
            }

            // Change orientation and system bars based on current screen
            LaunchedEffect(selectedChannel, isFullscreen) {
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

            if (selectedChannel != null && isPlayerReady) {
                // Use key() to force complete recreation when channel changes
                key(selectedChannel) {
                    TwitchPlayer(
                        channel = selectedChannel!!,
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        onBack = {
                            if (isFullscreen) {
                                isFullscreen = false
                            } else {
                                // Destroy player by setting channel to null
                                selectedChannel = null
                            }
                        }
                    )
                }
            } else if (selectedChannel == null) {
                // Show browser to select a channel
                TwitchBrowser(
                    onChannelSelected = { channel ->
                        selectedChannel = channel
                    }
                )
            }
        }
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