package com.akumasdk.samtch.data.emote

import com.akumasdk.samtch.data.model.TwitchUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitchBadgeDto(
    @SerialName("setID") val setID: String = "",
    val version: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("image1x") val image1x: String? = null,
    @SerialName("image2x") val image2x: String? = null,
    @SerialName("image4x") val image4x: String? = null,
    val user: TwitchUser? = null
) {
    val bestUrl: String? get() = image4x ?: image2x ?: image1x
    val isGlobal: Boolean get() = user == null
}

@Serializable
data class TwitchBadgeSetsResponse(
    val data: TwitchBadgeSetsData? = null
)

@Serializable
data class TwitchBadgeSetsData(
    val badges: List<TwitchBadgeDto>? = null,
    val user: TwitchUserBadgeData? = null
)

@Serializable
data class TwitchUserBadgeData(
    val id: String = "",
    val displayBadges: List<TwitchBadgeDto> = emptyList(),
    val broadcastBadges: List<TwitchBadgeDto> = emptyList()
)
