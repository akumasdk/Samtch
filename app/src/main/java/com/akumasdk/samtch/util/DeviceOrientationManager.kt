package com.akumasdk.samtch.util

import android.content.Context
import android.view.OrientationEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PhysicalOrientation {
    PORTRAIT,
    LANDSCAPE,
    UNKNOWN
}

class DeviceOrientationManager(context: Context) {
    private val _orientation = MutableStateFlow(PhysicalOrientation.UNKNOWN)
    val orientation: StateFlow<PhysicalOrientation> = _orientation.asStateFlow()

    private val listener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(angle: Int) {
            if (angle == ORIENTATION_UNKNOWN) {
                _orientation.value = PhysicalOrientation.UNKNOWN
                return
            }

            // Map angle to PhysicalOrientation with hysteresis (45 degree buffers)
            val newOrientation = when {
                (angle in 0..45) || (angle in 315..359) -> PhysicalOrientation.PORTRAIT
                (angle in 46..135) -> PhysicalOrientation.LANDSCAPE // Landscape Right (90 deg)
                (angle in 136..225) -> PhysicalOrientation.PORTRAIT // Portrait Upside Down (180 deg) -> Treat as Portrait
                (angle in 226..314) -> PhysicalOrientation.LANDSCAPE // Landscape Left (270 deg)
                else -> PhysicalOrientation.PORTRAIT
            }

            if (_orientation.value != newOrientation) {
                _orientation.value = newOrientation
            }
        }
    }

    fun enable() {
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
    }

    fun disable() {
        listener.disable()
    }
}
