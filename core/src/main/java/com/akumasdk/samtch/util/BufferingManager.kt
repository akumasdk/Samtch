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
                /* minBufferMs = */ 10_000, // More generous min buffer for TV stability
                /* maxBufferMs = */ 30_000, 
                /* bufferForPlaybackMs = */ 2_000, // Wait for 2s of video before starting
                /* bufferForPlaybackAfterRebufferMs = */ 3_000 
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
            builder.setTargetOffsetMs(3_000L) // 3.0s target for better stability on TV
                .setMinOffsetMs(1_500L) 
                .setMaxOffsetMs(10_000L)
                .setMaxPlaybackSpeed(1.10f) // Suble catch-up
                .setMinPlaybackSpeed(0.95f) // Prevent "slowmo" feel
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
