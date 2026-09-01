package com.akumasdk.samtch.data.api

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ExtM3UParser
import com.akumasdk.samtch.util.ExtMediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.regex.Pattern

/**
 * Port of Streamlink's Twitch HLS logic to Android/OkHttp.
 * Handles prefetch segments, metadata cleaning, and low-level ad-blocking 
 * by swapping manifests with clean backups (embed/popout) when ads are detected.
 */
class TwitchHlsInterceptor : Interceptor {

    private val m3u8Parser = ExtM3UParser()
    private val httpClient = com.akumasdk.samtch.util.NetworkUtil.getClient(useRelaxed = true)

    // Cache for backup variant URLs to avoid re-resolving through GQL every 2 seconds
    private val backupUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private val CACHE_EXPIRY = 2 * 60 * 1000 // 2 minutes

    // Mapping of variant manifest URLs to channel names
    private val variantToChannelMap = java.util.concurrent.ConcurrentHashMap<String, String>()

    companion object {
        private const val TAG = "TwitchHlsInterceptor"
        private val PREFETCH_REGEX = Pattern.compile("#EXT-X-TWITCH-PREFETCH:(.*)")
        private val EXTINF_REGEX = Pattern.compile("#EXTINF:([0-9.]+),(.*)")
        private val DATERANGE_REGEX = Pattern.compile("#EXT-X-DATERANGE:.*CLASS=\"twitch-stitched-ad\".*")
        
        private val BACKUP_PLAYER_TYPES = listOf("embed", "popout", "autoplay")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        
        // Skip internal requests to avoid infinite recursion
        if (request.header("X-Samtch-Internal") == "true") {
            return chain.proceed(request)
        }

        val response = chain.proceed(request)

        // Only process HLS playlists from Twitch
        if (!url.contains(".m3u8") || !response.isSuccessful) {
            return response
        }

        val bodyString = response.body?.string() ?: return response
        
        // Try to identify channel from URL or mapping
        var channelName = extractChannelName(url)
        if (channelName == null) {
            channelName = variantToChannelMap[url]
        } else {
            // If it's an usher URL (master manifest), parse it to map variants to this channel
            if (url.contains("usher.ttvnw.net")) {
                mapVariantsToChannel(bodyString, channelName)
            }
        }

        // 1. AD DETECTION
        if (containsAds(bodyString)) {
            if (channelName != null) {
                Log.d(TAG, "AD DETECTED in manifest for $channelName. Attempting backup swap...")
                val backupBody = tryFetchBackupManifest(channelName)
                if (backupBody != null) {
                    Log.d(TAG, "Successfully swapped with clean backup manifest for $channelName")
                    val transformedBackup = processPlaylist(backupBody)
                    return response.newBuilder()
                        .body(transformedBackup.toResponseBody(response.body?.contentType()))
                        .build()
                }
            } else {
                Log.w(TAG, "AD DETECTED but channel name unknown for URL: $url")
            }
        }

        // 2. REGULAR PROCESSING (Prefetch conversion for low latency)
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

    private fun mapVariantsToChannel(masterManifest: String, channelName: String) {
        val entries = m3u8Parser.parse(masterManifest)
        for (entry in entries) {
            val vUrl = entry.playlistUrl
            if (vUrl != null) {
                variantToChannelMap[vUrl] = channelName
            }
        }
    }

    private fun containsAds(manifest: String): Boolean {
        // Signifiers from AdBlockOrchestrator/Stripper
        if (manifest.contains("stitched", ignoreCase = true)) return true
        if (manifest.contains("MIDROLL", ignoreCase = true)) return true
        if (manifest.contains("PREROLL", ignoreCase = true)) return true
        if (DATERANGE_REGEX.matcher(manifest).find()) return true
        
        // Amazon segment detection in EXTINF
        val lines = manifest.lines()
        for (line in lines) {
            val matcher = EXTINF_REGEX.matcher(line)
            if (matcher.find()) {
                val title = matcher.group(2) ?: ""
                if (title.contains("Amazon", ignoreCase = true) || title.contains("Ad", ignoreCase = true)) {
                    return true
                }
            }
        }
        
        return false
    }

    private fun extractChannelName(url: String): String? {
        return try {
            // Extract from https://usher.ttvnw.net/api/v2/channel/hls/channelname.m3u8...
            val path = url.substringBefore("?").substringAfterLast("/")
            if (path.endsWith(".m3u8")) {
                path.substringBefore(".m3u8")
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun tryFetchBackupManifest(channelName: String): String? {
        val now = System.currentTimeMillis()
        val cached = backupUrlCache[channelName]
        
        // 1. Try to use cached clean variant URL first
        if (cached != null && (now - cached.second) < CACHE_EXPIRY) {
            val cachedManifest = fetchManifestInternal(cached.first)
            if (cachedManifest != null && !containsAds(cachedManifest)) {
                return cachedManifest
            }
            // If cached URL is no longer clean or reachable, clear it
            backupUrlCache.remove(channelName)
        }

        return runBlocking(Dispatchers.IO) {
            // 2. Resolve fresh backup through GQL
            for (playerType in BACKUP_PLAYER_TYPES) {
                try {
                    val tokenPair = TwitchGqlService.getPlaybackAccessToken(channelName, playerType)
                        ?: continue
                    
                    val masterUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
                    val masterManifest = fetchManifestInternal(masterUrl) ?: continue
                    
                    val entries = m3u8Parser.parse(masterManifest)
                    val variant = findBestVariant(entries)
                    val variantUrl = variant?.playlistUrl ?: continue
                    
                    val variantManifest = fetchManifestInternal(variantUrl) ?: continue
                    
                    if (!containsAds(variantManifest)) {
                        Log.d(TAG, "Resolved fresh clean backup ($playerType) for $channelName")
                        backupUrlCache[channelName] = variantUrl to System.currentTimeMillis()
                        return@runBlocking variantManifest
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving backup $playerType for $channelName", e)
                }
            }
            null
        }
    }

    private fun fetchManifestInternal(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("X-Samtch-Internal", "true")
                .header("User-Agent", Constants.UserAgents.DESKTOP)
                .build()
            httpClient.newCall(request).execute().body.string()
        } catch (_: Exception) {
            null
        }
    }

    private fun findBestVariant(entries: List<ExtMediaEntry>): ExtMediaEntry? {
        val variants = entries.filter { !it.playlistUrl.isNullOrEmpty() }
        if (variants.isEmpty()) return null
        
        // Fallback to highest quality
        return variants.filter { it.resolution != null }
            .maxByOrNull { (it.bandwidth ?: 0L) + (parseResolution(it.resolution) * 1000L) }
            ?: variants.firstOrNull()
    }

    private fun parseResolution(resolution: String?): Int {
        if (resolution == null) return 0
        return try {
            val parts = resolution.split('x')
            if (parts.size == 2) parts[0].toInt() * parts[1].toInt() else 0
        } catch (_: Exception) { 0 }
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
            
            // 1. Detect Ad Tags (DATERANGE) and strip them (just in case)
            if (DATERANGE_REGEX.matcher(line).find()) {
                i++
                continue
            }

            // 2. Convert Prefetch Segments to regular EXTINF
            val prefetchMatcher = PREFETCH_REGEX.matcher(line)
            if (prefetchMatcher.find()) {
                val segmentUrl = prefetchMatcher.group(1)
                if (!segmentUrl.isNullOrBlank()) {
                    tempOutput.add("#EXTINF:$formattedDuration,")
                    tempOutput.add(segmentUrl)
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
