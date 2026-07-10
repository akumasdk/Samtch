package com.akumasdk.samtch.ui.screens

import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.akumasdk.samtch.R
import com.akumasdk.samtch.util.SamtchBridge

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
            // Fullscreen Landscape View
            WebViewContainer(
                modifier = Modifier.fillMaxSize(),
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen
            )
        } else {
            // Portrait View
            Column(modifier = Modifier.fillMaxSize()) {
                // Video at top (16:9 aspect ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    WebViewContainer(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        navigator = navigator,
                        channel = channel,
                        onToggleFullscreen = onToggleFullscreen
                    )
                }

                // Twitch Chat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF18181B)) // Twitch background color
                ) {
                    TwitchChat(
                        channel = channel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun WebViewContainer(
    modifier: Modifier,
    state: com.multiplatform.webview.web.WebViewState,
    navigator: com.multiplatform.webview.web.WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit
) {
    WebView(
        modifier = modifier,
        state = state,
        navigator = navigator,
        captureBackPresses = false,
        onCreated = { webView ->
            Log.d("TwitchPlayer", "WebView created for channel: $channel")

            state.webSettings.apply {
                isJavaScriptEnabled = true

                androidWebSettings.apply {
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                }
            }

            webView.apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                // Add bridge for fullscreen using the dedicated class
                addJavascriptInterface(
                    SamtchBridge(onToggleFullscreen = {
                        post { onToggleFullscreen() }
                    }),
                    "SamtchBridge"
                )

                // Enable fullscreen for videos
                webChromeClient = WebChromeClient()

                // Enable mixed content for Twitch
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
    )
}

private fun createTwitchPlayerUrl(channel: String): String {
    // Load Twitch player directly without iframe
    // Using mobile view which is optimized for fullscreen
    return "https://player.twitch.tv/?channel=$channel&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
}

@Composable
private fun TwitchChat(
    channel: String,
    modifier: Modifier = Modifier
) {
    val chatUrl = "https://www.twitch.tv/embed/$channel/chat?parent=twitch.tv&darkpopout"
    val state = rememberSaveableWebViewState(chatUrl)
    val navigator = rememberWebViewNavigator()
    val context = LocalContext.current

    // Update URL when channel changes
    LaunchedEffect(channel) {
        navigator.loadUrl(chatUrl)
    }

    // Inject BTTV when page is loaded
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            try {
                val bttvScript = context.resources.openRawResource(R.raw.bttv)
                    .bufferedReader()
                    .use { it.readText() }

                navigator.evaluateJavaScript(bttvScript) {
                    Log.d("TwitchChat", "BTTV script injected")
                }
            } catch (e: Exception) {
                Log.e("TwitchChat", "Error injecting BTTV", e)
            }
        }
    }

    WebView(
        modifier = modifier,
        state = state,
        navigator = navigator,
        captureBackPresses = false,
        onCreated = { webView ->
            state.webSettings.apply {
                isJavaScriptEnabled = true
                androidWebSettings.apply {
                    domStorageEnabled = true
                }
            }
            webView.apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
        }
    )
}




