package com.akumasdk.samtch.ui.components

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.webkit.WebView as NativeWebView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerBackground(
    channel: String,
    previewUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.4f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.background(SamtchTheme.colors.rootBackground)
    ) {
        val finalUrl = previewUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())
        AsyncImage(
            model = finalUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = alpha
        )
        content()
    }
}

@Composable
fun PlayerLoadingScreen(
    channel: String,
    previewUrl: String?,
    loadingMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SamtchTheme.colors.rootBackground),
        contentAlignment = Alignment.Center
    ) {
        val finalUrl = previewUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())

        AsyncImage(
            model = finalUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SamtchTheme.colors.loadingOverlay)
        )
        CircularProgressIndicator(
            color = SamtchTheme.colors.loadingIndicator,
            strokeWidth = 3.dp
        )
        Text(
            text = loadingMessage,
            color = SamtchTheme.colors.primaryText,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.Center)
                .offset(y = 40.dp)
        )
    }
}

@Composable
fun TapTooltip(visible: Boolean, modifier: Modifier = Modifier) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SamtchTheme.colors.tooltipBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.fullscreen_double_tap_hint),
                color = SamtchTheme.colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


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
                        onLoadingStatusCallback = { message ->
                            post { currentOnLoadingStatus(message) }
                        },
                        onAdblockedCallback = { text ->
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

class TwitchPlayerBridge(
    private val onToggleFullscreen: () -> Unit,
    private val onToggleChat: () -> Unit = {},
    private val onToggleAudioOnly: () -> Unit = {},
    private val onPlaybackStartedCallback: () -> Unit = {},
    private val onLoadingStatusCallback: (String) -> Unit = {},
    private val onAdblockedCallback: (String) -> Unit = {}
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
    fun onLoadingStatus(message: String) {
        onLoadingStatusCallback(message)
    }

    @JavascriptInterface
    fun onAdblocked(text: String) {
        onAdblockedCallback(text)
    }
}
