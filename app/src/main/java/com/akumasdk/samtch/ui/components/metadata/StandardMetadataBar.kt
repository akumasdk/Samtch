package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.metadata.formatStreamDuration

@Composable
internal fun StandardMetadataBar(
    channel: String,
    displayName: String?,
    avatarUrl: String?,
    streamTitle: String?,
    gameName: String?,
    viewersCount: Int,
    streamStartedAt: String?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar Section
        Box(modifier = Modifier.size(46.dp)) {
            if (!avatarUrl.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            color = SamtchTheme.colors.accentColor,
                            strokeWidth = 2.dp
                        )
                    }
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = SamtchTheme.colors.twitchPurple,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White.copy(alpha = 0.7f),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = (displayName ?: channel).take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Multi-row Info Block
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            // ROW 1: Name & Uptime
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayName ?: channel,
                    color = SamtchTheme.colors.accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    modifier = Modifier.weight(1f)
                )

                val duration = formatStreamDuration(streamStartedAt)
                if (duration.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SamtchTheme.colors.liveDot, CircleShape)
                        )
                        Text(
                            text = duration,
                            color = SamtchTheme.colors.secondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }

            // ROW 2: Stream Title (Main Focus)
            Text(
                text = streamTitle ?: "Stream Offline",
                color = SamtchTheme.colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 16.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )

            // ROW 3: Game Category & Viewers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!gameName.isNullOrEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = SamtchTheme.colors.accentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp) // Matched size to text optics
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = gameName,
                        color = SamtchTheme.colors.accentColor.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (viewersCount > 0) {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        AnimatedViewerCount(
                            count = viewersCount,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
