package com.akumasdk.samtch.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import com.akumasdk.samtch.ui.screens.player.FullscreenPlayer
import com.akumasdk.samtch.ui.screens.player.PortraitPlayer
import com.akumasdk.samtch.ui.screens.player.WebViewContainer
import com.akumasdk.samtch.ui.screens.player.createTwitchPlayerUrl
import com.akumasdk.samtch.util.ScriptLoader
import kotlin.time.Duration.Companion.milliseconds

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
    val context = LocalContext.current

    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()

    Log.d("TwitchPlayer", "Creating player for channel: $channel (isPip: $isPip)")

    // Handle back button to return to browser
    if (!isPip) {
        BackHandler {
            Log.d("TwitchPlayer", "BackHandler triggered for $channel")
            onBack?.invoke()
        }
    }

    // Handle URL loading and refresh logic
    LaunchedEffect(channel, refreshTrigger) {
        val baseUrl = createTwitchPlayerUrl(channel)
        val finalUrl = if (refreshTrigger > 0) {
            "$baseUrl&refresh=$refreshTrigger"
        } else {
            baseUrl
        }
        Log.d("TwitchPlayer", "Loading URL: $finalUrl (trigger: $refreshTrigger)")
        navigator.loadUrl(finalUrl)
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
            if (script.isNotEmpty()) script else null
        }.toMutableList()

        val finalScripts = scripts.joinToString("\n")

        if (finalScripts.isEmpty()) return@LaunchedEffect

        // Small initial delay to let the WebView engine warm up
        delay(500.milliseconds)
        
        // Initial tight polling for early hooks
        repeat(8) {
            navigator.evaluateJavaScript(finalScripts)
            delay(250.milliseconds)
        }
        
        // Steady polling for dynamic hydration (reduced frequency)
        repeat(12) {
            navigator.evaluateJavaScript(finalScripts)
            delay(1500.milliseconds)
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
