package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.ui.components.AnimatedViewerCount
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun AudioOnlyMiniView(
    channel: String,
    avatarUrl: String?,
    displayName: String?
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!avatarUrl.isNullOrEmpty()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        color = SamtchTheme.colors.accentColor,
                        strokeWidth = 2.dp
                    )
                }
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = SamtchTheme.colors.accentColor.copy(alpha = 0.3f),
                    strokeWidth = 2.dp
                )
                Text(
                    text = (displayName ?: channel).take(1).uppercase(),
                    color = SamtchTheme.colors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AudioOnlyExpandedView(
    channel: String,
    avatarUrl: String?,
    displayName: String?,
    streamTitle: String?,
    subtitle: String?,
    gameName: String?,
    viewersCount: Int,
    isPlaying: Boolean,
    availableHeight: androidx.compose.ui.unit.Dp,
    onTogglePlayback: () -> Unit,
    onCloseAudioOnly: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // LEFT: Avatar
        Box(
            modifier = Modifier
                .size(if (availableHeight < 200.dp) 70.dp else 100.dp)
                .border(2.dp, SamtchTheme.colors.accentColor, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize().padding(if (availableHeight < 200.dp) 16.dp else 24.dp),
                            color = SamtchTheme.colors.accentColor,
                            strokeWidth = 2.dp
                        )
                    }
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize().padding(if (availableHeight < 200.dp) 4.dp else 8.dp),
                        color = SamtchTheme.colors.accentColor.copy(alpha = 0.5f),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = (displayName ?: channel).take(1).uppercase(),
                        color = SamtchTheme.colors.primaryText,
                        fontSize = if (availableHeight < 200.dp) 24.sp else 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // CENTER: Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = displayName ?: channel,
                color = SamtchTheme.colors.accentColor,
                fontSize = if (availableHeight < 200.dp) 18.sp else 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onCloseAudioOnly() }
            )
            
            val displayTitle = streamTitle ?: subtitle
            AnimatedContent(
                targetState = displayTitle ?: "",
                transitionSpec = {
                    (slideInVertically(animationSpec = SamtchAnimation.springInteractive()) { height -> height / 2 } + fadeIn())
                        .togetherWith(slideOutVertically(animationSpec = SamtchAnimation.springInteractive()) { height -> -height / 2 } + fadeOut())
                },
                label = "AudioTitleAnimation"
            ) { targetTitle ->
                if (targetTitle.isNotEmpty()) {
                    Text(
                        text = targetTitle,
                        color = SamtchTheme.colors.primaryText.copy(alpha = 0.9f),
                        fontSize = if (availableHeight < 200.dp) 12.sp else 14.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (!gameName.isNullOrEmpty()) {
                    Surface(
                        color = SamtchTheme.colors.accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = gameName,
                            color = SamtchTheme.colors.accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).basicMarquee()
                        )
                    }
                }

                if (viewersCount > 0) {
                    AnimatedViewerCount(count = viewersCount, fontSize = 10.sp)
                }

                Surface(
                    color = SamtchTheme.colors.accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onCloseAudioOnly() }
                ) {
                    Text(
                        text = "AUDIO ONLY",
                        color = SamtchTheme.colors.accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // RIGHT: Controls
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onCloseAudioOnly, modifier = Modifier.size(if (availableHeight < 200.dp) 32.dp else 40.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Return to Video",
                    tint = SamtchTheme.colors.primaryText.copy(alpha = 0.7f),
                    modifier = Modifier.size(if (availableHeight < 200.dp) 20.dp else 24.dp)
                )
            }

            IconButton(onClick = onTogglePlayback, modifier = Modifier.size(if (availableHeight < 200.dp) 56.dp else 64.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = SamtchTheme.colors.primaryText,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            IconButton(onClick = onRefresh, modifier = Modifier.size(if (availableHeight < 200.dp) 32.dp else 40.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = SamtchTheme.colors.primaryText.copy(alpha = 0.7f),
                    modifier = Modifier.size(if (availableHeight < 200.dp) 18.dp else 22.dp)
                )
            }
        }
    }
}
