package com.akumasdk.samtch.ui.screens.login

import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.akumasdk.samtch.data.api.helix.HelixApi
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.launch
import android.webkit.JavascriptInterface

import kotlin.time.Duration.Companion.milliseconds

class LoginActivity : ComponentActivity() {
    @Volatile
    private var isDismissing = false
    private var webViewInstance: android.webkit.WebView? = null

    inner class LoginBridge {
        @JavascriptInterface
        fun onLoginSuccess() {
            Log.d("LoginBridge", "Login success signal received from JS. isDismissing=$isDismissing")
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                
                if (!isDismissing) {
                    Log.d("LoginBridge", "Login success received but not dismissing yet. Forcing redirect check.")
                    webViewInstance?.url?.let { handleRedirect(it) }
                } else {
                    // If we are already dismissing, it means validation is in progress.
                    // We'll give it a moment and then force close if it's still stuck.
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(500.milliseconds)
                        if (!isFinishing) {
                            Log.d("LoginBridge", "Forcing activity finish via bridge fallback")
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val scopes = listOf(
            "chat:read",
            "chat:edit",
            "user:read:email",
            "user:read:follows",
            "channel:read:redemptions",
            "moderator:read:chatters"
        )
        val loginUrl = Constants.Twitch.Api.AUTH_BASE +
                "&client_id=${Constants.Twitch.LOGIN_CLIENT_ID}" +
                "&redirect_uri=${Constants.Twitch.REDIRECT_URL}" +
                "&scope=${scopes.joinToString("+")}" +
                "&force_verify=true"

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
                                webViewInstance = webView
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
                                    
                                    addJavascriptInterface(LoginBridge(), "LoginBridge")

                                    // Prevent onViewTypeAvailable crash by disabling Autofill
                                    importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

                                    // Set a robust WebViewClient for error logging
                                    webViewClient = object : WebViewClient() {
                                        override fun onReceivedError(view: android.webkit.WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            super.onReceivedError(view, request, error)
                                            Log.e("LoginActivity", "WebView Error: ${error?.description} at ${request?.url}")
                                        }

                                        override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            url?.let {
                                                if (it.startsWith(Constants.Twitch.REDIRECT_URL)) {
                                                    handleRedirect(it)
                                                }
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            if (url.startsWith(Constants.Twitch.REDIRECT_URL)) {
                                                handleRedirect(url)
                                                return true
                                            }
                                            return false
                                        }
                                    }

                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.userAgentString = Constants.UserAgents.MOBILE
                                    
                                    // Trigger load only after settings are established
                                    if (webView.url == null || webView.url == Constants.ABOUT_BLANK) {
                                        Log.d("LoginActivity", "Settings ready. Loading login page.")
                                        webView.loadUrl(loginUrl)
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

    private fun handleRedirect(url: String) {
        if (isDismissing) return
        
        Log.d("LoginActivity", "Redirect intercepted: $url")
        
        // 1. Check for errors in query parameters
        val uri = try { url.toUri() } catch (_: Exception) { null }
        val error = uri?.getQueryParameter("error")
        val errorDesc = uri?.getQueryParameter("error_description")
        
        if (error != null) {
            Log.e("LoginActivity", "OAuth Error: $error - $errorDesc")
            // Optionally show a toast or dialog here
            return
        }

        // 2. Check for access token in fragment
        val fragment = url.substringAfter("#", "")
        if (fragment.isEmpty()) return

        val params = fragment.split("&").associate {
            val pair = it.split("=")
            pair[0] to pair.getOrNull(1)
        }

        val accessToken = params["access_token"]
        if (accessToken != null) {
            isDismissing = true
            Log.d("LoginActivity", "Found access token. Validating...")
            lifecycleScope.launch {
                try {
                    val validateResponse = HelixApi.validateToken(accessToken)
                    if (validateResponse != null) {
                        val userId = validateResponse.user_id
                        val userName = validateResponse.login

                        if (userId != null && userName != null) {
                            Log.d("LoginActivity", "Token valid for user: $userName ($userId)")
                            SettingsManager.setAuthData(
                                context = this@LoginActivity,
                                token = accessToken,
                                clientId = Constants.Twitch.LOGIN_CLIENT_ID,
                                userName = userName,
                                userId = userId,
                                isLoggedIn = true
                            )
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Log.e("LoginActivity", "Validation response missing required fields")
                            isDismissing = false
                        }
                    } else {
                        Log.e("LoginActivity", "Token validation failed")
                        isDismissing = false
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error during handleRedirect", e)
                    isDismissing = false
                }
            }
        }
    }

    override fun onDestroy() {
        webViewInstance?.removeJavascriptInterface("LoginBridge")
        webViewInstance = null
        super.onDestroy()
    }
}
