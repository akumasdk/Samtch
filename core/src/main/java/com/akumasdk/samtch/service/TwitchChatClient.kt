package com.akumasdk.samtch.service

import android.util.Log
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import com.akumasdk.samtch.data.irc.IrcMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TwitchChatClient(
    private val context: android.content.Context,
    private val httpClient: HttpClient = HttpClient { install(WebSockets) }
) {
    private val TAG = "TwitchChatClient"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val _messages = Channel<IrcMessage>(capacity = Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var currentChannel: String? = null

    fun connect(channel: String) {
        if (_isConnected.value && currentChannel == channel) return
        
        connectionJob?.cancel()
        currentChannel = channel

        connectionJob = scope.launch {
            val authState = TwitchAuthManager.getAuthState(context)
            val nick = authState.userName ?: "justinfan${(10000..99999).random()}"
            val pass = if (authState.authToken != null) "oauth:${authState.authToken}" else "SCHMOOPIE"

            while (isActive) {
                try {
                    Log.d(TAG, "Connecting to Twitch IRC as $nick...")
                    httpClient.webSocket("wss://irc-ws.chat.twitch.tv") {
                        session = this
                        _isConnected.value = true

                        sendSerialized("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership")
                        sendSerialized("PASS $pass")
                        sendSerialized("NICK $nick")
                        sendSerialized("JOIN #$channel")

                        while (isActive) {
                            val frame = incoming.receive()
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                text.split("\r\n").filter { it.isNotEmpty() }.forEach { line ->
                                    val msg = IrcMessage.parse(line)
                                    if (msg.command == "PING") {
                                        sendSerialized("PONG :tmi.twitch.tv")
                                    } else {
                                        _messages.send(msg)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Websocket error: ${e.message}")
                } finally {
                    _isConnected.value = false
                    session = null
                    if (isActive) {
                        Log.d(TAG, "Reconnecting in 5 seconds...")
                        delay(5.seconds)
                    }
                }
            }
        }
    }

    suspend fun sendMessage(channel: String, message: String) {
        if (_isConnected.value) {
            sendSerialized("PRIVMSG #$channel :$message")
        }
    }

    private suspend fun sendSerialized(text: String) {
        session?.send(Frame.Text(text))
    }

    fun disconnect() {
        connectionJob?.cancel()
        _isConnected.value = false
        session = null
    }
}
