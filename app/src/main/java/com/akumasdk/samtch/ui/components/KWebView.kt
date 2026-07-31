package com.akumasdk.samtch.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import java.io.ByteArrayInputStream

/**
 * KWebView is a self-contained component that behaves like a normal component.
 * It follows the requested signature and internally handles TV optimizations and script injection.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KWebView(
    modifier: Modifier? = null,
    url: String? = null,
    htmlContent: String? = null,
    enableJavaScript: Boolean = false,
    allowCookies: Boolean = false,
    enableDomStorageForAndroid: Boolean = false,
    isLoading: ((isLoading: Boolean) -> Unit)? = null,
    onUrlClicked: ((url: String) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier ?: Modifier,
        factory = { context ->
            NonInteractiveTVWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setBackgroundColor(android.graphics.Color.BLACK)

                settings.apply {
                    javaScriptEnabled = enableJavaScript
                    domStorageEnabled = enableDomStorageForAndroid
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = Constants.USER_AGENT
                    
                    // TV Optimizations
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                if (allowCookies) {
                    CookieManager.getInstance().setAcceptCookie(true)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        isLoading?.invoke(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        isLoading?.invoke(false)
                        if (url?.contains("twitch.tv") == true) {
                            injectScripts(view)
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val clickedUrl = request?.url?.toString() ?: ""
                        onUrlClicked?.invoke(clickedUrl)
                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: ""
                        val blockedDomains = listOf(
                            "amazon-adsystem.com",
                            "google-analytics.com",
                            "googletagmanager.com",
                            "pubads.g.doubleclick.net",
                            "ads.pubmatic.com",
                            "telemetry.twitch.tv",
                            "analytics.twitch.tv",
                            "spade.twitch.tv"
                        )
                        if (blockedDomains.any { reqUrl.contains(it) }) {
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                // TV Specific focus behavior
                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                
                setLongClickable(false)
                setOnTouchListener { _, _ -> true }

                if (url != null) {
                    loadUrl(url)
                } else if (htmlContent != null) {
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            }
        },
        update = { webView ->
            if (url != null && webView.url != url) {
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}

private fun injectScripts(view: WebView?) {
    val context = view?.context ?: return
    val vaft = ScriptLoader.getScript(context, "js/player/vaft.js")
    val cleaner = ScriptLoader.getScript(context, "js/player/ui_cleaner.js")
    
    if (vaft.isNotEmpty()) view.evaluateJavascript(vaft, null)
    if (cleaner.isNotEmpty()) view.evaluateJavascript(cleaner, null)
}

private class NonInteractiveTVWebView(context: android.content.Context) : WebView(context) {
    override fun onCheckIsTextEditor(): Boolean = false
    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? = null
    
    // Deny all focus requests to prevent the engine from ever being the primary focus target
    override fun requestFocus(direction: Int, previouslyFocusedRect: android.graphics.Rect?): Boolean = false
    
    @SuppressLint("MissingSuperCall")
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        // Do nothing, we don't want to handle focus
    }
}
