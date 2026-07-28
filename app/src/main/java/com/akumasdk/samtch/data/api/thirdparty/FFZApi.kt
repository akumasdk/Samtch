package com.akumasdk.samtch.data.api.thirdparty

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FFZGlobalResponse(val default_sets: List<Int>, val sets: Map<String, FFZSet>)

@Serializable
data class FFZSet(val emotes: List<FFZEmote>)

@Serializable
data class FFZEmote(
    val id: Int,
    val name: String,
    val urls: Map<String, String>,
    val animated: Map<String, String>? = null
)

@Serializable
data class FFZRoomResponse(val sets: Map<String, FFZSet>)

object FFZApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getGlobalEmotes(): FFZGlobalResponse {
        return client.get("https://api.frankerfacez.com/v1/set/global").body()
    }

    suspend fun getChannelEmotes(userId: String): FFZRoomResponse {
        return client.get("https://api.frankerfacez.com/v1/room/id/$userId").body()
    }
}
