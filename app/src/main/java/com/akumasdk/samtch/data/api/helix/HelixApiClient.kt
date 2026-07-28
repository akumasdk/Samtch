package com.akumasdk.samtch.data.api.helix

import com.akumasdk.samtch.data.api.helix.dto.*
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import io.ktor.client.call.body
import io.ktor.http.isSuccess

object HelixApiClient {

    suspend fun getGlobalBadges(): List<BadgeSetDto> {
        if (!TwitchAuthManager.getAuthState().isLoggedIn) return emptyList()
        return try {
            val response = HelixApi.getGlobalBadges()
            if (!response.status.isSuccess()) return emptyList()
            response.body<DataListDto<BadgeSetDto>>().data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getChannelBadges(broadcasterId: String): List<BadgeSetDto> {
        if (!TwitchAuthManager.getAuthState().isLoggedIn) return emptyList()
        return try {
            val response = HelixApi.getChannelBadges(broadcasterId)
            if (!response.status.isSuccess()) return emptyList()
            response.body<DataListDto<BadgeSetDto>>().data
        } catch (e: Exception) {
            emptyList()
        }
    }
}
