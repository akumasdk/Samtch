package com.akumasdk.samtch.util

import android.webkit.JavascriptInterface

/**
 * Bridge class to handle JavaScript calls from the Twitch player.
 */
class SamtchBridge(
    private val onToggleFullscreen: () -> Unit,
    private val onToggleChat: () -> Unit = {}
) {
    @JavascriptInterface
    fun toggleFullscreen() {
        onToggleFullscreen()
    }

    @JavascriptInterface
    fun toggleChat() {
        onToggleChat()
    }
}