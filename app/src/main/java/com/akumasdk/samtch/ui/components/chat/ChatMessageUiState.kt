package com.akumasdk.samtch.ui.components.chat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.akumasdk.samtch.data.badge.TwitchBadgeDto

@Immutable
data class EmoteInfo(
    val id: String,
    val code: String,
    val url: String,
    val source: String = "",
    val isZeroWidth: Boolean = false
)

@Immutable
sealed interface ChatMessageUiState {
    val id: String
    val contentType: String

    data class PrivMessageUi(
        override val id: String,
        override val contentType: String = "privmsg",
        val displayName: String,
        val userColor: Color,
        val messageText: String,
        val annotatedString: AnnotatedString,
        val emotes: List<EmoteInfo>,
        val badgeUrls: List<String> = emptyList(),
        val badges: List<TwitchBadgeDto> = emptyList(),
        val isAction: Boolean = false
    ) : ChatMessageUiState

    data class SystemMessageUi(
        override val id: String,
        override val contentType: String = "system",
        val message: String
    ) : ChatMessageUiState
}
