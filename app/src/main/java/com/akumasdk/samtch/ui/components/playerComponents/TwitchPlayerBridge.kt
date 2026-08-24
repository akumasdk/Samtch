package com.akumasdk.samtch.ui.components.playerComponents

import android.webkit.JavascriptInterface

class TwitchPlayerBridge(
    private val onToggleFullscreen: () -> Unit,
    private val onToggleChat: () -> Unit = {},
    private val onToggleAudioOnly: () -> Unit = {},
    private val onPlaybackStartedCallback: () -> Unit = {},
    private val onLoadingStatusCallback: (String) -> Unit = {},
    private val onAdblockedCallback: (String) -> Unit = {},
    private val onStreamUrlFoundCallback: (String) -> Unit = {},
    private val onAdStatusChangedCallback: (Boolean, String) -> Unit = { _, _ -> }
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

    @JavascriptInterface
    fun onStreamUrlFound(url: String) {
        onStreamUrlFoundCallback(url)
    }

    @JavascriptInterface
    fun onAdStatusChanged(isAd: Boolean, message: String) {
        onAdStatusChangedCallback(isAd, message)
    }
}
