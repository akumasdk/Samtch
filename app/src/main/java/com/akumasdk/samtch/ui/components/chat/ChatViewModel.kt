package com.akumasdk.samtch.ui.components.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.service.TwitchChatClient
import com.akumasdk.samtch.util.adaptiveChunked
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"
    private val chatClient = TwitchChatClient()
    
    private val _messages = MutableStateFlow<ImmutableList<ChatMessageUiState>>(persistentListOf())
    val messages = _messages.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _loggedInUser = MutableStateFlow<String?>(null)
    val loggedInUser = _loggedInUser.asStateFlow()

    private var currentChannel: String? = null
    private var connectionJob: Job? = null
    private val messageHistory = mutableListOf<ChatMessageUiState>()
    private val rawIrcMessages = mutableListOf<IrcMessage>()
    private val messageBuffer = MutableSharedFlow<ChatMessageUiState>(extraBufferCapacity = 200)

    fun connect(
        channel: String, 
        loadingMessage: String, 
        welcomeMessageTemplate: String = "Welcome to %s's chat!",
        loginMessageTemplate: String? = null
    ) {
        if (currentChannel == channel) return
        
        // 1. Instantly cancel any active session logic for the previous channel
        connectionJob?.cancel()
        currentChannel = channel
        
        // 2. Wipe all state immediately to prevent "leakage" in UI
        rawIrcMessages.clear()
        messageHistory.clear()
        _messages.value = persistentListOf()

        // 3. Check login status
        val authState = TwitchAuthManager.getAuthState()
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

            // Watch for emote load status to trigger remapping
            launch {
                combine(
                    EmoteRepository.globalState,
                    EmoteRepository.getChannelState(channel)
                ) { global, channelState ->
                    global.isLoaded || channelState.isLoaded
                }.collectLatest { anyLoaded ->
                    if (anyLoaded) {
                        delay(1000) // Debounce re-mapping
                        remapMessages(channel)
                    }
                }
            }

            // Load emotes and badges
            launch { EmoteRepository.loadGlobalEmotes() }
            launch { EmoteRepository.loadChannelEmotes(channel) }

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
                    val systemMsg = ChatMessageUiState.SystemMessageUi(
                        id = msg.id,
                        message = msg.params.lastOrNull() ?: msg.raw
                    )
                    messageBuffer.emit(systemMsg)
                }
            }
        }
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
        val authState = TwitchAuthManager.getAuthState()
        
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
        currentChannel = null
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
