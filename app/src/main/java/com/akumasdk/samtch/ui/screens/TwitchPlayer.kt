package com.akumasdk.samtch.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.akumasdk.samtch.ui.screens.player.FullscreenPlayer
import com.akumasdk.samtch.ui.screens.player.PortraitPlayer
import com.akumasdk.samtch.ui.screens.player.WebViewContainer
import com.akumasdk.samtch.ui.screens.player.createTwitchPlayerUrl
import com.akumasdk.samtch.util.ScriptLoader

@Composable
fun TwitchPlayer(
    channel: String = "forsen",
    isFullscreen: Boolean = false,
    isPip: Boolean = false,
    refreshTrigger: Int = 0,
    onToggleFullscreen: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {}
) {
    val twitchUrl = createTwitchPlayerUrl(channel)
    val context = LocalContext.current

    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()

    Log.d("TwitchPlayer", "Creating player for channel: $channel (isPip: $isPip)")

    // Handle back button to return to browser
    if (!isPip) {
        BackHandler {
            onBack?.invoke()
        }
    }

    LaunchedEffect(channel) {
        Log.d("TwitchPlayer", "Loading URL: $twitchUrl")
        navigator.loadUrl(twitchUrl)
    }

    // Handle refresh trigger from PiP actions
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            Log.d("TwitchPlayer", "Refresh triggered in PiP (count: $refreshTrigger)")
            // Use loadUrl instead of reload to ensure a clean state transition and JS injection
            // We add a unique query param to force the WebView to treat it as a new navigation
            val refreshUrl = createTwitchPlayerUrl(channel) + "&refresh=$refreshTrigger"
            navigator.loadUrl(refreshUrl)
        }
    }

    // Aggressive injection strategy: Polling loop to catch hydration
    LaunchedEffect(channel, refreshTrigger) {
        val scripts = listOf(
            //"js/player/video_swap.js",
            "js/player/vaft.js", // using vaft script as of now
            "js/player/ui_cleaner.js",
            "js/player/controls_injector.js",
            "js/player/visibility_monitor.js",
            "js/player/link_disabler.js",
            "js/common/scroll_unlocker.js"
        ).mapNotNull { path ->
            val script = ScriptLoader.getScript(context, path)
            if (script.isNotEmpty()) {
                val guardVar = "samtch_" + path.replace(Regex("[^a-zA-Z0-9]"), "_")
                "if (typeof window.$guardVar === 'undefined') { window.$guardVar = true; $script }"
            } else null
        }.joinToString("\n")

        if (scripts.isEmpty()) return@LaunchedEffect

        // Small initial delay
        delay(300)
        
        // Initial tight polling for early hooks
        repeat(10) {
            navigator.evaluateJavaScript(scripts)
            delay(200)
        }
        
        // Steady polling for dynamic hydration
        repeat(15) {
            navigator.evaluateJavaScript(scripts)
            delay(1000)
        }
        Log.d("TwitchPlayer", "Completed injection polling for $channel")
    }
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

    // Stable WebView content that won't be recreated when moving in the tree
    val webView = remember(channel) {
        movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
            WebViewContainer(
                modifier = modifier.onGloballyPositioned { layoutCoordinates ->
                    val rect = layoutCoordinates.boundsInWindow()
                    onVideoBoundsChanged(
                        android.graphics.Rect(
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.right.toInt(),
                            rect.bottom.toInt()
                        )
                    )
                },
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen,
                onToggleChat = onToggleChat
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isPip) {
            // Simplified view for PiP: Just the WebView container
            webView(Modifier.fillMaxSize()) {}
        } else if (isFullscreen) {
            FullscreenPlayer(
                channel = channel,
                webView = { modifier, onToggleChat -> webView(modifier, onToggleChat) }
            )
        } else {
            PortraitPlayer(
                channel = channel,
                onToggleFullscreen = onToggleFullscreen,
                webView = { modifier, onToggleChat -> webView(modifier, onToggleChat) }
            )
        }
    }
}
