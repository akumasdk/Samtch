package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerGestureOverlay(
    isDraggingVolume: Boolean,
    isDraggingBrightness: Boolean,
    volumeProgress: Float,
    brightnessProgress: Float
) {
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(isDraggingVolume) {
        if (isDraggingVolume) {
            showVolumeOverlay = true
        } else {
            delay(2.seconds)
            showVolumeOverlay = false
        }
    }

    LaunchedEffect(isDraggingBrightness) {
        if (isDraggingBrightness) {
            showBrightnessOverlay = true
        } else {
            delay(2.seconds)
            showBrightnessOverlay = false
        }
    }

    PlayerGestureIndicators(
        showVolume = showVolumeOverlay,
        volumeProgress = volumeProgress,
        showBrightness = showBrightnessOverlay,
        brightnessProgress = brightnessProgress
    )
}
