package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView as NativeWebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

@Composable
fun WebViewContainer(
    modifier: Modifier,
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit = {},
    onToggleAudioOnly: () -> Unit = {},
    onMetadataDetected: (String, String) -> Unit = { _, _ -> }
) {
    // Ensure the bridge always uses the latest lambdas from the current composition context
    val currentOnToggleFullscreen by rememberUpdatedState(onToggleFullscreen)
    val currentOnToggleChat by rememberUpdatedState(onToggleChat)
    val currentOnToggleAudioOnly by rememberUpdatedState(onToggleAudioOnly)
    val currentOnMetadataDetected by rememberUpdatedState(onMetadataDetected)

    WebView(
        modifier = modifier,
        state = state,
        navigator = navigator,
        captureBackPresses = false,
        factory = { param ->
            NativeWebView(param.context)
        },
        onCreated = { webView ->
            Log.d("TwitchPlayer", "WebView created for channel: $channel")

            // Prevent the renderer process from being killed when hidden
            webView.setRendererPriorityPolicy(NativeWebView.RENDERER_PRIORITY_BOUND, false)

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
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                // Add bridge for fullscreen and chat using the dedicated class
                addJavascriptInterface(
                    TwitchPlayerBridge(
                        onToggleFullscreen = {
                            post { currentOnToggleFullscreen() }
                        },
                        onToggleChat = {
                            post { currentOnToggleChat() }
                        },
                        onToggleAudioOnly = {
                            post { currentOnToggleAudioOnly() }
                        },
                        onMetadataDetected = { avatarUrl, subtitle ->
                            post { currentOnMetadataDetected(avatarUrl, subtitle) }
                        }
                    ),
                    "TwitchPlayerBridge"
                )

                // Enable fullscreen for videos
                webChromeClient = WebChromeClient()

                // Enable mixed content for Twitch
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
    )
}

fun createTwitchPlayerUrl(channel: String): String {
    return "https://player.twitch.tv/?channel=$channel&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
}

@Composable
fun AudioOnlyPlayer(
    channel: String,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onCloseAudioOnly: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF18181B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = channel,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Audio Only Mode",
                color = Color.LightGray,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(
                onClick = onTogglePlayback,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        IconButton(
            onClick = onCloseAudioOnly,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow, // Replace with close icon if available, using play as placeholder for now
                contentDescription = "Return to Video",
                tint = Color.White
            )
        }
    }
}

class TwitchPlayerBridge(
    private val onToggleFullscreen: () -> Unit,
    private val onToggleChat: () -> Unit = {},
    private val onToggleAudioOnly: () -> Unit = {},
    private val onMetadataDetected: (String, String) -> Unit = { _, _ -> }
) {
    @JavascriptInterface
    fun toggleFullscreen() {
        onToggleFullscreen()
    }

    @JavascriptInterface
    fun toggleChat() {
        onToggleChat()
    }

    @JavascriptInterface
    fun toggleAudioOnly() {
        onToggleAudioOnly()
    }

    @JavascriptInterface
    fun updateMetadata(avatarUrl: String, subtitle: String) {
        onMetadataDetected(avatarUrl, subtitle)
    }
}
