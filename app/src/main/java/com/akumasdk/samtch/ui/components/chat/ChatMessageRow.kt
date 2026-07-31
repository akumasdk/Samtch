package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ChatMessageRow(
    message: ChatMessageUiState,
    isCompact: Boolean = false
) {
    val fontSize = if (isCompact) 14.sp else 18.sp
    
    when (message) {
        is ChatMessageUiState.PrivMessageUi -> {
            val annotatedString = buildAnnotatedString {
                // 1. Add Badges
                message.badgeUrls.forEachIndexed { index, _ ->
                    appendInlineContent("badge_$index", "[badge]")
                }

                // 2. Add Name
                withStyle(SpanStyle(color = message.userColor, fontWeight = FontWeight.Bold)) {
                    append(message.displayName)
                }

                // 3. Add Separator
                if (!message.isAction) {
                    append(": ")
                } else {
                    append(" ")
                }

                // 4. Add Message Content
                append(message.annotatedString)
            }

            // Combine Badge inline content
            val badgeInlineContent = message.badgeUrls.mapIndexed { index, url ->
                val id = "badge_$index"
                val size = if (isCompact) 14.sp else 18.sp
                id to InlineTextContent(
                    Placeholder(size, size, PlaceholderVerticalAlign.Center)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }.toMap()

            DynamicEmoteText(
                text = annotatedString,
                emotes = message.emotes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                isCompact = isCompact,
                style = TextStyle(
                    color = if (message.isAction) message.userColor else Color.White,
                    fontSize = fontSize,
                    fontWeight = if (message.isAction) FontWeight.Bold else FontWeight.Normal
                ),
                additionalInlineContent = badgeInlineContent
            )
        }
        is ChatMessageUiState.SystemMessageUi -> {
            Text(
                text = message.message,
                color = Color.Gray,
                fontSize = if (isCompact) 12.sp else 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
