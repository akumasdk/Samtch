package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

@Composable
fun FullscreenPlayer(
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit
) {
    var isChatVisible by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Video Player
        Box(modifier = Modifier.weight(1f)) {
            WebViewContainer(
                modifier = Modifier.fillMaxSize(),
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen,
                onToggleChat = { isChatVisible = !isChatVisible }
            )
        }

        // Optional Side Chat
        if (isChatVisible) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF18181B))
            ) {
                TwitchChat(
                    channel = channel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
