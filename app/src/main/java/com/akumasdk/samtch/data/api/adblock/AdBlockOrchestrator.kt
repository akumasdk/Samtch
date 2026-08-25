package com.akumasdk.samtch.data.api.adblock

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.util.ExtM3UParser
import com.akumasdk.samtch.util.ExtMediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

class AdBlockOrchestrator(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val m3u8Parser: ExtM3UParser = ExtM3UParser(),
    private val onAdStatusChanged: (AdStatus) -> Unit = {}
) {
    private val TAG = AdBlockConfig.LOG_TAG
    private val streamInfos = ConcurrentHashMap<String, StreamInfo>()
    
    // Cache for access tokens to avoid spamming GQL
    private val tokenCache = ConcurrentHashMap<String, Pair<String, Long>>() // channel_playerType -> (token_sig, timestamp)
    private val TOKEN_EXPIRY = 15 * 60 * 1000 // 15 minutes

    suspend fun getCleanStreamUrl(
        channelName: String,
        targetResolution: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val info = streamInfos.getOrPut(channelName) { 
            Log.d(TAG, "Creating new stream info for $channelName")
            StreamInfo(channelName) 
        }
        
        // 1. Get Access Token for 'site' (Main stream)
        val tokenPair = getCachedPlaybackAccessToken(channelName, "site") ?: run {
            Log.e(TAG, "Failed to get access token for $channelName")
            return@withContext null
        }
        val masterUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
        
        // 2. Fetch Master Playlist
        val masterManifest = fetchManifest(masterUrl) ?: run {
            Log.e(TAG, "Failed to fetch master manifest for $channelName")
            return@withContext null
        }
        val entries = m3u8Parser.parse(masterManifest)
        Log.d(TAG, "Fetched master playlist for $channelName: ${entries.size} variants found.")
        
        // 3. Find the variant matching target resolution (or highest) to check for ads
        val targetVariant = findBestVariant(entries, targetResolution) ?: run {
            Log.e(TAG, "No suitable variant found for $channelName")
            return@withContext null
        }
        val variantUrl = targetVariant.playlistUrl ?: return@withContext null
        Log.d(TAG, "Probing variant [${targetVariant.resolution ?: "original"}] for ads: $variantUrl")
        
        // 4. Check Variant for Ads
        val variantManifest = fetchManifest(variantUrl) ?: run {
            Log.w(TAG, "Failed to fetch variant manifest for $channelName")
            return@withContext null
        }
        
        if (AdBlockStripper.containsAds(variantManifest)) {
            // AD DETECTED
            val wasAlreadyShowingAd = info.isShowingAd
            info.isShowingAd = true
            info.isMidroll = AdBlockStripper.isMidroll(variantManifest)
            info.cleanManifestStreak = 0 // Reset streak immediately
            info.isInitialized = true
            
            Log.d(TAG, "AD DETECTED (midroll=${info.isMidroll}) in main stream for $channelName.")
            
            // If we already have an active backup and we were already in "ad mode",
            // DO NOT return a new URL. This prevents the "swap loop" (reloading the same backup).
            if (wasAlreadyShowingAd && info.activeBackupPlayerType != null) {
                Log.d(TAG, "Already on active backup [${info.activeBackupPlayerType}]. Keeping current stream.")
                return@withContext null
            }

            Log.d(TAG, "Searching for ad-free backup source...")
            
            // 5. Backup Discovery Loop (Mimic vaft.js)
            for (playerType in AdBlockConfig.BACKUP_PLAYER_TYPES) {
                Log.d(TAG, "Attempting ad-free token for type: $playerType")
                val backupUrl = tryGetBackupUrl(channelName, playerType, targetVariant)
                if (backupUrl != null) {
                    val backupManifest = fetchManifest(backupUrl)
                    if (backupManifest != null && !AdBlockStripper.containsAds(backupManifest)) {
                        Log.d(TAG, "SUCCESS: Discovered clean backup ($playerType): $backupUrl")
                        info.activeBackupPlayerType = playerType
                        
                        onAdStatusChanged(AdStatus(
                            hasAds = true,
                            isMidroll = info.isMidroll,
                            playerType = playerType
                        ))
                        
                        return@withContext backupUrl
                    } else {
                        Log.d(TAG, "Backup source $playerType still contains ads. Continuing search...")
                    }
                }
            }
            
            // If no clean backup found, use main stream (it will show ads, but we have no choice)
            Log.w(TAG, "No clean backup found for $channelName. Falling back to main stream.")
            onAdStatusChanged(AdStatus(hasAds = true, isMidroll = info.isMidroll, isStrippingAdSegments = true))
            return@withContext variantUrl
        } else {
            info.cleanManifestStreak++
            
            if (info.isShowingAd) {
                // HYSTERESIS: Wait for 2 clean checks OR a clean manifest with a discontinuity
                val hasDisc = AdBlockStripper.hasDiscontinuity(variantManifest)
                val isTrulyClean = info.cleanManifestStreak >= 2 || hasDisc
                
                if (isTrulyClean) {
                    val reason = if (hasDisc) "discontinuity detected" else "streak confirmed (count=${info.cleanManifestStreak})"
                    Log.d(TAG, "AD SESSION FINISHED ($reason) for $channelName. Restoring main stream.")
                    info.isShowingAd = false
                    info.activeBackupPlayerType = null
                    info.cleanManifestStreak = 0
                    info.isInitialized = true
                    onAdStatusChanged(AdStatus(hasAds = false))
                    return@withContext masterUrl
                } else {
                    Log.d(TAG, "Main stream clean but waiting for sync (streak=${info.cleanManifestStreak}) for $channelName")
                    return@withContext null // Stay on backup for now
                }
            }
            
            // NO CHANGE: We are already on a clean stream. 
            // Returning the URL ONLY if we haven't initialized yet.
            if (!info.isInitialized) {
                info.isInitialized = true
                Log.d(TAG, "Initial discovery: Main stream is CLEAN for $channelName. Returning master URL.")
                return@withContext masterUrl
            }
            return@withContext null
        }
    }

    private suspend fun getCachedPlaybackAccessToken(channelName: String, playerType: String): Pair<String, String>? {
        val key = "${channelName}_$playerType"
        val cached = tokenCache[key]
        val now = System.currentTimeMillis()
        
        if (cached != null && (now - cached.second) < TOKEN_EXPIRY) {
            val parts = cached.first.split("|", limit = 2)
            if (parts.size == 2) return parts[0] to parts[1]
        }
        
        val fresh = TwitchGqlService.getPlaybackAccessToken(channelName, playerType)
        if (fresh != null) {
            tokenCache[key] = "${fresh.first}|${fresh.second}" to now
            return fresh
        }
        return null
    }

    private suspend fun tryGetBackupUrl(
        channelName: String,
        playerType: String, 
        targetVariant: ExtMediaEntry
    ): String? {
        val tokenPair = getCachedPlaybackAccessToken(channelName, playerType) ?: return null
        val backupMasterUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
        
        val backupMasterManifest = fetchManifest(backupMasterUrl) ?: return null
        val backupEntries = m3u8Parser.parse(backupMasterManifest)
        
        // Try to match the resolution of the main stream
        val matchedVariant = findBestVariant(backupEntries, targetVariant.resolution)
        return matchedVariant?.playlistUrl
    }

    private fun findBestVariant(entries: List<ExtMediaEntry>, targetResolution: String?): ExtMediaEntry? {
        val variants = entries.filter { !it.playlistUrl.isNullOrEmpty() }
        if (variants.isEmpty()) return null
        
        if (targetResolution != null) {
            val matched = variants.find { it.resolution == targetResolution }
            if (matched != null) return matched
        }
        
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

    private fun fetchManifest(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().body.string()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching manifest: $url", e)
            null
        }
    }

    fun resetChannel(channelName: String) {
        streamInfos.remove(channelName.lowercase())
        Log.d(TAG, "Reset state for $channelName")
    }
}
