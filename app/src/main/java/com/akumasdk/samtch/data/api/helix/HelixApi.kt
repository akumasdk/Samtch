package com.akumasdk.samtch.data.api.helix

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
    }
    
    private suspend fun getClientId(): String = TwitchGqlService.getDynamicClientId()
    
    private fun HttpRequestBuilder.addAuth() {
        val auth = TwitchAuthManager.getAuthState()
        if (auth.isLoggedIn && auth.authToken != null) {
            bearerAuth(auth.authToken)
        }
    }

    suspend fun getGlobalBadges(): HttpResponse {
        val clientId = getClientId()
        Log.d(TAG, "getGlobalBadges: ID=$clientId")
        return client.get("https://api.twitch.tv/helix/chat/badges/global") {
            header("Client-Id", clientId)
            addAuth()
        }
    }

    suspend fun getChannelBadges(broadcasterId: String): HttpResponse {
        val clientId = getClientId()
        Log.d(TAG, "getChannelBadges: CID=$broadcasterId ID=$clientId")
        return client.get("https://api.twitch.tv/helix/chat/badges") {
            header("Client-Id", clientId)
            addAuth()
            parameter("broadcaster_id", broadcasterId)
        }
    }
}
