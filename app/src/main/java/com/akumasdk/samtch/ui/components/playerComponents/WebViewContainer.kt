package com.akumasdk.samtch.ui.components.playerComponents

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
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
    onLoadingStatus: (String) -> Unit = {},
    onAdblocked: (String) -> Unit = {},
    onStreamUrlFound: (String) -> Unit = {},
    onAdStatusChanged: (Boolean, String) -> Unit = { _, _ -> }
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
    val currentOnLoadingStatus by rememberUpdatedState(onLoadingStatus)
    val currentOnAdblocked by rememberUpdatedState(onAdblocked)
    val currentOnStreamUrlFound by rememberUpdatedState(onStreamUrlFound)
    val currentOnAdStatusChanged by rememberUpdatedState(onAdStatusChanged)

    // Script injection logic when page finishes loading
    LaunchedEffect(state.lastLoadedUrl, state.loadingState, adBlockMode) {
        if (state.loadingState is LoadingState.Finished) {
            val url = state.lastLoadedUrl ?: ""
            if (!url.contains(Constants.Twitch.DOMAIN)) return@LaunchedEffect

            // Minimal scripts for orchestrator mode
            val scripts = listOf(
                Constants.Scripts.PLAYER_PLAYBACK_MONITOR,
                Constants.Scripts.PLAYER_STREAM_DETECTOR,
                // Include VAFT/VIDEO_SWAP here too just in case it didn't catch onPageStarted
                if (adBlockMode == SettingsManager.AdBlockMode.VAFT) Constants.Scripts.PLAYER_VAFT else Constants.Scripts.PLAYER_VIDEO_SWAP
            ).mapNotNull { path ->
                val script = ScriptLoader.getScript(context, path)
                if (script.isNotEmpty()) script else null
            }

            if (scripts.isEmpty()) return@LaunchedEffect
            val finalScripts = scripts.joinToString("\n")

            // Wait for WebView to be ready
            delay(100.milliseconds)

            // Inject orchestrator scripts
            navigator.evaluateJavaScript(finalScripts)
            
            // Definitively mute and disable interactions
            navigator.evaluateJavaScript("""
                (function() {
                    const setupOrchestrator = () => {
                        const videos = document.getElementsByTagName('video');
                        for (let v of videos) { 
                            v.muted = true; 
                            v.volume = 0; 
                            v.removeAttribute('autoplay'); // Prevent it from fighting for focus
                            v.style.pointerEvents = 'none';
                        }
                        // Stop Twitch from pausing when not visible
                        if (window.Player && window.Player.prototype) {
                            window.Player.prototype.pause = function() { console.log('Pause blocked by Samtch Orchestrator'); };
                        }
                    };
                    setupOrchestrator();
                    setInterval(setupOrchestrator, 1000);
                    
                    // Disable all touch/click interactions
                    document.body.style.pointerEvents = 'none';
                    document.body.style.userSelect = 'none';
                })();
            """.trimIndent())
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
            Log.d("TwitchPlayer", "Orchestrator WebView created for channel: $channel")

            // Lower priority for orchestrator
            webView.setRendererPriorityPolicy(NativeWebView.RENDERER_PRIORITY_BOUND, false)

            state.webSettings.apply {
                isJavaScriptEnabled = true

                androidWebSettings.apply {
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false // Needed for extraction to start
                    allowFileAccess = true
                }
            }

            webView.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                
                // Disable all focus and interaction
                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = false
                
                // Prevent onViewTypeAvailable crash by disabling Autofill
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

                // Web Video Caster style: Native network interception
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: android.webkit.WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: ""
                        if (url.contains(".m3u8", ignoreCase = true)) {
                            Log.d("TwitchPlayer", "Orchestrator Intercepted M3U8: $url")
                            post { currentOnStreamUrlFound(url) }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

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
                            // Orchestrator playback started, but we don't notify the UI anymore
                            // to avoid hiding the loading screen too early for the NATIVE player.
                            Log.d("TwitchPlayer", "Orchestrator playback started")
                        },
                        onLoadingStatusCallback = { message: String ->
                            // Optional: Keep status updates if they are helpful
                            post { currentOnLoadingStatus(message) }
                        },
                        onAdblockedCallback = { text: String ->
                            post { currentOnAdblocked(text) }
                        },
                        onStreamUrlFoundCallback = { url: String ->
                            post { currentOnStreamUrlFound(url) }
                        },
                        onAdStatusChangedCallback = { isAd: Boolean, message: String ->
                            post { currentOnAdStatusChanged(isAd, message) }
                        }
                    ),
                    Constants.Bridges.PLAYER
                )

                // Enable fullscreen for videos (not needed for orchestrator but keep for stability)
                webChromeClient = WebChromeClient()

                // Enable mixed content for Twitch
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.userAgentString = Constants.UserAgents.MOBILE
            }
        }
    )
}

fun createTwitchPlayerUrl(channel: String): String {
    return Constants.Twitch.Templates.PLAYER_URL.format(channel)
}
