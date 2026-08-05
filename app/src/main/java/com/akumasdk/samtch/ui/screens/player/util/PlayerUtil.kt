package com.akumasdk.samtch.ui.screens.player.util

import com.akumasdk.samtch.util.Constants
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

fun unloadWebView(state: WebViewState, navigator: WebViewNavigator) {
    navigator.stopLoading()
    navigator.loadUrl(Constants.ABOUT_BLANK)
    try {
        state.nativeWebView.apply {
            onPause() // Pause JS and events for THIS instance only
            stopLoading()
            loadUrl(Constants.ABOUT_BLANK)
        }
    } catch (_: Exception) {}
}
