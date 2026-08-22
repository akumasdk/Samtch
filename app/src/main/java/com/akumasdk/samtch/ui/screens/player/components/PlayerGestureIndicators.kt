package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun PlayerGestureIndicators(
    showVolume: Boolean,
    volumeProgress: Float,
    showBrightness: Boolean,
    brightnessProgress: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showVolume) {
            GestureIndicator(label = "Volume", progress = volumeProgress)
        }
        if (showBrightness) {
            GestureIndicator(label = "Brightness", progress = brightnessProgress)
        }
    }
}

@Composable
private fun GestureIndicator(label: String, progress: Float) {
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(100.dp),
                color = SamtchTheme.colors.accentColor,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
