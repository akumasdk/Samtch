package com.akumasdk.samtch.ui.screens.settings.components

import android.webkit.JavascriptInterface

class BttvSettingsChatBridge(
    private val onComplete: () -> Unit
) {
    @JavascriptInterface
    fun onAutomationComplete() {
        onComplete()
    }
}
