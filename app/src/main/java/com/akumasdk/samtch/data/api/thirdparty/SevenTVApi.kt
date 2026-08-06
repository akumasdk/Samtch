package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.data.emote.SevenTVEmoteSet
import com.akumasdk.samtch.data.emote.SevenTVUserResponse
import com.akumasdk.samtch.util.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SevenTVApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getGlobalEmotes(): SevenTVEmoteSet {
        return client.get(Constants.ThirdParty.SevenTV.API_GLOBAL).body()
    }

    suspend fun getChannelEmotes(userId: String): SevenTVUserResponse {
        return client.get(Constants.ThirdParty.SevenTV.API_USER.format(userId)).body()
    }
}
