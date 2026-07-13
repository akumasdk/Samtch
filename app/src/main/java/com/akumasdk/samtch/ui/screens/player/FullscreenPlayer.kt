package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlin.math.abs

@Composable
fun FullscreenPlayer(
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit
) {
    var isChatVisible by remember { mutableStateOf(false) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Video Player
        Box(
            modifier = Modifier
                .weight(1f)
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
                                val isDoubleTap = (currentTime - lastTapTime) < viewConfiguration.doubleTapTimeoutMillis

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
                                        isChatVisible = !isChatVisible
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
