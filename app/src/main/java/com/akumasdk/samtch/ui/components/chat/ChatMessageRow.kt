package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.data.badge.TwitchBadgeDto
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun ChatMessageRow(
    message: ChatMessageUiState,
    isCompact: Boolean = false,
    onEmoteClick: ((EmoteInfo) -> Unit)? = null,
    onEmoteLongClick: ((EmoteInfo) -> Unit)? = null,
    onBadgeClick: ((TwitchBadgeDto) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
    fontSize: Int = 14,
    emoteSize: Int = 28,
    badgeSize: Int = 18
) {
    val displayFontSize = if (isCompact) (fontSize - 2).sp else fontSize.sp
    
    when (message) {
        is ChatMessageUiState.PrivMessageUi -> {
            val isLightMode = SamtchTheme.colors.chatBackground.luminance() > 0.5f
            val baseUserColor = if (message.userColor == Color.Unspecified) SamtchTheme.colors.defaultUserColor else message.userColor
            
            // Adjust user colors for readability in light mode if they are too light
            val userColor = if (isLightMode && baseUserColor.luminance() > 0.6f) {
                SamtchTheme.colors.accentColor
            } else {
                baseUserColor
            }
            
            val combinedEmotes = remember(message.emotes, message.badges) {
                val badgesAsEmotes = message.badges.mapIndexed { index, badge ->
                    EmoteInfo(
                        id = "badge_${message.id}_$index",
                        code = badge.title,
                        url = badge.bestUrl ?: "",
                        source = "Badge"
                    )
                }
                badgesAsEmotes + message.emotes
            }

            val fullAnnotatedString = buildAnnotatedString {
                // Inline Badges
                message.badges.forEachIndexed { index, _ ->
                    appendInlineContent("badge_${message.id}_$index", "[badge]")
                    append(" ")
                }

                // Name and Message content in one flow
                withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                    pushStringAnnotation(tag = "username", annotation = message.displayName)
                    append(message.displayName)
                    pop()
                }

                if (!message.isAction) {
                    append(": ")
                } else {
                    append(" ")
                }

                append(message.annotatedString)
            }

            DynamicEmoteText(
                text = fullAnnotatedString,
                emotes = combinedEmotes,
                isCompact = isCompact,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                onEmoteClick = { info ->
                    if (info.source == "Badge") {
                        val index = info.id.substringAfterLast("_").toIntOrNull()
                        if (index != null && index < message.badges.size) {
                            onBadgeClick?.invoke(message.badges[index])
                        }
                    } else {
                        onEmoteClick?.invoke(info)
                    }
                },
                onEmoteLongClick = onEmoteLongClick,
                onClick = { offset ->
                    fullAnnotatedString.getStringAnnotations(tag = "username", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            onUserClick?.invoke(annotation.item)
                        }
                },
                emoteSize = emoteSize,
                badgeSize = badgeSize,
                style = TextStyle(
                    color = if (message.isAction) userColor else SamtchTheme.colors.primaryText,
                    fontSize = displayFontSize,
                    fontWeight = if (message.isAction) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
        is ChatMessageUiState.SystemMessageUi -> {
            Text(
                text = message.message,
                color = SamtchTheme.colors.secondaryText,
                fontSize = if (isCompact) (fontSize - 3).sp else (fontSize + 1).sp,
                modifier = Modifier.padding(
                    horizontal = 8.dp, 
                    vertical = if (isCompact) 1.dp else 2.dp
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
