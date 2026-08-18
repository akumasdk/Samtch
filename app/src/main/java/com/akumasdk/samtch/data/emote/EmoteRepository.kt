package com.akumasdk.samtch.data.emote

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.api.helix.dto.HelixEmoteDto
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

    private val _userEmoteState = MutableStateFlow(UserEmoteState())
    val userEmoteState = _userEmoteState.asStateFlow()

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
            val twitchMap = mutableMapOf<String, Emote>()

            // Load Twitch Global
            if (auth.isLoggedIn) {
                try {
                    HelixApiClient.getGlobalEmotes(context).getOrNull()?.forEach {
                        twitchMap[it.name] = mapHelixEmote(it)
                    }
                } catch (e: Exception) { Log.e(TAG, "Twitch Global load failed", e) }
            }

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

            _globalState.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                isLoaded = true,
                loadedWithAuth = auth.isLoggedIn,
                twitchEmotes = twitchMap
            ) }
            Log.d(TAG, "Global emotes loaded")
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
            val twitchMap = mutableMapOf<String, Emote>()
            
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

            // Load Twitch Channel
            if (auth.isLoggedIn) {
                try {
                    HelixApiClient.getChannelEmotes(context, userId).getOrNull()?.forEach {
                        twitchMap[it.name] = mapHelixEmote(it)
                    }
                } catch (e: Exception) { Log.e(TAG, "Twitch Channel load failed for $channelLower", e) }
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

            stateFlow.update { it.copy(
                bttvEmotes = bttvMap,
                seventvEmotes = seventvMap,
                ffzEmotes = ffzMap,
                isLoaded = true,
                loadedWithAuth = auth.isLoggedIn,
                twitchEmotes = twitchMap
            ) }
            Log.d(TAG, "Channel emotes loaded for $channelLower")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading emotes for channel $channelLower", e)
        }
    }

    suspend fun loadUserEmotes(context: android.content.Context) = withContext(Dispatchers.IO) {
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(context)
        if (!auth.isLoggedIn || auth.userId == null) return@withContext
        if (_userEmoteState.value.isLoaded && _userEmoteState.value.userId == auth.userId) return@withContext

        try {
            val twitchMap = mutableMapOf<String, Emote>()
            HelixApiClient.getUserEmotes(context, auth.userId).getOrNull()?.forEach {
                twitchMap[it.name] = mapHelixEmote(it)
            }

            _userEmoteState.update { it.copy(
                twitchEmotes = twitchMap,
                isLoaded = true,
                userId = auth.userId
            ) }
            Log.d(TAG, "User emotes loaded for ${auth.userName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user emotes", e)
        }
    }

    fun getEmote(channelName: String, code: String): Emote? {
        val channelState = _channelStates[channelName.lowercase()]?.value
        val globalState = _globalState.value
        val userState = _userEmoteState.value
        
        return channelState?.twitchEmotes?.get(code)
            ?: channelState?.seventvEmotes?.get(code)
            ?: channelState?.bttvEmotes?.get(code)
            ?: channelState?.ffzEmotes?.get(code)
            ?: userState.twitchEmotes[code]
            ?: globalState.twitchEmotes[code]
            ?: globalState.seventvEmotes[code]
            ?: globalState.bttvEmotes[code]
            ?: globalState.ffzEmotes[code]
    }

    fun getAllEmotes(channelName: String): List<Emote> {
        val channelState = _channelStates[channelName.lowercase()]?.value
        val globalState = _globalState.value
        val userState = _userEmoteState.value
        
        return buildList {
            channelState?.let {
                addAll(it.twitchEmotes.values)
                addAll(it.seventvEmotes.values)
                addAll(it.bttvEmotes.values)
                addAll(it.ffzEmotes.values)
            }
            addAll(userState.twitchEmotes.values)
            addAll(globalState.twitchEmotes.values)
            addAll(globalState.seventvEmotes.values)
            addAll(globalState.bttvEmotes.values)
            addAll(globalState.ffzEmotes.values)
        }.distinctBy { it.id }
    }

    private fun mapHelixEmote(dto: HelixEmoteDto): Emote {
        val isAnimated = dto.format?.contains("animated") == true
        val format = if (isAnimated) "animated" else "static"
        // Use the template-style URL for better control, falling back to url_4x
        val url = "https://static-cdn.jtvnw.net/emoticons/v2/${dto.id}/$format/dark/3.0"
        
        return Emote(
            id = dto.id,
            code = dto.name,
            url = url,
            type = EmoteType.TWITCH
        )
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
