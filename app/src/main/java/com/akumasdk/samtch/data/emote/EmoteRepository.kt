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
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    data class GlobalEmoteState(
        val bttvEmotes: Map<String, Emote> = emptyMap(),
        val seventvEmotes: Map<String, Emote> = emptyMap(),
        val ffzEmotes: Map<String, Emote> = emptyMap(),
        val badges: Map<String, Map<String, TwitchBadgeDto>> = emptyMap(),
        val isLoaded: Boolean = false
    )

    data class ChannelEmoteState(
        val bttvEmotes: Map<String, Emote> = emptyMap(),
        val seventvEmotes: Map<String, Emote> = emptyMap(),
        val ffzEmotes: Map<String, Emote> = emptyMap(),
        val badges: Map<String, Map<String, TwitchBadgeDto>> = emptyMap(),
        val displayBadges: Map<String, TwitchBadgeDto> = emptyMap(),
        val isLoaded: Boolean = false
    )

    private val _globalState = MutableStateFlow(GlobalEmoteState())
    val globalState = _globalState.asStateFlow()

    private val _channelStates = ConcurrentHashMap<String, MutableStateFlow<ChannelEmoteState>>()
    private val aspectRatioCache = ConcurrentHashMap<String, Float>()

    fun getAspectRatio(url: String): Float? = aspectRatioCache[url]

    fun putAspectRatio(url: String, ratio: Float) {
        aspectRatioCache[url] = ratio
    }

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

            // Load Global Badges via GQL
            val globalBadges = parseFlatBadges(TwitchGqlService.getGlobalBadges())

            _globalState.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                badges = globalBadges,
                isLoaded = true
            ) }
            Log.d(TAG, "Global emotes and ${globalBadges.size} badge sets loaded")
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
                seventvUser.emote_set?.emotes?.forEach { emote ->
                    val hostUrl = emote.data.host.url
                    val bestFile = emote.data.host.files.find { it.name == "4x.webp" }
                                  ?: emote.data.host.files.find { it.format == "WEBP" }
                                  ?: emote.data.host.files.firstOrNull()
                    
                    val path = bestFile?.name ?: "4x.webp"
                    val baseUrl = if (hostUrl.startsWith("//")) "https:$hostUrl" else if (hostUrl.startsWith("http")) hostUrl else "https://$hostUrl"
                    val url = "$baseUrl/$path"
                    
                    seventvMap[emote.name] = Emote(emote.id, emote.name, url, EmoteType.SEVENTV, isZeroWidth = emote.isZeroWidth)
                }
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
            } catch (e: Exception) { Log.e(TAG, "FFZ Channel load failed for $channelName", e) }

            // Load Channel Badges via GQL
            val badgeData = parseBadgeData(TwitchGqlService.getBadgeSets(channelName))

            stateFlow.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                badges = badgeData.first,
                displayBadges = badgeData.second,
                isLoaded = true
            ) }
            Log.d(TAG, "Channel emotes and ${badgeData.first.size} badge sets loaded for $channelName")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emotes for channel $channelName", e)
        }
    }

    private fun parseFlatBadges(responseBody: String?): Map<String, Map<String, TwitchBadgeDto>> {
        if (responseBody == null) return emptyMap()
        return try {
            val response = json.decodeFromString<TwitchBadgeSetsResponse>(responseBody)
            val badgesList = response.data?.badges ?: emptyList()
            groupBadges(badgesList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse global flat badges", e)
            emptyMap()
        }
    }

    private fun groupBadges(badges: List<TwitchBadgeDto>): Map<String, Map<String, TwitchBadgeDto>> {
        val result = mutableMapOf<String, MutableMap<String, TwitchBadgeDto>>()
        badges.forEach { badge ->
            if (badge.setID.isNotEmpty() && badge.version.isNotEmpty()) {
                result.getOrPut(badge.setID) { mutableMapOf() }[badge.version] = badge
            }
        }
        return result
    }

    private fun parseBadgeData(responseBody: String?): Pair<Map<String, Map<String, TwitchBadgeDto>>, Map<String, TwitchBadgeDto>> {
        if (responseBody == null) return Pair(emptyMap(), emptyMap())
        return try {
            val response = json.decodeFromString<TwitchBadgeSetsResponse>(responseBody)
            val data = response.data
            
            // Definitions from root badges and broadcastBadges
            val allDefinitions = mutableListOf<TwitchBadgeDto>()
            data?.badges?.let { allDefinitions.addAll(it) }
            data?.user?.broadcastBadges?.let { allDefinitions.addAll(it) }
            
            val setsResult = groupBadges(allDefinitions)
            
            // Equipped icons
            val displayBadges = data?.user?.displayBadges ?: emptyList()
            val displayResult = mutableMapOf<String, TwitchBadgeDto>()
            val discoveredGlobalBadges = mutableListOf<TwitchBadgeDto>()

            displayBadges.forEach { badge ->
                displayResult[badge.setID] = badge
                
                if (badge.isGlobal) {
                    discoveredGlobalBadges.add(badge)
                }
            }
            
            if (discoveredGlobalBadges.isNotEmpty()) {
                _globalState.update { state ->
                    val newBadges = state.badges.toMutableMap()
                    val newDiscovered = groupBadges(discoveredGlobalBadges)
                    newDiscovered.forEach { (setId, versions) ->
                        val currentVersions = newBadges[setId]?.toMutableMap() ?: mutableMapOf()
                        currentVersions.putAll(versions)
                        newBadges[setId] = currentVersions
                    }
                    state.copy(badges = newBadges)
                }
            }
            
            Pair(setsResult, displayResult)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse channel flat badge data", e)
            Pair(emptyMap(), emptyMap())
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

    fun getBadgeUrl(channelName: String, setId: String, version: String): String? {
        val channelState = _channelStates[channelName]?.value
        val globalState = _globalState.value
        
        val badge = channelState?.displayBadges?.get(setId)?.takeIf { it.version == version }
            ?: channelState?.badges?.get(setId)?.get(version)
            ?: globalState.badges[setId]?.get(version)
            
        if (badge == null) {
            Log.d(TAG, "Badge not found: $setId/$version in $channelName")
        }

        val url = badge?.bestUrl ?: return null
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            else -> "https://$url"
        }
    }
}
