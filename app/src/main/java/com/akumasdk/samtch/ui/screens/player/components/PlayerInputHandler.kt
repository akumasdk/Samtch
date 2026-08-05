package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

fun Modifier.playerInputHandler(
    size: IntSize,
    isFullscreen: Boolean,
    isMinimized: Boolean,
    doubleTapTimeout: Long,
    onDoubleTapCenter: () -> Unit,
    onSingleTap: () -> Unit
): Modifier = this.pointerInput(isFullscreen, isMinimized, size) {
    var lastTapTime = 0L
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val isPress = event.type == PointerEventType.Press
            
            if (isPress) {
                val currentTime = event.changes.first().uptimeMillis
                val isDoubleTap = (currentTime - lastTapTime) < doubleTapTimeout

                if (isDoubleTap) {
                    val position = event.changes.first().position
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    val radiusX = size.width * 0.2f
                    val radiusY = size.height * 0.2f

                    val isInCenterZone = kotlin.math.abs(position.x - centerX) <= radiusX &&
                            kotlin.math.abs(position.y - centerY) <= radiusY

                    if (isInCenterZone) {
                        onDoubleTapCenter()
                        event.changes.forEach { it.consume() }
                    }
                }
                lastTapTime = currentTime
            }
        }
    }
}.pointerInput(isFullscreen, isMinimized) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.type == PointerEventType.Press && !isMinimized) {
                onSingleTap()
            }
        }
    }
}
