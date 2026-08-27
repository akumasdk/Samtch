package com.akumasdk.samtch.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Handles buffering configuration for ExoPlayer to balance low latency and playback stability.
 * Optimized for Twitch HLS live streams.
 */
@OptIn(UnstableApi::class)
object BufferingManager {

    /**
     * Creates a fine-tuned [LoadControl] for live streaming.
     * Calibrated for aggressive low latency (Streamlink style).
     */
    fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_000, // Minimal buffer for ultra-low latency
                /* maxBufferMs = */ 30_000, 
                /* bufferForPlaybackMs = */ 500, // Near-instant start
                /* bufferForPlaybackAfterRebufferMs = */ 1_500 
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 8_000, 
                /* retainBackBufferFromKeyframe = */ true
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Provides an optimized [MediaItem.LiveConfiguration] for Twitch live streams.
     * Calibrated for ultra-low latency matching the Twitch web player (~2-4s).
     */
    fun getLiveConfiguration(
        isLowLatencyEnabled: Boolean = true
    ): MediaItem.LiveConfiguration {
        val builder = MediaItem.LiveConfiguration.Builder()
        
        if (isLowLatencyEnabled) {
            builder.setTargetOffsetMs(3_000L) // 3s target - Aggressive low latency
                .setMinOffsetMs(1_500L) // Allow sitting very close to the edge
                .setMaxOffsetMs(10_000L)
                .setMaxPlaybackSpeed(1.20f) // Faster catch-up
                .setMinPlaybackSpeed(0.90f) 
        } else {
            builder.setTargetOffsetMs(8_000L) 
                .setMinOffsetMs(4_000L)
                .setMaxOffsetMs(30_000L)
                .setMaxPlaybackSpeed(1.10f)
                .setMinPlaybackSpeed(0.95f)
        }
        
        return builder.build()
    }

    /**
     * Applies the optimized buffering settings to a [MediaItem.Builder].
     */
    fun applyBuffering(builder: MediaItem.Builder, isLowLatency: Boolean = true): MediaItem.Builder {
        return builder.setLiveConfiguration(getLiveConfiguration(isLowLatency))
    }
}
