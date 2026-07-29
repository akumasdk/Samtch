package com.akumasdk.samtch.data.api.thirdparty

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SevenTVEmoteSet(val emotes: List<SevenTVEmote> = emptyList())

@Serializable
data class SevenTVEmote(val id: String, val name: String, val data: SevenTVEmoteData, val flags: Int = 0) {
    val isZeroWidth: Boolean get() = (flags and 256) != 0
}

@Serializable
data class SevenTVEmoteData(val host: SevenTVHost)

@Serializable
data class SevenTVHost(val url: String, val files: List<SevenTVFile>)

@Serializable
data class SevenTVFile(val name: String, val format: String)

@Serializable
data class SevenTVUserResponse(val emote_set: SevenTVEmoteSet? = null)

object SevenTVApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getGlobalEmotes(): SevenTVEmoteSet {
        return client.get("https://7tv.io/v3/emote-sets/global").body()
    }

    suspend fun getChannelEmotes(userId: String): SevenTVUserResponse {
        return client.get("https://7tv.io/v3/users/twitch/$userId").body()
    }
}
