package com.akumasdk.samtch.data.emote

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
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

    fun getChannelState(channelName: String) = _channelStates.getOrPut(channelName.lowercase()) {
        MutableStateFlow(ChannelEmoteState())
    }.asStateFlow()

    private val BTTV_ZERO_WIDTH = setOf(
        "SoSnowy", "IceCold", "SantaHat", "TopHat", "ReinDeer", "CandyCane", "cvMask", "cvHazmat"
    )

    suspend fun loadGlobalEmotes(context: android.content.Context) = withContext(Dispatchers.IO) {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (_globalState.value.isLoaded && _globalState.value.loadedWithAuth == auth.isLoggedIn) return@withContext
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
                    parseSevenTVEmote(emote)?.let { seventvMap[it.code] = it }
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

            // Load Global Badges via Helix
            val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
            Log.d(TAG, "Loading global badges. isLoggedIn=${auth.isLoggedIn}")
            val globalBadges = if (auth.isLoggedIn) {
                val badgeResult = HelixApiClient.getGlobalBadges(context)
                val badgeSets = badgeResult.getOrDefault(emptyList())
                Log.d(TAG, "Global badge fetch result size: ${badgeSets.size}")
                mapHelixBadges(badgeSets)
            } else {
                emptyMap()
            }

            _globalState.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                badges = globalBadges,
                isLoaded = true,
                loadedWithAuth = auth.isLoggedIn
            ) }
            Log.d(TAG, "Global emotes and ${globalBadges.size} badge sets loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading global emotes", e)
        }
    }

    suspend fun loadChannelEmotes(context: android.content.Context, channelName: String) = withContext(Dispatchers.IO) {
        val channelLower = channelName.lowercase()
        val stateFlow = _channelStates.getOrPut(channelLower) { MutableStateFlow(ChannelEmoteState()) }
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (stateFlow.value.isLoaded && stateFlow.value.loadedWithAuth == auth.isLoggedIn) return@withContext

        try {
            val bttvMap = mutableMapOf<String, Emote>()
            val seventvMap = mutableMapOf<String, Emote>()
            val ffzMap = mutableMapOf<String, Emote>()
            
            val userId = if (auth.isLoggedIn && auth.userName.equals(channelLower, ignoreCase = true)) {
                auth.userId
            } else {
                HelixApiClient.getUserIdByName(context, channelLower).getOrNull() 
                    ?: TwitchGqlService.getUserId(channelLower) // Fallback to GQL
            }

            if (userId == null) {
                Log.e(TAG, "Failed to get User ID for $channelLower, channel emotes won't load")
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
            } catch (e: Exception) { Log.e(TAG, "BTTV Channel load failed for $channelLower", e) }

            // Load 7TV Channel
            try {
                val seventvUser = SevenTVApi.getChannelEmotes(userId)
                seventvUser.emote_set?.emotes?.forEach { emote ->
                    parseSevenTVEmote(emote)?.let { seventvMap[it.code] = it }
                }
            } catch (e: Exception) { Log.e(TAG, "7TV Channel load failed for $channelLower", e) }

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
            } catch (e: Exception) { Log.e(TAG, "FFZ Channel load failed for $channelLower", e) }

            // Load Channel Badges via Helix
            Log.d(TAG, "Loading channel badges for $channelLower. isLoggedIn=${auth.isLoggedIn}, userId=$userId")
            val channelBadges = if (auth.isLoggedIn) {
                val badgeResult = HelixApiClient.getChannelBadges(context, userId)
                val badgeSets = badgeResult.getOrDefault(emptyList())
                Log.d(TAG, "Channel badge fetch result size for $channelLower: ${badgeSets.size}")
                mapHelixBadges(badgeSets)
            } else {
                emptyMap()
            }

            stateFlow.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                badges = channelBadges,
                isLoaded = true,
                loadedWithAuth = auth.isLoggedIn
            ) }
            Log.d(TAG, "Channel emotes and ${channelBadges.size} badge sets loaded for $channelLower")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emotes for channel $channelLower", e)
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
        val channelState = _channelStates[channelName.lowercase()]?.value
        val globalState = _globalState.value
        
        return channelState?.seventvEmotes?.get(code)
            ?: channelState?.bttvEmotes?.get(code)
            ?: channelState?.ffzEmotes?.get(code)
            ?: globalState.seventvEmotes[code]
            ?: globalState.bttvEmotes[code]
            ?: globalState.ffzEmotes[code]
    }

    fun getAllEmotes(channelName: String): List<Emote> {
        val channelState = _channelStates[channelName.lowercase()]?.value
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
        val channelLower = channelName.lowercase()
        val channelState = _channelStates[channelLower]?.value
        val globalState = _globalState.value

        // If no badges were loaded (e.g. user not logged in), don't log "not found"
        if (globalState.badges.isEmpty() && (channelState?.badges?.isEmpty() != false)) {
            return null
        }

        val badge = channelState?.displayBadges?.get(setId)?.takeIf { it.version == version }
            ?: channelState?.badges?.get(setId)?.get(version)
            ?: globalState.badges[setId]?.get(version)
            
        if (badge == null) {
            // Only log if we actually have some badges loaded, to avoid spam for anonymous users
            Log.d(TAG, "Badge not found: $setId/$version in $channelLower")
            return null
        }

        val url = badge.bestUrl ?: return null
                 
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            else -> "https://$url"
        }
    }

    private fun parseSevenTVEmote(emote: SevenTVEmote): Emote? {
        val data = emote.data ?: return null
        val hostUrl = data.host.url
        
        // Find best quality (webp preferably)
        val bestFile = data.host.files.find { it.name == "4x.webp" }
                      ?: data.host.files.find { it.format == "WEBP" }
                      ?: data.host.files.firstOrNull()
        
        val path = bestFile?.name ?: "4x.webp"
        val baseUrl = when {
            hostUrl.startsWith("//") -> "https:$hostUrl"
            hostUrl.startsWith("http") -> hostUrl
            else -> "https://$hostUrl"
        }
        val url = "$baseUrl/$path"
        
        return Emote(
            id = emote.id,
            code = emote.name,
            url = url,
            type = EmoteType.SEVENTV,
            isZeroWidth = emote.isZeroWidth
        )
    }
}
