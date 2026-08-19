package com.akumasdk.samtch.ui.screens.browser

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.screens.browser.components.TwitchBrowserBridge
import com.akumasdk.samtch.ui.screens.browser.components.TwitchBrowserClient
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import com.akumasdk.samtch.util.TwitchUrlUtil
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import android.webkit.WebView as NativeWebView

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

    var lastProcessedRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }
    var wasPlayerActive by remember { mutableStateOf(isPlayerActive) }
    
    // Detect theme change and trigger reload to apply new twilight.theme
    LaunchedEffect(themeMode, isSystemInDarkTheme) {
        Log.d("TwitchBrowser", "Theme change detected (mode=$themeMode, dark=$isSystemInDarkTheme). Refreshing browser.")
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
    // 1. Handle Background Purge when player launches
    LaunchedEffect(isPlayerActive, hasBackgroundReloaded) {
        if (isPlayerActive && !hasBackgroundReloaded) {
            webViewRef?.let { webView ->
                Log.d("TwitchBrowser", "Executing background purge (Player Active, Not Reloaded)")
                isUiLoading = true
                
                webView.stopLoading()
                webView.loadUrl(Constants.ABOUT_BLANK)
                webView.clearHistory()
                
                delay(1500.milliseconds)
                
                val targetUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                Log.d("TwitchBrowser", "Loading background context: $targetUrl")
                webView.loadUrl(targetUrl)
                
                onBackgroundReloadFinished()
            }
        }
    }

    // 2. Handle Manual Hard Refresh (Incremental Trigger)
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > lastProcessedRefreshTrigger) {
            lastProcessedRefreshTrigger = refreshTrigger
            webViewRef?.let { webView ->
                Log.d("TwitchBrowser", "Executing Manual Hard Refresh (trigger=$refreshTrigger)")
                isUiLoading = true
                
                webView.stopLoading()
                webView.loadUrl(Constants.ABOUT_BLANK)
                webView.clearHistory()
                
                delay(1000.milliseconds)
                
                val targetUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                webView.loadUrl(targetUrl)
                
                if (isPlayerActive && !hasBackgroundReloaded) {
                    onBackgroundReloadFinished()
                }
            }
        }
    }

    // 3. Handle Restoration when returning to Browser
    LaunchedEffect(isPlayerActive) {
        if (!isPlayerActive && wasPlayerActive) {
            webViewRef?.let { webView ->
                val restoreUrl = safeHistory.lastOrNull() ?: Constants.Twitch.MOBILE_URL
                val currentUrl = webView.url ?: ""
                
                Log.d("TwitchBrowser", "Restoring browser UI from background. target=$restoreUrl")
                
                val isAlreadyLoaded = currentUrl.trimEnd('/') == restoreUrl.trimEnd('/')
                if (!isAlreadyLoaded || currentUrl == Constants.ABOUT_BLANK) {
                    isUiLoading = true
                    webView.loadUrl(restoreUrl)
                }
                
                delay(3000.milliseconds)
                if (isUiLoading) isUiLoading = false
            }
        }
        wasPlayerActive = isPlayerActive
    }

    // Inject scripts when page is loaded (ONLY dialog closer)
    LaunchedEffect(state.loadingState, themeMode, isSystemInDarkTheme) {
        if (state.loadingState is LoadingState.Finished) {
            // Ensure splash screen is dismissed when loading completes
            currentOnLoaded()

            // Check if this is a channel URL - if so, don't inject scripts
            val currentUrl = state.lastLoadedUrl ?: ""
            val channelMatch = TwitchUrlUtil.extractChannelFromUrl(currentUrl)
            val currentUser = TwitchUrlUtil.getCurrentUser(context)

            if (TwitchUrlUtil.isPlayableChannel(channelMatch, currentUser)) {
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
                    val mobileUrl = TwitchUrlUtil.ensureMobileUrl(url)
                    val channelMatch = TwitchUrlUtil.extractChannelFromUrl(mobileUrl)
                    val isSafe = TwitchUrlUtil.isSafeExplorationUrl(mobileUrl)

                    if (!isSafe || requestBack) {
                        Log.d("TwitchBrowser", "Sentinel: Unsafe or Escape detected. blocked=$blocked, url=$mobileUrl")
                        
                        // 1. Trigger the player (if it's a channel)
                        if (TwitchUrlUtil.isPlayableChannel(channelMatch, TwitchUrlUtil.getCurrentUser(context))) {
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
                val channelMatch = restoredUrl?.let { TwitchUrlUtil.extractChannelFromUrl(it) }
                val currentUser = TwitchUrlUtil.getCurrentUser(context)

                val urlToLoad = if (TwitchUrlUtil.isPlayableChannel(channelMatch, currentUser)) {
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
                    webViewClient = TwitchBrowserClient(
                        context = context,
                        safeHistory = safeHistory,
                        onChannelSelected = { currentOnChannelSelected(it) },
                        onUiLoadingChanged = { isUiLoading = it },
                        onLoaded = { currentOnLoaded() }
                    )

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
