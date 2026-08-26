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
                /* minBufferMs = */ 8_000, // Increased to 8s for high stability
                /* maxBufferMs = */ 15_000, 
                /* bufferForPlaybackMs = */ 4_000, // Increased to 4s (2 segments) before starting playback
                /* bufferForPlaybackAfterRebufferMs = */ 4_000 
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 6_000, // Larger back-buffer
                /* retainBackBufferFromKeyframe = */ true
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Provides an optimized [MediaItem.LiveConfiguration] for Twitch live streams.
     * Replicates Streamlink's --twitch-low-latency behavior.
     */
    fun getLiveConfiguration(
        isLowLatencyEnabled: Boolean = true
    ): MediaItem.LiveConfiguration {
        val builder = MediaItem.LiveConfiguration.Builder()
        
        if (isLowLatencyEnabled) {
            builder.setTargetOffsetMs(6_000L) // Aim for ~6s delay (3 segments) for high stability
                .setMinOffsetMs(3_000L) 
                .setMaxOffsetMs(12_000L)
                .setMaxPlaybackSpeed(1.10f) 
                .setMinPlaybackSpeed(0.95f) 
        } else {
            builder.setTargetOffsetMs(5_000L) 
                .setMinOffsetMs(2_500L)
                .setMaxOffsetMs(15_000L)
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
