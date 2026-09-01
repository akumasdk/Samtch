package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.NetworkUtil
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class BTTVEmote(val id: String, val code: String, val imageType: String)

@Serializable
data class BTTVChannelResponse(
    val channelEmotes: List<BTTVEmote> = emptyList(),
    val sharedEmotes: List<BTTVEmote> = emptyList()
)

object BTTVApi {
    private val client = NetworkUtil.ktorClient

    suspend fun getGlobalEmotes(): List<BTTVEmote> {
        return client.get(Constants.ThirdParty.BTTV.API_GLOBAL).body()
    }

    suspend fun getChannelEmotes(userId: String): BTTVChannelResponse {
        return client.get(Constants.ThirdParty.BTTV.API_USER.format(userId)).body()
    }
}
