package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatMessageRow(
    message: ChatMessageUiState
) {
    when (message) {
        is ChatMessageUiState.PrivMessageUi -> {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                // Timestamp
                if (message.timestamp.isNotEmpty()) {
                    Text(
                        text = message.timestamp,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 6.dp)
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
                        fontSize = 14.sp
                    )
                }

                // Message text with dynamic emotes
                DynamicEmoteText(
                    text = message.annotatedString,
                    emotes = message.emotes,
                    style = TextStyle(
                        color = if (message.isAction) message.userColor else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (message.isAction) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
        is ChatMessageUiState.SystemMessageUi -> {
            Text(
                text = message.message,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
