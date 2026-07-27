package com.akumasdk.samtch.ui.components.chat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString

@Immutable
data class EmoteInfo(
    val id: String,
    val code: String,
    val url: String,
    val isZeroWidth: Boolean = false
)

@Immutable
sealed interface ChatMessageUiState {
    val id: String
    val timestamp: String

    data class PrivMessageUi(
        override val id: String,
        override val timestamp: String,
        val displayName: String,
        val userColor: Color,
        val messageText: String,
        val annotatedString: AnnotatedString,
        val emotes: List<EmoteInfo>,
        val isAction: Boolean = false
    ) : ChatMessageUiState

    data class SystemMessageUi(
        override val id: String,
        override val timestamp: String,
        val message: String
    ) : ChatMessageUiState
}
