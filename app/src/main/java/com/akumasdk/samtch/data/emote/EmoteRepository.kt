package com.akumasdk.samtch.data.emote

import android.content.Context
import android.util.Log
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.api.helix.dto.HelixEmoteDto
import com.akumasdk.samtch.data.api.thirdparty.*
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class EmoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val helixApiClient: HelixApiClient,
    private val bttvApi: BTTVApi,
    private val ffzApi: FFZApi,
    private val sevenTVApi: SevenTVApi,
    private val gqlService: TwitchGqlService,
    private val authManager: TwitchAuthManager,
    private val settingsManager: SettingsManager
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
    private val _tabCache = ConcurrentHashMap<String, StateFlow<List<EmoteTab>>>()
    private val _flattenedCache = ConcurrentHashMap<String, StateFlow<List<Emote>>>()
    
    private val repoScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val aspectRatioCache = ConcurrentHashMap<String, Float>()

    init {
        repoScope.launch {
            settingsManager.getEmoteQuality().drop(1).collect { quality ->
                Log.d(TAG, "Emote quality changed to ${quality.name}, reloading all active emote sets")
                val activeChannels = _channelStates.keys.toList()
                clearCache()
                launch { loadGlobalEmotes(force = true) }
                launch { loadUserEmotes(force = true) }
                activeChannels.forEach { channel ->
                    launch { loadChannelEmotes(channel, force = true) }
                }
            }
        }
    }

    fun getAspectRatio(url: String): Float? = aspectRatioCache[url]
    fun putAspectRatio(url: String, ratio: Float) { aspectRatioCache[url] = ratio }

    fun getChannelState(channelName: String) = _channelStates.getOrPut(channelName.lowercase()) {
        MutableStateFlow(ChannelEmoteState())
    }.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun getEmoteTabs(channelName: String, settingsManager: SettingsManager): StateFlow<List<EmoteTab>> {
        val channelLower = channelName.lowercase()
        @Suppress("UNCHECKED_CAST")
        return _tabCache.getOrPut(channelLower) {
            Log.d(TAG, "Initializing EmoteTabs for $channelLower")
            combine(
                settingsManager.getRecentEmotes(channelLower),
                settingsManager.isThirdPartyEmotesEnabledForChannel(channelLower),
                globalState,
                userEmoteState,
                getChannelState(channelLower)
            ) { recent, thirdPartyEnabled, global, userState, channelState ->
                val tabs = mutableListOf<EmoteTab>()
                val auth = authManager.getAuthState()
                val unlockedUserEmoteIds = userState.twitchEmotes.values.map { it.id }.toSet()
                val unlockedUserEmoteCodes = userState.twitchEmotes.values.map { it.code.lowercase() }.toSet()
                val isChannelOwner = auth.isLoggedIn && auth.userName.equals(channelLower, ignoreCase = true)
                val isSubscribed = isChannelOwner || channelState.isUserSubscribed

                // 1. Recent Emotes
                val filteredRecent = if (thirdPartyEnabled) recent else recent.filter { it.type == EmoteType.TWITCH }
                if (filteredRecent.isNotEmpty()) {
                    tabs.add(EmoteTab(titleResId = R.string.emote_menu_recent, emotes = filteredRecent))
                }

                // 2. Channel Twitch Emotes
                val processedChannelTwitchEmotes = channelState.twitchEmotes.values.map { emote ->
                    if (emote.isSubOnly) {
                        val isUnlocked = !auth.isLoggedIn || 
                                        isSubscribed || 
                                        emote.id in unlockedUserEmoteIds || 
                                        emote.code.lowercase() in unlockedUserEmoteCodes ||
                                        userState.twitchEmotes.values.any { it.ownerChannelId == emote.ownerChannelId && !emote.ownerChannelId.isNullOrEmpty() }
                        emote.copy(isUnlocked = isUnlocked)
                    } else {
                        emote.copy(isUnlocked = true)
                    }
                }

                if (processedChannelTwitchEmotes.isNotEmpty()) {
                    tabs.add(EmoteTab(titleResId = R.string.emote_menu_channel, emotes = processedChannelTwitchEmotes))
                }

                // 3. 7TV Emotes (Channel + Global 7TV)
                if (thirdPartyEnabled) {
                    val seventvEmotes = (channelState.seventvEmotes.values + global.seventvEmotes.values).distinctBy { it.id }
                    if (seventvEmotes.isNotEmpty()) {
                        tabs.add(EmoteTab(titleResId = R.string.emote_source_seventv, iconResId = R.drawable.ic_7tv, emotes = seventvEmotes))
                    }
                }

                // 4. BTTV Emotes (Channel + Global BTTV)
                if (thirdPartyEnabled) {
                    val bttvEmotes = (channelState.bttvEmotes.values + global.bttvEmotes.values).distinctBy { it.id }
                    if (bttvEmotes.isNotEmpty()) {
                        tabs.add(EmoteTab(titleResId = R.string.emote_source_bttv, iconResId = R.drawable.ic_bttv, emotes = bttvEmotes))
                    }
                }

                // 5. FFZ Emotes (Channel + Global FFZ)
                if (thirdPartyEnabled) {
                    val ffzEmotes = (channelState.ffzEmotes.values + global.ffzEmotes.values).distinctBy { it.id }
                    if (ffzEmotes.isNotEmpty()) {
                        tabs.add(EmoteTab(titleResId = R.string.emote_source_ffz, iconResId = R.drawable.ic_ffz, emotes = ffzEmotes))
                    }
                }

                // 6. Subscribed Emotes tab
                val subEmotes = userState.twitchEmotes.values.toList()
                if (subEmotes.isNotEmpty()) {
                    tabs.add(EmoteTab(titleResId = R.string.emote_menu_subscribed, emotes = subEmotes.map { it.copy(isUnlocked = true) }))
                }

                // 7. User Personal Emotes
                if (userState.twitchEmotes.isNotEmpty()) {
                    tabs.add(EmoteTab(titleResId = R.string.emote_menu_user, emotes = userState.twitchEmotes.values.map { it.copy(isUnlocked = true) }))
                }

                // 8. Twitch Global Emotes
                if (global.twitchEmotes.isNotEmpty()) {
                    tabs.add(EmoteTab(titleResId = R.string.emote_menu_twitch, emotes = global.twitchEmotes.values.map { it.copy(isUnlocked = true) }))
                }

                Log.d(TAG, "Tabs emission calculated for $channelLower: ${tabs.size} tabs")
                tabs
            }
            .debounce(100.milliseconds)
            .stateIn(
                scope = repoScope,
                started = SharingStarted.Eagerly, 
                initialValue = emptyList()
            )
        } as StateFlow<List<EmoteTab>>
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
        val quality = settingsManager.getEmoteQuality().first()
        val currentState = _globalState.value
        
        if (!force && currentState.isTwitchLoaded && currentState.loadedWithAuth == auth.isLoggedIn && 
            currentState.isBttvLoaded && currentState.isSeventvLoaded && currentState.isFfzLoaded) {
            return@withContext
        }
        
        Log.d(TAG, "Loading global emotes (quality=${quality.name}). isLoggedIn=${auth.isLoggedIn}, force=$force")

        supervisorScope {
            // Twitch Global
            val shouldLoadTwitch = auth.isLoggedIn && (force || !currentState.isTwitchLoaded || !currentState.loadedWithAuth)
            if (shouldLoadTwitch) {
                launch {
                    helixApiClient.getGlobalEmotes().onSuccess { helixEmotes ->
                        val twitchMap = helixEmotes.associateBy({ it.name }, { mapHelixEmote(it, quality) })
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
                            it.id, it.code, "https://cdn.betterttv.net/emote/${it.id}/${quality.bttvScale}", EmoteType.BTTV,
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
                        val seventvMap = sevenTVSet.emotes.mapNotNull { parseSevenTVEmote(it, quality) }.associateBy { it.code }
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
                            ffzGlobalToMap(ffzResponse, setId.toString(), ffzMap, quality)
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

    private fun ffzGlobalToMap(response: FFZGlobalResponse, setId: String, outMap: MutableMap<String, Emote>, quality: SettingsManager.EmoteQuality) {
        response.sets[setId]?.emotes?.forEach { emote ->
            val url = emote.animated?.get(quality.ffzScale) ?: emote.animated?.get("1")
                     ?: emote.urls[quality.ffzScale] ?: emote.urls["1"] ?: ""
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
        val quality = settingsManager.getEmoteQuality().first()
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

        Log.d(TAG, "Loading channel emotes (quality=${quality.name}) for $channelLower (ID: $resolvedUserId)")

        supervisorScope {
            // Check subscription status
            if (auth.isLoggedIn && auth.userId != null) {
                launch {
                    val isSubbed = if (auth.userName.equals(channelLower, ignoreCase = true)) {
                        true
                    } else {
                        helixApiClient.isUserSubscribed(resolvedUserId, auth.userId).getOrDefault(false)
                    }
                    stateFlow.update { it.copy(isUserSubscribed = isSubbed) }
                    Log.d(TAG, "Subscription check for $channelLower: isSubbed=$isSubbed")
                }
            }

            // Twitch Channel
            val shouldLoadTwitch = auth.isLoggedIn && (force || !currentState.isTwitchLoaded || !currentState.loadedWithAuth)
            if (shouldLoadTwitch) {
                launch {
                    helixApiClient.getChannelEmotes(resolvedUserId).onSuccess { helixEmotes ->
                        val twitchMap = helixEmotes.associateBy({ it.name }, { mapHelixEmote(it, quality) })
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
                            it.id, it.code, "https://cdn.betterttv.net/emote/${it.id}/${quality.bttvScale}", EmoteType.BTTV,
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
                        val seventvMap = (seventvUser.emoteSet ?: seventvUser.user?.emoteSet)?.emotes?.mapNotNull { parseSevenTVEmote(it, quality) }?.associateBy { it.code } ?: emptyMap()
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
                                val url = emote.animated?.get(quality.ffzScale) ?: emote.animated?.get("1")
                                         ?: emote.urls[quality.ffzScale] ?: emote.urls["1"] ?: ""
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

    suspend fun loadUserEmotes(force: Boolean = false) = withContext(Dispatchers.IO) {
        val auth = authManager.authStateFlow.first()
        val quality = settingsManager.getEmoteQuality().first()
        if (!auth.isLoggedIn || auth.userId == null) return@withContext
        if (!force && _userEmoteState.value.isLoaded && _userEmoteState.value.userId == auth.userId && _userEmoteState.value.twitchEmotes.isNotEmpty()) return@withContext

        Log.d(TAG, "Loading user emotes for ${auth.userName} (userId=${auth.userId}, force=$force)")
        
        // 1. Try Helix first (official API)
        helixApiClient.getUserEmotes(auth.userId).onSuccess { helixEmotes ->
            if (helixEmotes.isNotEmpty()) {
                val twitchMap = helixEmotes.associateBy({ it.name }, { dto ->
                    mapHelixEmote(dto, quality).copy(
                        isSubOnly = dto.emote_type == "subscriptions" || dto.tier != null || dto.owner_id != null,
                        isUnlocked = true
                    )
                })
                Log.d(TAG, "Successfully loaded ${twitchMap.size} user emotes from Helix")
                _userEmoteState.update { it.copy(twitchEmotes = twitchMap, isLoaded = true, userId = auth.userId) }
                return@withContext
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "Helix user emotes failed, trying GQL fallback: ${e.message}")
        }

        // 2. Fallback to GQL (often works better with browser session tokens)
        val gqlEmotes = gqlService.getUserEmotes()
        val twitchMap = gqlEmotes.associateBy({ it.code }, { emote ->
            emote.copy(isSubOnly = true, isUnlocked = true)
        })
        Log.d(TAG, "Loaded ${twitchMap.size} user emotes from GQL fallback")
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

    private fun mapHelixEmote(dto: HelixEmoteDto, quality: SettingsManager.EmoteQuality): Emote {
        val isAnimated = dto.format?.contains("animated") == true
        val format = if (isAnimated) "animated" else "static"
        val url = "https://static-cdn.jtvnw.net/emoticons/v2/${dto.id}/$format/dark/${quality.twitchScale}"
        val isSubOnly = dto.emote_type == "subscriptions" || dto.tier != null
        return Emote(
            id = dto.id,
            code = dto.name,
            url = url,
            type = EmoteType.TWITCH,
            isSubOnly = isSubOnly,
            isUnlocked = true,
            tier = dto.tier,
            ownerChannelId = dto.owner_id
        )
    }

    private fun parseSevenTVEmote(emote: SevenTVEmote, quality: SettingsManager.EmoteQuality): Emote? {
        val data = emote.data ?: return null
        val hostUrl = data.host.url
        if (hostUrl.isBlank()) return null
        
        val targetFile = data.host.files.find { it.name == quality.sevenTvFile }
                      ?: data.host.files.find { it.name == "1x.webp" }
                      ?: data.host.files.firstOrNull()
        
        val path = targetFile?.name ?: quality.sevenTvFile
        val baseUrl = if (hostUrl.startsWith("//")) "https:$hostUrl" else if (hostUrl.startsWith("http")) hostUrl else "https://$hostUrl"
        val url = if (baseUrl.endsWith("/")) "$baseUrl$path" else "$baseUrl/$path"
        
        return Emote(emote.id, emote.name, url, EmoteType.SEVENTV, isZeroWidth = emote.isZeroWidth)
    }

    fun clearChannelCache(channelName: String? = null) {
        if (channelName != null) {
            val channelLower = channelName.lowercase()
            Log.d(TAG, "Clearing channel emote cache for $channelLower")
            _channelStates.remove(channelLower)
            _tabCache.remove(channelLower)
            _flattenedCache.remove(channelLower)
        } else {
            Log.d(TAG, "Clearing all channel emote caches")
            _channelStates.clear()
            _tabCache.clear()
            _flattenedCache.clear()
        }
        aspectRatioCache.clear()
        try {
            coil.Coil.imageLoader(context).memoryCache?.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Coil memory cache", e)
        }
    }

    fun clearCache() {
        Log.d(TAG, "Clearing emote cache")
        _globalState.update { GlobalEmoteState() }
        _userEmoteState.update { UserEmoteState() }
        _channelStates.values.forEach { it.update { ChannelEmoteState() } }
        _tabCache.clear()
        _flattenedCache.clear()
        aspectRatioCache.clear()
        try {
            coil.Coil.imageLoader(context).memoryCache?.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Coil memory cache", e)
        }
    }
}
