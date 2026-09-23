package com.akumasdk.samtch.ui.components.metadata.util

import android.text.Html
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatViewerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

fun formatStreamDuration(startedAt: String?): String {
    if (startedAt == null) return "Offline"
    
    return try {
        val start = Instant.parse(startedAt)
        val now = Instant.now()
        val duration = Duration.between(start, now)
        
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        
        if (hours > 0) {
            String.format(Locale.US, "%dh %dm", hours, minutes)
        } else {
            String.format(Locale.US, "%dm", minutes)
        }
    } catch (_: Exception) {
        "Live"
    }
}

fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        dateString
    }
}

fun unifyPreviewUrl(url: String?, width: String = "640", height: String = "360"): String? {
    if (url == null) return null
    return url.replace("{width}", width)
        .replace("{height}", height)
        .replace("-853x480", "-${width}x${height}")
        .replace("-1280x720", "-${width}x${height}")
        .replace("-1920x1080", "-${width}x${height}")
        .replace("-640x360", "-${width}x${height}")
}

fun getAlternatingPreviewUrl(url: String?, key: Any?): String? {
    if (url == null) return null
    val trigger = (key as? Int) ?: 0
    val (w, h) = if (trigger % 2 == 0) "640" to "360" else "1280" to "720"
    return unifyPreviewUrl(url, w, h)
}

fun cleanMetadataText(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return try {
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    } catch (_: Exception) {
        text.trim()
    }
}
