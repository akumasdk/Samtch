package com.akumasdk.samtch.data.emote

data class GlobalEmoteState(
    val twitchEmotes: Map<String, Emote> = emptyMap(),
    val bttvEmotes: Map<String, Emote> = emptyMap(),
    val seventvEmotes: Map<String, Emote> = emptyMap(),
    val ffzEmotes: Map<String, Emote> = emptyMap(),
    val isTwitchLoaded: Boolean = false,
    val isBttvLoaded: Boolean = false,
    val isSeventvLoaded: Boolean = false,
    val isFfzLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
) {
    val isLoaded: Boolean get() = isTwitchLoaded || isBttvLoaded || isSeventvLoaded || isFfzLoaded
}

data class ChannelEmoteState(
    val twitchEmotes: Map<String, Emote> = emptyMap(),
    val bttvEmotes: Map<String, Emote> = emptyMap(),
    val seventvEmotes: Map<String, Emote> = emptyMap(),
    val ffzEmotes: Map<String, Emote> = emptyMap(),
    val isTwitchLoaded: Boolean = false,
    val isBttvLoaded: Boolean = false,
    val isSeventvLoaded: Boolean = false,
    val isFfzLoaded: Boolean = false,
    val loadedWithAuth: Boolean = false
) {
    val isLoaded: Boolean get() = isTwitchLoaded || isBttvLoaded || isSeventvLoaded || isFfzLoaded
}

data class UserEmoteState(
    val twitchEmotes: Map<String, Emote> = emptyMap(),
    val isLoaded: Boolean = false,
    val userId: String? = null
)
