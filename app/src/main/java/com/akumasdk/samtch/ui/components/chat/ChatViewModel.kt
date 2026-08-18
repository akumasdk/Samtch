package com.akumasdk.samtch.ui.components.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.badge.BadgeRepository
import com.akumasdk.samtch.data.badge.TwitchBadgeDto
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.service.TwitchChatClient
import com.akumasdk.samtch.util.adaptiveChunked
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"
    private val chatClient = TwitchChatClient(application)
    
    private val _messages = MutableStateFlow<ImmutableList<ChatMessageUiState>>(persistentListOf())
    val messages = _messages.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _loggedInUser = MutableStateFlow<String?>(null)
    val loggedInUser = _loggedInUser.asStateFlow()

    private val _emoteSuggestions = MutableStateFlow<List<Emote>>(emptyList())
    val emoteSuggestions = _emoteSuggestions.asStateFlow()

    private val _isEmoteMenuVisible = MutableStateFlow(false)
    val isEmoteMenuVisible = _isEmoteMenuVisible.asStateFlow()

    private val _selectedEmoteForInfo = MutableStateFlow<Emote?>(null)
    val selectedEmoteForInfo = _selectedEmoteForInfo.asStateFlow()

    private val _selectedBadgeForInfo = MutableStateFlow<TwitchBadgeDto?>(null)
    val selectedBadgeForInfo = _selectedBadgeForInfo.asStateFlow()

    private val _selectedUserForInfo = MutableStateFlow<com.akumasdk.samtch.data.api.helix.dto.UserDto?>(null)
    val selectedUserForInfo = _selectedUserForInfo.asStateFlow()

    private val _emoteMenuTabs = MutableStateFlow<Map<Int, List<Emote>>>(emptyMap())
    val emoteMenuTabs = _emoteMenuTabs.asStateFlow()

    private val _recentEmotes = MutableStateFlow<List<Emote>>(emptyList())
    val recentEmotes = _recentEmotes.asStateFlow()

    private val _emoteInsertFlow = MutableSharedFlow<Emote>()
    val emoteInsertFlow = _emoteInsertFlow.asSharedFlow()

    private val _keyboardHeightPx = MutableStateFlow(0)
    val keyboardHeightPx = _keyboardHeightPx.asStateFlow()

    private val _chatFontSize = MutableStateFlow(14)
    val chatFontSize = _chatFontSize.asStateFlow()

    private val _chatEmoteSize = MutableStateFlow(28)
    val chatEmoteSize = _chatEmoteSize.asStateFlow()

    private val _systemNotice = MutableStateFlow<String?>(null)
    val systemNotice = _systemNotice.asStateFlow()

    private val _isEmoteLoading = MutableStateFlow(false)
    val isEmoteLoading = _isEmoteLoading.asStateFlow()

    private var currentChannel: String? = null
    private var connectionJob: Job? = null
    private val messageHistory = mutableListOf<ChatMessageUiState>()
    private val rawIrcMessages = mutableListOf<IrcMessage>()
    private val messageBuffer = MutableSharedFlow<ChatMessageUiState>(extraBufferCapacity = 200)

    fun connect(
        context: android.content.Context,
        channel: String, 
        loadingMessage: String, 
        welcomeMessageTemplate: String = "Welcome to %s's chat!",
        loginMessageTemplate: String? = null,
        forceRefresh: Boolean = false
    ) {
        if (currentChannel == channel && !forceRefresh) return
        
        // 1. Instantly cancel any active session logic for the previous channel
        connectionJob?.cancel()
        currentChannel = channel
        
        // 2. Wipe all state immediately to prevent "leakage" in UI
        rawIrcMessages.clear()
        messageHistory.clear()
        _messages.value = persistentListOf()

        // 3. Check login status
        val authState = TwitchAuthManager.getAuthState(getApplication())
        _isLoggedIn.value = authState.isLoggedIn
        val loggedInUser = authState.userName
        _loggedInUser.value = loggedInUser
        
        if (authState.isLoggedIn && !loggedInUser.isNullOrEmpty() && !loginMessageTemplate.isNullOrEmpty()) {
            val loginMsg = ChatMessageUiState.SystemMessageUi(
                id = "login_${UUID.randomUUID()}",
                message = loginMessageTemplate.format(loggedInUser)
            )
            messageHistory.add(loginMsg)
        }

        // 4. Start new managed session job
        connectionJob = viewModelScope.launch {
            Log.d(TAG, "Starting new chat session for channel: $channel")
            
            val initialMsg = ChatMessageUiState.SystemMessageUi(
                id = "loading_${UUID.randomUUID()}",
                message = loadingMessage
            )
            messageHistory.add(initialMsg)
            _messages.value = messageHistory.toImmutableList()
            
            // Collect recent emotes
            launch {
                SettingsManager.getRecentEmotes(context, channel).collect { recent ->
                    _recentEmotes.value = recent
                    currentChannel?.let { updateEmoteMenuTabs(it) }
                }
            }

            // Collect chat settings
            launch {
                SettingsManager.getChatFontSize(context).collect { size ->
                    _chatFontSize.value = size
                }
            }
            launch {
                SettingsManager.getChatEmoteSize(context).collect { size ->
                    _chatEmoteSize.value = size
                }
            }

            // Collect messages in batches to prevent UI lag in high-traffic channels
            launch {
                messageBuffer
                    .adaptiveChunked(150, 400, 10) // Batch updates every 150-400ms
                    .collect { newBatch ->
                        // Deduplicate by ID to prevent LazyColumn duplicate key crash
                        newBatch.forEach { newMessage ->
                            val existingIndex = messageHistory.indexOfFirst { it.id == newMessage.id }
                            if (existingIndex != -1) {
                                messageHistory[existingIndex] = newMessage
                            } else {
                                messageHistory.add(newMessage)
                            }
                        }
                        
                        if (messageHistory.size > 500) { // Slightly larger history for high-traffic
                            val toRemove = messageHistory.size - 500
                            repeat(toRemove) { messageHistory.removeAt(0) }
                        }
                        _messages.value = messageHistory.toImmutableList()
                    }
            }

            // Watch for load status to trigger remapping (emotes and badges)
            launch {
                combine(
                    EmoteRepository.globalState,
                    EmoteRepository.getChannelState(channel),
                    BadgeRepository.globalState,
                    BadgeRepository.getChannelState(channel)
                ) { globalEmotes, channelEmotes, globalBadges, channelBadges ->
                    _isEmoteLoading.value = !globalEmotes.isLoaded || !channelEmotes.isLoaded
                    globalEmotes.isLoaded || channelEmotes.isLoaded || globalBadges.isLoaded || channelBadges.isLoaded
                }.collectLatest { anyLoaded ->
                    if (anyLoaded) {
                        delay(1000.milliseconds) // Debounce re-mapping
                        remapMessages(channel)
                    }
                }
            }

            // Load emotes and badges
            launch { 
                EmoteRepository.loadGlobalEmotes(context)
                EmoteRepository.loadUserEmotes(context)
                BadgeRepository.loadGlobalBadges(context)
                updateEmoteMenuTabs(channel)
            }
            launch { 
                val userId = if (authState.isLoggedIn && authState.userName.equals(channel, ignoreCase = true)) {
                    authState.userId
                } else {
                    com.akumasdk.samtch.data.api.helix.HelixApiClient.getUserIdByName(context, channel).getOrNull()
                }
                
                if (userId != null) {
                    EmoteRepository.loadChannelEmotes(context, channel)
                    BadgeRepository.loadChannelBadges(context, channel, userId)
                    updateEmoteMenuTabs(channel)
                }
            }

            chatClient.connect(channel)
            
            // Welcome message once connected
            launch {
                chatClient.isConnected.collect { connected ->
                    if (connected) {
                        val welcomeMsg = ChatMessageUiState.SystemMessageUi(
                            id = "welcome_${UUID.randomUUID()}",
                            message = welcomeMessageTemplate.format(channel)
                        )
                        messageBuffer.emit(welcomeMsg)
                    }
                }
            }

            chatClient.messages.collect { msg ->
                if (msg.command == "PRIVMSG") {
                    if (rawIrcMessages.none { it.id == msg.id }) {
                        rawIrcMessages.add(msg)
                        if (rawIrcMessages.size > 500) rawIrcMessages.removeAt(0)
                    }
                    
                    val uiState = ChatMessageMapper.mapToUiState(channel, msg)
                    messageBuffer.emit(uiState)
                } else if (msg.command == "NOTICE" || msg.command == "USERNOTICE") {
                    val messageText = msg.params.lastOrNull() ?: msg.raw
                    val systemMsg = ChatMessageUiState.SystemMessageUi(
                        id = msg.id,
                        message = messageText
                    )
                    messageBuffer.emit(systemMsg)
                    
                    // Only NOTICE (slow mode, sub mode, etc.) triggers the persistent banner
                    if (msg.command == "NOTICE") {
                        _systemNotice.value = messageText
                        // Auto-dismiss after 6 seconds
                        launch {
                            delay(6000.milliseconds)
                            if (_systemNotice.value == messageText) {
                                _systemNotice.value = null
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissSystemNotice() {
        _systemNotice.value = null
    }

    private fun updateEmoteMenuTabs(channel: String) {
        val channelState = EmoteRepository.getChannelState(channel).value
        val globalState = EmoteRepository.globalState.value
        val userState = EmoteRepository.userEmoteState.value

        val tabs = mutableMapOf<Int, List<Emote>>()
        
        val recent = _recentEmotes.value
        if (recent.isNotEmpty()) {
            tabs[com.akumasdk.samtch.R.string.emote_menu_recent] = recent
        }

        // 1. Twitch Channel Emotes
        val twitchChannel = channelState.twitchEmotes.values.toList()
        // 2. 3rd Party Channel Emotes
        val thirdPartyChannel = (channelState.seventvEmotes.values + channelState.bttvEmotes.values + channelState.ffzEmotes.values).toList()
        
        val allChannelEmotes = (twitchChannel + thirdPartyChannel).distinctBy { it.id }
        if (allChannelEmotes.isNotEmpty()) {
            tabs[com.akumasdk.samtch.R.string.emote_menu_channel] = allChannelEmotes
        }

        // 3. User's Owned Emotes
        val userEmotes = userState.twitchEmotes.values.toList()
        if (userEmotes.isNotEmpty()) {
            tabs[com.akumasdk.samtch.R.string.emote_menu_user] = userEmotes
        }

        // 4. Twitch Global Emotes
        val twitchGlobal = globalState.twitchEmotes.values.toList()
        if (twitchGlobal.isNotEmpty()) {
            tabs[com.akumasdk.samtch.R.string.emote_menu_twitch] = twitchGlobal
        }

        // 5. 3rd Party Global Emotes
        val globalEmotes = (globalState.seventvEmotes.values + globalState.bttvEmotes.values + globalState.ffzEmotes.values).toList()
        if (globalEmotes.isNotEmpty()) {
            tabs[com.akumasdk.samtch.R.string.emote_menu_global] = globalEmotes
        }

        _emoteMenuTabs.value = tabs
    }

    private fun remapMessages(channel: String) {
        if (rawIrcMessages.isEmpty()) return
        Log.d(TAG, "Remapping ${rawIrcMessages.size} messages for channel: $channel")
        
        val idToNewState = rawIrcMessages.associate { 
            it.id to ChatMessageMapper.mapToUiState(channel, it) 
        }
        
        // Update history in-place while preserving non-IRC messages
        val newHistory = messageHistory.map { oldState ->
            idToNewState[oldState.id] ?: oldState
        }
        
        messageHistory.clear()
        messageHistory.addAll(newHistory)
        _messages.value = messageHistory.toImmutableList()
    }

    suspend fun sendMessage(message: String) {
        val channel = currentChannel ?: return
        val authState = TwitchAuthManager.getAuthState(getApplication())
        
        // 1. Manually inject the message for immediate feedback
        if (authState.isLoggedIn && !authState.userName.isNullOrEmpty()) {
            val syntheticMsg = IrcMessage(
                id = UUID.randomUUID().toString(),
                raw = "", // Raw isn't needed for mapping
                prefix = "${authState.userName}!${authState.userName}@${authState.userName}.tmi.twitch.tv",
                command = "PRIVMSG",
                params = listOf("#$channel", message),
                tags = mapOf("display-name" to authState.userName)
            )
            
            val uiState = ChatMessageMapper.mapToUiState(channel, syntheticMsg)
            
            messageHistory.add(uiState)
            if (messageHistory.size > 500) {
                messageHistory.removeAt(0)
            }
            _messages.value = messageHistory.toImmutableList()
        }

        // 2. Transmit to server
        chatClient.sendMessage(channel, message)
    }

    fun disconnect() {
        Log.d(TAG, "Explicitly disconnecting chat client")
        connectionJob?.cancel()
        chatClient.disconnect()
        _messages.value = persistentListOf()
        messageHistory.clear()
        rawIrcMessages.clear()
        _emoteSuggestions.value = emptyList()
        _isEmoteMenuVisible.value = false
        _selectedEmoteForInfo.value = null
        currentChannel = null
    }

    fun toggleEmoteMenu() {
        _isEmoteMenuVisible.value = !_isEmoteMenuVisible.value
    }

    fun showEmoteInfo(emote: Emote) {
        _selectedEmoteForInfo.value = emote
    }

    fun showEmoteInfo(emoteInfo: EmoteInfo) {
        val channel = currentChannel ?: return
        // Try to find the emote in our state
        val emote = EmoteRepository.getEmote(channel, emoteInfo.code)
        if (emote != null) {
            _selectedEmoteForInfo.value = emote
        } else {
            // Fallback: create a temporary Emote from EmoteInfo
            // Note: EmoteType might be wrong but it's better than nothing
            _selectedEmoteForInfo.value = Emote(
                id = emoteInfo.id,
                code = emoteInfo.code,
                url = emoteInfo.url.split("|").first(),
                type = com.akumasdk.samtch.data.emote.EmoteType.TWITCH // Default
            )
        }
    }

    fun setEmoteMenuVisible(visible: Boolean) {
        _isEmoteMenuVisible.value = visible
    }

    fun updateKeyboardHeight(context: android.content.Context, heightPx: Int, isLandscape: Boolean) {
        if (heightPx > 0 && _keyboardHeightPx.value != heightPx) {
            _keyboardHeightPx.value = heightPx
            viewModelScope.launch {
                SettingsManager.setKeyboardHeight(context, isLandscape, heightPx)
            }
        }
    }

    fun initKeyboardHeight(context: android.content.Context, isLandscape: Boolean) {
        viewModelScope.launch {
            val height = SettingsManager.getKeyboardHeight(context, isLandscape).first()
            _keyboardHeightPx.value = height
        }
    }

    fun dismissEmoteInfo() {
        _selectedEmoteForInfo.value = null
    }

    fun showBadgeInfo(badge: TwitchBadgeDto) {
        _selectedBadgeForInfo.value = badge
    }

    fun dismissBadgeInfo() {
        _selectedBadgeForInfo.value = null
    }

    fun showUserInfo(userName: String) {
        viewModelScope.launch {
            val user = com.akumasdk.samtch.data.api.helix.HelixApiClient.getUsers(getApplication(), logins = listOf(userName)).getOrNull()?.firstOrNull()
            if (user != null) {
                _selectedUserForInfo.value = user
            }
        }
    }

    fun dismissUserInfo() {
        _selectedUserForInfo.value = null
    }

    fun insertEmote(emote: Emote) {
        viewModelScope.launch {
            _emoteInsertFlow.emit(emote)
        }
    }

    fun recordEmoteUsage(context: android.content.Context, emote: Emote) {
        val channel = currentChannel ?: return
        viewModelScope.launch {
            SettingsManager.addRecentEmote(context, channel, emote)
        }
    }

    fun updateSuggestions(text: String, cursorPosition: Int) {
        val channel = currentChannel ?: return
        val currentWord = extractCurrentWord(text, cursorPosition)
        
        if (currentWord.isBlank() || currentWord.length < 2) {
            _emoteSuggestions.value = emptyList()
            return
        }

        val query = if (currentWord.startsWith(':')) currentWord.substring(1) else currentWord
        
        viewModelScope.launch(Dispatchers.Default) {
            val allEmotes = EmoteRepository.getAllEmotes(channel)
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

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
