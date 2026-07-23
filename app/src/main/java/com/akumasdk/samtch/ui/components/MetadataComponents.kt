package com.akumasdk.samtch.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Formats large viewer counts into human-readable strings (e.g., 1.2k, 1.5M).
 */
fun formatViewerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000 -> "%.1fk".format(count / 1_000f)
        else -> count.toString()
    }
}

/**
 * A viewer count component with a red dot and a vertical scroll animation when the value updates.
 */
@Composable
fun AnimatedViewerCount(
    count: Int,
    textColor: Color = Color.LightGray,
    fontSize: TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Black,
    dotSize: Dp = 5.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Red Live Dot
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(Color.Red, CircleShape)
        )
        
        // Animated text content
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                if (targetState > initialState) {
                    // Scroll up
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                } else {
                    // Scroll down
                    (slideInVertically { height -> -height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> height } + fadeOut())
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "ViewerCountAnimation"
        ) { targetCount ->
            Text(
                text = formatViewerCount(targetCount),
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1
            )
        }
    }
}

/**
 * A unified metadata bar for displaying streamer name, title, category and viewers.
 */
@Composable
fun StreamMetadataBar(
    channel: String,
    displayName: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
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
            // Streamer Name
            Text(
                text = displayName ?: channel,
                color = Color(0xFFBF94FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                text = " • ",
                color = Color.Gray,
                fontSize = 12.sp
            )

            // Flexible title (Marquee with Animation)
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = streamTitle ?: "",
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                    },
                    label = "StreamTitleAnimation"
                ) { targetTitle ->
                    if (targetTitle.isNotEmpty()) {
                        Text(
                            text = targetTitle,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .basicMarquee()
                        )
                    } else {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Fixed Category/Viewer info on the right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = " • ",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                AnimatedContent(
                    targetState = gameName ?: "",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "GameNameAnimation"
                ) { targetGame ->
                    if (targetGame.isNotEmpty()) {
                        Text(
                            text = targetGame,
                            color = Color(0xFFBF94FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                    }
                }

                if (viewersCount > 0) {
                    Text(
                        text = "  ",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    AnimatedViewerCount(
                        count = viewersCount,
                        fontSize = 10.sp,
                        dotSize = 5.dp
                    )
                }
            }
        }
    }
}
