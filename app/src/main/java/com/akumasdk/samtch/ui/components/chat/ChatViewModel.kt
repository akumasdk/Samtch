package com.akumasdk.samtch.ui.components.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akumasdk.samtch.data.emote.EmoteRepository
import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.service.TwitchChatClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"
    private val chatClient = TwitchChatClient()
    
    private val _messages = MutableStateFlow<ImmutableList<ChatMessageUiState>>(persistentListOf())
    val messages = _messages.asStateFlow()

    private var currentChannel: String? = null
    private val messageHistory = mutableListOf<ChatMessageUiState>()
    private val rawIrcMessages = mutableListOf<IrcMessage>()
    private val messageBuffer = MutableSharedFlow<ChatMessageUiState>(extraBufferCapacity = 100)

    fun connect(channel: String, loadingMessage: String = "Connecting to chat…") {
        if (currentChannel == channel) return
        currentChannel = channel
        rawIrcMessages.clear()
        messageHistory.clear()
        
        // Start with a loading message
        val initialMsg = ChatMessageUiState.SystemMessageUi(
            id = UUID.randomUUID().toString(),
            message = loadingMessage
        )
        messageHistory.add(initialMsg)
        _messages.value = persistentListOf(initialMsg)
        
        viewModelScope.launch {
            Log.d(TAG, "Connecting to channel: $channel")
            
            // Collect messages in batches to prevent UI lag in high-traffic channels
            launch {
                messageBuffer
                    .chunked(150) // Batch updates every 150ms
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
                        
                        if (messageHistory.size > 300) {
                            val toRemove = messageHistory.size - 300
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
            chatClient.messages.collect { msg ->
                if (msg.command == "PRIVMSG") {
                    // Check for duplicate PRIVMSG IDs before adding to raw list
                    if (rawIrcMessages.none { it.id == msg.id }) {
                        rawIrcMessages.add(msg)
                        if (rawIrcMessages.size > 300) rawIrcMessages.removeAt(0)
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
        currentChannel?.let {
            chatClient.sendMessage(it, message)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Explicitly disconnecting chat client")
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

/**
 * Custom flow operator to chunk elements by time
 */
fun <T> Flow<T>.chunked(durationMillis: Long): Flow<List<T>> = flow {
    val buffer = mutableListOf<T>()
    var lastEmitTime = System.currentTimeMillis()
    
    collect { value ->
        buffer.add(value)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmitTime >= durationMillis || buffer.size >= 50) {
            emit(buffer.toList())
            buffer.clear()
            lastEmitTime = currentTime
        }
    }
}
