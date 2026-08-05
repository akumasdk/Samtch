package com.akumasdk.samtch.ui.screens.player

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.components.AnimatedViewerCount

@Composable
fun AudioOnlyPlayer(
    channel: String,
    avatarUrl: String?,
    subtitle: String?,
    displayName: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onCloseAudioOnly: () -> Unit,
    onRefresh: () -> Unit,
    previewImageUrl: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SamtchTheme.colors.audioPlayerBackgroundStart,
                        SamtchTheme.colors.audioPlayerBackgroundEnd
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background Image with Crossfade animation on refresh
        Crossfade(
            targetState = previewImageUrl,
            animationSpec = tween(durationMillis = 1000),
            label = "AudioOnlyBackgroundFade"
        ) { url ->
            if (!url.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Add a theme-aware overlay to ensure readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SamtchTheme.colors.rootBackground.copy(alpha = 0.7f))
                    )
                }
            }
        }

        val availableHeight = maxHeight
        val isMini = availableHeight < 100.dp
        
        if (isMini) {
            AudioOnlyMiniView(channel, avatarUrl, displayName)
        } else {
            AudioOnlyExpandedView(
                channel = channel,
                avatarUrl = avatarUrl,
                displayName = displayName,
                streamTitle = streamTitle,
                subtitle = subtitle,
                gameName = gameName,
                viewersCount = viewersCount,
                isPlaying = isPlaying,
                availableHeight = availableHeight,
                onTogglePlayback = onTogglePlayback,
                onCloseAudioOnly = onCloseAudioOnly,
                onRefresh = onRefresh
            )
            
            if (availableHeight > 250.dp) {
                Text(
                    text = stringResource(R.string.audio_only_hint),
                    color = SamtchTheme.colors.secondaryText.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}


