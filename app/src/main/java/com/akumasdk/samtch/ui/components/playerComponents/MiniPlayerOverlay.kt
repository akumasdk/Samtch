package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.ui.theme.SamtchTheme

/**
 * A dedicated overlay for the mini-player window that can show custom visual indicators.
 */
@Composable
fun MiniPlayerOverlay(
    channel: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    previewImageUrl: String? = null,
    badgeText: String? = null,
    usePreview: Boolean = false,
    showLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val displayUrl = if (usePreview && !previewImageUrl.isNullOrEmpty()) previewImageUrl else avatarUrl
        
        if (!displayUrl.isNullOrEmpty()) {
            SubcomposeAsyncImage(
                model = displayUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (usePreview) RoundedCornerShape(8.dp) else CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (usePreview) 12.dp else 16.dp),
                        color = SamtchTheme.colors.accentColor,
                        strokeWidth = 2.dp
                    )
                }
            )
        } else {
            // Initial placeholder
            Surface(
                shape = if (usePreview) RoundedCornerShape(8.dp) else CircleShape,
                color = SamtchTheme.colors.twitchPurple,
                modifier = Modifier.size(if (usePreview) 56.dp else 40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = channel.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        }

        if (showLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                color = SamtchTheme.colors.accentColor.copy(alpha = 0.5f),
                strokeWidth = 2.dp
            )
        }

        if (!badgeText.isNullOrEmpty()) {
            Surface(
                color = SamtchTheme.colors.accentColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp, end = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
    }
}
