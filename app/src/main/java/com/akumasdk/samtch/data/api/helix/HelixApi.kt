package com.akumasdk.samtch.data.api.helix

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.util.Constants
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ValidateResponse(
    val client_id: String,
    val login: String? = null,
    val scopes: List<String>? = null,
    val user_id: String? = null,
    val expires_in: Int? = null
)

@Singleton
class HelixApi @Inject constructor(
    private val client: HttpClient,
    private val authManager: TwitchAuthManager,
    private val gqlService: TwitchGqlService,
    private val json: Json
) {
    companion object {
        private const val TAG = "HelixApi"
    }

    suspend fun getClientId(): String {
        val auth = authManager.getAuthState()
        if (auth.isLoggedIn && auth.clientId != null) {
            return auth.clientId
        }
        return gqlService.getDynamicClientId()
    }

    suspend fun validateToken(token: String): ValidateResponse? {
        return try {
            val response = client.get(Constants.Twitch.Api.HELIX_VALIDATE) {
                header("Authorization", "OAuth $token")
            }
            val responseBody = response.bodyAsText()
            if (response.status.value == 200) {
                json.decodeFromString<ValidateResponse>(responseBody)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error validating token", e)
            null
        }
    }
    
    private suspend fun HttpRequestBuilder.addAuth() {
        val auth = authManager.getAuthState()
        if (auth.isLoggedIn && auth.authToken != null) {
            bearerAuth(auth.authToken)
        }
    }

    suspend fun getGlobalBadges(): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_GLOBAL_BADGES) {
            header("Client-Id", clientId)
            addAuth()
        }
    }

    suspend fun getChannelBadges(broadcasterId: String): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_CHANNEL_BADGES) {
            header("Client-Id", clientId)
            addAuth()
            parameter("broadcaster_id", broadcasterId)
        }
    }

    suspend fun getUsers(logins: List<String>? = null, ids: List<String>? = null): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_USERS) {
            header("Client-Id", clientId)
            addAuth()
            logins?.forEach { parameter("login", it) }
            ids?.forEach { parameter("id", it) }
        }
    }

    suspend fun getStreams(logins: List<String>): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_STREAMS) {
            header("Client-Id", clientId)
            addAuth()
            logins.forEach { parameter("user_login", it) }
        }
    }

    suspend fun getGlobalEmotes(): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_GLOBAL_EMOTES) {
            header("Client-Id", clientId)
            addAuth()
        }
    }

    suspend fun getChannelEmotes(broadcasterId: String): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_CHANNEL_EMOTES) {
            header("Client-Id", clientId)
            addAuth()
            parameter("broadcaster_id", broadcasterId)
        }
    }

    suspend fun getUserEmotes(userId: String): HttpResponse {
        val clientId = getClientId()
        return client.get(Constants.Twitch.Api.HELIX_USER_EMOTES) {
            header("Client-Id", clientId)
            addAuth()
            parameter("user_id", userId)
        }
    }
}
