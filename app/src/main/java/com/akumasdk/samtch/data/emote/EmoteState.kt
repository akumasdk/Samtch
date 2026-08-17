package com.akumasdk.samtch.data.emote

data class GlobalEmoteState(
    val bttvEmotes: Map<String, Emote> = emptyMap(),
    val seventvEmotes: Map<String, Emote> = emptyMap(),
    val ffzEmotes: Map<String, Emote> = emptyMap(),
    val isLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
)

data class ChannelEmoteState(
    val bttvEmotes: Map<String, Emote> = emptyMap(),
    val seventvEmotes: Map<String, Emote> = emptyMap(),
    val ffzEmotes: Map<String, Emote> = emptyMap(),
    val isLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
)
