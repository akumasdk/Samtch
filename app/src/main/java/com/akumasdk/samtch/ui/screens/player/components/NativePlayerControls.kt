package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun NativePlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isAudioOnly: Boolean,
    isFullscreen: Boolean,
    onTogglePlayback: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit,
    onToggleAudioOnly: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSettingsEnabled: Boolean = true
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isEffectivelyFullscreen = isFullscreen || isLandscape

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = SamtchAnimation.StandardTween) + 
                expandVertically(animationSpec = androidx.compose.animation.core.tween(SamtchAnimation.StandardDuration)),
        exit = fadeOut(animationSpec = SamtchAnimation.FastTween) + 
               shrinkVertically(animationSpec = androidx.compose.animation.core.tween(SamtchAnimation.FastDuration)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume to avoid accidental taps */ }
                )
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                ControlIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    enabled = isSettingsEnabled,
                    contentDescription = "Settings"
                )
            }

            // --- BOTTOM CONTROL BAR ---
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 32.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Group: Play/Pause & Refresh
                    GlassPillContainer {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            ControlIconButton(
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                onClick = onTogglePlayback,
                                isBuffering = isBuffering,
                                size = if (isEffectivelyFullscreen) 56.dp else 48.dp,
                                backgroundColor = SamtchTheme.colors.twitchPurple.copy(alpha = 0.7f),
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ControlIconButton(
                                icon = Icons.Default.Refresh,
                                onClick = onRefresh,
                                size = if (isEffectivelyFullscreen) 52.dp else 44.dp,
                                backgroundColor = Color.Transparent,
                                contentDescription = "Refresh Stream"
                            )
                        }
                    }

                    // Right Group: Chat & Fullscreen
                    GlassPillContainer {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            if (isEffectivelyFullscreen) {
                                ControlIconButton(
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    onClick = onToggleChat,
                                    size = 56.dp, // Bigger for fullscreen
                                    backgroundColor = Color.Transparent,
                                    contentDescription = "Toggle Chat"
                                )
                            } else {
                                ControlIconButton(
                                    icon = if (isAudioOnly) Icons.Default.Videocam else Icons.Default.Headset,
                                    onClick = onToggleAudioOnly,
                                    size = 40.dp,
                                    backgroundColor = if (isAudioOnly) SamtchTheme.colors.twitchPurple.copy(alpha = 0.5f) else Color.Transparent,
                                    contentDescription = if (isAudioOnly) "Switch to Video" else "Switch to Audio"
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                            ControlIconButton(
                                icon = if (isEffectivelyFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                onClick = onToggleFullscreen,
                                size = if (isEffectivelyFullscreen) 56.dp else 40.dp, // Bigger for fullscreen
                                backgroundColor = Color.Transparent,
                                contentDescription = if (isEffectivelyFullscreen) "Exit Fullscreen" else "Toggle Fullscreen"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPillContainer(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier.wrapContentSize(),
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
private fun ControlIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBuffering: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    contentDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
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
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.5f),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
