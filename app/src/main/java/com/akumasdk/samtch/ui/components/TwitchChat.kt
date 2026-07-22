package com.akumasdk.samtch.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("JavascriptInterface")
@Composable
fun TwitchChat(
    channel: String,
    modifier: Modifier = Modifier
) {
    val chatUrl = "https://www.twitch.tv/embed/$channel/chat?parent=twitch.tv&darkpopout"
    val state = rememberSaveableWebViewState(chatUrl)
    val navigator = rememberWebViewNavigator()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Track if chat is fully loaded (chat-input element is present)
    var isChatFullyLoaded by remember { mutableStateOf(false) }

    val chatBridge = remember(coroutineScope) {
        TwitchChatBridge(
            onChatLoadedCallback = {
                Log.d("TwitchChat", "Chat input element detected, waiting to prevent flash...")
                // Add small delay to prevent flash when loading is very fast
                coroutineScope.launch {
                    delay(300.milliseconds) // Wait 300ms to prevent flashing
                    isChatFullyLoaded = true
                    Log.d("TwitchChat", "Chat fully loaded - hiding loading screen")
                }
            }
        )
    }

    // Update URL when channel changes
    LaunchedEffect(channel) {
        isChatFullyLoaded = false // Reset loading state
        navigator.loadUrl(chatUrl)
    }

    // Inject scripts when page is loaded
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            try {
                // Load and inject Chat Loader Observer first
                val observerScript = ScriptLoader.getScript(context, "js/chat/chat_loader_observer.js")
                if (observerScript.isNotEmpty()) {
                    navigator.evaluateJavaScript(observerScript) {
                        Log.d("TwitchChat", "Chat Loader Observer script injected")
                    }
                }

                // Load and inject UI Cleaner
                val cleanerScript = ScriptLoader.getScript(context, "js/chat/ui_cleaner.js")
                if (cleanerScript.isNotEmpty()) {
                    navigator.evaluateJavaScript(cleanerScript) {
                        Log.d("TwitchChat", "Chat UI Cleaner script injected")
                    }
                }

                // Load and inject BTTV
                val bttvScript = ScriptLoader.getScript(context, "js/chat/bttv.js")
                if (bttvScript.isNotEmpty()) {
                    navigator.evaluateJavaScript(bttvScript) {
                        Log.d("TwitchChat", "BTTV script injected")
                    }
                }
            } catch (e: Exception) {
                Log.e("TwitchChat", "Error injecting scripts", e)
                isChatFullyLoaded = true // Show chat on error
            }
        }
    }

    Box(modifier = modifier) {
        WebView(
            modifier = Modifier.fillMaxSize(),
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

                    // Add JavaScript bridge for chat load detection
                    addJavascriptInterface(chatBridge, "TwitchChatBridge")
                }
            }
        )

        // Show loading indicator with background until chat-input element is fully loaded
        if (!isChatFullyLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF18181B)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

class TwitchChatBridge(
    private val onChatLoadedCallback: () -> Unit = {}
) {
    @JavascriptInterface
    fun onChatLoaded() {
        onChatLoadedCallback()
    }
}
