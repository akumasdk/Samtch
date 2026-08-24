package com.akumasdk.samtch.ui.screens.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    var channel by mutableStateOf<String?>(null)
        private set
        
    var portraitMode by mutableStateOf(PortraitMode.VIDEO_AND_CHAT)
    var isAudioOnly by mutableStateOf(false)
    
    var streamMetadata by mutableStateOf<TwitchStreamMetadata?>(null)
    var avatarUrl by mutableStateOf<String?>(null)
    var streamSubtitle by mutableStateOf<String?>(null)
    
    var metadataRefreshTrigger by mutableStateOf(0)
    
    var hasBackgroundReloaded by mutableStateOf(false)
    
    var mediaController by mutableStateOf<MediaController?>(null)
        private set
    var isPlaying by mutableStateOf(false)

    var currentStreamUrl by mutableStateOf<String?>(null)
        private set
    var isAdActive by mutableStateOf(false)
        private set
        
    private var hasAppliedCleanStreamDuringAd = false
    private var lastUrlUpdateTime = 0L
        
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
            currentStreamUrl = null
            isAdActive = false
            hasAppliedCleanStreamDuringAd = false
            lastUrlUpdateTime = 0L
            
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
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Sync our local stream URL if it was changed by the service or elsewhere
                    val newUri = mediaItem?.localConfiguration?.uri?.toString()
                    if (newUri != null && currentStreamUrl != newUri) {
                        currentStreamUrl = newUri
                    }
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
        
        // If we already have a clean URL, don't trigger the service resolution
        if (currentStreamUrl != null) return

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
                // Double check if clean URL wasn't found while we were building this
                if (currentStreamUrl == null) {
                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                }
            }
        }
    }

    fun onStreamUrlFound(url: String, isValidated: Boolean = false, source: String = "unknown") {
        if (currentStreamUrl == url) return
        
        val now = System.currentTimeMillis()
        
        // ORCHESTRATION LOGIC (Mimic VAFT Swap)
        // 1. If an ad is active, we PRIORITIZE "validated" streams from VAFT logic.
        if (isAdActive) {
            // Ignore non-validated streams (probing/network noise) during ad blocking
            if (!isValidated && source != "main") {
                Log.d("PlayerViewModel", "Ignoring unvalidated stream during ad block: $source -> $url")
                return
            }
            
            // If we already applied a clean stream for THIS ad session, 
            // only allow a swap if it's from a higher-quality source (optional, but keep it simple for now)
            if (hasAppliedCleanStreamDuringAd && source != "embed") { 
                Log.d("PlayerViewModel", "Lock active. Ignoring subsequent clean stream ($source): $url")
                return
            }
        } else {
            // NORMAL FLOW:
            // Respect "main" stream found by VAFT immediately.
            // For others (network sniffer, detector), apply a debounce to avoid stutter.
            if (source != "main" && currentStreamUrl != null && now - lastUrlUpdateTime < 8000) {
                return
            }
        }
        
        Log.d("PlayerViewModel", "Applying stream swap [$source] (isValidated=$isValidated, isAdActive=$isAdActive): $url")
        currentStreamUrl = url
        lastUrlUpdateTime = now
        
        if (isAdActive && isValidated) {
            hasAppliedCleanStreamDuringAd = true
        }
        
        val controller = mediaController ?: run {
            Log.w("PlayerViewModel", "onStreamUrlFound: mediaController is null!")
            return
        }
        val channelName = channel ?: return

        viewModelScope.launch(Dispatchers.Main) {
            Log.d("PlayerViewModel", "Setting new MediaItem ($source) for $channelName")
            val metadata = MediaMetadata.Builder()
                .setTitle(streamMetadata?.user?.stream?.title ?: channelName)
                .setArtist(streamMetadata?.user?.displayName ?: channelName)
                .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
                .setArtworkUri(avatarUrl?.toUri())
                .build()
                
            val mediaItem = MediaItem.Builder()
                .setMediaId(channelName)
                .setUri(url.toUri())
                .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                .setMediaMetadata(metadata)
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    fun onAdStatusChanged(isAd: Boolean, message: String) {
        if (isAdActive == isAd) return
        
        Log.d("PlayerViewModel", "Ad status changed: isAd=$isAd, message=$message")
        isAdActive = isAd
        
        if (!isAd) {
            // Reset for next ad session
            hasAppliedCleanStreamDuringAd = false
            // Reset URL update time to allow immediate switch back to main stream
            lastUrlUpdateTime = 0L 
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
        val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(getApplication())
        
        if (auth.isLoggedIn) {
            try {
                val helixStream = HelixApiClient.getStreamMetadata(getApplication(), channel).getOrNull()
                val helixUser = HelixApiClient.getUsers(getApplication(), logins = listOf(channel)).getOrNull()?.firstOrNull()
                
                if (helixUser != null) {
                    return TwitchHelixMapper.mapHelixToMetadata(helixUser, helixStream)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Helix metadata fetch failed, falling back to GQL", e)
            }
        }
        
        return TwitchGqlService.getStreamMetadata(channel)
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
