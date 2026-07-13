package com.akumasdk.samtch.util

import android.content.Context
import android.util.Log

object ScriptLoader {
    /**
     * Reads a script from the assets folder.
     * @param context Android context
     * @param path Path to the script relative to assets/ (e.g., "js/player/video_swap.js")
     * @return The script content as a String, or an empty string if failed.
     */
    fun loadAsset(context: Context, path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ScriptLoader", "Error loading script from assets: $path", e)
            ""
        }
    }
}
