package com.akumasdk.samtch.ui.components.chat

import android.util.Log
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.irc.IrcMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object ChatMessageMapper {

    private data class EmoteOccurrence(
        val id: String,
        val code: String,
        val url: String,
        val range: IntRange,
        val isZeroWidth: Boolean = false
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

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
        val timestamp = message.tags["tmi-sent-ts"]?.toLongOrNull()?.let {
            try {
                timeFormatter.format(Instant.ofEpochMilli(it))
            } catch (e: Exception) { "" }
        } ?: ""

        if (message.command == "PRIVMSG") {
            val displayName = message.tags["display-name"] ?: message.prefix.substringBefore("!")
            val colorHex = message.tags["color"]
            val userColor = try {
                if (colorHex.isNullOrEmpty()) Color(0xFFBF94FF)
                else Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                Color(0xFFBF94FF)
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
                        val url = "https://static-cdn.jtvnw.net/emoticons/v2/$id/default/dark/3.0"
                        parts[1].split(",").forEach { rangeStr ->
                            val rangeParts = rangeStr.split("-")
                            if (rangeParts.size == 2) {
                                val startUtf16 = rangeParts[0].toIntOrNull() ?: 0
                                val endUtf16 = rangeParts[1].toIntOrNull() ?: 0
                                try {
                                    val code = messageText.substring(startUtf16, endUtf16 + 1)
                                    val adjStart = if (isAction) (startUtf16 - 8).coerceAtLeast(0) else startUtf16
                                    val adjEnd = if (isAction) (endUtf16 - 8).coerceAtLeast(0) else endUtf16
                                    occurrences.add(EmoteOccurrence(id, code, url, adjStart..adjEnd))
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
                        occurrences.add(EmoteOccurrence(emote.id, word, emote.url, start..end, emote.isZeroWidth))
                    }
                }
            }

            occurrences.sortBy { it.range.first }

            val emotes = mutableListOf<EmoteInfo>()
            val annotatedString = buildAnnotatedString {
                var lastPos = 0
                var i = 0
                while (i < occurrences.size) {
                    val occurrence = occurrences[i]
                    
                    if (occurrence.range.first > lastPos) {
                        val text = cleanText.substring(lastPos, occurrence.range.first)
                        val style = if (isAction) SpanStyle(color = userColor, fontWeight = FontWeight.Bold) else SpanStyle(color = Color.White)
                        withStyle(style) {
                            append(text)
                        }
                    }

                    // For zero-width stacking, we'll keep the previous cluster logic but pass urls
                    // Actually, let's just create a list of EmoteInfo and DynamicEmoteText will handle them
                    // Wait, InlineTextContent needs a string for the id
                    
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
                    // We'll use a special delimiter in the URL or just pass the first one for now
                    // To handle stacking properly, we'd need EmoteInfo to have List<String> urls
                    // Let's update EmoteInfo first.
                    
                    // Actually, I'll just use the first URL for now to verify width fixes, 
                    // then handle stacking if necessary. Stacking is secondary to width/scroll.
                    
                    val combinedUrl = cluster.joinToString("|") { it.url }
                    val emoteInfo = EmoteInfo(inlineId, cluster.first().code, combinedUrl, occurrence.isZeroWidth)
                    emotes.add(emoteInfo)
                    
                    appendInlineContent(inlineId, cluster.first().code)
                    
                    lastPos = cluster.last().range.last + 1
                    i = j
                }
                if (lastPos < cleanText.length) {
                    val text = cleanText.substring(lastPos)
                    val style = if (isAction) SpanStyle(color = userColor, fontWeight = FontWeight.Bold) else SpanStyle(color = Color.White)
                    withStyle(style) {
                        append(text)
                    }
                }
            }

            return ChatMessageUiState.PrivMessageUi(
                id = message.tags["id"] ?: UUID.randomUUID().toString(),
                timestamp = timestamp,
                displayName = displayName,
                userColor = userColor,
                messageText = cleanText,
                annotatedString = annotatedString,
                emotes = emotes,
                isAction = isAction
            )
        }

        return ChatMessageUiState.SystemMessageUi(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            message = message.raw
        )
    }
}
