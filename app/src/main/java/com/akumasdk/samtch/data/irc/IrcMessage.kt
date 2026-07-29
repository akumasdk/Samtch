package com.akumasdk.samtch.data.irc

import java.util.UUID

data class IrcMessage(
    val id: String,
    val raw: String,
    val prefix: String,
    val command: String,
    val params: List<String> = listOf(),
    val tags: Map<String, String> = mapOf(),
) {
    fun isLoginFailed(): Boolean = command == "NOTICE" && params.getOrNull(0) == "*" && params.getOrNull(1) == "Login authentication failed"

    companion object {
        private fun unescapeIrcTagValue(value: String): String {
            val idx = value.indexOf('\\')
            if (idx == -1) return value

            return buildString(value.length) {
                var i = 0
                while (i < value.length) {
                    if (value[i] == '\\' && i + 1 < value.length) {
                        when (value[i + 1]) {
                            ':' -> append(';')
                            's' -> append(' ')
                            'r' -> append('\r')
                            'n' -> append('\n')
                            '\\' -> append('\\')
                            else -> {
                                append(value[i])
                                append(value[i + 1])
                            }
                        }
                        i += 2
                    } else {
                        append(value[i])
                        i++
                    }
                }
            }
        }

        fun parse(line: String): IrcMessage {
            var current = line
            val tags = mutableMapOf<String, String>()
            if (current.startsWith("@")) {
                val spaceIdx = current.indexOf(' ')
                val tagsStr = current.substring(1, spaceIdx)
                tagsStr.split(";").forEach { tag ->
                    val parts = tag.split("=")
                    if (parts.size == 2) {
                        tags[parts[0]] = unescapeIrcTagValue(parts[1])
                    }
                }
                current = current.substring(spaceIdx + 1)
            }

            var prefix = ""
            if (current.startsWith(":")) {
                val spaceIdx = current.indexOf(' ')
                prefix = current.substring(1, spaceIdx)
                current = current.substring(spaceIdx + 1)
            }

            val parts = current.split(" :", limit = 2)
            val mainPart = parts[0]
            val trailingPart = parts.getOrNull(1)

            val mainParts = mainPart.split(" ")
            val command = mainParts[0]
            val params = mutableListOf<String>()
            if (mainParts.size > 1) {
                params.addAll(mainParts.subList(1, mainParts.size))
            }
            if (trailingPart != null) {
                params.add(trailingPart)
            }

            // Ensure stable ID
            val id = tags["id"] ?: UUID.randomUUID().toString()

            return IrcMessage(id, line, prefix, command, params, tags)
        }
    }
}
