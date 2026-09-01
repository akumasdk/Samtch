package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.data.emote.SevenTVEmoteSet
import com.akumasdk.samtch.data.emote.SevenTVUserResponse
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.NetworkUtil
import io.ktor.client.call.body
import io.ktor.client.request.get

object SevenTVApi {
    private val client = NetworkUtil.ktorClient

    suspend fun getGlobalEmotes(): SevenTVEmoteSet {
        return client.get(Constants.ThirdParty.SevenTV.API_GLOBAL).body()
    }

    suspend fun getChannelEmotes(userId: String): SevenTVUserResponse {
        return client.get(Constants.ThirdParty.SevenTV.API_USER.format(userId)).body()
    }
}
