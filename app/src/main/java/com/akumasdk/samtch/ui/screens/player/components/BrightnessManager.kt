package com.akumasdk.samtch.ui.screens.player.components

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.util.SystemSettingsUtil

@Composable
fun BrightnessManager(
    isFullscreen: Boolean,
    isPip: Boolean,
    isDraggingBrightness: Boolean,
    brightnessProgress: Float,
    onBrightnessProgressChanged: (Float) -> Unit,
    hasExplicitBrightness: Boolean,
    onHasExplicitBrightnessChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var lastKnownSystemBrightness by remember { mutableFloatStateOf(-1f) }
    var originalBrightness by remember { mutableFloatStateOf(-100f) }
    val currentOriginalBrightness by rememberUpdatedState(originalBrightness)

    LaunchedEffect(isFullscreen, isPip) {
        if (isFullscreen && !isPip) {
            val systemB = SystemSettingsUtil.getSystemBrightness(context)
            lastKnownSystemBrightness = systemB
            onBrightnessProgressChanged(systemB)
            onHasExplicitBrightnessChanged(false)
            
            activity?.let {
                val lp = it.window.attributes
                originalBrightness = lp.screenBrightness
                lp.screenBrightness = -1f
                it.window.attributes = lp
            }
        } else {
            if (originalBrightness != -100f) {
                activity?.let {
                    val lp = it.window.attributes
                    lp.screenBrightness = currentOriginalBrightness
                    it.window.attributes = lp
                }
            }
        }
    }

    LaunchedEffect(isFullscreen, isPip, isDraggingBrightness) {
        if (isFullscreen && !isPip && !isDraggingBrightness) {
            SystemSettingsUtil.observeSystemBrightness(context).collect { systemB ->
                if (lastKnownSystemBrightness != -1f && kotlin.math.abs(systemB - lastKnownSystemBrightness) > 0.02f) {
                    onBrightnessProgressChanged(systemB)
                    onHasExplicitBrightnessChanged(false)
                }
                lastKnownSystemBrightness = systemB
            }
        }
    }

    DisposableEffect(activity) {
        onDispose {
            if (currentOriginalBrightness != -100f) {
                activity?.let {
                    val lp = it.window.attributes
                    lp.screenBrightness = currentOriginalBrightness
                    it.window.attributes = lp
                }
            }
        }
    }

    LaunchedEffect(brightnessProgress, isFullscreen, isPip, isDraggingBrightness, hasExplicitBrightness) {
        if (isFullscreen && !isPip) {
            activity?.let {
                val lp = it.window.attributes
                val currentSystemB = SystemSettingsUtil.getSystemBrightness(context)
                val isSameAsSystem = kotlin.math.abs(brightnessProgress - currentSystemB) < 0.01f
                
                if (!isDraggingBrightness && (!hasExplicitBrightness || isSameAsSystem)) {
                    lp.screenBrightness = -1f
                } else {
                    lp.screenBrightness = brightnessProgress.coerceIn(0.01f, 1f)
                }
                it.window.attributes = lp
            }
        }
    }
}
