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
        private val SEQUENCE_REGEX = Pattern.compile("#EXT-X-MEDIA-SEQUENCE:(\\d+)")
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
        
        var totalDuration = 0.0
        var segmentCount = 0
        var adRemovedCount = 0
        
        // First pass: Calculate average duration
        lines.forEach { line ->
            val extinfMatcher = EXTINF_REGEX.matcher(line)
            if (extinfMatcher.find()) {
                extinfMatcher.group(1)?.toDoubleOrNull()?.let {
                    totalDuration += it
                    segmentCount++
                }
            }
        }
        
        val avgDuration = if (segmentCount > 0) totalDuration / segmentCount else 2.0
        val formattedAvg = "%.3f".format(avgDuration)

        // Second pass: Filter ads and rewrite prefetch
        var i = 0
        val tempOutput = mutableListOf<String>()
        var foundFirstSegment = false

        while (i < lines.size) {
            val line = lines[i]
            
            // 1. Detect Ad Segments via EXTINF title (Streamlink style)
            val extinfMatcher = EXTINF_REGEX.matcher(line)
            if (extinfMatcher.find()) {
                val title = extinfMatcher.group(2) ?: ""
                if (title.contains("Amazon", ignoreCase = true)) {
                    // This is an ad segment. Skip this line and the following URL line.
                    Log.d(TAG, "Stripping ad segment (Amazon title detected)")
                    i += 2 // Skip EXTINF and URL
                    
                    // Increment the sequence ONLY if we haven't found a valid segment yet.
                    // This means the ad was at the TOP of the playlist.
                    if (!foundFirstSegment) {
                        adRemovedCount++
                    }
                    continue
                }
                foundFirstSegment = true
            }

            // 2. Detect Ad Tags (DATERANGE)
            if (DATERANGE_REGEX.matcher(line).find()) {
                Log.d(TAG, "Stripping ad daterange tag")
                i++
                continue
            }

            // 3. Convert Prefetch
            val prefetchMatcher = PREFETCH_REGEX.matcher(line)
            if (prefetchMatcher.find()) {
                val segmentUrl = prefetchMatcher.group(1)
                if (!segmentUrl.isNullOrBlank()) {
                    tempOutput.add("#EXTINF:$formattedAvg,")
                    tempOutput.add(segmentUrl)
                    Log.v(TAG, "Converted prefetch segment to regular EXTINF")
                    foundFirstSegment = true
                }
                i++
                continue
            }

            // 4. Sequence Number handling
            if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                if (adRemovedCount > 0) {
                    val seqMatcher = SEQUENCE_REGEX.matcher(line)
                    if (seqMatcher.find()) {
                        val originalSeq = seqMatcher.group(1)?.toLongOrNull() ?: 0L
                        val newSeq = originalSeq + adRemovedCount
                        tempOutput.add("#EXT-X-MEDIA-SEQUENCE:$newSeq")
                        Log.d(TAG, "Updated MEDIA-SEQUENCE: $originalSeq -> $newSeq (ads removed from top: $adRemovedCount)")
                    } else {
                        tempOutput.add(line)
                    }
                } else {
                    tempOutput.add(line)
                }
                i++
                continue
            }

            // Standard line
            if (line.isNotBlank()) {
                if (!line.startsWith("#EXTINF") && !line.startsWith("http") && !line.startsWith("/")) {
                    // Not a segment line, just add it
                } else {
                    foundFirstSegment = true
                }
                tempOutput.add(line)
            }
            i++
        }
        
        return tempOutput.joinToString("\n")
    }
}
