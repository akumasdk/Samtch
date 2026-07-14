package com.akumasdk.samtch.util

import android.webkit.JavascriptInterface

/**
 * Bridge class to handle JavaScript calls from the Twitch player.
 */
class SamtchBridge(
    private val onToggleFullscreen: () -> Unit,
    private val onToggleChat: () -> Unit = {},
    private val onChatLoadedCallback: () -> Unit = {}
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
    fun onChatLoaded() {
        onChatLoadedCallback()
    }
}