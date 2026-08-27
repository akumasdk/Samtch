package com.akumasdk.samtch.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.regex.Pattern

/**
 * Port of Streamlink's Twitch HLS logic to Android/OkHttp.
 * Handles prefetch segments and metadata cleaning for aggressive low-latency.
 */
class TwitchHlsInterceptor : Interceptor {

    companion object {
        private const val TAG = "TwitchHlsInterceptor"
        private val PREFETCH_REGEX = Pattern.compile("#EXT-X-TWITCH-PREFETCH:(.*)")
        private val EXTINF_REGEX = Pattern.compile("#EXTINF:([0-9.]+),(.*)")
        private val DATERANGE_REGEX = Pattern.compile("#EXT-X-DATERANGE:.*CLASS=\"twitch-stitched-ad\".*")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val response = chain.proceed(request)

        // Only process HLS playlists from Twitch
        if (!url.contains(".m3u8") || !response.isSuccessful) {
            return response
        }

        val bodyString = response.body?.string() ?: return response
        
        // If it's a variant/media playlist (contains EXTINF or PREFETCH)
        if (bodyString.contains("#EXTINF") || bodyString.contains("#EXT-X-TWITCH-PREFETCH")) {
            val transformedBody = processPlaylist(bodyString)
            return response.newBuilder()
                .body(transformedBody.toResponseBody(response.body?.contentType()))
                .build()
        }

        return response.newBuilder()
            .body(bodyString.toResponseBody(response.body?.contentType()))
            .build()
    }

    private fun processPlaylist(input: String): String {
        val lines = input.lines()
        
        var lastDuration = 2.0
        
        // Find the last valid segment duration to use for prefetch conversion
        for (line in lines.reversed()) {
            val matcher = EXTINF_REGEX.matcher(line)
            if (matcher.find()) {
                lastDuration = matcher.group(1)?.toDoubleOrNull() ?: 2.0
                break
            }
        }
        
        val formattedDuration = "%.3f".format(java.util.Locale.US, lastDuration)

        var i = 0
        val tempOutput = mutableListOf<String>()
        var prefetchCount = 0

        while (i < lines.size) {
            val line = lines[i]
            
            // 1. Detect Ad Tags (DATERANGE) and strip them
            if (DATERANGE_REGEX.matcher(line).find()) {
                Log.d(TAG, "Stripping ad daterange tag")
                i++
                continue
            }

            // 2. Convert Prefetch Segments to regular EXTINF
            // Re-enabling full conversion for ultra-low latency.
            // This replicates Twitch web player behavior by requesting segments while they are still being uploaded.
            val prefetchMatcher = PREFETCH_REGEX.matcher(line)
            if (prefetchMatcher.find()) {
                val segmentUrl = prefetchMatcher.group(1)
                if (!segmentUrl.isNullOrBlank()) {
                    tempOutput.add("#EXTINF:$formattedDuration,")
                    tempOutput.add(segmentUrl)
                    Log.v(TAG, "Converted prefetch segment for ultra-low latency: $segmentUrl")
                    prefetchCount++
                }
                i++
                continue
            }

            if (line.isNotBlank()) {
                tempOutput.add(line)
            }
            i++
        }
        
        return tempOutput.joinToString("\n")
    }
}
