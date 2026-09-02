package com.akumasdk.samtch.ui.components.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.badge.BadgeRepository
import com.akumasdk.samtch.data.badge.TwitchBadgeDto
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.service.TwitchChatClient
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatClient: TwitchChatClient,
    val emoteRepository: EmoteRepository,
    val badgeRepository: BadgeRepository,
    val settingsManager: SettingsManager,
    private val twitchAuthManager: TwitchAuthManager,
    private val helixApiClient: HelixApiClient,
    private val chatMessageMapper: ChatMessageMapper,
    private val emoteManager: ChatEmoteManager,
    private val messageStore: ChatMessageStore
) : androidx.lifecycle.ViewModel() {
    private val TAG = "ChatViewModel"
    
    val messages: StateFlow<ImmutableList<ChatMessageUiState>> = messageStore.messages

    val isLoggedIn: StateFlow<Boolean> = twitchAuthManager.authStateFlow
        .map { it.isLoggedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val loggedInUser: StateFlow<String?> = twitchAuthManager.authStateFlow
        .map { it.userName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emoteSuggestions = emoteManager.emoteSuggestions
    val isEmoteMenuVisible = emoteManager.isEmoteMenuVisible
    val selectedEmoteForInfo = emoteManager.selectedEmoteForInfo
    val recentEmotes = emoteManager.recentEmotes
    val emoteInsertFlow = emoteManager.emoteInsertFlow
    val isEmoteLoading = emoteManager.isEmoteLoading

    private val _selectedBadgeForInfo = MutableStateFlow<TwitchBadgeDto?>(null)
    val selectedBadgeForInfo = _selectedBadgeForInfo.asStateFlow()

    private val _selectedUserForInfo = MutableStateFlow<com.akumasdk.samtch.data.api.helix.dto.UserDto?>(null)
    val selectedUserForInfo = _selectedUserForInfo.asStateFlow()

    private val _keyboardHeightPx = MutableStateFlow(0)
    val keyboardHeightPx = _keyboardHeightPx.asStateFlow()

    private val _chatFontSize = MutableStateFlow(14)
    val chatFontSize = _chatFontSize.asStateFlow()

    private val _chatEmoteSize = MutableStateFlow(28)
    val chatEmoteSize = _chatEmoteSize.asStateFlow()

    private val _chatBadgeSize = MutableStateFlow(18)
    val chatBadgeSize = _chatBadgeSize.asStateFlow()

    private val _systemNotice = MutableStateFlow<String?>(null)
    val systemNotice = _systemNotice.asStateFlow()

    private val _isInputFocused = MutableStateFlow(false)
    val isInputFocused = _isInputFocused.asStateFlow()

    private val _currentChannel = MutableStateFlow<String?>(null)
    private val userTags = ConcurrentHashMap<String, String>()

    val emoteMenuTabs = emoteManager.getEmoteMenuTabs(viewModelScope, _currentChannel.asStateFlow())

    init {
        emoteManager.initialize(viewModelScope, _currentChannel.asStateFlow())
        messageStore.startMessageProcessing(viewModelScope)

        // Automatically refresh emotes when login state changes for the current channel
        viewModelScope.launch {
            isLoggedIn.collectLatest { loggedIn ->
                val channel = _currentChannel.value
                if (channel != null) {
                    Log.d(TAG, "Auth change detected (loggedIn=$loggedIn). Refreshing emotes for $channel.")
                    emoteManager.refreshEmotes(viewModelScope, channel)
                }
            }
        }
    }

    private var lastLoadedRoomId: String? = null
    private var connectionJob: Job? = null

    fun connect(
        context: android.content.Context,
        channel: String, 
        loadingMessage: String, 
        welcomeMessageTemplate: String = "Welcome to %s's chat!",
        loginMessageTemplate: String? = null,
        forceRefresh: Boolean = false
    ) {
        if (_currentChannel.value == channel && !forceRefresh) return
        
        // 1. Instantly cancel any active session logic for the previous channel
        connectionJob?.cancel()
        _currentChannel.value = channel
        emoteManager.resetLoadTrigger(forceRefresh)
        lastLoadedRoomId = null
        
        // 2. Wipe all state immediately
        messageStore.clear()
        userTags.clear()
        
        // 3. Start new managed session job
        connectionJob = viewModelScope.launch {
            Log.d(TAG, "Starting new chat session for channel: $channel")
            
            val authState = twitchAuthManager.getAuthState()
            if (authState.isLoggedIn && !authState.userName.isNullOrEmpty() && !loginMessageTemplate.isNullOrEmpty()) {
                val loginMsg = ChatMessageUiState.SystemMessageUi(
                    id = "login_${UUID.randomUUID()}",
                    message = loginMessageTemplate.format(authState.userName)
                )
                messageStore.addLocalMessage(loginMsg)
            }

            val initialMsg = ChatMessageUiState.SystemMessageUi(
                id = "loading_${UUID.randomUUID()}",
                message = loadingMessage
            )
            messageStore.addLocalMessage(initialMsg)
            
            // Collect chat settings
            launch { settingsManager.getChatFontSize().collect { _chatFontSize.value = it } }
            launch { settingsManager.getChatEmoteSize().collect { _chatEmoteSize.value = it } }
            launch { settingsManager.getChatBadgeSize().collect { _chatBadgeSize.value = it } }

            // Watch for load status to trigger remapping (emotes and badges)
            launch {
                combine(
                    emoteRepository.globalState,
                    emoteRepository.getChannelState(channel),
                    badgeRepository.globalState,
                    badgeRepository.getChannelState(channel),
                    emoteManager.hasTriggeredEmoteLoad
                ) { globalEmotes, channelEmotes, globalBadges, channelBadges, triggered ->
                    emoteManager.setEmoteLoading(triggered && (!globalEmotes.isLoaded || !channelEmotes.isLoaded))
                    globalEmotes.isLoaded || channelEmotes.isLoaded || globalBadges.isLoaded || channelBadges.isLoaded
                }.collectLatest { anyLoaded ->
                    if (anyLoaded) {
                        delay(1000.milliseconds) // Debounce re-mapping
                        messageStore.remapMessages(viewModelScope, channel)
                    }
                }
            }

            chatClient.connect(channel)
            
            // Always refresh emotes to ensure 3rd party emotes are available for chat parsing.
            emoteManager.refreshEmotes(viewModelScope, channel, force = forceRefresh)
            
            // Welcome message once connected
            launch {
                chatClient.isConnected.collect { connected ->
                    if (connected) {
                        val welcomeMsg = ChatMessageUiState.SystemMessageUi(
                            id = "welcome_${UUID.randomUUID()}",
                            message = welcomeMessageTemplate.format(channel)
                        )
                        messageStore.emitMessage(welcomeMsg)
                    }
                }
            }

            chatClient.messages.collect { msg ->
                launch(Dispatchers.Default) {
                    // Extract room-id for guest users to load 3rd party emotes
                    val roomId = msg.tags["room-id"]
                    if (!roomId.isNullOrEmpty() && _currentChannel.value == channel) {
                        // Only trigger if we have a NEW room ID that we haven't successfully associated yet.
                        // We check lastLoadedRoomId to avoid spamming the refresh for every single message.
                        if (lastLoadedRoomId != roomId) {
                            lastLoadedRoomId = roomId
                            Log.d(TAG, "Detected room-id from chat: $roomId. Triggering background refresh.")
                            launch(Dispatchers.Main) {
                                // NEVER force refresh on room-id detection as it can loop
                                emoteManager.refreshEmotes(viewModelScope, channel, roomId, force = false)
                                emoteManager.resetLoadTrigger(true)
                            }
                        }
                    }

                    if (msg.command == "PRIVMSG") {
                        messageStore.addRawMessage(msg)
                        val uiState = chatMessageMapper.mapToUiState(channel, msg)
                        messageStore.emitMessage(uiState)
                    } else if (msg.command == "NOTICE" || msg.command == "USERNOTICE") {
                        val messageText = msg.params.lastOrNull() ?: msg.raw
                        val systemMsg = ChatMessageUiState.SystemMessageUi(
                            id = msg.id,
                            message = messageText
                        )
                        messageStore.emitMessage(systemMsg)
                        
                        if (msg.command == "NOTICE") {
                            withContext(Dispatchers.Main) {
                                _systemNotice.value = messageText
                                launch {
                                    delay(6000.milliseconds)
                                    if (_systemNotice.value == messageText) {
                                        _systemNotice.value = null
                                    }
                                }
                            }
                        }
                    } else if (msg.command == "USERSTATE" || msg.command == "GLOBALUSERSTATE") {
                        userTags.putAll(msg.tags)
                    }
                }
            }
        }
    }

    fun dismissSystemNotice() {
        _systemNotice.value = null
    }

    suspend fun sendMessage(message: String) {
        val channel = _currentChannel.value ?: return
        val authState = twitchAuthManager.getAuthState()
        
        if (authState.isLoggedIn && !authState.userName.isNullOrEmpty()) {
            val tags = userTags.toMutableMap()
            if (!tags.containsKey("display-name")) {
                tags["display-name"] = authState.userName
            }

            val syntheticMsg = IrcMessage(
                id = UUID.randomUUID().toString(),
                raw = "",
                prefix = "${authState.userName}!${authState.userName}@${authState.userName}.tmi.twitch.tv",
                command = "PRIVMSG",
                params = listOf("#$channel", message),
                tags = tags
            )
            
            withContext(Dispatchers.Default) {
                val uiState = chatMessageMapper.mapToUiState(channel, syntheticMsg)
                withContext(Dispatchers.Main) {
                    messageStore.addLocalMessage(uiState)
                }
            }
        }

        chatClient.sendMessage(channel, message)
    }

    fun disconnect() {
        connectionJob?.cancel()
        chatClient.disconnect()
        messageStore.clear()
        userTags.clear()
        emoteManager.clear()
        _currentChannel.value = null
    }

    fun toggleEmoteMenu() = emoteManager.toggleEmoteMenu()

    fun showEmoteInfo(emote: Emote) = emoteManager.showEmoteInfo(emote)

    fun showEmoteInfo(emoteInfo: EmoteInfo) = emoteManager.showEmoteInfo(emoteInfo, _currentChannel.value)

    fun setEmoteMenuVisible(visible: Boolean) = emoteManager.setEmoteMenuVisible(visible, viewModelScope, _currentChannel.value)

    fun setInputFocused(focused: Boolean) { _isInputFocused.value = focused }

    fun updateKeyboardHeight(context: android.content.Context, heightPx: Int, isLandscape: Boolean) {
        if (heightPx > 0 && _keyboardHeightPx.value != heightPx) {
            _keyboardHeightPx.value = heightPx
            viewModelScope.launch {
                settingsManager.setKeyboardHeight(isLandscape, heightPx)
            }
        }
    }

    fun initKeyboardHeight(isLandscape: Boolean) {
        viewModelScope.launch {
            val height = settingsManager.getKeyboardHeight(isLandscape).first()
            _keyboardHeightPx.value = height
        }
    }

    fun dismissEmoteInfo() = emoteManager.dismissEmoteInfo()

    fun showBadgeInfo(badge: TwitchBadgeDto) { _selectedBadgeForInfo.value = badge }

    fun dismissBadgeInfo() { _selectedBadgeForInfo.value = null }

    fun showUserInfo(userName: String) {
        viewModelScope.launch {
            val user = helixApiClient.getUsers(logins = listOf(userName)).getOrNull()?.firstOrNull()
            if (user != null) {
                _selectedUserForInfo.value = user
            }
        }
    }

    fun dismissUserInfo() { _selectedUserForInfo.value = null }

    fun insertEmote(emote: Emote) = emoteManager.insertEmote(viewModelScope, emote)

    fun recordEmoteUsage(emote: Emote) {
        _currentChannel.value?.let { emoteManager.recordEmoteUsage(viewModelScope, it, emote) }
    }

    fun updateSuggestions(text: String, cursorPosition: Int) {
        _currentChannel.value?.let { emoteManager.updateSuggestions(viewModelScope, it, text, cursorPosition) }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
