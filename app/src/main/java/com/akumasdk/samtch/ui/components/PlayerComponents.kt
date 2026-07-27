package com.akumasdk.samtch.ui.components

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.webkit.WebView as NativeWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.data.settings.SettingsManager
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
    onPlaybackStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    var isVaftReady by remember { mutableStateOf(false) }
    val adBlockMode by SettingsManager.getAdBlockMode(context).collectAsState(initial = SettingsManager.AdBlockMode.VAFT)

    // Reset VAFT status and inject early when loading starts
    LaunchedEffect(state.loadingState, adBlockMode) {
        if (state.loadingState is LoadingState.Loading) {
            isVaftReady = false
            val scriptPath = when (adBlockMode) {
                SettingsManager.AdBlockMode.VAFT -> "js/player/vaft.js"
                SettingsManager.AdBlockMode.VIDEO_SWAP -> "js/player/video_swap.js"
            }
            val adScript = ScriptLoader.getScript(context, scriptPath)
            if (adScript.isNotEmpty()) {
                Log.d("TwitchPlayer", "Injecting $adBlockMode early (Loading state detected)")
                navigator.evaluateJavaScript(adScript)
            }

            // Also inject playback monitor early to catch fast starts
            val monitorScript = ScriptLoader.getScript(context, "js/player/playback_monitor.js")
            if (monitorScript.isNotEmpty()) {
                navigator.evaluateJavaScript(monitorScript)
            }
        }
    }

    // Ensure the bridge always uses the latest lambdas from the current composition context
    val currentOnToggleFullscreen by rememberUpdatedState(onToggleFullscreen)
    val currentOnToggleChat by rememberUpdatedState(onToggleChat)
    val currentOnToggleAudioOnly by rememberUpdatedState(onToggleAudioOnly)
    val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)

    // Script injection logic when page finishes loading
    LaunchedEffect(state.lastLoadedUrl, state.loadingState, isVaftReady, adBlockMode) {
        if (state.loadingState is LoadingState.Finished) {
            // Wait for VAFT to be ready, but don't hang forever (max 2.5s)
            if (!isVaftReady) {
                var waitCount = 0
                while (!isVaftReady && waitCount < 25) {
                    delay(100.milliseconds)
                    waitCount++
                }
                if (!isVaftReady) {
                    Log.w("TwitchPlayer", "AdBlock ($adBlockMode) ready signal timed out, proceeding with other scripts anyway")
                }
            }

            val url = state.lastLoadedUrl ?: ""
            if (!url.contains("twitch.tv")) return@LaunchedEffect

            val scripts = listOf(
                "js/player/ui_cleaner.js",
                "js/player/controls_injector.js",
                "js/player/playback_monitor.js"
            ).mapNotNull { path ->
                val script = ScriptLoader.getScript(context, path)
                if (script.isNotEmpty()) script else null
            }

            if (scripts.isEmpty()) return@LaunchedEffect
            val finalScripts = scripts.joinToString("\n")

            // Wait for WebView to be ready
            delay(100.milliseconds)

            // Initial tight polling for early hooks (catch hydration)
            repeat(8) {
                navigator.evaluateJavaScript(finalScripts)
                delay(300.milliseconds)
            }

            // Fallback: if scripts don't trigger signals, do it ourselves
            delay(3000.milliseconds)
            if (state.loadingState is LoadingState.Finished) {
                Log.d("TwitchPlayer", "Fallback: Triggering finish signals after timeout")
                currentOnPlaybackStarted()
            }

            // Steady polling for dynamic hydration (catch late UI elements)
            repeat(10) {
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
                        onPlaybackStartedCallback = {
                            post { currentOnPlaybackStarted() }
                        },
                        adBlockedCallback = { isBlocking ->
                            Log.d("TwitchPlayer", "Ad blocking status: $isBlocking")
                        },
                        vaftReadyCallback = {
                            post { isVaftReady = true }
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
    private val onPlaybackStartedCallback: () -> Unit = {},
    private val adBlockedCallback: (Boolean) -> Unit = {},
    private val vaftReadyCallback: () -> Unit = {}
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
    fun onPlaybackStarted() {
        onPlaybackStartedCallback()
    }

    @JavascriptInterface
    fun onAdBlocked(isBlocking: Boolean) {
        adBlockedCallback(isBlocking)
    }

    @JavascriptInterface
    fun onVaftReady() {
        vaftReadyCallback()
    }
}
