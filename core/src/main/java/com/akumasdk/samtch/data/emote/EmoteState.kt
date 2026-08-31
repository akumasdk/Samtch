package com.akumasdk.samtch.data.emote

data class GlobalEmoteState(
    val twitchEmotes: Map<String, Emote> = emptyMap(),
    val bttvEmotes: Map<String, Emote> = emptyMap(),
    val seventvEmotes: Map<String, Emote> = emptyMap(),
    val ffzEmotes: Map<String, Emote> = emptyMap(),
    val isLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
)

data class ChannelEmoteState(
    val twitchEmotes: Map<String, Emote> = emptyMap(),
    val bttvEmotes: Map<String, Emote> = emptyMap(),
    val seventvEmotes: Map<String, Emote> = emptyMap(),
    val ffzEmotes: Map<String, Emote> = emptyMap(),
    val isLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
)

data class UserEmoteState(
    val twitchEmotes: Map<String, Emote> = emptyMap(),
    val isLoaded: Boolean = false,
    val userId: String? = null
)
