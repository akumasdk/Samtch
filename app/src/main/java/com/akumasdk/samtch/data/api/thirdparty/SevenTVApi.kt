package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.data.emote.SevenTVEmoteSet
import com.akumasdk.samtch.data.emote.SevenTVUserResponse
import com.akumasdk.samtch.util.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SevenTVApi @Inject constructor(
    private val client: HttpClient
) {
    suspend fun getGlobalEmotes(): Result<SevenTVEmoteSet> = runCatching {
        val response: HttpResponse = client.get(Constants.ThirdParty.SevenTV.API_GLOBAL)
        if (response.status.isSuccess()) {
            response.body()
        } else {
            throw Exception("7TV API error: ${response.status}")
        }
    }

    suspend fun getChannelEmotes(userId: String): Result<SevenTVUserResponse> = runCatching {
        val response: HttpResponse = client.get(Constants.ThirdParty.SevenTV.API_USER.format(userId))
        if (response.status.isSuccess()) {
            response.body()
        } else {
            throw Exception("7TV API error: ${response.status}")
        }
    }
}
