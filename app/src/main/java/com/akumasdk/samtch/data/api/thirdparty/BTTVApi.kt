package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.data.emote.BTTVChannelResponse
import com.akumasdk.samtch.data.emote.BTTVEmote
import com.akumasdk.samtch.util.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BTTVApi @Inject constructor(
    private val client: HttpClient
) {
    suspend fun getGlobalEmotes(): Result<List<BTTVEmote>> = runCatching {
        val response: HttpResponse = client.get(Constants.ThirdParty.BTTV.API_GLOBAL)
        if (response.status.isSuccess()) {
            response.body()
        } else {
            throw Exception("BTTV API error: ${response.status}")
        }
    }

    suspend fun getChannelEmotes(userId: String): Result<BTTVChannelResponse> = runCatching {
        val response: HttpResponse = client.get(Constants.ThirdParty.BTTV.API_USER.format(userId))
        if (response.status.isSuccess()) {
            response.body()
        } else {
            throw Exception("BTTV API error: ${response.status}")
        }
    }
}
