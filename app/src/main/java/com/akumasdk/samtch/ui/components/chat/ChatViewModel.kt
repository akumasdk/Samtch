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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"
    private val chatClient = TwitchChatClient()
    
    private val _messages = MutableStateFlow<ImmutableList<ChatMessageUiState>>(persistentListOf())
    val messages = _messages.asStateFlow()

    private var currentChannel: String? = null
    private val rawMessages = mutableListOf<IrcMessage>()

    fun connect(channel: String) {
        if (currentChannel == channel) return
        currentChannel = channel
        rawMessages.clear()
        _messages.value = persistentListOf()
        
        viewModelScope.launch {
            Log.d(TAG, "Connecting to channel: $channel")
            
            // Watch for emote load status to trigger remapping
            launch {
                combine(
                    EmoteRepository.globalState,
                    EmoteRepository.getChannelState(channel)
                ) { global, channelState ->
                    global.isLoaded || channelState.isLoaded
                }.collectLatest { anyLoaded ->
                    if (anyLoaded) {
                        delay(500) // Batch re-mapping
                        remapMessages(channel)
                    }
                }
            }

            // Load emotes
            launch { EmoteRepository.loadGlobalEmotes() }
            launch { EmoteRepository.loadChannelEmotes(channel) }

            chatClient.connect(channel)
            chatClient.messages.collect { msg ->
                if (msg.command == "PRIVMSG") {
                    rawMessages.add(msg)
                    if (rawMessages.size > 300) rawMessages.removeAt(0)
                    
                    val uiState = ChatMessageMapper.mapToUiState(channel, msg)
                    _messages.value = (_messages.value + uiState).takeLast(300).toImmutableList()
                } else if (msg.command == "NOTICE" || msg.command == "USERNOTICE") {
                    // Handle other message types if needed
                }
            }
        }
    }

    private fun remapMessages(channel: String) {
        if (rawMessages.isEmpty()) return
        Log.d(TAG, "Remapping ${rawMessages.size} messages for channel: $channel")
        val newMessages = rawMessages.map { 
            ChatMessageMapper.mapToUiState(channel, it) 
        }.toImmutableList()
        _messages.value = newMessages
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
        rawMessages.clear()
        currentChannel = null
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
