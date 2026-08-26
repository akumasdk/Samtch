package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.akumasdk.samtch.ui.components.metadata.AnimatedViewerCount
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlIconButton(
                icon = Icons.Default.Videocam,
                onClick = onCloseAudioOnly,
                size = if (availableHeight < 200.dp) 32.dp else 40.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                contentDescription = "Return to Video"
            )

            LargePlaybackButton(
                isPlaying = isPlaying,
                onToggle = onTogglePlayback,
                size = if (availableHeight < 200.dp) 56.dp else 72.dp,
                iconSize = if (availableHeight < 200.dp) 28.dp else 36.dp
            )
            
            ControlIconButton(
                icon = Icons.Default.Refresh,
                onClick = onRefresh,
                size = if (availableHeight < 200.dp) 32.dp else 40.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                contentDescription = "Refresh Stream"
            )
        }
    }
}

@Composable
private fun LargePlaybackButton(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    iconSize: androidx.compose.ui.unit.Dp = 36.dp
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = SamtchAnimation.springInteractive(),
        label = "PlaybackButtonScale"
    )

    Surface(
        onClick = onToggle,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = SamtchTheme.colors.twitchPurple.copy(alpha = 0.85f),
        modifier = Modifier
            .size(size)
            .scale(scale)
            .shadow(12.dp, CircleShape),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    fadeIn(animationSpec = SamtchAnimation.StandardTween) + 
                    scaleIn(initialScale = 0.8f) togetherWith
                    fadeOut(animationSpec = SamtchAnimation.FastTween) + 
                    scaleOut(targetScale = 0.8f)
                },
                label = "PlayPauseAnimation"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    contentDescription: String? = null
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = SamtchAnimation.springInteractive(),
        label = "ButtonScale"
    )

    Surface(
        onClick = if (enabled) onClick else ({}),
        interactionSource = interactionSource,
        shape = CircleShape,
        color = if (enabled) backgroundColor else Color.Transparent,
        contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
        modifier = modifier
            .size(size)
            .scale(scale),
        tonalElevation = if (enabled) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
