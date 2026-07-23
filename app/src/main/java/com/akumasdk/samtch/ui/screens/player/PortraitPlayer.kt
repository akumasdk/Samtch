package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.components.TwitchChat
import kotlin.math.abs

@Composable
fun PortraitPlayer(
    channel: String,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    isAudioOnly: Boolean = false,
    onToggleFullscreen: () -> Unit,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var isChatVisible by remember { mutableStateOf(true) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .animateContentSize(),
        verticalArrangement = if (isChatVisible) Arrangement.Top else Arrangement.Center
    ) {
        // Dynamic height container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isAudioOnly) {
                        Modifier.height(240.dp) // Reclaimed space for Audio Only table design
                    } else {
                        Modifier.aspectRatio(16f / 9f)
                    }
                )
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

        // Tiny metadata space above chat
        AnimatedVisibility(
            visible = isChatVisible && !isAudioOnly && (!streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty()),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                color = Color(0xFF1F1F23), // Twitch dark gray
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (!streamTitle.isNullOrEmpty()) {
                        Text(
                            text = streamTitle,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                    }
                    
                    if (!gameName.isNullOrEmpty()) {
                        Text(
                            text = " • ",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = gameName,
                            color = Color(0xFFBF94FF), // Twitch light purple
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (viewersCount > 0) {
                        Text(
                            text = " • ",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                            )
                            Text(
                                text = formatViewerCount(viewersCount),
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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

private fun formatViewerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000 -> "%.1fk".format(count / 1_000f)
        else -> count.toString()
    }
}
