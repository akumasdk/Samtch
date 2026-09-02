package com.akumasdk.samtch.data.emote

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.api.helix.dto.HelixEmoteDto
import com.akumasdk.samtch.data.api.thirdparty.*
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class EmoteRepository @Inject constructor(
    private val helixApiClient: HelixApiClient,
    private val bttvApi: BTTVApi,
    private val ffzApi: FFZApi,
    private val sevenTVApi: SevenTVApi,
    private val gqlService: TwitchGqlService,
    private val authManager: TwitchAuthManager
) {
    companion object {
        private const val TAG = "EmoteRepository"
        private val BTTV_ZERO_WIDTH = setOf(
            "SoSnowy", "IceCold", "SantaHat", "TopHat", "ReinDeer", "CandyCane", "cvMask", "cvHazmat"
        )
    }

    private val _globalState = MutableStateFlow(GlobalEmoteState())
    val globalState = _globalState.asStateFlow()

    private val _userEmoteState = MutableStateFlow(UserEmoteState())
    val userEmoteState = _userEmoteState.asStateFlow()

    private val _channelStates = ConcurrentHashMap<String, MutableStateFlow<ChannelEmoteState>>()
    private val _tabCache = ConcurrentHashMap<String, StateFlow<Map<Int, List<Emote>>>>()
    private val _flattenedCache = ConcurrentHashMap<String, StateFlow<List<Emote>>>()
    
    private val repoScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val aspectRatioCache = ConcurrentHashMap<String, Float>()

    fun getAspectRatio(url: String): Float? = aspectRatioCache[url]
    fun putAspectRatio(url: String, ratio: Float) { aspectRatioCache[url] = ratio }

    fun getChannelState(channelName: String) = _channelStates.getOrPut(channelName.lowercase()) {
        MutableStateFlow(ChannelEmoteState())
    }.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun getEmoteTabs(channelName: String, settingsManager: SettingsManager): StateFlow<Map<Int, List<Emote>>> {
        val channelLower = channelName.lowercase()
        @Suppress("UNCHECKED_CAST")
        return _tabCache.getOrPut(channelLower) {
            Log.d(TAG, "Initializing EmoteTabs for $channelLower")
            combine(
                settingsManager.getRecentEmotes(channelLower),
                globalState,
                userEmoteState,
                getChannelState(channelLower)
            ) { recent, global, userState, channelState ->
                val tabs = mutableMapOf<Int, List<Emote>>()
                
                if (recent.isNotEmpty()) {
                    tabs[com.akumasdk.samtch.R.string.emote_menu_recent] = recent
                }

                // Channel Emotes
                val allChannelEmotes = (channelState.twitchEmotes.values + channelState.seventvEmotes.values + channelState.bttvEmotes.values + channelState.ffzEmotes.values).distinctBy { it.id }
                if (allChannelEmotes.isNotEmpty()) {
                    tabs[com.akumasdk.samtch.R.string.emote_menu_channel] = allChannelEmotes
                }

                // User Emotes
                if (userState.twitchEmotes.isNotEmpty()) {
                    tabs[com.akumasdk.samtch.R.string.emote_menu_user] = userState.twitchEmotes.values.toList()
                }

                // Global Twitch
                if (global.twitchEmotes.isNotEmpty()) {
                    tabs[com.akumasdk.samtch.R.string.emote_menu_twitch] = global.twitchEmotes.values.toList()
                }

                // Global 3rd Party
                val allGlobal3rdParty = (global.seventvEmotes.values + global.bttvEmotes.values + global.ffzEmotes.values).toList()
                if (allGlobal3rdParty.isNotEmpty()) {
                    tabs[com.akumasdk.samtch.R.string.emote_menu_global] = allGlobal3rdParty
                }

                Log.d(TAG, "Tabs emission calculated for $channelLower: ${tabs.size} tabs")
                tabs
            }
            .debounce(100.milliseconds) // Avoid UI jumps when multiple providers load back-to-back
            .stateIn(
                scope = repoScope,
                started = SharingStarted.Eagerly, 
                initialValue = emptyMap()
            )
        } as StateFlow<Map<Int, List<Emote>>>
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFlattenedEmotes(channelName: String): StateFlow<List<Emote>> {
        val channelLower = channelName.lowercase()
        @Suppress("UNCHECKED_CAST")
        return _flattenedCache.getOrPut(channelLower) {
            combine(
                globalState,
                userEmoteState,
                getChannelState(channelLower)
            ) { global, userState, channelState ->
                buildList {
                    addAll(channelState.twitchEmotes.values)
                    addAll(channelState.seventvEmotes.values)
                    addAll(channelState.bttvEmotes.values)
                    addAll(channelState.ffzEmotes.values)
                    addAll(userState.twitchEmotes.values)
                    addAll(global.twitchEmotes.values)
                    addAll(global.seventvEmotes.values)
                    addAll(global.bttvEmotes.values)
                    addAll(global.ffzEmotes.values)
                }.distinctBy { it.id }
            }.stateIn(
                scope = repoScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
        } as StateFlow<List<Emote>>
    }

    suspend fun loadGlobalEmotes(force: Boolean = false) = withContext(Dispatchers.IO) {
        val auth = authManager.authStateFlow.first()
        val currentState = _globalState.value
        
        if (!force && currentState.isTwitchLoaded && currentState.loadedWithAuth == auth.isLoggedIn && 
            currentState.isBttvLoaded && currentState.isSeventvLoaded && currentState.isFfzLoaded) {
            return@withContext
        }
        
        Log.d(TAG, "Loading global emotes. isLoggedIn=${auth.isLoggedIn}, force=$force")

        supervisorScope {
            // Twitch Global
            val shouldLoadTwitch = auth.isLoggedIn && (force || !currentState.isTwitchLoaded || !currentState.loadedWithAuth)
            if (shouldLoadTwitch) {
                launch {
                    helixApiClient.getGlobalEmotes().onSuccess { helixEmotes ->
                        val twitchMap = helixEmotes.associateBy({ it.name }, { mapHelixEmote(it) })
                        _globalState.update { it.copy(twitchEmotes = twitchMap, isTwitchLoaded = true, loadedWithAuth = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Twitch Global load failed", e)
                        _globalState.update { it.copy(loadedWithAuth = true) }
                    }
                }
            } else if (!auth.isLoggedIn) {
                _globalState.update { it.copy(twitchEmotes = emptyMap(), isTwitchLoaded = true, loadedWithAuth = false) }
            }

            // BTTV Global
            if (force || !currentState.isBttvLoaded) {
                launch {
                    bttvApi.getGlobalEmotes().onSuccess { bttvList ->
                        val bttvMap = bttvList.associate { it.code to Emote(
                            it.id, it.code, Constants.ThirdParty.BTTV.CDN_EMOTE.format(it.id), EmoteType.BTTV,
                            isZeroWidth = it.code in BTTV_ZERO_WIDTH
                        )}
                        _globalState.update { it.copy(bttvEmotes = bttvMap, isBttvLoaded = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "BTTV Global load failed", e)
                        _globalState.update { it.copy(isBttvLoaded = true) }
                    }
                }
            }

            // 7TV Global
            if (force || !currentState.isSeventvLoaded) {
                launch {
                    sevenTVApi.getGlobalEmotes().onSuccess { sevenTVSet ->
                        val seventvMap = sevenTVSet.emotes.mapNotNull { parseSevenTVEmote(it) }.associateBy { it.code }
                        _globalState.update { it.copy(seventvEmotes = seventvMap, isSeventvLoaded = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "7TV Global load failed", e)
                        _globalState.update { it.copy(isSeventvLoaded = true) }
                    }
                }
            }
            
            // FFZ Global
            if (force || !currentState.isFfzLoaded) {
                launch {
                    ffzApi.getGlobalEmotes().onSuccess { ffzResponse ->
                        val ffzMap = mutableMapOf<String, Emote>()
                        ffzResponse.default_sets.forEach { setId ->
                            ffzGlobalToMap(ffzResponse, setId.toString(), ffzMap)
                        }
                        _globalState.update { it.copy(ffzEmotes = ffzMap, isFfzLoaded = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "FFZ Global load failed", e)
                        _globalState.update { it.copy(isFfzLoaded = true) }
                    }
                }
            }
        }
    }

    private fun ffzGlobalToMap(response: FFZGlobalResponse, setId: String, outMap: MutableMap<String, Emote>) {
        response.sets[setId]?.emotes?.forEach { emote ->
            val url = emote.animated?.get("4") ?: emote.animated?.get("2") ?: emote.animated?.get("1")
                     ?: emote.urls["4"] ?: emote.urls["2"] ?: emote.urls["1"] ?: ""
            if (url.isNotEmpty()) {
                val fullUrl = if (url.startsWith("http") || url.startsWith("//")) {
                    if (url.startsWith("//")) "https:$url" else url
                } else "https:$url"
                outMap[emote.name] = Emote(emote.id.toString(), emote.name, fullUrl, EmoteType.FFZ)
            }
        }
    }

    suspend fun loadChannelEmotes(channelName: String, userId: String? = null, force: Boolean = false) = withContext(Dispatchers.IO) {
        val channelLower = channelName.lowercase()
        val stateFlow = _channelStates.getOrPut(channelLower) { MutableStateFlow(ChannelEmoteState()) }
        val auth = authManager.authStateFlow.first()
        
        val currentState = stateFlow.value
        if (!force && currentState.isTwitchLoaded && currentState.loadedWithAuth == auth.isLoggedIn && 
            currentState.isBttvLoaded && currentState.isSeventvLoaded && currentState.isFfzLoaded) {
             return@withContext
        }

        val resolvedUserId = userId ?: if (auth.isLoggedIn && auth.userName.equals(channelLower, ignoreCase = true)) {
            auth.userId
        } else {
            helixApiClient.getUserIdByName(channelLower).getOrNull() ?: gqlService.getUserId(channelLower)
        }

        if (resolvedUserId == null) {
            stateFlow.update { it.copy(isTwitchLoaded = true, isBttvLoaded = true, isSeventvLoaded = true, isFfzLoaded = true, loadedWithAuth = auth.isLoggedIn) }
            return@withContext
        }

        Log.d(TAG, "Loading channel emotes for $channelLower (ID: $resolvedUserId)")

        supervisorScope {
            // Twitch Channel
            val shouldLoadTwitch = auth.isLoggedIn && (force || !currentState.isTwitchLoaded || !currentState.loadedWithAuth)
            if (shouldLoadTwitch) {
                launch {
                    helixApiClient.getChannelEmotes(resolvedUserId).onSuccess { helixEmotes ->
                        val twitchMap = helixEmotes.associateBy({ it.name }, { mapHelixEmote(it) })
                        stateFlow.update { it.copy(twitchEmotes = twitchMap, isTwitchLoaded = true, loadedWithAuth = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Twitch Channel load failed for $channelLower", e)
                        stateFlow.update { it.copy(isTwitchLoaded = true, loadedWithAuth = true) }
                    }
                }
            } else if (!auth.isLoggedIn) {
                stateFlow.update { it.copy(twitchEmotes = emptyMap(), isTwitchLoaded = true, loadedWithAuth = false) }
            }

            // BTTV Channel
            if (force || !currentState.isBttvLoaded) {
                launch {
                    bttvApi.getChannelEmotes(resolvedUserId).onSuccess { bttvChannel ->
                        val bttvMap = (bttvChannel.channelEmotes + bttvChannel.sharedEmotes).associate { it.code to Emote(
                            it.id, it.code, Constants.ThirdParty.BTTV.CDN_EMOTE.format(it.id), EmoteType.BTTV,
                            isZeroWidth = it.code in BTTV_ZERO_WIDTH
                        )}
                        stateFlow.update { it.copy(bttvEmotes = bttvMap, isBttvLoaded = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "BTTV Channel load failed", e)
                        stateFlow.update { it.copy(isBttvLoaded = true) }
                    }
                }
            }

            // 7TV Channel
            if (force || !currentState.isSeventvLoaded) {
                launch {
                    sevenTVApi.getChannelEmotes(resolvedUserId).onSuccess { seventvUser ->
                        val seventvMap = (seventvUser.emoteSet ?: seventvUser.user?.emoteSet)?.emotes?.mapNotNull { parseSevenTVEmote(it) }?.associateBy { it.code } ?: emptyMap()
                        stateFlow.update { it.copy(seventvEmotes = seventvMap, isSeventvLoaded = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "7TV Channel load failed", e)
                        stateFlow.update { it.copy(isSeventvLoaded = true) }
                    }
                }
            }

            // FFZ Channel
            if (force || !currentState.isFfzLoaded) {
                launch {
                    ffzApi.getChannelEmotes(resolvedUserId).onSuccess { ffzRoom ->
                        val ffzMap = mutableMapOf<String, Emote>()
                        ffzRoom.sets.values.forEach { set ->
                            set.emotes.forEach { emote ->
                                val url = emote.animated?.get("4") ?: emote.animated?.get("2") ?: emote.animated?.get("1")
                                         ?: emote.urls["4"] ?: emote.urls["2"] ?: emote.urls["1"] ?: ""
                                if (url.isNotEmpty()) {
                                    val fullUrl = if (url.startsWith("http") || url.startsWith("//")) {
                                        if (url.startsWith("//")) "https:$url" else url
                                    } else "https:$url"
                                    ffzMap[emote.name] = Emote(emote.id.toString(), emote.name, fullUrl, EmoteType.FFZ)
                                }
                            }
                        }
                        stateFlow.update { it.copy(ffzEmotes = ffzMap, isFfzLoaded = true) }
                    }.onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "FFZ Channel load failed", e)
                        stateFlow.update { it.copy(isFfzLoaded = true) }
                    }
                }
            }
        }
    }

    suspend fun loadUserEmotes() = withContext(Dispatchers.IO) {
        val auth = authManager.authStateFlow.first()
        if (!auth.isLoggedIn || auth.userId == null) return@withContext
        if (_userEmoteState.value.isLoaded && _userEmoteState.value.userId == auth.userId) return@withContext

        Log.d(TAG, "Loading user emotes for ${auth.userName}")
        
        // 1. Try Helix first (official API)
        helixApiClient.getUserEmotes(auth.userId).onSuccess { helixEmotes ->
            if (helixEmotes.isNotEmpty()) {
                val twitchMap = helixEmotes.associateBy({ it.name }, { mapHelixEmote(it) })
                _userEmoteState.update { it.copy(twitchEmotes = twitchMap, isLoaded = true, userId = auth.userId) }
                return@withContext
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "Helix user emotes failed, trying GQL fallback: ${e.message}")
        }

        // 2. Fallback to GQL (often works better with browser session tokens)
        val gqlEmotes = gqlService.getUserEmotes()
        val twitchMap = gqlEmotes.associateBy { it.code }
        _userEmoteState.update { it.copy(twitchEmotes = twitchMap, isLoaded = true, userId = auth.userId) }
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

    private fun mapHelixEmote(dto: HelixEmoteDto): Emote {
        val isAnimated = dto.format?.contains("animated") == true
        val format = if (isAnimated) "animated" else "static"
        val url = "https://static-cdn.jtvnw.net/emoticons/v2/${dto.id}/$format/dark/3.0"
        return Emote(dto.id, dto.name, url, EmoteType.TWITCH)
    }

    private fun parseSevenTVEmote(emote: SevenTVEmote): Emote? {
        val data = emote.data ?: return null
        val hostUrl = data.host.url
        if (hostUrl.isBlank()) return null
        
        val bestFile = data.host.files.find { it.name == "4x.webp" }
                      ?: data.host.files.find { it.format == "WEBP" && it.name.contains("4x") }
                      ?: data.host.files.find { it.name == "2x.webp" }
                      ?: data.host.files.firstOrNull()
        
        val path = bestFile?.name ?: "4x.webp"
        val baseUrl = if (hostUrl.startsWith("//")) "https:$hostUrl" else if (hostUrl.startsWith("http")) hostUrl else "https://$hostUrl"
        val url = if (baseUrl.endsWith("/")) "$baseUrl$path" else "$baseUrl/$path"
        
        return Emote(emote.id, emote.name, url, EmoteType.SEVENTV, isZeroWidth = emote.isZeroWidth)
    }

    fun clearCache() {
        Log.d(TAG, "Clearing emote cache")
        _globalState.update { GlobalEmoteState() }
        _userEmoteState.update { UserEmoteState() }
        _channelStates.values.forEach { it.update { ChannelEmoteState() } }
        _tabCache.clear()
        _flattenedCache.clear()
        aspectRatioCache.clear()
    }
}
