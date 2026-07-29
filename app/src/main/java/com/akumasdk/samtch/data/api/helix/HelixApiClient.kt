package com.akumasdk.samtch.data.api.helix

import android.util.Log
import com.akumasdk.samtch.data.api.helix.dto.*
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

object HelixApiClient {
    private const val TAG = "HelixApiClient"

    suspend fun getGlobalBadges(): List<BadgeSetDto> {
        return emptyList() // Disabled for now
        /*
        if (!TwitchAuthManager.getAuthState().isLoggedIn) {
            Log.d(TAG, "getGlobalBadges: User not logged in, skipping")
            return emptyList()
        }
        return try {
            Log.d(TAG, "getGlobalBadges: Fetching...")
            val response = HelixApi.getGlobalBadges()
            if (!response.status.isSuccess()) {
                Log.e(TAG, "getGlobalBadges failed: ${response.status} ${response.bodyAsText()}")
                return emptyList()
            }
            val result = response.body<DataListDto<BadgeSetDto>>().data
            Log.d(TAG, "getGlobalBadges success: ${result.size} sets found")
            result
        } catch (e: Exception) {
            Log.e(TAG, "getGlobalBadges error", e)
            emptyList()
        }
        */
    }

    suspend fun getChannelBadges(broadcasterId: String): List<BadgeSetDto> {
        return emptyList() // Disabled for now
        /*
        if (!TwitchAuthManager.getAuthState().isLoggedIn) {
            Log.d(TAG, "getChannelBadges: User not logged in, skipping for $broadcasterId")
            return emptyList()
        }
        return try {
            Log.d(TAG, "getChannelBadges: Fetching for $broadcasterId...")
            val response = HelixApi.getChannelBadges(broadcasterId)
            if (!response.status.isSuccess()) {
                Log.e(TAG, "getChannelBadges failed for $broadcasterId: ${response.status} ${response.bodyAsText()}")
                return emptyList()
            }
            val result = response.body<DataListDto<BadgeSetDto>>().data
            Log.d(TAG, "getChannelBadges success for $broadcasterId: ${result.size} sets found")
            result
        } catch (e: Exception) {
            Log.e(TAG, "getChannelBadges error for $broadcasterId", e)
            emptyList()
        }
        */
    }
}
