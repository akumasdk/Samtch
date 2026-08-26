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
     * Starts playback faster while maintaining a healthy background buffer to prevent stutters.
     */
    fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000, // Increased to 10s to prevent starvation rewinds
                /* maxBufferMs = */ 20_000, 
                /* bufferForPlaybackMs = */ 1_000, 
                /* bufferForPlaybackAfterRebufferMs = */ 2_500 
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 5_000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Provides an optimized [MediaItem.LiveConfiguration] for Twitch live streams.
     * Uses adaptive playback speed to maintain low latency without constant buffering.
     */
    fun getLiveConfiguration(isLowLatencyEnabled: Boolean = true): MediaItem.LiveConfiguration {
        val builder = MediaItem.LiveConfiguration.Builder()
        
        if (isLowLatencyEnabled) {
            builder.setTargetOffsetMs(2_500) // Stabilized at 2.5s to avoid edge collisions
                .setMinOffsetMs(1_500) // Minimum 1.5s to prevent rewinds
                .setMaxOffsetMs(8_000)
                .setMaxPlaybackSpeed(1.10f) // Smoother catch-up
                .setMinPlaybackSpeed(0.95f) 
        } else {
            builder.setTargetOffsetMs(5_000) 
                .setMinOffsetMs(2_500)
                .setMaxOffsetMs(15_000)
                .setMaxPlaybackSpeed(1.05f)
                .setMinPlaybackSpeed(0.97f)
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
