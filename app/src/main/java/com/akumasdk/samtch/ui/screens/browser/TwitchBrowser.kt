package com.akumasdk.samtch.ui.screens.browser

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView as NativeWebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("JavascriptInterface")
@Composable
fun TwitchBrowser(
    state: WebViewState,
    navigator: WebViewNavigator,
    isPlayerActive: Boolean,
    onChannelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0,
    hasBackgroundReloaded: Boolean = false,
    onBackgroundReloadFinished: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLoginRequested: () -> Unit = {},
    onRefreshRequested: () -> Unit = {},
    onLoaded: () -> Unit = {}
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val themeMode by SettingsManager.getThemeMode(context).collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    var webViewRef by remember { mutableStateOf<NativeWebView?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isUiLoading by remember { mutableStateOf(true) }
    
    // Custom history stack for safe exploration URLs
    val safeHistory = remember { mutableStateListOf<String>(Constants.Twitch.MOBILE_URL) }
    
    // Safety: ensure callbacks are always fresh without bridge recreation
    val currentOnChannelSelected by rememberUpdatedState(onChannelSelected)
    val currentOnSettingsClick by rememberUpdatedState(onSettingsClick)
    val currentOnLoginRequested by rememberUpdatedState(onLoginRequested)
    val currentOnRefreshRequested by rememberUpdatedState(onRefreshRequested)
    val currentOnLoaded by rememberUpdatedState(onLoaded)

    // Detect theme change and trigger reload to apply new twilight.theme
    LaunchedEffect(themeMode, isSystemInDarkTheme) {
        Log.d("TwitchBrowser", "Theme change detected (mode=$themeMode, dark=$isSystemInDarkTheme). Refreshing browser.")
        // If the player is active, the browser is managed by the isPlayerActive effect.
        // We trigger a reload on the navigator which will either refresh the active page
        // or the background pre-loaded page.
        navigator.reload()
    }

    // Handle back button
    BackHandler(enabled = !isPlayerActive) {
        Log.d("TwitchBrowser", "BackHandler triggered. safeHistory.size=${safeHistory.size}")
        if (safeHistory.size <= 1) {
            Log.d("TwitchBrowser", "At root of safe history, showing exit dialog")
            showExitDialog = true
        } else {
            // Pop the current page and load the previous one
            safeHistory.removeAt(safeHistory.size - 1)
            val previousUrl = safeHistory.last()
            Log.d("TwitchBrowser", "Navigating back to managed history: $previousUrl")
            navigator.loadUrl(previousUrl)
        }
    }

    // Handle Background Reload when player launches OR Manual Hard Refresh
    LaunchedEffect(isPlayerActive, refreshTrigger, hasBackgroundReloaded) {
        webViewRef?.let { webView ->
            val isManualRefresh = refreshTrigger > 0
            val shouldDoBackgroundReload = isPlayerActive && !hasBackgroundReloaded
            
            if (shouldDoBackgroundReload || isManualRefresh) {
                Log.d("TwitchBrowser", "Hard Refresh/Background Reload (Active=$isPlayerActive, Manual=$isManualRefresh, AlreadyDone=$hasBackgroundReloaded)")
                isUiLoading = true
                
                kotlinx.coroutines.yield()
                webView.stopLoading()
                webView.loadUrl(Constants.ABOUT_BLANK)
                webView.clearHistory()
                
                // Wait for the purge to settle
                delay(if (isPlayerActive) 1500.milliseconds else 1000.milliseconds)
                
                val targetUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                Log.d("TwitchBrowser", "Loading context: $targetUrl")
                webView.loadUrl(targetUrl)
                
                if (isPlayerActive && !hasBackgroundReloaded) {
                    onBackgroundReloadFinished()
                }
            } else if (!isPlayerActive) {
                // Restoration logic when returning to browser
                val restoreUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                val currentUrl = webView.url ?: ""
                
                Log.d("TwitchBrowser", "Restoring browser UI. current=$currentUrl, target=$restoreUrl")
                
                val isAlreadyLoaded = currentUrl.trimEnd('/') == restoreUrl.trimEnd('/')
                
                if (!isAlreadyLoaded || currentUrl == Constants.ABOUT_BLANK) {
                    isUiLoading = true
                    webView.loadUrl(restoreUrl)
                }
                
                // Safety timeout to ensure loading screen eventually goes away if cleaning script fails
                delay(5000.milliseconds)
                if (isUiLoading) isUiLoading = false
            }
        }
    }

    // Inject scripts when page is loaded (ONLY dialog closer)
    LaunchedEffect(state.loadingState, themeMode, isSystemInDarkTheme) {
        if (state.loadingState is LoadingState.Finished) {
            // Ensure splash screen is dismissed when loading completes
            currentOnLoaded()

            // Check if this is a channel URL - if so, don't inject scripts
            val currentUrl = state.lastLoadedUrl ?: ""
            val channelMatch = extractChannelFromUrl(currentUrl)
            val currentUser = getCurrentUserFromCookies()

            if (isPlayableChannel(channelMatch, currentUser)) {
                Log.d("TwitchBrowser", "Channel page detected, skipping script injection and stopping load")
                navigator.stopLoading()
                return@LaunchedEffect
            }

            // Inject theme
            val twitchTheme = when (themeMode) {
                SettingsManager.ThemeMode.DARK -> 1
                SettingsManager.ThemeMode.LIGHT -> 0
                SettingsManager.ThemeMode.SYSTEM -> if (isSystemInDarkTheme) 1 else 0
            }
            navigator.evaluateJavaScript("localStorage.setItem('twilight.theme', '$twitchTheme');")

            try {
                // Inject granular scripts for browser mode
                val scripts = listOf(
                    Constants.Scripts.COMMON_APP_BANNERS_REMOVER,
                    Constants.Scripts.COMMON_SCROLL_UNLOCKER,
                    Constants.Scripts.COMMON_SPLASH_CONTROLLER,
                    Constants.Scripts.COMMON_BROWSER_NAV_INJECTOR,
                    Constants.Scripts.COMMON_PULL_TO_REFRESH,
                    Constants.Scripts.COMMON_SPA_DETECTOR
                )
                
                scripts.forEach { path ->
                    val script = ScriptLoader.getScript(context, path)
                    if (script.isNotEmpty()) {
                        navigator.evaluateJavaScript(script)
                    }
                }
                Log.d("TwitchBrowser", "Browser scripts injected successfully")
            } catch (_: Exception) {
                Log.e("TwitchBrowser", "Error injecting scripts")
            }
        }
    }

    val androidInterface = remember {
        TwitchBrowserBridge(
            activity = activity,
            onSettingsClicked = { currentOnSettingsClick() },
            onLoginRequested = { currentOnLoginRequested() },
            onLoadedCallback = { currentOnLoaded() },
            onUiCleanFinished = { isUiLoading = false },
            onRefreshRequested = { 
                // Rely on the parent incrementing refreshTrigger to perform a Hard Refresh
                currentOnRefreshRequested()
            },
            onUrlChanged = { url, blocked, requestBack ->
                try {
                    val mobileUrl = ensureMobileUrl(url)
                    val channelMatch = extractChannelFromUrl(mobileUrl)
                    val isSafe = isSafeExplorationUrl(mobileUrl)

                    if (!isSafe || requestBack) {
                        Log.d("TwitchBrowser", "Sentinel: Unsafe or Escape detected. blocked=$blocked, url=$mobileUrl")
                        
                        // 1. Trigger the player (if it's a channel)
                        if (isPlayableChannel(channelMatch, getCurrentUserFromCookies())) {
                            currentOnChannelSelected(channelMatch!!)
                        }
                        
                        // 2. Redirection:
                        // If it WAS blocked in JS, the browser is already frozen on a safe page.
                        // If it was NOT blocked (leaked), we force a hard return to safety.
                        if (!blocked) {
                            val lastSafeUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                            Log.d("TwitchBrowser", "Navigation leaked, forcing hard return to $lastSafeUrl")
                            webViewRef?.loadUrl(lastSafeUrl)
                        }
                    } else {
                        // Track safe exploration URL in our custom stack
                        if (mobileUrl.isNotEmpty() && !mobileUrl.contains(Constants.ABOUT_BLANK)) {
                            if (safeHistory.lastOrNull() != mobileUrl) {
                                Log.d("TwitchBrowser", "Adding to safe history stack: $mobileUrl")
                                safeHistory.add(mobileUrl)
                                if (safeHistory.size > 20) safeHistory.removeAt(0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TwitchBrowser", "Error in bridge onUrlChange", e)
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        WebView(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            state = state,
            navigator = navigator,
            captureBackPresses = false,
            onCreated = { webView ->
                webViewRef = webView
                
                // Perform initial load or restore previous URL when returning from player.
                // "At All Costs" Guard: If the restored URL is a channel, force Home instead.
                val restoredUrl = state.lastLoadedUrl
                val channelMatch = restoredUrl?.let { extractChannelFromUrl(it) }
                val currentUser = getCurrentUserFromCookies()

                val urlToLoad = if (isPlayableChannel(channelMatch, currentUser)) {
                    Log.d("TwitchBrowser", "onCreated: Restored URL is a channel ($channelMatch). Forcing Home instead.")
                    Constants.Twitch.MOBILE_URL
                } else if (!restoredUrl.isNullOrEmpty()) {
                    restoredUrl
                } else {
                    Constants.Twitch.MOBILE_URL
                }

                if (webView.url == null || webView.url == Constants.ABOUT_BLANK) {
                    Log.d("TwitchBrowser", "onCreated: Initializing load of $urlToLoad")
                    webView.loadUrl(urlToLoad)
                }

                webView.addJavascriptInterface(androidInterface, Constants.Bridges.BROWSER)
                
                state.webSettings.apply {
                    isJavaScriptEnabled = true

                    androidWebSettings.apply {
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                    }
                }

                webView.apply {
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    overScrollMode = android.view.View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = false
                    
                    // Prevent onViewTypeAvailable crash by disabling Autofill
                    importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

                    // Enable fullscreen for videos
                    webChromeClient = WebChromeClient()

                    // Custom WebViewClient to intercept URL changes
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: NativeWebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            Log.d("TwitchBrowser", "shouldOverrideUrlLoading: $url")

                            // Force mobile domain
                            val mobileUrl = ensureMobileUrl(url)
                            if (mobileUrl != url) {
                                Log.d("TwitchBrowser", "Desktop URL detected, forcing mobile: $mobileUrl")
                                view?.loadUrl(mobileUrl)
                                return true
                            }

                            // Force full reload for the global home to avoid SPA issues
                            if (isGlobalHome(url)) {
                                Log.d("TwitchBrowser", "Global home path detected in shouldOverride, forcing full load")
                                view?.loadUrl(url)
                                return true
                            }

                            // Detect if user navigated to an unsafe page
                            val channelMatch = extractChannelFromUrl(url)
                            val isSafe = isSafeExplorationUrl(url)

                            if (!isSafe) {
                                val lastSafeUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                                Log.d("TwitchBrowser", "Intercepted unsafe URL: $url. Redirecting to $lastSafeUrl")
                                if (isPlayableChannel(channelMatch, getCurrentUserFromCookies())) {
                                    currentOnChannelSelected(channelMatch!!)
                                }
                                view?.loadUrl(lastSafeUrl)
                                return true
                            }

                            return false // Allow normal navigation
                        }

                        override fun onPageStarted(view: NativeWebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            Log.d("TwitchBrowser", "Page started: $url")
                            isUiLoading = true

                            // Inject splash controller early
                            val splashScript = ScriptLoader.getScript(context, Constants.Scripts.COMMON_SPLASH_CONTROLLER)
                            if (splashScript.isNotEmpty()) {
                                view?.evaluateJavascript(splashScript, null)
                            }

                            // Detect if a desktop URL leaked through and force mobile
                            url?.let {
                                val mobileUrl = ensureMobileUrl(it)
                                if (mobileUrl != it) {
                                    Log.d("TwitchBrowser", "onPageStarted: Desktop URL detected, forcing mobile: $mobileUrl")
                                    view?.loadUrl(mobileUrl)
                                    return
                                }
                            }

                            url?.let {
                                val channelMatch = extractChannelFromUrl(it)
                                val isSafe = isSafeExplorationUrl(it)

                                if (!isSafe) {
                                    val lastSafeUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                                    Log.d("TwitchBrowser", "Page started on unsafe URL: $it. Redirecting to $lastSafeUrl")
                                    if (isPlayableChannel(channelMatch, getCurrentUserFromCookies())) {
                                        currentOnChannelSelected(channelMatch!!)
                                    }
                                    view?.loadUrl(lastSafeUrl)
                                }
                            }
                        }

                        override fun onPageFinished(view: NativeWebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("TwitchBrowser", "Page finished: $url")
                            // Ensure splash screen dismisses even on restoration
                            currentOnLoaded()
                        }
                    }

                    // Enable mixed content for Twitch
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.userAgentString = Constants.UserAgents.MOBILE
                }
            }
        )

        // Loading Overlay
        AnimatedVisibility(
            visible = isUiLoading,
            enter = SamtchAnimation.FadeIn,
            exit = SamtchAnimation.FadeOut,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SamtchTheme.colors.rootBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SamtchTheme.colors.twitchPurple,
                    strokeWidth = 3.dp
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.exit_dialog_title)) },
            text = { Text(stringResource(R.string.exit_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.exit_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

private fun ensureMobileUrl(url: String): String {
    val uri = try { java.net.URI(url) } catch (_: Exception) { return url }
    if (uri.host == "www.twitch.tv" || uri.host == Constants.Twitch.DOMAIN) {
        return url.replaceFirst(uri.host ?: "", "m.twitch.tv")
    }
    return url
}

private fun isSafeExplorationUrl(url: String?): Boolean {
    if (url.isNullOrEmpty() || url.contains(Constants.ABOUT_BLANK)) return true
    
    val uri = try { java.net.URI(url) } catch (_: Exception) { return false }
    if (uri.host != null && !uri.host.contains(Constants.Twitch.DOMAIN)) return true

    val path = uri.path ?: "/"
    val segments = path.split("/").filter { it.isNotEmpty() }

    // Root/Home cases
    if (segments.isEmpty() || path == "/" || path == "/home" || path == "/home/") return true
    
    // Exploration zones
    val safeRoots = setOf(
        "directory", "search", "following", "browse", "p", 
        "settings", "inventory", "wallet", "drops", "turbo", 
        "friends", "activity", "bits", "about", "jobs", "security"
    )
    
    if (safeRoots.contains(segments[0].lowercase())) return true
    
    // Everything else (single segment usernames or /username/home) is "Unsafe"
    return false
}

private fun isPlayableChannel(channelMatch: String?, currentUser: String?): Boolean {
    if (channelMatch == null) return false
    if (currentUser == null) return true // If not logged in, all channels are playable
    
    // The current user should never trigger the player
    return !channelMatch.equals(currentUser, ignoreCase = true)
}

private fun extractChannelFromUrl(url: String?): String? {
    val uri = try {
        if (url == null) return null
        val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
        java.net.URI(cleanUrl)
    } catch (_: Exception) {
        return null
    }

    if (uri.host != null && !uri.host.contains(Constants.Twitch.DOMAIN)) return null

    val path = uri.path ?: return null
    val segments = path.split("/").filter { it.isNotEmpty() }

    if (segments.isEmpty()) return null

    val channelCandidate = segments[0].trim()

    val excludedNames = setOf(
        "directory", "search", "videos", "clips", "events",
        "esports", "music", "about", "jobs", "security",
        "p", "settings", "subscriptions", "inventory", "wallet",
        "drops", "turbo", "friends", "popout", "embed", "home",
        "activity", "bits", "browse", "following"
    )

    if (excludedNames.any { it.equals(channelCandidate, ignoreCase = true) }) {
        return null
    }

    return channelCandidate
}

private fun getCurrentUserFromCookies(): String? {
    return try {
        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookies = cookieManager.getCookie(Constants.Twitch.BASE_URL) ?: return null

        // The login cookie contains the username: login=username;
        val loginCookie = cookies.split(";").find { it.trim().startsWith("login=") }
        val username = loginCookie?.split("=")?.getOrNull(1)?.trim()?.lowercase()
        
        if (!username.isNullOrEmpty()) {
            Log.d("TwitchBrowser", "Detected logged-in user: $username")
            username
        } else null
    } catch (e: Exception) {
        Log.e("TwitchBrowser", "Error getting user from cookies", e)
        null
    }
}

private fun isGlobalHome(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false
    val uri = try { java.net.URI(url) } catch (_: Exception) { return false }
    val path = uri.path ?: "/"
    // Only /home and /home/ are considered "Exploration zones"
    // Root / is NOT home, so navigating from / to a user WILL trigger the player
    return path == "/home" || path == "/home/"
}

private fun isBrowserRoot(url: String?): Boolean {
    if (url.isNullOrEmpty()) return true
    val uri = try { java.net.URI(url) } catch (_: Exception) { return false }
    val path = uri.path ?: ""
    return path == "/" || path == "" || path == "/home" || path == "/home/"
}

class TwitchBrowserBridge(
    private val activity: android.app.Activity?,
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
            Log.d("TwitchBrowser", "URL change via JS Bridge: $url (blocked=$blocked, requestBack=$requestBack)")
            onUrlChanged(url, blocked, requestBack)
        }
    }

    @JavascriptInterface
    fun onDomLoaded() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "DOM Loaded via JS Bridge class")
            onLoadedCallback()
        }
    }

    @JavascriptInterface
    fun uiCleanFinish() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "UI cleaning finished via JS Bridge")
            onUiCleanFinished()
        }
    }

    @JavascriptInterface
    fun openSettings() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "Settings button clicked in JS")
            onSettingsClicked()
        }
    }

    @JavascriptInterface
    fun openLogin() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "Login button clicked in JS")
            onLoginRequested()
        }
    }

    @JavascriptInterface
    fun onRefresh() {
        activity?.runOnUiThread {
            Log.d("TwitchBrowser", "Refresh triggered via JS Bridge")
            onRefreshRequested()
        }
    }
}
