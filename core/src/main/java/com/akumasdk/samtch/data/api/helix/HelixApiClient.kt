package com.akumasdk.samtch.data.api.helix

import android.content.Context
import android.util.Log
import com.akumasdk.samtch.data.api.helix.dto.*
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

object HelixApiClient {
    private const val TAG = "HelixApiClient"

    suspend fun getGlobalBadges(context: Context): Result<List<BadgeSetDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) return@runCatching emptyList()

        val response = HelixApi.getGlobalBadges(context)
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            Log.e(TAG, "Failed to fetch global badges: ${response.status} body=$body")
            throw Exception("Failed to fetch global badges: ${response.status}")
        }
        response.body<DataListDto<BadgeSetDto>>().data
    }

    suspend fun getChannelBadges(context: Context, broadcasterId: String): Result<List<BadgeSetDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) return@runCatching emptyList()

        val response = HelixApi.getChannelBadges(context, broadcasterId)
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            Log.e(TAG, "Failed to fetch channel badges for $broadcasterId: ${response.status} body=$body")
            throw Exception("Failed to fetch channel badges: ${response.status}")
        }
        response.body<DataListDto<BadgeSetDto>>().data
    }

    suspend fun getUsers(context: Context, logins: List<String>? = null, ids: List<String>? = null): Result<List<UserDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) throw Exception("Helix requires authentication")

        val response = HelixApi.getUsers(context, logins, ids)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch users: ${response.status}")
        }
        response.body<DataListDto<UserDto>>().data
    }

    suspend fun getUserIdByName(context: Context, name: String): Result<String> = runCatching {
        getUsers(context, logins = listOf(name)).getOrThrow().firstOrNull()?.id 
            ?: throw Exception("User not found: $name")
    }

    suspend fun getStreams(context: Context, logins: List<String>): Result<List<StreamDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) throw Exception("Helix requires authentication")

        val response = HelixApi.getStreams(context, logins)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch streams: ${response.status}")
        }
        response.body<DataListDto<StreamDto>>().data
    }

    suspend fun getStreamMetadata(context: Context, login: String): Result<StreamDto?> = runCatching {
        getStreams(context, listOf(login)).getOrThrow().firstOrNull()
    }

    suspend fun getGlobalEmotes(context: Context): Result<List<HelixEmoteDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) return@runCatching emptyList()

        val response = HelixApi.getGlobalEmotes(context)
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            Log.e(TAG, "Failed to fetch global emotes: ${response.status} body=$body")
            throw Exception("Failed to fetch global emotes")
        }
        response.body<HelixEmoteResponse>().data
    }

    suspend fun getChannelEmotes(context: Context, broadcasterId: String): Result<List<HelixEmoteDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) return@runCatching emptyList()

        val response = HelixApi.getChannelEmotes(context, broadcasterId)
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            Log.e(TAG, "Failed to fetch channel emotes for $broadcasterId: ${response.status} body=$body")
            throw Exception("Failed to fetch channel emotes")
        }
        response.body<HelixEmoteResponse>().data
    }

    suspend fun getUserEmotes(context: Context, userId: String): Result<List<HelixEmoteDto>> = runCatching {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn) return@runCatching emptyList()

        val response = HelixApi.getUserEmotes(context, userId)
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            Log.e(TAG, "Failed to fetch user emotes for $userId: ${response.status} body=$body")
            throw Exception("Failed to fetch user emotes")
        }
        response.body<HelixEmoteResponse>().data
    }
}
