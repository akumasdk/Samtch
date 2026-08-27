package com.akumasdk.samtch.util

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.seconds

/**
 * Monitors the real-time FPS (Frames Per Second) being rendered by the ExoPlayer.
 * Hooks into the hardware decoder counters for high precision.
 */
@OptIn(UnstableApi::class)
object FpsMonitor {
    private const val TAG = "FpsMonitor"
    
    private val _currentFps = MutableStateFlow(0f)
    val currentFps = _currentFps.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastRenderedFrames = 0
    private var lastTimestamp = 0L

    /**
     * Starts monitoring FPS for the given player.
     */
    fun start(player: Player) {
        if (player !is ExoPlayer) {
            Log.w(TAG, "Player is not an ExoPlayer instance. FPS monitoring disabled.")
            return
        }

        if (monitorJob?.isActive == true) return

        Log.d(TAG, "Starting FPS Monitor...")
        lastRenderedFrames = 0
        lastTimestamp = System.currentTimeMillis()
        
        monitorJob = scope.launch {
            while (isActive) {
                delay(1.seconds)
                updateFps(player)
            }
        }
    }

    private fun updateFps(player: ExoPlayer) {
        val counters = player.videoDecoderCounters ?: return
        val now = System.currentTimeMillis()
        val durationMs = now - lastTimestamp
        
        if (durationMs <= 0) return

        val totalFrames = counters.renderedOutputBufferCount
        val deltaFrames = totalFrames - lastRenderedFrames
        
        // Calculate FPS: (frames / ms) * 1000
        val fps = (deltaFrames.toFloat() / durationMs.toFloat()) * 1000f
        
        _currentFps.value = fps
        
        lastRenderedFrames = totalFrames
        lastTimestamp = now

        //Log.v(TAG, "Current Render FPS: %.2f".format(fps))
    }

    fun stop() {
        Log.d(TAG, "Stopping FPS Monitor...")
        monitorJob?.cancel()
        monitorJob = null
        _currentFps.value = 0f
    }
}
