package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.util.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FFZGlobalResponse(
    val default_sets: List<Int> = emptyList(),
    val sets: Map<String, FFZSet> = emptyMap()
)

@Serializable
data class FFZSet(val emotes: List<FFZEmote> = emptyList())

@Serializable
data class FFZEmote(
    val id: Int,
    val name: String,
    val urls: Map<String, String> = emptyMap(),
    val animated: Map<String, String>? = null
)

@Serializable
data class FFZRoomResponse(val sets: Map<String, FFZSet> = emptyMap())

object FFZApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }

    suspend fun getGlobalEmotes(): FFZGlobalResponse {
        return client.get(Constants.ThirdParty.FFZ.API_GLOBAL).body()
    }

    suspend fun getChannelEmotes(userId: String): FFZRoomResponse {
        return client.get(Constants.ThirdParty.FFZ.API_USER.format(userId)).body()
    }
}
