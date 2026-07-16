package com.akumasdk.samtch.ui.screens.settings

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
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

@SuppressLint("JavascriptInterface")
@Composable
fun BttvSettingsChat(
    modifier: Modifier = Modifier
) {
    val chatUrl = "https://www.twitch.tv/embed/twitch/chat?parent=twitch.tv&darkpopout"
    
    // Start with empty URL to ensure we can configure the WebView before loading starts
    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Track if chat and BTTV settings are fully open
    var isReady by remember { mutableStateOf(false) }
    var webViewConfigured by remember { mutableStateOf(false) }

    // Safety timeout: if automation doesn't complete in 12 seconds, show the chat anyway
    LaunchedEffect(Unit) {
        delay(12000)
        if (!isReady) {
            Log.w("BttvSettingsChat", "Safety timeout reached, forcing ready state")
            isReady = true
        }
    }

    // Load the URL only after the WebView has been created and configured
    LaunchedEffect(webViewConfigured) {
        if (webViewConfigured) {
            Log.d("BttvSettingsChat", "WebView configured, loading chat URL")
            navigator.loadUrl(chatUrl)
        }
    }

    val bttvBridge = remember(coroutineScope) {
        BttvSettingsChatBridge(
            onComplete = {
                coroutineScope.launch {
                    Log.d("BttvSettingsChat", "Bridge received completion signal, waiting a second...")
                    delay(500)
                    isReady = true
                }
            }
        )
    }

    // Inject automation script
    LaunchedEffect(state.loadingState, webViewConfigured) {
        val loadingState = state.loadingState
        
        // Try to inject as soon as there's some progress or it's finished
        val shouldAttempt = loadingState is LoadingState.Finished || 
                         (loadingState is LoadingState.Loading && loadingState.progress > 0.6f)
                         
        if (shouldAttempt && !isReady && webViewConfigured) {
            try {
                // 1. Inject BTTV script
                val bttvScript = ScriptLoader.getScript(context, "js/chat/bttv.js")
                if (bttvScript.isNotEmpty()) {
                    navigator.evaluateJavaScript(bttvScript)
                }

                // 2. Inject automation sequence
                val automationScript = """
                    (function() {
                        if (window.samtch_automation_running) return;
                        window.samtch_automation_running = true;
                        console.log('[Samtch] BTTV Automation started');
                        
                        function notifyAndroid() {
                            if (window.BttvSettingsBridge) {
                                window.BttvSettingsBridge.onAutomationComplete();
                            }
                        }

                        function startAutomation() {
                            const twitchSettingsBtn = document.querySelector('[data-a-target="chat-settings"]') ||
                                                      document.querySelector('.chat-settings button');
                                                      
                            if (twitchSettingsBtn) {
                                console.log('[Samtch] Clicking Twitch settings');
                                twitchSettingsBtn.click();
                                
                                let attempts = 0;
                                const bttvInterval = setInterval(() => {
                                    const bttvBtn = document.querySelector('.openSettings') || 
                                                    document.querySelector('.bttv-settings-button');
                                                    
                                    if (bttvBtn) {
                                        console.log('[Samtch] Clicking BTTV settings');
                                        bttvBtn.click();
                                        
                                        // Notify Android that everything is ready
                                        notifyAndroid();
                                        
                                        clearInterval(bttvInterval);
                                    } else if (attempts++ > 40) {
                                        clearInterval(bttvInterval);
                                        notifyAndroid(); 
                                    }
                                }, 250);
                            } else {
                                setTimeout(startAutomation, 1000);
                            }
                        }
                        
                        startAutomation();
                    })();
                """.trimIndent()
                
                navigator.evaluateJavaScript(automationScript)
            } catch (e: Exception) {
                Log.e("BttvSettingsChat", "Injection error", e)
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
                Log.d("BttvSettingsChat", "WebView onCreated")
                state.webSettings.apply {
                    isJavaScriptEnabled = true
                    androidWebSettings.apply {
                        domStorageEnabled = true
                    }
                }
                webView.apply {
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    addJavascriptInterface(bttvBridge, "BttvSettingsBridge")
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
