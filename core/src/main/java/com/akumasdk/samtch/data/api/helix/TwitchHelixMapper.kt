package com.akumasdk.samtch.data.api.helix

import com.akumasdk.samtch.data.api.helix.dto.StreamDto
import com.akumasdk.samtch.data.api.helix.dto.UserDto
import com.akumasdk.samtch.data.model.*

object TwitchHelixMapper {
    fun mapHelixToMetadata(user: UserDto, stream: StreamDto?): TwitchStreamMetadata {
        return TwitchStreamMetadata(
            user = TwitchUser(
                id = user.id,
                login = user.name,
                displayName = user.displayName,
                description = user.description,
                profileImageUrl = user.avatarUrl,
                createdAt = user.createdAt,
                roles = TwitchRoles(isPartner = user.broadcasterType == "partner"),
                stream = stream?.let {
                    TwitchStream(
                        id = "", // Helix stream doesn't give ID easily in the same DTO sometimes
                        title = it.title ?: "",
                        type = "live",
                        viewersCount = it.viewerCount,
                        previewImageUrl = null,
                        createdAt = it.startedAt,
                        game = TwitchGame(name = it.category ?: ""),
                    )
                },
            ),
        )
    }
}
