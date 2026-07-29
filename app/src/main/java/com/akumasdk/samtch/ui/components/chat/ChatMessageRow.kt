package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges
                message.badgeUrls.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(if (isCompact) 14.dp else 18.dp)
                    )
                }

                // Name
                if (!message.isAction) {
                    val annotatedName = buildAnnotatedString {
                        withStyle(SpanStyle(color = message.userColor, fontWeight = FontWeight.Bold)) {
                            append(message.displayName)
                        }
                        append(": ")
                    }
                    Text(
                        text = annotatedName,
                        fontSize = fontSize
                    )
                }

                // Message text with dynamic emotes
                DynamicEmoteText(
                    text = message.annotatedString,
                    emotes = message.emotes,
                    isCompact = isCompact,
                    style = TextStyle(
                        color = if (message.isAction) message.userColor else Color.White,
                        fontSize = fontSize,
                        fontWeight = if (message.isAction) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
        is ChatMessageUiState.SystemMessageUi -> {
            Text(
                text = message.message,
                color = Color.Gray,
                fontSize = if (isCompact) 12.sp else 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
