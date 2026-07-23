package com.akumasdk.samtch.ui.components

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView as NativeWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WebViewContainer(
    modifier: Modifier,
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit = {},
    onToggleAudioOnly: () -> Unit = {},
    onUiCleanFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    // Ensure the bridge always uses the latest lambdas from the current composition context
    val currentOnToggleFullscreen by rememberUpdatedState(onToggleFullscreen)
    val currentOnToggleChat by rememberUpdatedState(onToggleChat)
    val currentOnToggleAudioOnly by rememberUpdatedState(onToggleAudioOnly)
    val currentOnUiCleanFinish by rememberUpdatedState(onUiCleanFinish)

    // Script injection logic when page finishes loading
    LaunchedEffect(state.lastLoadedUrl, state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            val url = state.lastLoadedUrl ?: ""
            if (!url.contains("twitch.tv")) return@LaunchedEffect

            val scripts = listOf(
                "js/player/vaft.js",
                "js/player/ui_cleaner.js",
                "js/player/controls_injector.js",
                "js/player/visibility_monitor.js",
                "js/player/link_disabler.js",
                "js/common/scroll_unlocker.js"
            ).mapNotNull { path ->
                val script = ScriptLoader.getScript(context, path)
                if (script.isNotEmpty()) script else null
            }

            if (scripts.isEmpty()) return@LaunchedEffect
            val finalScripts = scripts.joinToString("\n")

            // Wait for WebView to be ready
            delay(800.milliseconds)

            // Initial tight polling for early hooks (catch hydration)
            repeat(10) {
                navigator.evaluateJavaScript(finalScripts)
                delay(300.milliseconds)
            }

            // Steady polling for dynamic hydration (catch late UI elements)
            repeat(15) {
                navigator.evaluateJavaScript(finalScripts)
                delay(1500.milliseconds)
            }
        }
    }

    WebView(
        modifier = modifier,
        state = state,
        navigator = navigator,
        captureBackPresses = false,
        factory = { param ->
            NativeWebView(param.context)
        },
        onCreated = { webView ->
            Log.d("TwitchPlayer", "WebView created for channel: $channel")

            // Prevent the renderer process from being killed when hidden
            webView.setRendererPriorityPolicy(NativeWebView.RENDERER_PRIORITY_BOUND, false)

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
                    TwitchPlayerBridge(
                        onToggleFullscreen = {
                            post { currentOnToggleFullscreen() }
                        },
                        onToggleChat = {
                            post { currentOnToggleChat() }
                        },
                        onToggleAudioOnly = {
                            post { currentOnToggleAudioOnly() }
                        },
                        onUiCleanFinish = {
                            post { currentOnUiCleanFinish() }
                        }
                    ),
                    "TwitchPlayerBridge"
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

class TwitchPlayerBridge(
    private val onToggleFullscreen: () -> Unit,
    private val onToggleChat: () -> Unit = {},
    private val onToggleAudioOnly: () -> Unit = {},
    private val onUiCleanFinish: () -> Unit = {}
) {
    @JavascriptInterface
    fun toggleFullscreen() {
        onToggleFullscreen()
    }

    @JavascriptInterface
    fun toggleChat() {
        onToggleChat()
    }

    @JavascriptInterface
    fun toggleAudioOnly() {
        onToggleAudioOnly()
    }

    @JavascriptInterface
    fun uiCleanFinish() {
        onUiCleanFinish()
    }
}
