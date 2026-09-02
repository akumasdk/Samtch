package com.akumasdk.samtch.data.api.helix

import android.util.Log
import com.akumasdk.samtch.data.api.helix.dto.*
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelixApiClient @Inject constructor(
    private val helixApi: HelixApi,
    private val authManager: TwitchAuthManager
) {
    companion object {
        private const val TAG = "HelixApiClient"
    }

    suspend fun getGlobalBadges(): Result<List<BadgeSetDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn || auth.authToken.isNullOrEmpty()) {
            return Result.success(emptyList())
        }

        val response = helixApi.getGlobalBadges()
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch global badges: ${response.status}")
        }
        response.body<DataListDto<BadgeSetDto>>().data
    }

    suspend fun getChannelBadges(broadcasterId: String): Result<List<BadgeSetDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn || auth.authToken.isNullOrEmpty()) {
            return Result.success(emptyList())
        }

        val response = helixApi.getChannelBadges(broadcasterId)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch channel badges: ${response.status}")
        }
        response.body<DataListDto<BadgeSetDto>>().data
    }

    suspend fun getUsers(logins: List<String>? = null, ids: List<String>? = null): Result<List<UserDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn) throw Exception("Helix requires authentication")

        val response = helixApi.getUsers(logins, ids)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch users: ${response.status}")
        }
        response.body<DataListDto<UserDto>>().data
    }

    suspend fun getUserIdByName(name: String): Result<String> = runCatching {
        getUsers(logins = listOf(name)).getOrThrow().firstOrNull()?.id 
            ?: throw Exception("User not found: $name")
    }

    suspend fun getStreams(logins: List<String>): Result<List<StreamDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn) throw Exception("Helix requires authentication")

        val response = helixApi.getStreams(logins)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch streams: ${response.status}")
        }
        response.body<DataListDto<StreamDto>>().data
    }

    suspend fun getStreamMetadata(login: String): Result<StreamDto?> = runCatching {
        getStreams(listOf(login)).getOrThrow().firstOrNull()
    }

    suspend fun getGlobalEmotes(): Result<List<HelixEmoteDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn || auth.authToken.isNullOrEmpty()) {
            return Result.success(emptyList())
        }

        val response = helixApi.getGlobalEmotes()
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch global emotes")
        }
        response.body<HelixEmoteResponse>().data
    }

    suspend fun getChannelEmotes(broadcasterId: String): Result<List<HelixEmoteDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn || auth.authToken.isNullOrEmpty()) {
            return Result.success(emptyList())
        }

        val response = helixApi.getChannelEmotes(broadcasterId)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch channel emotes")
        }
        response.body<HelixEmoteResponse>().data
    }

    suspend fun getUserEmotes(userId: String): Result<List<HelixEmoteDto>> = runCatching {
        val auth = authManager.getAuthState()
        if (!auth.isLoggedIn || auth.authToken.isNullOrEmpty()) {
            return Result.success(emptyList())
        }

        val response = helixApi.getUserEmotes(userId)
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            Log.w(TAG, "Failed to fetch user emotes: ${response.status}. Body: $errorBody")
            
            // If it's an auth error, it's likely missing 'user:read:emotes' scope.
            // We return an empty list to avoid constant error logging and retries.
            if (response.status.value == 401 || response.status.value == 403) {
                return Result.success(emptyList())
            }
            
            throw Exception("Failed to fetch user emotes: ${response.status}")
        }
        response.body<HelixEmoteResponse>().data
    }
}
