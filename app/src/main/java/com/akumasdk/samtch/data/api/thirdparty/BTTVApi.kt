package com.akumasdk.samtch.data.api.thirdparty

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BTTVEmote(val id: String, val code: String, val imageType: String)

@Serializable
data class BTTVChannelResponse(
    val channelEmotes: List<BTTVEmote> = emptyList(),
    val sharedEmotes: List<BTTVEmote> = emptyList()
)

object BTTVApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getGlobalEmotes(): List<BTTVEmote> {
        return client.get("https://api.betterttv.net/3/cached/emotes/global").body()
    }

    suspend fun getChannelEmotes(userId: String): BTTVChannelResponse {
        return client.get("https://api.betterttv.net/3/cached/users/twitch/$userId").body()
    }
}
