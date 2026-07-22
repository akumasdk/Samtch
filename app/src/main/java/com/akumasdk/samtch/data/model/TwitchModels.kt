package com.akumasdk.samtch.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class TwitchStreamMetadata(
    val user: TwitchUser?
)

data class TwitchUser(
    val id: String,
    val login: String,
    val displayName: String,
    val description: String?,
    val createdAt: String?,
    val roles: TwitchRoles?,
    val stream: TwitchStream?
)

data class TwitchRoles(
    val isPartner: Boolean
)

data class TwitchStream(
    val id: String,
    val title: String,
    val type: String?,
    val viewersCount: Int,
    val createdAt: String?,
    val game: TwitchGame?
)

data class TwitchGame(
    val name: String
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
