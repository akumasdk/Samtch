package com.akumasdk.samtch.ui.screens.player.viewmodel

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.service.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.core.net.toUri
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode

class PlayerViewModel : ViewModel() {
    var channel by mutableStateOf<String?>(null)
        private set
        
    var portraitMode by mutableStateOf(PortraitMode.VIDEO_AND_CHAT)
    var isAudioOnly by mutableStateOf(false)
    
    var streamMetadata by mutableStateOf<TwitchStreamMetadata?>(null)
    var avatarUrl by mutableStateOf<String?>(null)
    var streamSubtitle by mutableStateOf<String?>(null)
    
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
        val metadata = MediaMetadata.Builder()
            .setTitle(streamMetadata?.user?.stream?.title ?: channelName)
            .setArtist(streamMetadata?.user?.displayName ?: channelName)
            .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
            .setArtworkUri(avatarUrl?.toUri())
            .build()
            
        controller.setMediaItem(
            MediaItem.Builder()
                .setMediaId(channelName)
                .setMediaMetadata(metadata)
                .build()
        )
        controller.prepare()
        controller.play()
    }

    private fun startMetadataFetch(channel: String) {
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            // Initial fetch attempt immediately with retries
            var success = false
            repeat(3) { attempt ->
                if (success) return@repeat
                Log.d("PlayerViewModel", "Initial metadata fetch for $channel (attempt ${attempt + 1})")
                val metadata = TwitchGqlService.getStreamMetadata(channel)
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
                val metadata = TwitchGqlService.getStreamMetadata(channel)
                if (metadata != null) {
                    updateMetadataState(metadata)
                }
            }
        }
    }

    private fun updateMetadataState(metadata: TwitchStreamMetadata) {
        val now = System.currentTimeMillis()
        val timestampedMetadata = metadata.copy(
            user = metadata.user?.copy(
                stream = metadata.user.stream?.let { stream ->
                    stream.copy(
                        previewImageUrl = stream.previewImageUrl?.let { url ->
                            val separator = if (url.contains("?")) "&" else "?"
                            "$url${separator}t=$now"
                        }
                    )
                }
            )
        )
        streamMetadata = timestampedMetadata
        
        timestampedMetadata.user?.let { user ->
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
