package com.akumasdk.samtch.ui.screens.player.models

enum class PortraitMode {
    VIDEO_AND_CHAT,
    AUDIO_AND_CHAT,
    CHAT_ONLY
}

data class ChatContentConfig(
    val isCompact: Boolean,
    val showInput: Boolean,
    val refreshTrigger: Int,
    val isFullscreen: Boolean = false,
    val portraitMode: PortraitMode? = null,
    val onToggleMode: (() -> Unit)? = null,
    val onInteraction: (() -> Unit)? = null
)
