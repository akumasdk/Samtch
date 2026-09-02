package com.akumasdk.samtch.ui.screens.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.api.helix.TwitchHelixMapper
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.service.PlaybackService
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val gqlService: TwitchGqlService,
    private val helixApiClient: HelixApiClient,
    private val authManager: TwitchAuthManager
) : androidx.lifecycle.ViewModel() {
    var channel by mutableStateOf<String?>(null)
        private set
        
    var portraitMode by mutableStateOf(PortraitMode.VIDEO_AND_CHAT)
    var isAudioOnly by mutableStateOf(false)
    
    var streamMetadata by mutableStateOf<TwitchStreamMetadata?>(null)
    var avatarUrl by mutableStateOf<String?>(null)
    var streamSubtitle by mutableStateOf<String?>(null)
    
    var metadataRefreshTrigger by mutableStateOf(0)
    
    var isUiLoading by mutableStateOf(true)
    var loadingMessage by mutableStateOf("")
    var adblockText by mutableStateOf("")
    
    var isChatVisible by mutableStateOf(true)
    var showFullscreenControls by mutableStateOf(true)

    var isChatTemporarilyExpanded by mutableStateOf(false)
    var chatInteractionTrigger by mutableIntStateOf(0)

    fun onChatInteraction() {
        chatInteractionTrigger++
        isChatTemporarilyExpanded = true
    }

    fun toggleChat() {
        isChatVisible = !isChatVisible
    }

    fun toggleFullscreenControls() {
        showFullscreenControls = !showFullscreenControls
    }

    fun isVideoRequired(isFullscreen: Boolean): Boolean {
        return (!isAudioOnly) && (portraitMode != PortraitMode.CHAT_ONLY) && (isFullscreen || portraitMode == PortraitMode.VIDEO_AND_CHAT)
    }

    fun getChatRatio(
        chatRatioPercent: Int,
        screenWidth: Dp,
        screenHeight: Dp,
        isFullscreen: Boolean
    ): Float {
        val baseChatRatio = if (chatRatioPercent == 0) {
            val targetAspectRatio = 16f / 9f
            val screenAspectRatio = screenWidth / screenHeight

            if (screenAspectRatio > targetAspectRatio) {
                val idealPlayerWidth = screenHeight * targetAspectRatio
                val autoRatio = 1f - (idealPlayerWidth / screenWidth)
                autoRatio.coerceIn(0.10f, 0.50f)
            } else {
                0.15f
            }
        } else {
            chatRatioPercent / 100f
        }

        return if (isFullscreen && isChatTemporarilyExpanded) {
            baseChatRatio.coerceAtLeast(0.30f)
        } else {
            baseChatRatio
        }
    }

    var hasBackgroundReloaded by mutableStateOf(false)
    
    var mediaController by mutableStateOf<MediaController?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        
    private var metadataJob: Job? = null

    fun updateChannel(newChannel: String?, forceRefresh: Boolean = false) {
        val isNewChannel = channel != newChannel
        if (!isNewChannel && !forceRefresh) return
        
        // Reset state for the new channel or forced refresh
        channel = newChannel
        
        // Only reset background reload flag if it's a COMPLETELY new channel.
        if (isNewChannel) {
            hasBackgroundReloaded = false
            metadataRefreshTrigger = 0
            
            // Always reset UI mode to standard when changing channels to avoid "breaking logic"
            portraitMode = PortraitMode.VIDEO_AND_CHAT
            isAudioOnly = false
            
            // Clear metadata for new channel
            streamMetadata = null
            avatarUrl = null
            streamSubtitle = null
        }
        
        if (newChannel != null) {
            startMetadataFetch(newChannel)
        } else {
            stopMetadataFetch()
        }
    }

    fun connectMediaController(context: Context) {
        if (mediaController != null) return
        
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            isPlaying = controller.isPlaying
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
            
            // Sync current channel if already playing
            channel?.let { updateMediaItem(it) }
        }, MoreExecutors.directExecutor())
    }

    fun disconnectMediaController() {
        mediaController?.release()
        mediaController = null
        isPlaying = false
    }

    fun togglePlayback() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun updateMediaItem(channelName: String) {
        val controller = mediaController ?: return
        
        viewModelScope.launch(Dispatchers.Default) {
            val metadata = MediaMetadata.Builder()
                .setTitle(streamMetadata?.user?.stream?.title ?: channelName)
                .setArtist(streamMetadata?.user?.displayName ?: channelName)
                .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
                .setArtworkUri(avatarUrl?.toUri())
                .build()
                
            val mediaItem = MediaItem.Builder()
                .setMediaId(channelName)
                .setMediaMetadata(metadata)
                .build()

            withContext(Dispatchers.Main) {
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        }
    }

    private fun startMetadataFetch(channel: String) {
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            // Initial fetch attempt immediately with retries
            var success = false
            repeat(3) { attempt ->
                if (success) return@repeat
                Log.d("PlayerViewModel", "Initial metadata fetch for $channel (attempt ${attempt + 1})")
                
                val metadata = fetchMetadata(channel)
                
                if (metadata != null) {
                    updateMetadataState(metadata)
                    success = true
                } else {
                    delay(2.seconds) // Standard duration delay
                }
            }

            // Periodic fetch
            while (true) {
                delay(1.minutes)
                Log.d("PlayerViewModel", "Periodic metadata fetch for $channel")
                
                val metadata = fetchMetadata(channel)
                
                if (metadata != null) {
                    updateMetadataState(metadata)
                }
            }
        }
    }

    private suspend fun fetchMetadata(channel: String): TwitchStreamMetadata? {
        val auth = authManager.getAuthState()
        
        if (auth.isLoggedIn) {
            try {
                val helixStream = helixApiClient.getStreamMetadata(channel).getOrNull()
                val helixUser = helixApiClient.getUsers(logins = listOf(channel)).getOrNull()?.firstOrNull()
                
                if (helixUser != null) {
                    return TwitchHelixMapper.mapHelixToMetadata(helixUser, helixStream)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Helix metadata fetch failed, falling back to GQL", e)
            }
        }
        
        return gqlService.getStreamMetadata(channel)
    }

    private fun updateMetadataState(metadata: TwitchStreamMetadata) {
        Log.d("PlayerViewModel", "Updating metadata state for ${metadata.user?.login}. Stream live: ${metadata.user?.stream != null}")
        streamMetadata = metadata
        metadataRefreshTrigger++
        
        metadata.user?.let { user ->
            // Optimization: Only update avatarUrl once per channel session to prevent flicker
            if (avatarUrl == null) {
                avatarUrl = user.profileImageUrl
            }
            streamSubtitle = user.stream?.title
        }
    }

    private fun stopMetadataFetch() {
        metadataJob?.cancel()
        metadataJob = null
        streamMetadata = null
        avatarUrl = null
        streamSubtitle = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMetadataFetch()
    }
}
