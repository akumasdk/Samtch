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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

@Composable
fun PortraitPlayer(
    channel: String,
    onToggleFullscreen: () -> Unit,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var isChatVisible by remember { mutableStateOf(true) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

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
                .onSizeChanged { size ->
                    playerSize = size
                }
                .pointerInput(Unit) {
                    var lastTapTime = 0L
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) {
                                val currentTime = event.changes.first().uptimeMillis
                                val isDoubleTap =
                                    (currentTime - lastTapTime) < viewConfiguration.doubleTapTimeoutMillis

                                if (isDoubleTap) {
                                    val position = event.changes.first().position
                                    val centerX = playerSize.width / 2f
                                    val centerY = playerSize.height / 2f

                                    // Define central region (30% width and height from center)
                                    val radiusX = playerSize.width * 0.15f
                                    val radiusY = playerSize.height * 0.15f

                                    val isInCenterZone =
                                        abs(position.x - centerX) <= radiusX &&
                                                abs(position.y - centerY) <= radiusY

                                    if (isInCenterZone) {
                                        onToggleFullscreen()
                                        // Consume the second tap to prevent WebView from seeing it
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                                lastTapTime = currentTime
                            }
                        }
                    }
                }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF18181B))
                )
            }
        }
    }
}
