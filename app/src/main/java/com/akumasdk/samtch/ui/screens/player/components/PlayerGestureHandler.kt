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
import androidx.compose.ui.unit.IntSize
import com.akumasdk.samtch.util.SystemSettingsUtil
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun Modifier.playerGestureHandler(
    isFullscreen: Boolean,
    isMinimized: Boolean,
    size: IntSize,
    doubleTapTimeout: Long,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeDragging: (Boolean) -> Unit,
    onBrightnessDragging: (Boolean) -> Unit,
    onSingleTap: () -> Unit,
    onDoubleTapCenter: () -> Unit
): Modifier {
    if (isMinimized) return this

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = context as? Activity
    val viewConfiguration = LocalViewConfiguration.current

    return this.pointerInput(isFullscreen, size) {
        var lastTapTime = 0L

        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val downTime = down.uptimeMillis
                val isDoubleTapCandidate = (downTime - lastTapTime) < doubleTapTimeout

                var isDragging = false
                var isBrightnessDrag = false
                val touchSlop = viewConfiguration.touchSlop
                val pointerId = down.id
                val startPos = down.position

                var initialBrightness = 0.5f
                var initialVolume = 0f
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.find { it.id == pointerId } ?: break

                    if (event.type == PointerEventType.Move) {
                        val totalDist = hypot(change.position.x - startPos.x, change.position.y - startPos.y)

                        if (isFullscreen && !isDragging && totalDist > touchSlop) {
                            isDragging = true
                            val isLeftSide = startPos.x < size.width / 2f

                            if (isLeftSide) {
                                isBrightnessDrag = true
                                onBrightnessDragging(true)
                                onVolumeDragging(false)

                                val currentWindowB = activity?.window?.attributes?.screenBrightness ?: -1f
                                initialBrightness = if (currentWindowB < 0f) {
                                    SystemSettingsUtil.getSystemBrightness(context)
                                } else {
                                    currentWindowB
                                }
                                onBrightnessChange(initialBrightness)
                            } else {
                                isBrightnessDrag = false
                                onVolumeDragging(true)
                                onBrightnessDragging(false)

                                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            }
                        }

                        if (isFullscreen && isDragging) {
                            change.consume()

                            val dragDistanceY = startPos.y - change.position.y
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            val fraction = dragDistanceY / height

                            if (isBrightnessDrag) {
                                val targetBrightness = (initialBrightness + fraction).coerceIn(0.01f, 1f)

                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = targetBrightness
                                    act.window.attributes = lp
                                }
                                onBrightnessChange(targetBrightness)
                            } else {
                                val targetVolume = (initialVolume + fraction * maxVolume).coerceIn(0f, maxVolume)

                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume.roundToInt(), 0)
                                onVolumeChange(targetVolume / maxVolume)
                            }
                        }
                    }

                    if (!change.pressed) {
                        if (isDragging) {
                            onVolumeDragging(false)
                            onBrightnessDragging(false)
                        } else {
                            // Single or double tap detection upon finger release
                            if (isFullscreen) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val radiusX = size.width * 0.25f
                                val radiusY = size.height * 0.25f

                                val isInCenterZone = kotlin.math.abs(change.position.x - centerX) <= radiusX &&
                                        kotlin.math.abs(change.position.y - centerY) <= radiusY

                                if (isDoubleTapCandidate && isInCenterZone) {
                                    onDoubleTapCenter()
                                    lastTapTime = 0L
                                } else {
                                    lastTapTime = downTime
                                    onSingleTap()
                                }
                            } else {
                                // Portrait mode: double tap anywhere on stream toggles fullscreen!
                                if (isDoubleTapCandidate) {
                                    onDoubleTapCenter()
                                    lastTapTime = 0L
                                } else {
                                    lastTapTime = downTime
                                    onSingleTap()
                                }
                            }
                        }
                        break
                    }
                }
            }
        }
    }
}
