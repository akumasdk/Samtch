package com.akumasdk.samtch.ui.screens.player.components

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun BrightnessManager(
    isFullscreen: Boolean,
    isPip: Boolean,
    brightnessProgress: Float,
    isDraggingBrightness: Boolean
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var originalBrightness by remember { mutableFloatStateOf(-100f) }
    val currentOriginalBrightness by rememberUpdatedState(originalBrightness)

    DisposableEffect(isFullscreen, isPip) {
        if (isFullscreen && !isPip) {
            activity?.let { act ->
                val lp = act.window.attributes
                if (originalBrightness == -100f) {
                    originalBrightness = lp.screenBrightness
                }
            }
        }
        onDispose {
            if (currentOriginalBrightness != -100f) {
                activity?.let { act ->
                    val lp = act.window.attributes
                    lp.screenBrightness = currentOriginalBrightness
                    act.window.attributes = lp
                }
            }
        }
    }

    LaunchedEffect(brightnessProgress, isFullscreen, isPip, isDraggingBrightness) {
        if (isFullscreen && !isPip && brightnessProgress in 0f..1f) {
            activity?.let { act ->
                val lp = act.window.attributes
                lp.screenBrightness = brightnessProgress.coerceIn(0.01f, 1f)
                act.window.attributes = lp
            }
        }
    }
}
