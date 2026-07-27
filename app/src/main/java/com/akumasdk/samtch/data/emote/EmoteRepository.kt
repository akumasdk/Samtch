package com.akumasdk.samtch.data.emote

import android.util.Log
import com.akumasdk.samtch.service.TwitchGqlService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object EmoteRepository {
    private const val TAG = "EmoteRepository"
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    data class GlobalEmoteState(
        val bttvEmotes: Map<String, Emote> = emptyMap(),
        val seventvEmotes: Map<String, Emote> = emptyMap(),
        val ffzEmotes: Map<String, Emote> = emptyMap(),
        val isLoaded: Boolean = false
    )

    data class ChannelEmoteState(
        val bttvEmotes: Map<String, Emote> = emptyMap(),
        val seventvEmotes: Map<String, Emote> = emptyMap(),
        val ffzEmotes: Map<String, Emote> = emptyMap(),
        val isLoaded: Boolean = false
    )

    private val _globalState = MutableStateFlow(GlobalEmoteState())
    val globalState = _globalState.asStateFlow()

    private val _channelStates = ConcurrentHashMap<String, MutableStateFlow<ChannelEmoteState>>()

    fun getChannelState(channelName: String) = _channelStates.getOrPut(channelName) {
        MutableStateFlow(ChannelEmoteState())
    }.asStateFlow()

    private val BTTV_ZERO_WIDTH = setOf(
        "SoSnowy", "IceCold", "SantaHat", "TopHat", "ReinDeer", "CandyCane", "cvMask", "cvHazmat"
    )

    suspend fun loadGlobalEmotes() = withContext(Dispatchers.IO) {
        if (_globalState.value.isLoaded) return@withContext
        try {
            val bttvMap = mutableMapOf<String, Emote>()
            val seventvMap = mutableMapOf<String, Emote>()
            val ffzMap = mutableMapOf<String, Emote>()

            // Load BTTV Global
            try {
                val bttvGlobal: List<BTTVEmote> = client.get("https://api.betterttv.net/3/cached/emotes/global").body()
                bttvGlobal.forEach {
                    bttvMap[it.code] = Emote(
                        it.id, it.code, "https://cdn.betterttv.net/emote/${it.id}/3x", EmoteType.BTTV,
                        isZeroWidth = it.code in BTTV_ZERO_WIDTH
                    )
                }
            } catch (e: Exception) { Log.e(TAG, "BTTV Global load failed", e) }

            // Load 7TV Global
            try {
                val seventvGlobal: SevenTVEmoteSet = client.get("https://7tv.io/v3/emote-sets/global").body()
                seventvGlobal.emotes.forEach { emote ->
                    val hostUrl = emote.data.host.url
                    val bestFile = emote.data.host.files.find { it.name == "4x.webp" }
                                  ?: emote.data.host.files.find { it.format == "WEBP" }
                                  ?: emote.data.host.files.firstOrNull()
                    
                    val path = bestFile?.name ?: "4x.webp"
                    val baseUrl = if (hostUrl.startsWith("//")) "https:$hostUrl" else if (hostUrl.startsWith("http")) hostUrl else "https://$hostUrl"
                    val url = "$baseUrl/$path"
                    
                    seventvMap[emote.name] = Emote(emote.id, emote.name, url, EmoteType.SEVENTV, isZeroWidth = emote.isZeroWidth)
                }
                Log.d(TAG, "Loaded ${seventvGlobal.emotes.size} 7TV global emotes")
            } catch (e: Exception) { Log.e(TAG, "7TV Global load failed", e) }
            
            // Load FFZ Global
            try {
                val ffzGlobal: FFZGlobalResponse = client.get("https://api.frankerfacez.com/v1/set/global").body()
                ffzGlobal.default_sets.forEach { setId ->
                    ffzGlobal.sets[setId.toString()]?.emotes?.forEach { emote ->
                        val url = emote.animated?.get("4") ?: emote.animated?.get("2") ?: emote.animated?.get("1")
                                 ?: emote.urls["4"] ?: emote.urls["2"] ?: emote.urls["1"] ?: ""
                        if (url.isNotEmpty()) {
                            val fullUrl = when {
                                url.startsWith("http") -> url
                                url.startsWith("//") -> "https:$url"
                                else -> "https:$url"
                            }
                            ffzMap[emote.name] = Emote(emote.id.toString(), emote.name, fullUrl, EmoteType.FFZ)
                        }
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "FFZ Global load failed", e) }

            _globalState.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                isLoaded = true
            ) }
            Log.d(TAG, "Global emotes loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading global emotes", e)
        }
    }

    suspend fun loadChannelEmotes(channelName: String) = withContext(Dispatchers.IO) {
        val stateFlow = _channelStates.getOrPut(channelName) { MutableStateFlow(ChannelEmoteState()) }
        if (stateFlow.value.isLoaded) return@withContext

        try {
            val bttvMap = mutableMapOf<String, Emote>()
            val seventvMap = mutableMapOf<String, Emote>()
            val ffzMap = mutableMapOf<String, Emote>()
            
            val metadata = TwitchGqlService.getStreamMetadata(channelName)
            var userId = metadata?.user?.id
            
            if (userId == null) {
                Log.d(TAG, "Metadata User ID null, trying GetUserId fallback for $channelName")
                userId = TwitchGqlService.getUserId(channelName)
            }
            
            if (userId == null) {
                Log.e(TAG, "Failed to get User ID for $channelName, channel emotes won't load")
                return@withContext
            }

            // Load BTTV Channel
            try {
                val bttvChannel: BTTVChannelResponse = client.get("https://api.betterttv.net/3/cached/users/twitch/$userId").body()
                (bttvChannel.channelEmotes + bttvChannel.sharedEmotes).forEach {
                    bttvMap[it.code] = Emote(
                        it.id, it.code, "https://cdn.betterttv.net/emote/${it.id}/3x", EmoteType.BTTV,
                        isZeroWidth = it.code in BTTV_ZERO_WIDTH
                    )
                }
            } catch (e: Exception) { Log.e(TAG, "BTTV Channel load failed for $channelName", e) }

            // Load 7TV Channel
            try {
                val seventvUser: SevenTVUserResponse = client.get("https://7tv.io/v3/users/twitch/$userId").body()
                seventvUser.emote_set.emotes.forEach { emote ->
                    val hostUrl = emote.data.host.url
                    val bestFile = emote.data.host.files.find { it.name == "4x.webp" }
                                  ?: emote.data.host.files.find { it.format == "WEBP" }
                                  ?: emote.data.host.files.firstOrNull()
                    
                    val path = bestFile?.name ?: "4x.webp"
                    val baseUrl = if (hostUrl.startsWith("//")) "https:$hostUrl" else if (hostUrl.startsWith("http")) hostUrl else "https://$hostUrl"
                    val url = "$baseUrl/$path"
                    
                    seventvMap[emote.name] = Emote(emote.id, emote.name, url, EmoteType.SEVENTV, isZeroWidth = emote.isZeroWidth)
                }
                Log.d(TAG, "Loaded ${seventvUser.emote_set.emotes.size} 7TV emotes for $channelName")
            } catch (e: Exception) { Log.e(TAG, "7TV Channel load failed for $channelName", e) }

            // Load FFZ Channel
            try {
                val ffzRoom: FFZRoomResponse = client.get("https://api.frankerfacez.com/v1/room/id/$userId").body()
                ffzRoom.sets.values.forEach { set ->
                    set.emotes.forEach { emote ->
                        val url = emote.animated?.get("4") ?: emote.animated?.get("2") ?: emote.animated?.get("1")
                                 ?: emote.urls["4"] ?: emote.urls["2"] ?: emote.urls["1"] ?: ""
                        if (url.isNotEmpty()) {
                            val fullUrl = when {
                                url.startsWith("http") -> url
                                url.startsWith("//") -> "https:$url"
                                else -> "https:$url"
                            }
                            ffzMap[emote.name] = Emote(emote.id.toString(), emote.name, fullUrl, EmoteType.FFZ)
                        }
                    }
                }
                Log.d(TAG, "Loaded FFZ emotes for $channelName")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading FFZ emotes for $channelName", e)
            }

            stateFlow.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                isLoaded = true
            ) }
            Log.d(TAG, "Channel emotes loaded for $channelName")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emotes for channel $channelName", e)
        }
    }

    fun getEmote(channelName: String, code: String): Emote? {
        val channelState = _channelStates[channelName]?.value
        val globalState = _globalState.value
        
        return channelState?.seventvEmotes?.get(code)
            ?: channelState?.bttvEmotes?.get(code)
            ?: channelState?.ffzEmotes?.get(code)
            ?: globalState.seventvEmotes[code]
            ?: globalState.bttvEmotes[code]
            ?: globalState.ffzEmotes[code]
    }
}
