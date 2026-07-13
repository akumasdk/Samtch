package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun PortraitPlayer(
    channel: String,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var isChatVisible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = if (isChatVisible) Arrangement.Top else Arrangement.Center
    ) {
        // Video at top (16:9 aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            webView(Modifier.fillMaxSize()) {
                isChatVisible = !isChatVisible
            }
        }

        // Twitch Chat
        if (isChatVisible) {
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
}
