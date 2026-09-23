package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.components.metadata.util.cleanMetadataText
import com.akumasdk.samtch.ui.components.metadata.util.formatStreamDuration

@Composable
internal fun StandardMetadataBar(
    channel: String,
    displayName: String?,
    avatarUrl: String?,
    streamTitle: String?,
    gameName: String?,
    viewersCount: Int,
    streamStartedAt: String?,
    maxWidth: Dp = 400.dp
) {
    val cleanedTitle = remember(streamTitle) { cleanMetadataText(streamTitle).ifEmpty { "Stream Offline" } }
    val cleanedGame = remember(gameName) { cleanMetadataText(gameName) }
    val cleanedName = remember(displayName, channel) { cleanMetadataText(displayName ?: channel) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
                        Text(
                            text = cleanedName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Multi-row Info Block centered with safe vertical spacing
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            // ROW 1: Name & Uptime
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cleanedName,
                    color = SamtchTheme.colors.accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                val duration = formatStreamDuration(streamStartedAt)
                if (duration.isNotEmpty() && maxWidth >= 260.dp) {
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
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }

            // ROW 2: Stream Title
            Text(
                text = cleanedTitle,
                color = SamtchTheme.colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                if (cleanedGame.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = SamtchTheme.colors.accentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cleanedGame,
                            color = SamtchTheme.colors.accentColor.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (viewersCount > 0 && maxWidth >= 200.dp) {
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
