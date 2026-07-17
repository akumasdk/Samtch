package com.akumasdk.samtch.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import kotlinx.coroutines.delay
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.akumasdk.samtch.R
import com.akumasdk.samtch.util.ScriptLoader

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("JavascriptInterface")
@Composable
fun TwitchBrowser(
    onChannelSelected: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onLoaded: () -> Unit = {}
) {
    val state = rememberSaveableWebViewState("https://m.twitch.tv/")
    val navigator = rememberWebViewNavigator()
    val activity = LocalActivity.current
    val context = LocalContext.current
    var lastCheckedUrl by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    var isRefreshing by remember { mutableStateOf(false) }

    // Sync isRefreshing with WebView loading state
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            isRefreshing = false
        }
    }

    // Handle back button
    BackHandler {
        if (navigator.canGoBack) {
            navigator.navigateBack()
        } else {
            activity?.finish()
        }
    }

    // Safety timeout to ensure splash screen eventually disappears
    LaunchedEffect(Unit) {
        delay(8000)
        Log.d("TwitchBrowser", "Safety timeout reached, forcing splash screen dismissal")
        onLoaded()
    }

    LaunchedEffect(Unit) {
        navigator.loadUrl("https://m.twitch.tv/")
    }

    // Monitor URL changes including SPA transitions via polling
    LaunchedEffect(Unit) {
        while (true) {
            val currentUrl = webViewRef?.url ?: ""
            if (currentUrl.isNotEmpty() && currentUrl != lastCheckedUrl) {
                val previousUrl = lastCheckedUrl
                lastCheckedUrl = currentUrl
                Log.d("TwitchBrowser", "URL change detected: $currentUrl (Previous: $previousUrl)")

                if (isGlobalHome(currentUrl)) {
                    Log.d("TwitchBrowser", "Global home path detected, triggering full reload")
                    navigator.reload()
                } else {
                    val channelMatch = extractChannelFromUrl(currentUrl)
                    val currentUser = getCurrentUserFromCookies()

                    if (channelMatch != null && channelMatch != currentUser) {
                        // If we are coming from the global home, stay in browser to allow exploration
                        if (isGlobalHome(previousUrl)) {
                            Log.d("TwitchBrowser", "Channel detected from home, staying in browser: $channelMatch")
                        } else {
                            Log.d("TwitchBrowser", "Channel detected: $channelMatch. Triggering player.")
                            navigator.stopLoading()
                            onChannelSelected(channelMatch)
                        }
                    }
                }
            }
            delay(500)
        }
    }

    // Inject scripts when page is loaded (ONLY dialog closer)
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            // Check if this is a channel URL - if so, don't inject scripts
            val currentUrl = state.lastLoadedUrl ?: ""
            val channelMatch = extractChannelFromUrl(currentUrl)
            val currentUser = getCurrentUserFromCookies()

            if (channelMatch != null && channelMatch != currentUser) {
                Log.d("TwitchBrowser", "Channel page detected, skipping script injection and stopping load")
                navigator.stopLoading()
                return@LaunchedEffect
            }

            try {
                // Inject granular scripts for browser mode
                val scripts = listOf(
                    "js/common/app_banners_remover.js",
                    "js/common/scroll_unlocker.js",
                    "js/common/splash_controller.js",
                    "js/common/browser_nav_injector.js"
                )
                
                scripts.forEach { path ->
                    val script = ScriptLoader.getScript(context, path)
                    if (script.isNotEmpty()) {
                        navigator.evaluateJavaScript(script)
                    }
                }
                Log.d("TwitchBrowser", "Browser scripts injected successfully")
            } catch (e: Exception) {
                Log.e("TwitchBrowser", "Error injecting scripts", e)
            }
        }
    }

    val androidInterface = remember { TwitchBrowserBridge(activity, onSettingsClick, onLoaded) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            navigator.reload()
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                com.multiplatform.webview.web.WebView(
                    modifier = Modifier.fillParentMaxSize(),
                    state = state,
                    navigator = navigator,
                    captureBackPresses = false,
                    onCreated = { webView ->
                        webViewRef = webView
                        webView.addJavascriptInterface(androidInterface, "TwitchBrowserBridge")
                        state.webSettings.apply {
                            isJavaScriptEnabled = true

                            androidWebSettings.apply {
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                            }
                        }

                        webView.apply {
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            overScrollMode = View.OVER_SCROLL_NEVER
                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = false
                            isNestedScrollingEnabled = true

                            // Enable fullscreen for videos
                            webChromeClient = WebChromeClient()

                            // Custom WebViewClient to intercept URL changes
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    Log.d("TwitchBrowser", "shouldOverrideUrlLoading: $url")

                                    // Force full reload for the global home to avoid SPA issues
                                    if (isGlobalHome(url)) {
                                        Log.d("TwitchBrowser", "Global home path detected in shouldOverride, forcing full load")
                                        view?.loadUrl(url)
                                        return true
                                    }

                                    // Detect if user navigated to a channel
                                    val channelMatch = extractChannelFromUrl(url)
                                    val currentUser = getCurrentUserFromCookies()

                                    if (channelMatch != null && channelMatch != currentUser) {
                                        // If we are coming from global home, stay in browser
                                        if (isGlobalHome(view?.url)) {
                                            Log.d("TwitchBrowser", "Channel detected from home in shouldOverride, staying in browser: $channelMatch")
                                            return false
                                        } else {
                                            Log.d("TwitchBrowser", "Channel detected in shouldOverride: $channelMatch. Triggering player.")
                                            // Stop loading immediately
                                            view?.stopLoading()
                                            // User clicked on a channel, trigger callback
                                            onChannelSelected(channelMatch)
                                            return true // Prevent navigation, we'll handle it
                                        }
                                    }

                                    return false // Allow normal navigation
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    Log.d("TwitchBrowser", "Page started: $url")

                                    // Inject splash controller early
                                    val splashScript = ScriptLoader.getScript(context, "js/common/splash_controller.js")
                                    if (splashScript.isNotEmpty()) {
                                        view?.evaluateJavascript(splashScript, null)
                                    }

                                    // Double-check if this is a channel URL
                                    url?.let {
                                        val channelMatch = extractChannelFromUrl(it)
                                        val currentUser = getCurrentUserFromCookies()

                                        if (channelMatch != null && channelMatch != currentUser && !isGlobalHome(lastCheckedUrl)) {
                                            Log.d("TwitchBrowser", "Channel URL detected in onPageStarted, stopping: $channelMatch")
                                            view?.stopLoading()
                                            onChannelSelected(channelMatch)
                                        }
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("TwitchBrowser", "Page finished: $url")
                                }
                            }

                            // Enable mixed content for Twitch
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                        }
                    }
                )
            }
        }
    }
}

