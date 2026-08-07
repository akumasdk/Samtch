package com.akumasdk.samtch.ui.screens.player.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlin.math.roundToInt

@Composable
fun Modifier.playerGestureHandler(
    isFullscreen: Boolean,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeDragging: (Boolean) -> Unit,
    onBrightnessDragging: (Boolean) -> Unit
): Modifier {
    if (!isFullscreen) return this

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = context as? Activity
    val viewConfiguration = LocalViewConfiguration.current

    var volumeAccumulator by remember { mutableFloatStateOf(0f) }

    return this.pointerInput(isFullscreen) {
        awaitPointerEventScope {
            while (true) {
                // Peek at the Down event but don't consume it (allow iframe/buttons interaction)
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                
                var isDragging = false
                var totalDragY = 0f
                val touchSlop = viewConfiguration.touchSlop
                val pointerId = down.id
                
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.find { it.id == pointerId } ?: break
                    
                    if (event.type == PointerEventType.Move) {
                        val dragDelta = change.position.y - change.previousPosition.y
                        totalDragY += dragDelta
                        
                        if (!isDragging && kotlin.math.abs(totalDragY) > touchSlop) {
                            isDragging = true
                            
                            val isLeftSide = change.position.x < size.width / 2
                            if (isLeftSide) {
                                onBrightnessDragging(true)
                                onVolumeDragging(false)
                            } else {
                                onVolumeDragging(true)
                                onBrightnessDragging(false)
                                volumeAccumulator = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            }
                        }
                        
                        if (isDragging) {
                            // CONSUME to "steal" the drag from WebView
                            change.consume()
                            
                            val isLeftSide = change.position.x < size.width / 2
                            if (isLeftSide) {
                                val sensitivity = 2.5f
                                val delta = (-dragDelta / size.height) * sensitivity
                                val currentWindowB = activity?.window?.attributes?.screenBrightness ?: -1f
                                val baseB = if (currentWindowB < 0) {
                                    com.akumasdk.samtch.util.SystemSettingsUtil.getSystemBrightness(context)
                                } else {
                                    currentWindowB
                                }
                                val newB = (baseB + delta).coerceIn(0f, 1f)
                                onBrightnessChange(newB)
                            } else {
                                val maxV = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)
                                val sensitivity = 3.5f
                                val delta = (-dragDelta / size.height) * maxV * sensitivity
                                
                                volumeAccumulator = (volumeAccumulator + delta).coerceIn(0f, maxV)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeAccumulator.roundToInt(), 0)
                                onVolumeChange(volumeAccumulator / maxV)
                            }
                        }
                    }
                    if (!change.pressed) {
                        onVolumeDragging(false)
                        onBrightnessDragging(false)
                        break
                    }
                }
            }
        }
    }
}
