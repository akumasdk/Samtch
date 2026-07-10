package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

@Composable
fun PortraitPlayer(
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Video at top (16:9 aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            WebViewContainer(
                modifier = Modifier.fillMaxSize(),
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen
            )
        }

        // Twitch Chat
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF18181B)) // Twitch background color
        ) {
            TwitchChat(
                channel = channel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
