package com.akumasdk.samtch.data.emote

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.dto.BadgeSetDto
import com.akumasdk.samtch.data.api.thirdparty.BTTVApi
import com.akumasdk.samtch.data.api.thirdparty.FFZApi
import com.akumasdk.samtch.data.api.thirdparty.SevenTVApi
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object EmoteRepository {
    private const val TAG = "EmoteRepository"

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
                BTTVApi.getGlobalEmotes().forEach {
                    bttvMap[it.code] = Emote(
                        it.id, it.code, Constants.ThirdParty.BTTV.CDN_EMOTE.format(it.id), EmoteType.BTTV,
                        isZeroWidth = it.code in BTTV_ZERO_WIDTH
                    )
                }
            } catch (e: Exception) { Log.e(TAG, "BTTV Global load failed", e) }

            // Load 7TV Global
            try {
                SevenTVApi.getGlobalEmotes().emotes.forEach { emote ->
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
                val ffzGlobal = FFZApi.getGlobalEmotes()
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

            // Load Global Badges via Helix (Disabled for now)
            // val globalBadges = mapHelixBadges(HelixApiClient.getGlobalBadges())
            val globalBadges = emptyMap<String, Map<String, TwitchBadgeDto>>()

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
            
            val userId = TwitchGqlService.getUserId(channelName)
            if (userId == null) {
                Log.e(TAG, "Failed to get User ID for $channelName, channel emotes won't load")
                return@withContext
            }

            // Load BTTV Channel
            try {
                val bttvChannel = BTTVApi.getChannelEmotes(userId)
                (bttvChannel.channelEmotes + bttvChannel.sharedEmotes).forEach {
                    bttvMap[it.code] = Emote(
                        it.id, it.code, Constants.ThirdParty.BTTV.CDN_EMOTE.format(it.id), EmoteType.BTTV,
                        isZeroWidth = it.code in BTTV_ZERO_WIDTH
                    )
                }
            } catch (e: Exception) { Log.e(TAG, "BTTV Channel load failed for $channelName", e) }

            // Load 7TV Channel
            try {
                val seventvUser = SevenTVApi.getChannelEmotes(userId)
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
                val ffzRoom = FFZApi.getChannelEmotes(userId)
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

            // Load Channel Badges via Helix (Disabled for now)
            // val channelBadges = mapHelixBadges(HelixApiClient.getChannelBadges(userId))
            val channelBadges = emptyMap<String, Map<String, TwitchBadgeDto>>()

            stateFlow.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                badges = channelBadges,
                isLoaded = true
            ) }
            Log.d(TAG, "Channel emotes and ${channelBadges.size} badge sets loaded for $channelName")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emotes for channel $channelName", e)
        }
    }

    private fun mapHelixBadges(badgeSets: List<BadgeSetDto>): Map<String, Map<String, TwitchBadgeDto>> {
        val result = mutableMapOf<String, MutableMap<String, TwitchBadgeDto>>()
        badgeSets.forEach { setDto ->
            val versions = mutableMapOf<String, TwitchBadgeDto>()
            setDto.versions.forEach { badgeDto ->
                versions[badgeDto.id] = TwitchBadgeDto(
                    setID = setDto.id,
                    version = badgeDto.id,
                    title = badgeDto.title,
                    image1x = badgeDto.imageUrlLow,
                    image2x = badgeDto.imageUrlMedium,
                    image4x = badgeDto.imageUrlHigh
                )
            }
            if (versions.isNotEmpty()) {
                result[setDto.id] = versions
            }
        }
        return result
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

    fun getAllEmotes(channelName: String): List<Emote> {
        val channelState = _channelStates[channelName]?.value
        val globalState = _globalState.value
        
        return buildList {
            channelState?.let {
                addAll(it.seventvEmotes.values)
                addAll(it.bttvEmotes.values)
                addAll(it.ffzEmotes.values)
            }
            addAll(globalState.seventvEmotes.values)
            addAll(globalState.bttvEmotes.values)
            addAll(globalState.ffzEmotes.values)
        }.distinctBy { it.id }
    }

    fun getBadgeUrl(channelName: String, setId: String, version: String): String? {
        return null // Badges disabled for now
        /*
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
        */
    }
}
