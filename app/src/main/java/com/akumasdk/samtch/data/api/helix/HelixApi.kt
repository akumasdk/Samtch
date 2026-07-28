package com.akumasdk.samtch.data.api.helix

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
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
private data class ValidateResponse(
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

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        /*
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
            level = LogLevel.HEADERS
        }
        */
    }
    
    private suspend fun getClientId(): String {
        return TwitchGqlService.getDynamicClientId()
        /*
        val auth = TwitchAuthManager.getAuthState()
        if (auth.isLoggedIn) {
            auth.clientId?.let { return it }
            
            auth.authToken?.let { token ->
                validateToken(token)?.let { id ->
                    TwitchAuthManager.setValidatedClientId(id)
                    return id
                }
            }
        }
        return TwitchGqlService.getDynamicClientId()
        */
    }

    private suspend fun validateToken(token: String): String? {
        return try {
            Log.d(TAG, "Validating OAuth token...")
            val response = client.get("https://id.twitch.tv/oauth2/validate") {
                header("Authorization", "OAuth $token")
            }
            val responseBody = response.bodyAsText()
            Log.d(TAG, "Validate response: $responseBody")
            
            if (response.status.value == 200) {
                val body: ValidateResponse = json.decodeFromString(responseBody)
                Log.d(TAG, "Token validated. Client-ID: ${body.client_id}")
                body.client_id
            } else {
                Log.w(TAG, "Token validation failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating token", e)
            null
        }
    }
    
    private fun HttpRequestBuilder.addAuth() {
        val auth = TwitchAuthManager.getAuthState()
        if (auth.isLoggedIn && auth.authToken != null) {
            bearerAuth(auth.authToken)
        }
    }

    suspend fun getGlobalBadges(): HttpResponse {
        val clientId = getClientId()
        return client.get("https://api.twitch.tv/helix/chat/badges/global") {
            header("Client-Id", clientId)
            addAuth()
        }
    }

    suspend fun getChannelBadges(broadcasterId: String): HttpResponse {
        val clientId = getClientId()
        return client.get("https://api.twitch.tv/helix/chat/badges") {
            header("Client-Id", clientId)
            addAuth()
            parameter("broadcaster_id", broadcasterId)
        }
    }
}
