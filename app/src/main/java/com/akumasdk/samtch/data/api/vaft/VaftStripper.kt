package com.akumasdk.samtch.data.api.vaft

import android.util.Log

object VaftStripper {
    private const val TAG = VaftConfig.LOG_TAG
    
    fun stripAdSegments(manifest: String, stripAllSegments: Boolean, info: StreamInfo): String {
        var hasStrippedAdSegments = false
        val lines = manifest.replace("\r", "").split("\n").toMutableList()
        val newAdUrl = "https://twitch.tv"
        
        for (i in lines.indices) {
            var line = lines[i]
            
            // Remove tracking urls which appear in the overlay UI
            line = line
                .replace(Regex("(X-TV-TWITCH-AD-URL=\")(?:[^\"]*)(\")"), "$1${newAdUrl}$2")
                .replace(Regex("(X-TV-TWITCH-AD-CLICK-TRACKING-URL=\")(?:[^\"]*)(\")"), "$1${newAdUrl}$2")
            
            lines[i] = line
            
            if (i < lines.size - 1 && line.startsWith("#EXTINF") && 
                (!line.contains(",live") || stripAllSegments)) {
                
                val segmentUrl = lines[i + 1]
                // AdSegmentCache logic could go here if we were doing native fetch interception
                hasStrippedAdSegments = true
            }
            
            if (line.contains(VaftConfig.AD_SIGNIFIER)) {
                hasStrippedAdSegments = true
            }
        }
        
        if (hasStrippedAdSegments) {
            for (i in lines.indices) {
                // No low latency during ads (otherwise it's possible for the player to prefetch and display ad segments)
                if (lines[i].startsWith("#EXT-X-TWITCH-PREFETCH:")) {
                    lines[i] = ""
                }
            }
        } else {
            info.numStrippedAdSegments = 0
        }
        
        info.isStrippingAdSegments = hasStrippedAdSegments
        return lines.filter { it.isNotEmpty() }.joinToString("\n")
    }

    fun containsAds(manifest: String): Boolean {
        val found = manifest.contains(VaftConfig.AD_SIGNIFIER)
        if (found) {
            Log.d(TAG, "AD SIGNIFIER FOUND in manifest: ${VaftConfig.AD_SIGNIFIER}")
        }
        return found
    }

    fun isMidroll(manifest: String): Boolean {
        val found = manifest.contains("\"MIDROLL\"", ignoreCase = true)
        if (found) {
            Log.d(TAG, "MIDROLL TAG FOUND in manifest.")
        }
        return found
    }

    fun hasDiscontinuity(manifest: String): Boolean {
        val found = manifest.contains("#EXT-X-DISCONTINUITY")
        if (found) {
            Log.d(TAG, "DISCONTINUITY TAG FOUND in manifest.")
        }
        return found
    }
}
