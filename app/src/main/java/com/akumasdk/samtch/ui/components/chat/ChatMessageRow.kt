package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun ChatMessageRow(
    message: ChatMessageUiState,
    isCompact: Boolean = false,
    onEmoteLongClick: ((EmoteInfo) -> Unit)? = null
) {
    val fontSize = if (isCompact) 14.sp else 18.sp
    
    when (message) {
        is ChatMessageUiState.PrivMessageUi -> {
            val userColor = if (message.userColor == Color.Unspecified) SamtchTheme.colors.defaultUserColor else message.userColor
            
            val combinedEmotes = remember(message.emotes, message.badgeUrls) {
                val badgesAsEmotes = message.badgeUrls.mapIndexed { index, url ->
                    EmoteInfo(
                        id = "badge_${message.id}_$index",
                        code = "badge",
                        url = url
                    )
                }
                badgesAsEmotes + message.emotes
            }

            val fullAnnotatedString = buildAnnotatedString {
                // Inline Badges
                message.badgeUrls.forEachIndexed { index, _ ->
                    appendInlineContent("badge_${message.id}_$index", "[badge]")
                    append(" ")
                }

                // Name and Message content in one flow
                withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                    append(message.displayName)
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
                onEmoteLongClick = onEmoteLongClick,
                style = TextStyle(
                    color = if (message.isAction) userColor else SamtchTheme.colors.primaryText,
                    fontSize = fontSize,
                    fontWeight = if (message.isAction) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
        is ChatMessageUiState.SystemMessageUi -> {
            Text(
                text = message.message,
                color = SamtchTheme.colors.secondaryText,
                fontSize = if (isCompact) 12.sp else 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
