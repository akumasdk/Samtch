package com.akumasdk.samtch.ui.components.chat

import android.util.Log
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.akumasdk.samtch.data.badge.BadgeRepository
import com.akumasdk.samtch.data.badge.TwitchBadgeDto
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.util.Constants

object ChatMessageMapper {

    private data class EmoteOccurrence(
        val id: String,
        val code: String,
        val url: String,
        val source: String,
        val range: IntRange,
        val isZeroWidth: Boolean = false
    )

    private fun Char.isWordSeparator(): Boolean = isWhitespace()

    private inline fun String.forEachWord(action: (word: String, startIndex: Int) -> Unit) {
        var wordStart = 0
        for (index in indices) {
            if (this[index].isWordSeparator()) {
                if (index > wordStart) {
                    action(substring(wordStart, index), wordStart)
                }
                wordStart = index + 1
            }
        }
        if (length > wordStart) {
            action(substring(wordStart), wordStart)
        }
    }

    fun mapToUiState(channelName: String, message: IrcMessage): ChatMessageUiState {
        if (message.command == "PRIVMSG") {
            val displayName = message.tags["display-name"] ?: message.prefix.substringBefore("!")
            val colorHex = message.tags["color"]
            val userColor = try {
                if (colorHex.isNullOrEmpty()) Color.Unspecified
                else Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                Color.Unspecified
            }

            val messageText = message.params.getOrNull(1) ?: ""
            val isAction = messageText.startsWith("\u0001ACTION ")
            val cleanText = if (isAction) {
                messageText.removePrefix("\u0001ACTION ").removeSuffix("\u0001")
            } else {
                messageText
            }

            val occurrences = mutableListOf<EmoteOccurrence>()

            // 1. Parse Twitch emotes
            val twitchEmotesTag = message.tags["emotes"]
            if (!twitchEmotesTag.isNullOrEmpty()) {
                twitchEmotesTag.split("/").forEach { emoteData ->
                    val parts = emoteData.split(":")
                    if (parts.size == 2) {
                        val id = parts[0]
                        val url = Constants.Twitch.Templates.EMOTE_CDN.format(id)
                        parts[1].split(",").forEach { rangeStr ->
                            val rangeParts = rangeStr.split("-")
                            if (rangeParts.size == 2) {
                                val startUtf16 = rangeParts[0].toIntOrNull() ?: 0
                                val endUtf16 = rangeParts[1].toIntOrNull() ?: 0
                                try {
                                    val code = messageText.substring(startUtf16, endUtf16 + 1)
                                    val adjStart = if (isAction) (startUtf16 - 8).coerceAtLeast(0) else startUtf16
                                    val adjEnd = if (isAction) (endUtf16 - 8).coerceAtLeast(0) else endUtf16
                                    occurrences.add(EmoteOccurrence(id, code, url, "Twitch", adjStart..adjEnd))
                                } catch (e: Exception) {
                                    Log.e("ChatMessageMapper", "Error parsing Twitch emote range", e)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Parse 3rd party emotes
            cleanText.forEachWord { word, start ->
                val end = start + word.length - 1
                if (occurrences.none { it.range.first <= start && it.range.last >= end }) {
                    val emote = EmoteRepository.getEmote(channelName, word)
                    if (emote != null) {
                        occurrences.add(EmoteOccurrence(emote.id, word, emote.url, emote.type.name, start..end, emote.isZeroWidth))
                    }
                }
            }

            occurrences.sortBy { it.range.first }

            val badgesInfo = parseBadgesInfo(message, channelName)
            val badgeUrls = badgesInfo.mapNotNull { it.bestUrl }
            val emotes = mutableListOf<EmoteInfo>()
            val annotatedString = buildAnnotatedString {
                var lastPos = 0
                var i = 0
                while (i < occurrences.size) {
                    val occurrence = occurrences[i]
                    
                    if (occurrence.range.first > lastPos) {
                        val text = cleanText.substring(lastPos, occurrence.range.first)
                        val style = if (isAction) SpanStyle(color = userColor, fontWeight = FontWeight.Bold) else SpanStyle()
                        withStyle(style) {
                            append(text)
                        }
                    }

                    val cluster = mutableListOf(occurrence)
                    var j = i + 1
                    while (j < occurrences.size) {
                        val next = occurrences[j]
                        if (next.isZeroWidth) {
                            cluster.add(next)
                            j++
                        } else {
                            break
                        }
                    }

                    val inlineId = "cluster_${i}_${occurrence.id}"
                    val combinedUrl = cluster.joinToString("|") { it.url }
                    val emoteInfo = EmoteInfo(inlineId, cluster.first().code, combinedUrl, cluster.first().source, occurrence.isZeroWidth)
                    emotes.add(emoteInfo)
                    
                    appendInlineContent(inlineId, cluster.first().code)
                    
                    lastPos = cluster.last().range.last + 1
                    i = j
                }
                if (lastPos < cleanText.length) {
                    val text = cleanText.substring(lastPos)
                    val style = if (isAction) SpanStyle(color = userColor, fontWeight = FontWeight.Bold) else SpanStyle()
                    withStyle(style) {
                        append(text)
                    }
                }
            }

            return ChatMessageUiState.PrivMessageUi(
                id = message.id, // Use stable ID from IrcMessage
                contentType = "privmsg",
                displayName = displayName,
                userColor = userColor,
                messageText = cleanText,
                annotatedString = annotatedString,
                emotes = emotes,
                badgeUrls = badgeUrls,
                badges = badgesInfo,
                isAction = isAction
            )
        }

        return ChatMessageUiState.SystemMessageUi(
            id = message.id, // Use stable ID from IrcMessage
            contentType = "system",
            message = message.raw
        )
    }

    private fun parseBadgesInfo(message: IrcMessage, channelName: String): List<TwitchBadgeDto> {
        val badgesTag = message.tags["badges"] ?: return emptyList()
        return badgesTag.split(",").mapNotNull { badgeStr ->
            val parts = badgeStr.split("/")
            if (parts.size == 2) {
                val setId = parts[0]
                val version = parts[1]
                BadgeRepository.getBadge(channelName, setId, version)
            } else {
                null
            }
        }
    }
}
