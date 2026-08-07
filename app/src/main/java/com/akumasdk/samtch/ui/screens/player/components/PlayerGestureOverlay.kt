package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PlayerGestureIndicators(
    showVolume: Boolean,
    volumeProgress: Float,
    showBrightness: Boolean,
    brightnessProgress: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        GestureIndicator(
            visible = showBrightness,
            progress = brightnessProgress,
            icon = Icons.Default.BrightnessLow,
            alignment = Alignment.CenterStart
        )

        GestureIndicator(
            visible = showVolume,
            progress = volumeProgress,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            alignment = Alignment.CenterEnd
        )
    }
}

@Composable
private fun GestureIndicator(
    visible: Boolean,
    progress: Float,
    icon: ImageVector,
    alignment: Alignment
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = alignment
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .width(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress.coerceIn(0.01f, 1f))
                            .background(Color.White)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}
