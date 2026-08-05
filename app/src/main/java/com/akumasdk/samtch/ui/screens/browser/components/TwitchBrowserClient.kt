package com.akumasdk.samtch.ui.screens.browser.components

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import com.akumasdk.samtch.util.TwitchUrlUtil

class TwitchBrowserClient(
    private val context: android.content.Context,
    private val safeHistory: List<String>,
    private val onChannelSelected: (String) -> Unit,
    private val onUiLoadingChanged: (Boolean) -> Unit,
    private val onLoaded: () -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        Log.d("TwitchBrowserClient", "shouldOverrideUrlLoading: $url")

        // Force mobile domain
        val mobileUrl = TwitchUrlUtil.ensureMobileUrl(url)
        if (mobileUrl != url) {
            Log.d("TwitchBrowserClient", "Desktop URL detected, forcing mobile: $mobileUrl")
            view?.loadUrl(mobileUrl)
            return true
        }

        // Force full reload for the global home to avoid SPA issues
        if (TwitchUrlUtil.isGlobalHome(url)) {
            Log.d("TwitchBrowserClient", "Global home path detected, forcing full load")
            view?.loadUrl(url)
            return true
        }

        // Detect if user navigated to an unsafe page
        val channelMatch = TwitchUrlUtil.extractChannelFromUrl(url)
        val isSafe = TwitchUrlUtil.isSafeExplorationUrl(url)

        if (!isSafe) {
            val lastSafeUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
            Log.d("TwitchBrowserClient", "Intercepted unsafe URL: $url. Redirecting to $lastSafeUrl")
            if (TwitchUrlUtil.isPlayableChannel(channelMatch, TwitchUrlUtil.getCurrentUserFromCookies())) {
                onChannelSelected(channelMatch!!)
            }
            view?.loadUrl(lastSafeUrl)
            return true
        }

        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("TwitchBrowserClient", "Page started: $url")
        onUiLoadingChanged(true)

        // Inject splash controller early
        val splashScript = ScriptLoader.getScript(context, Constants.Scripts.COMMON_SPLASH_CONTROLLER)
        if (splashScript.isNotEmpty()) {
            view?.evaluateJavascript(splashScript, null)
        }

        url?.let {
            val mobileUrl = TwitchUrlUtil.ensureMobileUrl(it)
            if (mobileUrl != it) {
                Log.d("TwitchBrowserClient", "onPageStarted: Desktop URL detected, forcing mobile")
                view?.loadUrl(mobileUrl)
                return
            }

            val channelMatch = TwitchUrlUtil.extractChannelFromUrl(it)
            val isSafe = TwitchUrlUtil.isSafeExplorationUrl(it)

            if (!isSafe) {
                val lastSafeUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                Log.d("TwitchBrowserClient", "Page started on unsafe URL. Redirecting.")
                if (TwitchUrlUtil.isPlayableChannel(channelMatch, TwitchUrlUtil.getCurrentUserFromCookies())) {
                    onChannelSelected(channelMatch!!)
                }
                view?.loadUrl(lastSafeUrl)
            }
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d("TwitchBrowserClient", "Page finished: $url")
        onLoaded()
    }
}
