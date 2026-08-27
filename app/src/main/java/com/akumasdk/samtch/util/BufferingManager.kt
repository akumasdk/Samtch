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
                /* minBufferMs = */ 6_000, // Balanced buffer for stability
                /* maxBufferMs = */ 30_000, 
                /* bufferForPlaybackMs = */ 1_000, 
                /* bufferForPlaybackAfterRebufferMs = */ 2_500 
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
            builder.setTargetOffsetMs(2_500L) // 2.5s target for continuity
                .setMinOffsetMs(1_000L) 
                .setMaxOffsetMs(6_000L)
                .setMaxPlaybackSpeed(1.25f) // Smooth catch-up
                .setMinPlaybackSpeed(0.85f) 
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
