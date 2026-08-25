package com.akumasdk.samtch.util

import android.content.Context
import android.util.Log
import com.akumasdk.samtch.util.Constants.Scripts
import java.util.concurrent.ConcurrentHashMap

object ScriptLoader {
    private val scriptCache = ConcurrentHashMap<String, String>()

    /**
     * Preloads all scripts into memory.
     */
    fun initialize(context: Context) {
        val scripts = listOf(
            Scripts.COMMON_SCROLL_UNLOCKER,
            Scripts.COMMON_SPLASH_CONTROLLER,
            Scripts.COMMON_APP_BANNERS_REMOVER,
            Scripts.CHAT_BTTV,
            Scripts.CHAT_UI_CLEANER,
            Scripts.CHAT_LOADER_OBSERVER
        )

        scripts.forEach { path ->
            if (!scriptCache.containsKey(path)) {
                scriptCache[path] = loadAssetFromAssets(context, path)
            }
        }
        Log.d("ScriptLoader", "All scripts preloaded into memory (${scriptCache.size} scripts)")
    }

    /**
     * Gets a script from cache or loads it if not present.
     * @param context Android context
     * @param path Path to the script relative to assets/
     * @return The script content
     */
    fun getScript(context: Context, path: String): String {
        return scriptCache.getOrPut(path) {
            loadAssetFromAssets(context, path)
        }
    }

    private fun loadAssetFromAssets(context: Context, path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ScriptLoader", "Error loading script from assets: $path", e)
            ""
        }
    }
}
