package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.R
import com.akumasdk.samtch.util.SamtchBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator

@Composable
fun WebViewContainer(
    modifier: Modifier,
    state: WebViewState,
    navigator: WebViewNavigator,
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

@Composable
fun TwitchChat(
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

fun createTwitchPlayerUrl(channel: String): String {
    return "https://player.twitch.tv/?channel=$channel&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
}
