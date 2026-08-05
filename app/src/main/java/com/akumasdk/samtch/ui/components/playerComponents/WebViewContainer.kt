package com.akumasdk.samtch.ui.components.playerComponents

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView as NativeWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextResourcesRead")
@Composable
fun WebViewContainer(
    modifier: Modifier,
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit = {},
    onToggleAudioOnly: () -> Unit = {},
    onPlaybackStarted: () -> Unit = {},
    onLoadingStatus: (String) -> Unit = {},
    onAdblocked: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val resources = context.resources
    val adBlockMode by SettingsManager.getAdBlockMode(context).collectAsState(initial = SettingsManager.AdBlockMode.VAFT)

    // Reset status and inject early when loading starts
    LaunchedEffect(state.loadingState, adBlockMode) {
        if (state.loadingState is LoadingState.Loading) {
            onAdblocked("")
            
            // Inject localized strings for scripts
            val stringMap = mapOf(
                "loading_stream" to resources.getString(R.string.loading_stream),
                "searching_video" to resources.getString(R.string.searching_video),
                "preparing_playback" to resources.getString(R.string.preparing_playback),
                "initializing_player" to resources.getString(R.string.initializing_player),
                "bypassing_ads" to resources.getString(R.string.bypassing_ads)
            )
            val stringsJson = stringMap.entries.joinToString(",") { "\"${it.key}\": \"${it.value}\"" }
            navigator.evaluateJavaScript("window.SamtchStrings = { $stringsJson };")

            val scriptPath = when (adBlockMode) {
                SettingsManager.AdBlockMode.VAFT -> Constants.Scripts.PLAYER_VAFT
                SettingsManager.AdBlockMode.VIDEO_SWAP -> Constants.Scripts.PLAYER_VIDEO_SWAP
            }
            val adScript = ScriptLoader.getScript(context, scriptPath)
            if (adScript.isNotEmpty()) {
                Log.d("TwitchPlayer", "Injecting $adBlockMode early (Loading state detected)")
                navigator.evaluateJavaScript(adScript)
            }

            // Also inject playback monitor and early hider to catch fast starts
            val earlyScripts = listOf(
                Constants.Scripts.PLAYER_PLAYBACK_MONITOR,
                Constants.Scripts.PLAYER_EARLY_HIDER
            ).mapNotNull { path ->
                val s = ScriptLoader.getScript(context, path)
                if (s.isNotEmpty()) s else null
            }
            if (earlyScripts.isNotEmpty()) {
                navigator.evaluateJavaScript(earlyScripts.joinToString("\n"))
            }
        }
    }

    // Ensure the bridge always uses the latest lambdas from the current composition context
    val currentOnToggleFullscreen by rememberUpdatedState(onToggleFullscreen)
    val currentOnToggleChat by rememberUpdatedState(onToggleChat)
    val currentOnToggleAudioOnly by rememberUpdatedState(onToggleAudioOnly)
    val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnLoadingStatus by rememberUpdatedState(onLoadingStatus)
    val currentOnAdblocked by rememberUpdatedState(onAdblocked)

    // Script injection logic when page finishes loading
    LaunchedEffect(state.lastLoadedUrl, state.loadingState, adBlockMode) {
        if (state.loadingState is LoadingState.Finished) {
            val url = state.lastLoadedUrl ?: ""
            if (!url.contains(Constants.Twitch.DOMAIN)) return@LaunchedEffect

            val scripts = listOf(
                Constants.Scripts.PLAYER_UI_CLEANER,
                Constants.Scripts.PLAYER_CONTROLS_INJECTOR,
                Constants.Scripts.PLAYER_PLAYBACK_MONITOR
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
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                
                // Prevent onViewTypeAvailable crash by disabling Autofill
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

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
                        onLoadingStatusCallback = { message: String ->
                            post { currentOnLoadingStatus(message) }
                        },
                        onAdblockedCallback = { text: String ->
                            post { currentOnAdblocked(text) }
                        }
                    ),
                    Constants.Bridges.PLAYER
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
    return Constants.Twitch.Templates.PLAYER_URL.format(channel)
}
