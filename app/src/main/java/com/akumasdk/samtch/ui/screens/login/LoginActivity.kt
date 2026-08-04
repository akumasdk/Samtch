package com.akumasdk.samtch.ui.screens.login

import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator

class LoginActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            val themeMode by SettingsManager.getThemeMode(this).collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
            val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            
            val darkTheme = when (themeMode) {
                SettingsManager.ThemeMode.DARK -> true
                SettingsManager.ThemeMode.LIGHT -> false
                SettingsManager.ThemeMode.SYSTEM -> isSystemInDarkTheme
            }

            // Start with blank to avoid early load with wrong settings
            val state = rememberSaveableWebViewState(Constants.ABOUT_BLANK)
            val navigator = rememberWebViewNavigator()

            // Login Detection Logic
            LaunchedEffect(state.loadingState) {
                if (state.loadingState is LoadingState.Finished) {
                    val cookies = CookieManager.getInstance().getCookie(Constants.Twitch.BASE_URL)
                    if (cookies?.contains("login=") == true) {
                        Log.d("LoginActivity", "Login success. Closing.")
                        setResult(RESULT_OK)
                        finish()
                    }
                    
                    // Inject theme
                    val twitchTheme = if (darkTheme) 1 else 0
                    state.nativeWebView.evaluateJavascript("localStorage.setItem('twilight.theme', '$twitchTheme');", null)
                }
            }

            SamtchTheme(darkTheme = darkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Twitch Login", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        WebView(
                            state = state,
                            navigator = navigator,
                            modifier = Modifier.fillMaxSize(),
                            captureBackPresses = false,
                            onCreated = { webView ->
                                // Apply exact same settings pattern from TwitchBrowser.kt
                                state.webSettings.apply {
                                    isJavaScriptEnabled = true
                                    androidWebSettings.apply {
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        allowFileAccess = true
                                    }
                                }

                                webView.apply {
                                    webChromeClient = WebChromeClient()
                                    
                                    // Prevent onViewTypeAvailable crash by disabling Autofill
                                    importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

                                    // Set a robust WebViewClient for error logging
                                    webViewClient = object : WebViewClient() {
                                        override fun onReceivedError(view: android.webkit.WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            super.onReceivedError(view, request, error)
                                            Log.e("LoginActivity", "WebView Error: ${error?.description} at ${request?.url}")
                                        }

                                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: WebResourceRequest?): Boolean {
                                            return false // Allow all redirects
                                        }
                                    }

                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.userAgentString = Constants.UserAgents.MOBILE
                                    
                                    // Trigger load only after settings are established
                                    if (webView.url == null || webView.url == Constants.ABOUT_BLANK) {
                                        Log.d("LoginActivity", "Settings ready. Loading login page.")
                                        webView.loadUrl("https://www.twitch.tv/login")
                                    }
                                }
                            }
                        )

                        // Loading Overlay
                        val isLoading = state.loadingState is LoadingState.Loading
                        AnimatedVisibility(
                            visible = isLoading,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
