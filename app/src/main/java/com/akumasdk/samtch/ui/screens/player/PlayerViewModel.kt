package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlayerViewModel : ViewModel() {
    var channel by mutableStateOf<String?>(null)
        private set
        
    var portraitMode by mutableStateOf(PortraitMode.VIDEO_AND_CHAT)
    var isAudioOnly by mutableStateOf(false)
    
    var streamMetadata by mutableStateOf<TwitchStreamMetadata?>(null)
    var avatarUrl by mutableStateOf<String?>(null)
    var streamSubtitle by mutableStateOf<String?>(null)
    
    var hasBackgroundReloaded by mutableStateOf(false)
    
    private var metadataJob: Job? = null

    fun updateChannel(newChannel: String?, forceRefresh: Boolean = false) {
        if (channel == newChannel && !forceRefresh) return
        
        // Reset state for the new channel or forced refresh
        channel = newChannel
        hasBackgroundReloaded = false
        
        // Always reset UI mode to standard when changing channels to avoid "breaking logic"
        if (!forceRefresh) {
            portraitMode = PortraitMode.VIDEO_AND_CHAT
            isAudioOnly = false
        }
        
        if (newChannel != null) {
            // Clear old metadata immediately so the UI doesn't show stale info
            streamMetadata = null
            avatarUrl = null
            streamSubtitle = null
            
            startMetadataFetch(newChannel)
        } else {
            stopMetadataFetch()
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
        val timestampedMetadata = metadata.copy(
            user = metadata.user?.copy(
                stream = metadata.user.stream?.let { stream ->
                    stream.copy(
                        previewImageUrl = stream.previewImageUrl?.let { url ->
                            val separator = if (url.contains("?")) "&" else "?"
                            "$url${separator}t=${System.currentTimeMillis()}"
                        }
                    )
                }
            )
        )
        streamMetadata = timestampedMetadata
        
        timestampedMetadata.user?.let { user ->
            avatarUrl = user.profileImageUrl
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
