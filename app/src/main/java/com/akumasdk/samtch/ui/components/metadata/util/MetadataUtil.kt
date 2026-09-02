package com.akumasdk.samtch.ui.components.metadata.util

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

fun unifyPreviewUrl(url: String?): String? {
    if (url == null) return null
    return url.replace("{width}", "640")
        .replace("{height}", "360")
        .replace("-853x480", "-640x360")
        .replace("-1280x720", "-640x360")
        .replace("-1920x1080", "-640x360")
}

fun getAlternatingPreviewUrl(url: String?, key: Any?): String? {
    if (url == null) return null
    val trigger = (key as? Int) ?: 0
    
    val base = url.replace("{width}", "WIDTH").replace("{height}", "HEIGHT")
        .replace("640x360", "WIDTHxHEIGHT")
        .replace("1280x720", "WIDTHxHEIGHT")
        .replace("853x480", "WIDTHxHEIGHT")
        .replace("1920x1080", "WIDTHxHEIGHT")
    
    val (w, h) = if (trigger % 2 == 0) "640" to "360" else "1280" to "720"
    return base.replace("WIDTH", w).replace("HEIGHT", h)
}
