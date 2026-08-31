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
     * Calibrated for balanced stability and low latency.
     */
    fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                /* minBufferMs = */ 3_000, // Aggressive low buffer for TV
                /* maxBufferMs = */ 15_000, 
                /* bufferForPlaybackMs = */ 500, 
                /* bufferForPlaybackAfterRebufferMs = */ 1_000 
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 10_000, 
                /* retainBackBufferFromKeyframe = */ true
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Provides an optimized [MediaItem.LiveConfiguration] for Twitch live streams.
     * Balanced for ~2.5s delay with smooth continuous playback.
     */
    fun getLiveConfiguration(
        isLowLatencyEnabled: Boolean = true
    ): MediaItem.LiveConfiguration {
        val builder = MediaItem.LiveConfiguration.Builder()
        
        if (isLowLatencyEnabled) {
            builder.setTargetOffsetMs(1_000L) // 1.0s target for minimum delay
                .setMinOffsetMs(500L) 
                .setMaxOffsetMs(3_000L)
                .setMaxPlaybackSpeed(1.50f) // Aggressive catch-up
                .setMinPlaybackSpeed(0.50f) 
        } else {
            builder.setTargetOffsetMs(5_000L) 
                .setMinOffsetMs(2_000L)
                .setMaxOffsetMs(20_000L)
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
