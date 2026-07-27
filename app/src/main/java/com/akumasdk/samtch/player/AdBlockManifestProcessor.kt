package com.akumasdk.samtch.player

import android.util.Log

object AdBlockManifestProcessor {
    private const val TAG = "AdBlockProcessor"
    
    // Keywords used by Twitch to signify ad segments or stitched streams
    private val AD_MARKERS = listOf(
        "stitched-ad",
        "stitched",
        "#EXT-X-TWITCH-AD",
        "amazon-adsystem",
        "doubleclick"
    )

    /**
     * Checks if the manifest contains any ad markers.
     */
    fun containsAds(content: String): Boolean {
        return AD_MARKERS.any { content.contains(it, ignoreCase = true) }
    }

    /**
     * Strips ad segments from the manifest content.
     * This mirrors the `stripAdSegments` logic in vaft.js.
     */
    fun stripAds(content: String): String {
        if (!containsAds(content)) return content

        val lines = content.lines()
        val result = mutableListOf<String>()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            // If we find an ad segment marker, skip it and the following URL
            if (line.startsWith("#EXTINF") && i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                // If it's NOT a live segment (doesn't contain ",live") and we are stripping
                if (!line.contains(",live") || AD_MARKERS.any { nextLine.contains(it, ignoreCase = true) }) {
                    Log.d(TAG, "Stripping ad segment: $nextLine")
                    i += 2 // Skip the INF line and the URL line
                    continue
                }
            }

            // Clean tracking URLs in tags
            var cleanedLine = line
            cleanedLine = cleanedLine.replace(Regex("""X-TV-TWITCH-AD-URL="[^"]*""""), """X-TV-TWITCH-AD-URL="https://twitch.tv"""")
            cleanedLine = cleanedLine.replace(Regex("""X-TV-TWITCH-AD-CLICK-TRACKING-URL="[^"]*""""), """X-TV-TWITCH-AD-CLICK-TRACKING-URL="https://twitch.tv"""")

            result.add(cleanedLine)
            i++
        }

        return result.joinToString("\n")
    }
}
