package com.akumasdk.samtch.util

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.akumasdk.samtch.data.api.TwitchHlsInterceptor
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
object StreamingPlayerFactory {

    private const val TAG = "StreamingPlayerFactory"

    // 1. Reusable Network Client with HTTP/2, Instant Retries, and Streamlink HLS Logic
    internal val okHttpClient: OkHttpClient by lazy {
        Log.d(TAG, "Initializing shared OkHttpClient with TwitchHlsInterceptor...")
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(20, 5, TimeUnit.MINUTES)) // More persistent connections
            .addInterceptor(TwitchHlsInterceptor()) 
            .retryOnConnectionFailure(true) 
            .connectTimeout(3, TimeUnit.SECONDS) // Faster timeouts
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Prewarms the shared network client and connection pool.
     * Performs an asynchronous dummy request to warm up DNS, SSL handshake and socket pool.
     */
    fun prewarm() {
        Log.d(TAG, "Prewarming StreamingPlayerFactory resources...")
        try {
            val startTime = System.currentTimeMillis()
            val request = okhttp3.Request.Builder()
                .url("https://usher.ttvnw.net/api/channel/hls/twitch.m3u8") // Safe dummy HLS endpoint
                .head() // Minimal HEAD request to trigger handshake without downloading body
                .build()

            okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.w(TAG, "Prewarm HEAD request failed after ${System.currentTimeMillis() - startTime}ms: ${e.message}")
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val duration = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Prewarm HEAD request successful! Duration: ${duration}ms, Code: ${response.code}")
                    response.close() // Close to return socket to pool
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Prewarm request execution error", e)
        }
    }

    /**
     * Creates a production-ready ExoPlayer.Builder calibrated for 2s segments.
     */
    fun createLowLatencyPlayerBuilder(context: Context): ExoPlayer.Builder {
        Log.d(TAG, "Creating new LowLatencyPlayerBuilder (Streamlink Mode)...")
        
        // Use centralized LoadControl from BufferingManager
        val loadControl = BufferingManager.createLoadControl()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
    }

    /**
     * Creates an HLS MediaSource with instant preparation.
     */
    fun createHlsMediaSource(url: String): HlsMediaSource {
        Log.d(TAG, "Creating HlsMediaSource for URL: $url")
        
        // 3. Instant Load HLS Parser
        val dataSourceFactory = getDataSourceFactory()
        
        return HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true) // Skip segment analysis for fast startup
            .createMediaSource(MediaItem.fromUri(url))
    }

    /**
     * Provides the DataSource.Factory using the persistent OkHttpClient.
     */
    fun getDataSourceFactory(): OkHttpDataSource.Factory {
        Log.v(TAG, "Providing shared OkHttpDataSource.Factory")
        return OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(Constants.UserAgents.MOBILE)
            .setDefaultRequestProperties(mapOf(
                "Client-ID" to Constants.Twitch.CLIENT_ID,
                "Referer" to "https://m.twitch.tv/",
                "Origin" to "https://m.twitch.tv"
            ))
    }
}
