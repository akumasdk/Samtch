package com.akumasdk.samtch.util

import android.util.Log
import androidx.media3.common.Player
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.seconds

/**
 * Watchdog that monitors stream health via FPS and playback state.
 * Automatically triggers a recovery action if the stream appears frozen.
 */
object PlaybackWatchdog {
    private const val TAG = "PlaybackWatchdog"
    private const val FPS_THRESHOLD = 2.0f
    private const val STALL_DURATION_THRESHOLD = 6 // seconds

    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var stallCounter = 0

    /**
     * Starts the watchdog for the given player.
     * @param player The player to monitor
     * @param onRecover Callback to execute when a stall is detected and recovery is needed
     */
    fun start(player: Player, onRecover: () -> Unit) {
        if (watchdogJob?.isActive == true) return

        Log.d(TAG, "Starting Playback Watchdog...")
        stallCounter = 0

        watchdogJob = scope.launch {
            FpsMonitor.currentFps.collectLatest { fps ->
                // Only monitor if the player is supposed to be playing
                if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                    if (fps < FPS_THRESHOLD) {
                        stallCounter++
                        Log.w(TAG, "Low FPS detected: $fps (Stall counter: $stallCounter/$STALL_DURATION_THRESHOLD)")
                    } else {
                        if (stallCounter > 0) {
                            Log.d(TAG, "FPS recovered: $fps. Resetting stall counter.")
                        }
                        stallCounter = 0
                    }

                    if (stallCounter >= STALL_DURATION_THRESHOLD) {
                        Log.e(TAG, "Stream stall detected (FPS < $FPS_THRESHOLD for ${STALL_DURATION_THRESHOLD}s). Triggering recovery...")
                        stallCounter = 0
                        onRecover()
                    }
                } else {
                    stallCounter = 0
                }
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping Playback Watchdog...")
        watchdogJob?.cancel()
        watchdogJob = null
        stallCounter = 0
    }
}
