package com.akumasdk.samtch.util

import android.webkit.JavascriptInterface

/**
 * Bridge class to handle JavaScript calls from the Twitch chat WebView.
 */
class TwitchChatBridge(
    private val onChatLoadedCallback: () -> Unit = {}
) {
    @JavascriptInterface
    fun onChatLoaded() {
        onChatLoadedCallback()
    }
}