private fun extractChannelFromUrl(url: String): String? {
    // Match ONLY the root channel URL: twitch.tv/channelname
    // Exclude sub-paths like /channelname/videos, /channelname/home, etc.
    // Also exclude reserved paths like /directory, /search

    val uri = try {
        val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
        java.net.URI(cleanUrl)
    } catch (e: Exception) {
        return null
    }

    val path = uri.path ?: return null
    val segments = path.split("/").filter { it.isNotEmpty() }

    // Channel root URLs have exactly one segment: /channelname
    if (segments.size != 1) {
        Log.d("TwitchBrowser", "extractChannelFromUrl - URL: $url, Channel: null (too many segments)")
        return null
    }

    val channelCandidate = segments[0]

    val excludedNames = listOf(
        "directory", "search", "videos", "clips", "events",
        "esports", "music", "about", "jobs", "security",
        "p", "settings", "subscriptions", "inventory", "wallet",
        "drops", "turbo", "friends", "popout", "embed", "home",
        "activity", "bits"
    )

    if (excludedNames.any { it.equals(channelCandidate, ignoreCase = true) }) {
        Log.d("TwitchBrowser", "extractChannelFromUrl - URL: $url, Channel: null (reserved name: $channelCandidate)")
        return null
    }

    Log.d("TwitchBrowser", "extractChannelFromUrl - URL: $url, Channel: $channelCandidate")
    return channelCandidate
}

private fun getCurrentUserFromCookies(): String? {
    return try {
        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://www.twitch.tv") ?: return null

        // The login cookie contains the username: login=username;
        val loginCookie = cookies.split(";").find { it.trim().startsWith("login=") }
        val username = loginCookie?.split("=")?.getOrNull(1)
        Log.d("TwitchBrowser", "Detected logged-in user: $username")
        username
    } catch (e: Exception) {
        Log.e("TwitchBrowser", "Error getting user from cookies", e)
        null
    }
}

private fun isGlobalHome(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false
    val uri = try { java.net.URI(url) } catch (e: Exception) { return false }
    val path = uri.path ?: "/"
    // Only /home and /home/ are considered "Exploration zones"
    // Root / is NOT home, so navigating from / to a user WILL trigger the player
    return path == "/home" || path == "/home/"
}

class TwitchBrowserBridge(
    private val activity: android.app.Activity?,
    private val onSettingsClick: () -> Unit,
    private val onLoaded: () -> Unit
) {
    @JavascriptInterface
    fun onDomLoaded() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "DOM Loaded via JS Bridge class")
            onLoaded()
        }
    }

    @JavascriptInterface
    fun openSettings() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "Settings button clicked in JS")
            onSettingsClick()
        }
    }
}
