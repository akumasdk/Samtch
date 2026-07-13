package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.akumasdk.samtch.util.SamtchBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

@Composable
fun WebViewContainer(
    modifier: Modifier,
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit = {}
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

                // Add bridge for fullscreen and chat using the dedicated class
                addJavascriptInterface(
                    SamtchBridge(
                        onToggleFullscreen = {
                            post { onToggleFullscreen() }
                        },
                        onToggleChat = {
                            post { onToggleChat() }
                        }
                    ),
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

fun createTwitchPlayerUrl(channel: String): String {
    return "https://player.twitch.tv/?channel=$channel&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
}
