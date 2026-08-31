package com.akumasdk.samtch.util.metadata

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
    if (startedAt.isNullOrBlank()) return "Offline"
    
    return try {
        // Handle ISO-8601 variations and potential space issues
        val trimmed = startedAt.trim()
        val sanitized = trimmed.replace(" ", "T")
        
        // Ensure it ends with Z if no timezone is present
        var finalStr = sanitized
        if (!finalStr.endsWith("Z") && !finalStr.contains("+")) {
            finalStr = "${finalStr}Z"
        }

        val start = Instant.parse(finalStr)
        val now = Instant.now()
        val duration = Duration.between(start, now)
        
        val totalSeconds = duration.seconds.coerceAtLeast(0)
        
        // If it's more than 48 hours or exactly 0, it might be wrong data
        if (totalSeconds > 172800 || totalSeconds == 0L) return "Live"

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        
        if (hours > 0) {
            String.format(Locale.US, "%dh %dm", hours, minutes)
        } else {
            String.format(Locale.US, "%dm", minutes.coerceAtLeast(1))
        }
    } catch (e: Exception) {
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
        dateString ?: ""
    }
}
