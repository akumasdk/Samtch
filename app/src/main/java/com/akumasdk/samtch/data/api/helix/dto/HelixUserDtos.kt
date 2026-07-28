package com.akumasdk.samtch.data.api.helix.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    @SerialName("login") val name: String,
    @SerialName("display_name") val displayName: String,
    val type: String = "",
    @SerialName("broadcaster_type") val broadcasterType: String = "",
    val description: String = "",
    @SerialName("profile_image_url") val avatarUrl: String = "",
    @SerialName("offline_image_url") val offlineImageUrl: String = "",
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class StreamDto(
    @SerialName("viewer_count") val viewerCount: Int,
    @SerialName("user_login") val userLogin: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("game_name") val category: String? = null,
    val title: String? = null,
)
