package com.akumasdk.samtch.data.api

import com.akumasdk.samtch.util.Constants

/**
 * Centralized service to handle Twitch preview image URL processing.
 */
object PreviewImageService {
    
    const val DEFAULT_WIDTH = 1920
    const val DEFAULT_HEIGHT = 1080
    
    const val NOTIFICATION_WIDTH = 1024
    const val NOTIFICATION_HEIGHT = 576

    /**
     * Cleans up a Twitch preview URL by replacing width and height template markers.
     */
    fun cleanUrl(url: String, width: Int = DEFAULT_WIDTH, height: Int = DEFAULT_HEIGHT): String {
        return if (url.contains("{width}") || url.contains("{height}")) {
            url.replace("{width}", width.toString())
               .replace("{height}", height.toString())
        } else if (url.contains("-853x480.jpg")) {
            // Also attempt to upscale the static template if it's the 853x480 version
            url.replace("-853x480.jpg", "-${width}x${height}.jpg")
        } else {
            url
        }
    }

    /**
     * Provides a processed preview URL for a given channel name, with optional resolution.
     * If the URL is null or empty, it falls back to the default Twitch preview template.
     */
    fun getProcessedUrl(url: String?, channelName: String, width: Int = DEFAULT_WIDTH, height: Int = DEFAULT_HEIGHT): String {
        val rawUrl = if (url.isNullOrBlank()) {
            Constants.Twitch.Templates.PREVIEW_URL.format(channelName.lowercase())
        } else {
            url
        }
        return cleanUrl(rawUrl, width, height)
    }
}
