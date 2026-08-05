package com.akumasdk.samtch.ui.components.metadata.util

import java.time.Duration
import java.time.Instant
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
