package com.akumasdk.samtch.util

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object ScriptLoader {
    private val scriptCache = ConcurrentHashMap<String, String>()

    /**
     * Preloads all scripts into memory.
     */
    fun initialize(context: Context) {
        val scripts = listOf(
            "js/common/scroll_unlocker.js",
            "js/common/splash_controller.js",
            "js/common/app_banners_remover.js",
            "js/chat/bttv.js",
            "js/chat/ui_cleaner.js",
            "js/chat/chat_loader_observer.js",
            "js/player/ui_cleaner.js",
            "js/player/video_swap.js",
            "js/player/link_disabler.js",
            "js/player/controls_injector.js",
            "js/player/visibility_monitor.js"
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
