package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.components.StreamMetadataBar
import com.akumasdk.samtch.ui.components.TwitchChat
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FullscreenPlayer(
    channel: String,
    displayName: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var isChatVisible by remember { mutableStateOf(false) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var showTooltip by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000.milliseconds)
        showTooltip = false
    }

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
            webView(Modifier.fillMaxSize()) {
                isChatVisible = !isChatVisible
            }

            // Double tap hint tooltip
            TapTooltip(
                visible = showTooltip,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Optional Side Chat with Metadata Bar
        if (isChatVisible) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF18181B))
            ) {
                // Metadata space above chat (Only visible when chat is open)
                AnimatedVisibility(
                    visible = !streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    StreamMetadataBar(
                        channel = channel,
                        displayName = displayName,
                        streamTitle = streamTitle,
                        gameName = gameName,
                        viewersCount = viewersCount,
                        modifier = Modifier.padding(horizontal = 4.dp) // Subtle extra padding for side panel
                    )
                }

                TwitchChat(
                    channel = channel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF18181B))
                )
            }
        }
    }
}

@Composable
private fun TapTooltip(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.fullscreen_double_tap_hint),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
