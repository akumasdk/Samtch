package com.akumasdk.samtch.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitchStreamMetadata(
    val user: TwitchUser? = null
)

@Serializable
data class TwitchUser(
    val id: String = "",
    val login: String = "",
    val displayName: String = "",
    val description: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: String? = null,
    val roles: TwitchRoles? = null,
    val stream: TwitchStream? = null
)

@Serializable
data class TwitchRoles(
    val isPartner: Boolean = false
)

@Serializable
data class TwitchStream(
    val id: String = "",
    val title: String = "",
    val type: String? = null,
    val viewersCount: Int = 0,
    val previewImageUrl: String? = null,
    val createdAt: String? = null,
    val game: TwitchGame? = null
)

@Serializable
data class TwitchGame(
    val name: String = ""
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)
