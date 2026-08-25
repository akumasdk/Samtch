package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun NativePlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSettingsEnabled: Boolean = true
) {
    android.util.Log.d("NativePlayerControls", "Rendering controls: visible=$isVisible, settingsEnabled=$isSettingsEnabled")
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // Darken whole screen when controls are visible
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume to avoid accidental taps */ }
                )
        ) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp) // Normalized padding
            ) {
                ControlIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    enabled = isSettingsEnabled,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            // Bottom Bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = onTogglePlayback
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ControlIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = onRefresh
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlIconButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        onClick = onToggleChat
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ControlIconButton(
                        icon = Icons.Default.Fullscreen,
                        onClick = onToggleFullscreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = if (enabled) onClick else ({}),
        shape = CircleShape,
        color = Color.Black.copy(alpha = if (enabled) 0.35f else 0.1f),
        contentColor = if (enabled) Color.White else Color.Gray.copy(alpha = 0.5f),
        modifier = modifier.size(44.dp) // Normalized size
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
