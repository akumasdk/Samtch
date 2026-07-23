package com.akumasdk.samtch.util

class ExtM3UParser {
    fun parse(input: String): List<ExtMediaEntry> {
        val entries = mutableListOf<ExtMediaEntry>()
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var currentEntry: ExtMediaEntry? = null

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-MEDIA") -> {
                    currentEntry = parseExtMedia(line)?.also {
                        entries.add(it)
                    }
                }
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    currentEntry?.let {
                        parseStreamInf(line, it)
                    }
                }
                line.startsWith("http") || line.startsWith("https") -> {
                    currentEntry?.apply {
                        playlistUrl = line
                        currentEntry = null // Reset after capturing URL
                    }
                }
            }
        }

        return entries
    }

    private fun parseExtMedia(line: String): ExtMediaEntry? {
        val parts = line.split(',').map { it.trim() }

        if (parts.isEmpty()) return null

        val entry = ExtMediaEntry()

        // Extract TYPE from first part (e.g., "#EXT-X-MEDIA:TYPE=VIDEO")
        val firstPart = parts[0]
        if (firstPart.contains("TYPE=")) {
            entry.type = firstPart.substringAfter("TYPE=").trim()
        }

        // Parse remaining key-value pairs
        parts.drop(1).forEach { part ->
            val keyValue = part.split('=', limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim().trim('"')

                when (key) {
                    "GROUP-ID" -> entry.groupId = value
                    "NAME" -> entry.name = value
                    "AUTOSELECT" -> entry.autoSelect = value.equals("YES", ignoreCase = true)
                    "DEFAULT" -> entry.default = value.equals("YES", ignoreCase = true)
                }
            }
        }

        return entry
    }

    private fun parseStreamInf(line: String, entry: ExtMediaEntry) {
        val parts = line.split(',').map { it.trim() }
        parts.forEach { part ->
            val keyValue = part.split('=', limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim().trim('"')

                when (key) {
                    "BANDWIDTH" -> entry.bandwidth = value.toLongOrNull()
                    "RESOLUTION" -> entry.resolution = value
                    "CODECS" -> entry.codecs = value
                    "VIDEO" -> entry.video = value
                    "FRAME-RATE" -> entry.frameRate = value.toDoubleOrNull()
                }
            }
        }
    }
}

data class ExtMediaEntry(
    var type: String? = null,
    var groupId: String? = null,
    var name: String? = null,
    var autoSelect: Boolean = false,
    var default: Boolean = false,
    var bandwidth: Long? = null,
    var resolution: String? = null,
    var codecs: String? = null,
    var video: String? = null,
    var frameRate: Double? = null,
    var playlistUrl: String? = null
)
