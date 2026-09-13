package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlin.math.roundToInt

@Composable
fun PlayerGestureIndicators(
    showVolume: Boolean,
    volumeProgress: () -> Float,
    showBrightness: Boolean,
    brightnessProgress: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = showVolume,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GestureIndicator(
                iconProvider = {
                    val p = volumeProgress()
                    when {
                        p <= 0f -> Icons.AutoMirrored.Filled.VolumeMute
                        p < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                },
                label = "Volume",
                progress = volumeProgress
            )
        }

        AnimatedVisibility(
            visible = showBrightness,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GestureIndicator(
                iconProvider = {
                    val p = brightnessProgress()
                    when {
                        p < 0.35f -> Icons.Default.BrightnessLow
                        p < 0.7f -> Icons.Default.Brightness5
                        else -> Icons.Default.Brightness7
                    }
                },
                label = "Brightness",
                progress = brightnessProgress
            )
        }
    }
}

@Composable
private fun GestureIndicator(
    iconProvider: () -> ImageVector,
    label: String,
    progress: () -> Float
) {
    Surface(
        color = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconProvider(),
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(progress().coerceIn(0f, 1f) * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress().coerceIn(0f, 1f) },
                modifier = Modifier.width(110.dp).height(4.dp),
                color = SamtchTheme.colors.accentColor,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}
