package com.akumasdk.samtch.util

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
