package com.akumasdk.samtch.ui.components.chat

import android.webkit.JavascriptInterface

class TwitchChatBridge(
    private val onChatLoadedCallback: () -> Unit = {}
) {
    @JavascriptInterface
    fun onChatLoaded() {
        onChatLoadedCallback()
    }
}
