package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.R
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator

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
