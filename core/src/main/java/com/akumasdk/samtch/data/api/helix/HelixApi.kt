package com.akumasdk.samtch.data.api.helix

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.NetworkUtil
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ValidateResponse(
    val client_id: String,
    val login: String? = null,
    val scopes: List<String>? = null,
    val user_id: String? = null,
    val expires_in: Int? = null
)

object HelixApi {
    private const val TAG = "HelixApi"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val client = NetworkUtil.ktorClient
    
    suspend fun getClientId(context: android.content.Context): String {
        val auth = TwitchAuthManager.getAuthState(context)
        if (auth.isLoggedIn && auth.clientId != null) {
            return auth.clientId
        }
        return TwitchGqlService.getDynamicClientId()
    }

    suspend fun validateToken(token: String): ValidateResponse? {
        return try {
            Log.d(TAG, "Validating OAuth token...")
            val response = client.get(Constants.Twitch.Api.HELIX_VALIDATE) {
                header("Authorization", "OAuth $token")
            }
            val responseBody = response.bodyAsText()
            Log.d(TAG, "Validate response: $responseBody")
            
            if (response.status.value == 200) {
                val body: ValidateResponse = json.decodeFromString(responseBody)
                Log.d(TAG, "Token validated for user: ${body.login}")
                body
            } else {
                Log.w(TAG, "Token validation failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating token", e)
            null
        }
    }
    
    private fun HttpRequestBuilder.addAuth(context: android.content.Context) {
        val auth = TwitchAuthManager.getAuthState(context)
        if (auth.isLoggedIn && auth.authToken != null) {
            bearerAuth(auth.authToken)
        }
    }

    suspend fun getGlobalBadges(context: android.content.Context): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_GLOBAL_BADGES) {
            header("Client-Id", clientId)
            addAuth(context)
        }
    }

    suspend fun getChannelBadges(context: android.content.Context, broadcasterId: String): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_CHANNEL_BADGES) {
            header("Client-Id", clientId)
            addAuth(context)
            parameter("broadcaster_id", broadcasterId)
        }
    }

    suspend fun getUsers(context: android.content.Context, logins: List<String>? = null, ids: List<String>? = null): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_USERS) {
            header("Client-Id", clientId)
            addAuth(context)
            logins?.forEach { parameter("login", it) }
            ids?.forEach { parameter("id", it) }
        }
    }

    suspend fun getStreams(context: android.content.Context, logins: List<String>): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_STREAMS) {
            header("Client-Id", clientId)
            addAuth(context)
            logins.forEach { parameter("user_login", it) }
        }
    }

    suspend fun getGlobalEmotes(context: android.content.Context): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_GLOBAL_EMOTES) {
            header("Client-Id", clientId)
            addAuth(context)
        }
    }

    suspend fun getChannelEmotes(context: android.content.Context, broadcasterId: String): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_CHANNEL_EMOTES) {
            header("Client-Id", clientId)
            addAuth(context)
            parameter("broadcaster_id", broadcasterId)
        }
    }

    suspend fun getUserEmotes(context: android.content.Context, userId: String): HttpResponse {
        val clientId = getClientId(context)
        return client.get(Constants.Twitch.Api.HELIX_USER_EMOTES) {
            header("Client-Id", clientId)
            addAuth(context)
            parameter("user_id", userId)
        }
    }
}
