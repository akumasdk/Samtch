package com.akumasdk.samtch.data.badge

data class GlobalBadgeState(
    val badges: Map<String, Map<String, TwitchBadgeDto>> = emptyMap(),
    val isLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
)

data class ChannelBadgeState(
    val badges: Map<String, Map<String, TwitchBadgeDto>> = emptyMap(),
    val displayBadges: Map<String, TwitchBadgeDto> = emptyMap(),
    val isLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
)
