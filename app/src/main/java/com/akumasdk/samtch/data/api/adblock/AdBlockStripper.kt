package com.akumasdk.samtch.data.api.adblock

import android.util.Log

object AdBlockStripper {
    private val TAG = AdBlockConfig.LOG_TAG

    fun containsAds(manifest: String): Boolean {
        val found = manifest.contains(AdBlockConfig.AD_SIGNIFIER)
        if (found) {
            Log.d(TAG, "AD SIGNIFIER FOUND in manifest: ${AdBlockConfig.AD_SIGNIFIER}")
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
