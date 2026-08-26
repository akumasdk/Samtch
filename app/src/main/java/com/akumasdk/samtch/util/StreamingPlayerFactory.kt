package com.akumasdk.samtch.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
object StreamingPlayerFactory {

    // 1. Reusable Network Client with HTTP/2 and Instant Retries
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES)) // Socket persistence
            .retryOnConnectionFailure(true) // Automatic retries on micro-interruptions
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates a production-ready ExoPlayer.Builder calibrated for 2s segments.
     */
    fun createLowLatencyPlayerBuilder(context: Context): ExoPlayer.Builder {
        // 2. Safe Buffer (2700ms = 1.35 segments)
        // Calibrated to prevent rewinds and absorb network jitter
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2700, // minBufferMs: Does not start downloading until having less than this
                15000, // maxBufferMs
                1200,  // bufferForPlaybackMs: Starts with 0.6 segments
                2700   // bufferForPlaybackAfterRebufferMs: After rebuffer, requires 1.35 segments
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
    }

    /**
     * Creates an HLS MediaSource with instant preparation.
     */
    fun createHlsMediaSource(url: String): HlsMediaSource {
        // 3. Instant Load HLS Parser
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(Constants.UserAgents.MOBILE)
            .setDefaultRequestProperties(mapOf(
                "Client-ID" to Constants.Twitch.CLIENT_ID,
                "Referer" to "https://m.twitch.tv/",
                "Origin" to "https://m.twitch.tv"
            ))
        
        return HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true) // Skip segment analysis for fast startup
            .createMediaSource(MediaItem.fromUri(url))
    }

    /**
     * Provides the DataSource.Factory using the persistent OkHttpClient.
     */
    fun getDataSourceFactory(): OkHttpDataSource.Factory {
        return OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(Constants.UserAgents.MOBILE)
            .setDefaultRequestProperties(mapOf(
                "Client-ID" to Constants.Twitch.CLIENT_ID,
                "Referer" to "https://m.twitch.tv/",
                "Origin" to "https://m.twitch.tv"
            ))
    }
}
