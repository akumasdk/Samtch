package com.akumasdk.samtch.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.screens.player.FullscreenPlayer
import com.akumasdk.samtch.ui.screens.player.PortraitPlayer
import com.akumasdk.samtch.ui.screens.player.createTwitchPlayerUrl

@Composable
fun TwitchPlayer(
    channel: String = "shroud",
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val twitchUrl = createTwitchPlayerUrl(channel)
    val context = LocalContext.current

    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()

    Log.d("TwitchPlayer", "Creating player for channel: $channel")

    // Handle back button to return to browser
    BackHandler {
        onBack?.invoke()
    }

    LaunchedEffect(channel) {
        Log.d("TwitchPlayer", "Loading URL: $twitchUrl")
        navigator.loadUrl(twitchUrl)
    }

    // Inject scripts when page is loaded
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            try {
                // First inject video swap script
                val videoSwapScript = context.resources.openRawResource(R.raw.video_swap_new)
                    .bufferedReader()
                    .use { it.readText() }

                navigator.evaluateJavaScript(videoSwapScript) {
                    Log.d("TwitchPlayer", "Video swap script injected")
                }

                // Inject clean UI script
                val cleanUiScript = context.resources.openRawResource(R.raw.clean_ui)
                    .bufferedReader()
                    .use { it.readText() }

                navigator.evaluateJavaScript(cleanUiScript) {
                    Log.d("TwitchPlayer", "Clean UI script injected")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Cleanup when player is destroyed
    DisposableEffect(channel) {
        onDispose {
            Log.d("TwitchPlayer", "Disposing player for channel: $channel")
            // Clean up WebView resources aggressively
            try {
                state.nativeWebView.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearCache(true)
                    clearHistory()
                    clearFormData()
                    // Remove all views to prevent overlap
                    removeAllViews()
                }
            } catch (e: Exception) {
                Log.e("TwitchPlayer", "Error disposing WebView", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isFullscreen) {
            FullscreenPlayer(
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen
            )
        } else {
            PortraitPlayer(
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen
            )
        }
    }
}
