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
                /* minBufferMs = */ 500, // Absolute minimum
                /* maxBufferMs = */ 30_000, 
                /* bufferForPlaybackMs = */ 100, 
                /* bufferForPlaybackAfterRebufferMs = */ 500 
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 2_000, // Reduced to minimize memory overhead
                /* retainBackBufferFromKeyframe = */ true
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Provides an optimized [MediaItem.LiveConfiguration] for Twitch live streams.
     * Pushed to the absolute limit for zero-delay experience (~1s).
     */
    fun getLiveConfiguration(
        isLowLatencyEnabled: Boolean = true
    ): MediaItem.LiveConfiguration {
        val builder = MediaItem.LiveConfiguration.Builder()
        
        if (isLowLatencyEnabled) {
            builder.setTargetOffsetMs(1_000L) // 1s target - Absolute bleeding edge
                .setMinOffsetMs(200L) // Allow sitting at the absolute edge
                .setMaxOffsetMs(3_000L)
                .setMaxPlaybackSpeed(1.50f) // Very aggressive catch-up
                .setMinPlaybackSpeed(0.80f) 
        } else {
            builder.setTargetOffsetMs(4_000L) 
                .setMinOffsetMs(1_000L)
                .setMaxOffsetMs(15_000L)
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
