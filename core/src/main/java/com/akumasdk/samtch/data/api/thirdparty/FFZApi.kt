package com.akumasdk.samtch.data.api.thirdparty

import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.NetworkUtil
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

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
    private val client = NetworkUtil.ktorClient

    suspend fun getGlobalEmotes(): FFZGlobalResponse {
        return client.get(Constants.ThirdParty.FFZ.API_GLOBAL).body()
    }

    suspend fun getChannelEmotes(userId: String): FFZRoomResponse {
        return client.get(Constants.ThirdParty.FFZ.API_USER.format(userId)).body()
    }
}
