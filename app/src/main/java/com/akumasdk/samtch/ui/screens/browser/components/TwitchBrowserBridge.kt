package com.akumasdk.samtch.ui.screens.browser.components

import android.app.Activity
import android.util.Log
import android.webkit.JavascriptInterface

class TwitchBrowserBridge(
    private val activity: Activity?,
    private val onSettingsClicked: () -> Unit,
    private val onLoginRequested: () -> Unit = {},
    private val onLoadedCallback: () -> Unit,
    private val onUiCleanFinished: () -> Unit = {},
    private val onRefreshRequested: () -> Unit,
    private val onUrlChanged: (String, Boolean, Boolean) -> Unit
) {
    @JavascriptInterface
    fun onUrlChange(url: String, blocked: Boolean) {
        onUrlChange(url, blocked, false)
    }

    @JavascriptInterface
    fun onUrlChange(url: String, blocked: Boolean, requestBack: Boolean) {
        activity?.runOnUiThread {
            Log.d("TwitchBrowserBridge", "URL change via JS Bridge: $url (blocked=$blocked, requestBack=$requestBack)")
            onUrlChanged(url, blocked, requestBack)
        }
    }

    @JavascriptInterface
    fun onDomLoaded() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowserBridge", "DOM Loaded via JS Bridge class")
            onLoadedCallback()
        }
    }

    @JavascriptInterface
    fun uiCleanFinish() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowserBridge", "UI cleaning finished via JS Bridge")
            onUiCleanFinished()
        }
    }

    @JavascriptInterface
    fun openSettings() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowserBridge", "Settings button clicked in JS")
            onSettingsClicked()
        }
    }

    @JavascriptInterface
    fun openLogin() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowserBridge", "Login button clicked in JS")
            onLoginRequested()
        }
    }

    @JavascriptInterface
    fun onRefresh() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowserBridge", "Refresh triggered via JS Bridge")
            onRefreshRequested()
        }
    }
}
