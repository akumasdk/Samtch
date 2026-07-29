package com.akumasdk.samtch.data.api.helix.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BadgeSetDto(
    @SerialName("set_id") val id: String,
    val versions: List<BadgeDto>,
)

@Serializable
data class BadgeDto(
    val id: String,
    val title: String,
    @SerialName("image_url_1x") val imageUrlLow: String,
    @SerialName("image_url_2x") val imageUrlMedium: String,
    @SerialName("image_url_4x") val imageUrlHigh: String,
)
