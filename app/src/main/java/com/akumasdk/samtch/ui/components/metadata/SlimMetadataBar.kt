package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.components.metadata.util.formatStreamDuration

@Composable
internal fun SlimMetadataBar(
    channel: String,
    displayName: String?,
    avatarUrl: String?,
    streamTitle: String?,
    viewersCount: Int,
    streamStartedAt: String?,
    maxWidth: androidx.compose.ui.unit.Dp = 400.dp,
) {
    val isUltraSlim = maxWidth < 180.dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isUltraSlim) {
            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        } else {
            Arrangement.Start
        },
    ) {
        // Mini Avatar
        Box(modifier = Modifier.size(24.dp)) {
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
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            color = SamtchTheme.colors.accentColor,
                            strokeWidth = 1.5.dp
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
                            text = (displayName ?: channel).take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Streamer Info (Name + Title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (isUltraSlim) Modifier else Modifier.weight(1f).padding(start = 8.dp)
        ) {
            Text(
                text = displayName ?: channel,
                color = SamtchTheme.colors.accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isUltraSlim) {
                Text(text = ": ", color = SamtchTheme.colors.secondaryText, fontSize = 13.sp)
                
                Text(
                    text = streamTitle ?: "Stream Offline",
                    color = SamtchTheme.colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isUltraSlim) {
            val duration = formatStreamDuration(streamStartedAt)
            if ((duration.isNotEmpty()) && (maxWidth >= 220.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(SamtchTheme.colors.liveDot, CircleShape)
                    )
                    Text(
                        text = duration,
                        color = SamtchTheme.colors.secondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if ((viewersCount > 0) && (maxWidth >= 180.dp)) {
                AnimatedViewerCount(
                    count = viewersCount,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
