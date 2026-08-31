package com.akumasdk.samtch.util

class ExtM3UParser {
    fun parse(input: String): List<ExtMediaEntry> {
        val entries = mutableListOf<ExtMediaEntry>()
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val mediaNames = mutableMapOf<String, String>() // groupId -> Name

        // First pass: collect all media names
        for (line in lines) {
            if (line.startsWith("#EXT-X-MEDIA")) {
                val attrs = parseAttributes(line.substringAfter(":"))
                val type = attrs["TYPE"]
                val groupId = attrs["GROUP-ID"]
                val name = attrs["NAME"]
                if (type == "VIDEO" && groupId != null && name != null) {
                    mediaNames[groupId] = name
                }
            }
        }

        var currentEntry: ExtMediaEntry? = null

        // Second pass: parse streams
        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    currentEntry = ExtMediaEntry(type = "VIDEO")
                    val attrs = parseAttributes(line.substringAfter(":"))
                    parseStreamInf(line, currentEntry)
                    val videoGroup = attrs["VIDEO"]
                    if (videoGroup != null) {
                        currentEntry.name = mediaNames[videoGroup]
                    }
                    
                    // Fallback name from resolution if still null
                    if (currentEntry.name == null && currentEntry.resolution != null) {
                        currentEntry.name = currentEntry.resolution
                    }
                    
                    entries.add(currentEntry)
                }
                line.startsWith("http") || line.startsWith("https") -> {
                    currentEntry?.apply {
                        playlistUrl = line
                        currentEntry = null
                    }
                }
            }
        }

        return entries
    }

    private fun parseExtMedia(line: String): ExtMediaEntry? {
        val entry = ExtMediaEntry()
        val content = line.substringAfter(":")
        
        parseAttributes(content).forEach { (key, value) ->
            when (key) {
                "TYPE" -> entry.type = value
                "GROUP-ID" -> entry.groupId = value
                "NAME" -> entry.name = value
                "AUTOSELECT" -> entry.autoSelect = value.equals("YES", ignoreCase = true)
                "DEFAULT" -> entry.default = value.equals("YES", ignoreCase = true)
                "URI" -> entry.playlistUrl = value
            }
        }

        return entry
    }

    private fun parseStreamInf(line: String, entry: ExtMediaEntry) {
        val content = line.substringAfter(":")
        parseAttributes(content).forEach { (key, value) ->
            when (key) {
                "BANDWIDTH" -> entry.bandwidth = value.toLongOrNull()
                "RESOLUTION" -> entry.resolution = value
                "CODECS" -> entry.codecs = value
                "VIDEO" -> entry.video = value
                "FRAME-RATE" -> entry.frameRate = value.toDoubleOrNull()
            }
        }
    }

    private fun parseAttributes(content: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val regex = "([A-Z0-9-]+)=([^, \"]+|\"[^\"]*\")".toRegex()
        regex.findAll(content).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].trim('"')
            attributes[key] = value
        }
        return attributes
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
