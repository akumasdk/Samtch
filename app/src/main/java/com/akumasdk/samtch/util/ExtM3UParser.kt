package com.akumasdk.samtch.util

class ExtM3UParser {
    fun parse(input: String): List<ExtMediaEntry> {
        val entries = mutableListOf<ExtMediaEntry>()
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var currentEntry: ExtMediaEntry? = null

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    currentEntry = ExtMediaEntry(type = "VIDEO")
                    parseStreamInf(line, currentEntry)
                    entries.add(currentEntry)
                }
                line.startsWith("#EXT-X-MEDIA") -> {
                    val entry = parseExtMedia(line)
                    if (entry != null) {
                        entries.add(entry)
                    }
                    currentEntry = null
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
