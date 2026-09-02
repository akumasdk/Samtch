package com.akumasdk.samtch.ui.components.chat

import com.akumasdk.samtch.data.irc.IrcMessage
import com.akumasdk.samtch.util.adaptiveChunked
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

class ChatMessageStore @Inject constructor(
    private val chatMessageMapper: ChatMessageMapper
) {
    private val _messages = MutableStateFlow<ImmutableList<ChatMessageUiState>>(persistentListOf())
    val messages = _messages.asStateFlow()

    private val messageHistory = Collections.synchronizedList(mutableListOf<ChatMessageUiState>())
    private val rawIrcMessages = Collections.synchronizedList(mutableListOf<IrcMessage>())
    private val messageBuffer = MutableSharedFlow<ChatMessageUiState>(extraBufferCapacity = 200)

    fun startMessageProcessing(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            messageBuffer
                .adaptiveChunked(150, 400, 10) // Batch updates every 150-400ms
                .collect { newBatch ->
                    val updatedList = synchronized(messageHistory) { messageHistory.toMutableList() }
                    
                    // Deduplicate by ID to prevent LazyColumn duplicate key crash
                    newBatch.forEach { newMessage ->
                        val existingIndex = updatedList.indexOfFirst { it.id == newMessage.id }
                        if (existingIndex != -1) {
                            updatedList[existingIndex] = newMessage
                        } else {
                            updatedList.add(newMessage)
                        }
                    }
                    
                    if (updatedList.size > 500) { 
                        val toRemove = updatedList.size - 500
                        repeat(toRemove) { updatedList.removeAt(0) }
                    }
                    
                    val immutableBatch = updatedList.toImmutableList()
                    
                    withContext(Dispatchers.Main) {
                        synchronized(messageHistory) {
                            messageHistory.clear()
                            messageHistory.addAll(updatedList)
                        }
                        _messages.value = immutableBatch
                    }
                }
        }
    }

    suspend fun emitMessage(message: ChatMessageUiState) {
        messageBuffer.emit(message)
    }

    fun addRawMessage(msg: IrcMessage) {
        synchronized(rawIrcMessages) {
            if (rawIrcMessages.none { it.id == msg.id }) {
                rawIrcMessages.add(msg)
                if (rawIrcMessages.size > 500) rawIrcMessages.removeAt(0)
            }
        }
    }

    fun clear() {
        synchronized(messageHistory) {
            messageHistory.clear()
        }
        synchronized(rawIrcMessages) {
            rawIrcMessages.clear()
        }
        _messages.value = persistentListOf()
    }

    fun remapMessages(scope: CoroutineScope, channel: String) {
        if (rawIrcMessages.isEmpty()) return
        
        scope.launch(Dispatchers.Default) {
            val idToNewState = synchronized(rawIrcMessages) {
                rawIrcMessages.associate { 
                    it.id to chatMessageMapper.mapToUiState(channel, it) 
                }
            }
            
            withContext(Dispatchers.Main) {
                // Update history in-place while preserving non-IRC messages
                synchronized(messageHistory) {
                    val newHistory = messageHistory.map { oldState ->
                        idToNewState[oldState.id] ?: oldState
                    }
                    
                    messageHistory.clear()
                    messageHistory.addAll(newHistory)
                    _messages.value = messageHistory.toImmutableList()
                }
            }
        }
    }

    fun addLocalMessage(uiState: ChatMessageUiState) {
        synchronized(messageHistory) {
            messageHistory.add(uiState)
            if (messageHistory.size > 500) {
                messageHistory.removeAt(0)
            }
        }
        _messages.value = messageHistory.toImmutableList()
    }
}
