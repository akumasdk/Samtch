package com.akumasdk.samtch.data.emote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Emote(
    val id: String,
    val code: String,
    val url: String,
    val type: EmoteType,
    val isZeroWidth: Boolean = false
)

@Serializable
enum class EmoteType {
    TWITCH, BTTV, SEVENTV, FFZ
}

@Serializable
data class BTTVEmote(
    val id: String,
    val code: String,
    val imageType: String = ""
)

@Serializable
data class BTTVChannelResponse(
    val id: String,
    val bots: List<String> = emptyList(),
    val avatar: String = "",
    val channelEmotes: List<BTTVEmote> = emptyList(),
    val sharedEmotes: List<BTTVEmote> = emptyList()
)

@Serializable
data class SevenTVEmote(
    val id: String = "",
    val name: String = "",
    val flags: Int = 0,
    val data: SevenTVEmoteData? = null
) {
    val isZeroWidth: Boolean get() = (flags and (1 shl 8)) != 0
}

@Serializable
data class SevenTVEmoteData(
    val id: String = "",
    val name: String = "",
    val flags: Int = 0,
    val animated: Boolean = false,
    val host: SevenTVHost = SevenTVHost()
)

@Serializable
data class SevenTVHost(
    val url: String = "",
    val files: List<SevenTVFile> = emptyList()
)

@Serializable
data class SevenTVFile(
    val name: String = "",
    val format: String = ""
)

@Serializable
data class SevenTVEmoteSet(
    val id: String = "",
    val name: String = "",
    val emotes: List<SevenTVEmote> = emptyList()
)

@Serializable
data class SevenTVUserResponse(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("emote_set") val emoteSet: SevenTVEmoteSet? = null,
    val user: SevenTVUserData? = null
)

@Serializable
data class SevenTVUserData(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("emote_set") val emoteSet: SevenTVEmoteSet? = null
)

@Serializable
data class FFZEmote(
    val id: Int,
    val name: String,
    val urls: Map<String, String> = emptyMap(),
    val animated: Map<String, String>? = null
)

@Serializable
data class FFZEmoteSet(
    val id: Int,
    val emotes: List<FFZEmote> = emptyList()
)

@Serializable
data class FFZRoomResponse(
    val sets: Map<String, FFZEmoteSet> = emptyMap()
)

@Serializable
data class FFZGlobalResponse(
    val default_sets: List<Int> = emptyList(),
    val sets: Map<String, FFZEmoteSet> = emptyMap()
)
