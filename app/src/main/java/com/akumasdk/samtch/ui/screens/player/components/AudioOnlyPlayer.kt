package com.akumasdk.samtch.ui.screens.player.components

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchTheme

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
                            .background(SamtchTheme.colors.rootBackground.copy(alpha = 0.85f))
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
