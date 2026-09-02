package com.akumasdk.samtch.ui.components.chat

import android.util.Log
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.badge.BadgeRepository
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.settings.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class ChatEmoteManager @Inject constructor(
    private val emoteRepository: EmoteRepository,
    private val badgeRepository: BadgeRepository,
    private val settingsManager: SettingsManager,
    private val twitchAuthManager: TwitchAuthManager,
    private val helixApiClient: HelixApiClient,
    private val gqlService: TwitchGqlService,
) {
    private val TAG = "ChatEmoteManager"

    private val _emoteSuggestions = MutableStateFlow<List<Emote>>(emptyList())
    val emoteSuggestions = _emoteSuggestions.asStateFlow()

    private val _isEmoteMenuVisible = MutableStateFlow(false)
    val isEmoteMenuVisible = _isEmoteMenuVisible.asStateFlow()

    private val _selectedEmoteForInfo = MutableStateFlow<Emote?>(null)
    val selectedEmoteForInfo = _selectedEmoteForInfo.asStateFlow()

    private val _recentEmotes = MutableStateFlow<List<Emote>>(emptyList())
    val recentEmotes = _recentEmotes.asStateFlow()

    private val _emoteInsertFlow = MutableSharedFlow<Emote>()
    val emoteInsertFlow = _emoteInsertFlow.asSharedFlow()

    private val _isEmoteLoading = MutableStateFlow(false)
    val isEmoteLoading = _isEmoteLoading.asStateFlow()

    private val _hasTriggeredEmoteLoad = MutableStateFlow(false)
    val hasTriggeredEmoteLoad = _hasTriggeredEmoteLoad.asStateFlow()

    private val _tabUpdateTrigger = MutableStateFlow(0)
    
    private val refreshMutex = Mutex()
    private var lastRefreshParams: Triple<String, String?, Boolean>? = null
    private var lastRefreshTime = 0L
    private var refreshJob: Job? = null

    fun initialize(scope: CoroutineScope, currentChannelFlow: StateFlow<String?>) {
        // Collect recent emotes
        scope.launch {
            currentChannelFlow.collectLatest { channel ->
                if (channel != null) {
                    settingsManager.getRecentEmotes(channel).collect { recent ->
                        _recentEmotes.value = recent
                    }
                } else {
                    _recentEmotes.value = emptyList()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getEmoteMenuTabs(scope: CoroutineScope, currentChannelFlow: StateFlow<String?>): StateFlow<Map<Int, List<Emote>>> {
        return currentChannelFlow.flatMapLatest { channelName ->
            if (channelName == null) {
                Log.d(TAG, "Emote menu tabs: channel is null, returning emptyMap")
                return@flatMapLatest flowOf(emptyMap<Int, List<Emote>>())
            }
            
            Log.d(TAG, "Starting emote menu tabs flow for $channelName")
            
            // Observe the repository tabs AND the update trigger
            // Using combine ensures that manual refresh always triggers a re-calculation
            combine(
                emoteRepository.getEmoteTabs(channelName, settingsManager),
                _tabUpdateTrigger
            ) { tabs, trigger ->
                Log.d(TAG, "Emote tabs combined for $channelName (trigger=$trigger). Tab count: ${tabs.size}")
                tabs
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    }

    fun refreshEmotes(scope: CoroutineScope, channel: String, userId: String? = null, force: Boolean = false): Job {
        val now = System.currentTimeMillis()
        
        refreshJob?.cancel()
        
        val job = scope.launch {
            val auth = twitchAuthManager.getAuthState()
            
            // Resolve User ID early
            val resolvedUserId = userId ?: if (auth.isLoggedIn && auth.userName.equals(channel, ignoreCase = true)) {
                auth.userId
            } else {
                helixApiClient.getUserIdByName(channel).getOrNull()
                    ?: gqlService.getUserId(channel)
            }

            val params = Triple(channel, resolvedUserId, auth.isLoggedIn)
            
            // If it's a redundant call (same channel, same user id, same login status) within 5 seconds, skip.
            if (!force && lastRefreshParams == params && (now - lastRefreshTime) < 5000) {
                return@launch
            }

            // Short debounce for rapid UI triggers only if we don't have params yet
            if (!force && lastRefreshParams == null) delay(100.milliseconds)

            refreshMutex.withLock {
                // Re-check inside mutex
                if (!force && lastRefreshParams == params && (System.currentTimeMillis() - lastRefreshTime) < 5000) return@withLock
                
                lastRefreshParams = params
                lastRefreshTime = System.currentTimeMillis()
                
                Log.d(TAG, "Refreshing emotes/badges for $channel (userId=$resolvedUserId, isLoggedIn=${auth.isLoggedIn})")
                
                supervisorScope {
                    launch { emoteRepository.loadGlobalEmotes(force) }
                    launch { emoteRepository.loadUserEmotes() }
                    launch { badgeRepository.loadGlobalBadges(force) }

                    if (resolvedUserId != null) {
                        launch { emoteRepository.loadChannelEmotes(channel, resolvedUserId, force) }
                        launch { badgeRepository.loadChannelBadges(channel, resolvedUserId, force) }
                    } else {
                        launch { emoteRepository.loadChannelEmotes(channel, force = force) }
                    }
                }
                _tabUpdateTrigger.value += 1
            }
        }
        refreshJob = job
        return job
    }

    fun toggleEmoteMenu() {
        _isEmoteMenuVisible.value = !_isEmoteMenuVisible.value
    }

    fun setEmoteMenuVisible(visible: Boolean, scope: CoroutineScope, channel: String?) {
        _isEmoteMenuVisible.value = visible
        
        if (visible && !_hasTriggeredEmoteLoad.value) {
            if (channel != null) {
                _hasTriggeredEmoteLoad.value = true
                refreshEmotes(scope, channel)
            }
        }
    }

    fun showEmoteInfo(emote: Emote) {
        _selectedEmoteForInfo.value = emote
    }

    fun showEmoteInfo(emoteInfo: EmoteInfo, channel: String?) {
        if (channel == null) return
        val emote = emoteRepository.getEmote(channel, emoteInfo.code)
        if (emote != null) {
            _selectedEmoteForInfo.value = emote
        } else {
            _selectedEmoteForInfo.value = Emote(
                id = emoteInfo.id,
                code = emoteInfo.code,
                url = emoteInfo.url.split("|").first(),
                type = com.akumasdk.samtch.data.emote.EmoteType.TWITCH
            )
        }
    }

    fun dismissEmoteInfo() {
        _selectedEmoteForInfo.value = null
    }

    fun insertEmote(scope: CoroutineScope, emote: Emote) {
        scope.launch {
            _emoteInsertFlow.emit(emote)
        }
    }

    fun recordEmoteUsage(scope: CoroutineScope, channel: String, emote: Emote) {
        scope.launch {
            settingsManager.addRecentEmote(channel, emote)
        }
    }

    fun updateSuggestions(scope: CoroutineScope, channel: String, text: String, cursorPosition: Int) {
        val currentWord = extractCurrentWord(text, cursorPosition)
        
        if (currentWord.isBlank() || currentWord.length < 2) {
            _emoteSuggestions.value = emptyList()
            return
        }

        val query = if (currentWord.startsWith(':')) currentWord.substring(1) else currentWord
        
        scope.launch(Dispatchers.Default) {
            val allEmotes = emoteRepository.getFlattenedEmotes(channel).value
            val filtered = allEmotes.mapNotNull { emote ->
                val score = scoreEmote(emote.code, query)
                if (score != Int.MIN_VALUE) {
                    emote to score
                } else {
                    null
                }
            }.sortedBy { it.second }
             .map { it.first }
             .take(20)
            
            _emoteSuggestions.value = filtered
        }
    }

    private fun extractCurrentWord(text: String, cursorPosition: Int): String {
        val cursorPos = cursorPosition.coerceIn(0, text.length)
        var start = cursorPos
        while (start > 0 && text[start - 1] != ' ') start--
        return text.substring(start, cursorPos)
    }

    private fun scoreEmote(code: String, query: String): Int {
        val matchIndex = code.indexOf(query, ignoreCase = true)
        if (matchIndex < 0) return Int.MIN_VALUE

        var caseDiffs = 0
        for (i in query.indices) {
            if (code[matchIndex + i] != query[i]) caseDiffs++
        }

        val extraChars = code.length - query.length
        val caseCost = if (caseDiffs == 0) -10 else caseDiffs
        return caseCost + extraChars * 100
    }

    fun resetLoadTrigger(force: Boolean) {
        _hasTriggeredEmoteLoad.value = force
        if (force) {
            lastRefreshParams = null
        }
    }

    fun setEmoteLoading(loading: Boolean) {
        _isEmoteLoading.value = loading
    }
    
    fun clear() {
        _emoteSuggestions.value = emptyList()
        _isEmoteMenuVisible.value = false
        _selectedEmoteForInfo.value = null
        _hasTriggeredEmoteLoad.value = false
        lastRefreshParams = null
        _tabUpdateTrigger.value = 0
    }
}
