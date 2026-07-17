package com.akumasdk.samtch.ui.screens.settings

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SuppressLint("JavascriptInterface")
@Composable
fun BttvSettingsChat(
    modifier: Modifier = Modifier
) {
    // Aggressive URL parameters to prevent mobile redirection
    val targetUrl = "https://www.twitch.tv/directory?desktop-redirect=true&no-mobile-redirect=true"
    
    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isReady by remember { mutableStateOf(false) }
    var webViewConfigured by remember { mutableStateOf(false) }

    // Safety timeout
    LaunchedEffect(Unit) {
        delay(30.seconds)
        if (!isReady) {
            Log.w("BttvSettingsChat", "Safety timeout reached")
            isReady = true
        }
    }

    LaunchedEffect(webViewConfigured) {
        if (webViewConfigured) {
            navigator.loadUrl(targetUrl)
        }
    }

    val bttvBridge = remember(coroutineScope) {
        BttvSettingsChatBridge(
            onComplete = {
                coroutineScope.launch {
                    Log.d("BttvSettingsChat", "Automation complete signal received")
                    delay(500)
                    isReady = true
                }
            }
        )
    }

    // Simplified automation script
    LaunchedEffect(state.loadingState, webViewConfigured) {
        val loadingState = state.loadingState
        val shouldAttempt = loadingState is LoadingState.Finished || 
                         (loadingState is LoadingState.Loading && loadingState.progress > 0.6f)
                         
        if (shouldAttempt && !isReady && webViewConfigured) {
            try {
                val bttvScript = ScriptLoader.getScript(context, "js/chat/bttv.js")
                if (bttvScript.isEmpty()) return@LaunchedEffect

                val automationScript = """
                    (function() {
                        if (window.samtch_automation_running) return;
                        window.samtch_automation_running = true;
                        console.log('[Samtch] BttvSettingsChat automation active');
                        
                        // 0. Aggressive fingerprinting override to hide mobile identity
                        try {
                            Object.defineProperty(navigator, 'platform', { get: function () { return 'Win32'; } });
                            Object.defineProperty(navigator, 'maxTouchPoints', { get: function () { return 0; } });
                        } catch (e) { console.error('Fingerprint override failed', e); }

                        function notifyAndroid() {
                            if (window.BttvSettingsBridge) {
                                window.BttvSettingsBridge.onAutomationComplete();
                            }
                        }

                        // Inject BetterTTV
                        $bttvScript

                        // Wait for the official BetterTTV button to be added to the DOM and click it
                        let waitLogged = false;
                        function findAndClick() {
                            const btn = document.querySelector('[data-a-target="betterttv-settings-button"]');
                            if (btn) {
                                console.log('[Samtch] Official BTTV button found, clicking...');
                                btn.click();
                                
                                // Show success overlay
                                const overlay = document.createElement('div');
                                overlay.style.cssText = "position:fixed;top:0;left:0;width:100%;height:100%;background:white;display:flex;align-items:center;justify-content:center;z-index:9998;color:black;font-size:22px;font-weight:bold;text-align:center;font-family:sans-serif;";
                                overlay.innerText = "Go back and try again";
                                document.body.appendChild(overlay);

                                setTimeout(notifyAndroid, 1000);
                            } else {
                                if (!waitLogged) {
                                    console.log('[Samtch] Waiting for BTTV settings button...');
                                    waitLogged = true;
                                }
                                setTimeout(findAndClick, 1000);
                            }
                        }
                        
                        findAndClick();
                    })();
                """.trimIndent()

                navigator.evaluateJavaScript(automationScript)
                
            } catch (e: Exception) {
                Log.e("BttvSettingsChat", "Injection error", e)
                isReady = true 
            }
        }
    }

    Box(modifier = modifier) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = navigator,
            captureBackPresses = false,
            onCreated = { webView ->
                state.webSettings.apply {
                    isJavaScriptEnabled = true
                    androidWebSettings.apply {
                        domStorageEnabled = true
                    }
                }
                webView.apply {
                    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    
                    // Native settings for desktop rendering
                    settings.userAgentString = desktopUserAgent
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    
                    overScrollMode = android.view.View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    addJavascriptInterface(bttvBridge, "BttvSettingsBridge")

                    // Clear cookies to avoid being tagged as mobile from previous sessions
                    try {
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.removeSessionCookies(null)
                    } catch (e: Exception) {
                        Log.e("BttvSettingsChat", "Error clearing session cookies", e)
                    }

                    // Intercept and prevent mobile redirection
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.contains("m.twitch.tv")) {
                                Log.d("BttvSettingsChat", "Blocking mobile redirect to: $url")
                                val desktopUrl = url.replace("m.twitch.tv", "www.twitch.tv")
                                    .let { if (!it.contains("desktop-redirect")) "$it${if (it.contains("?")) "&" else "?"}desktop-redirect=true" else it }
                                view?.loadUrl(desktopUrl)
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            // Double-check User Agent on page start
                            view?.settings?.userAgentString = desktopUserAgent
                        }
                    }
                }
                webViewConfigured = true
            }
        )

        if (!isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF18181B)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

class BttvSettingsChatBridge(
    private val onComplete: () -> Unit
) {
    @JavascriptInterface
    fun onAutomationComplete() {
        onComplete()
    }
}
