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

class PlayerViewModel : ViewModel() {
    var channel by mutableStateOf<String?>(null)
        private set
        
    var portraitMode by mutableStateOf(PortraitMode.VIDEO_AND_CHAT)
    var isAudioOnly by mutableStateOf(false)
    
    var streamMetadata by mutableStateOf<TwitchStreamMetadata?>(null)
    var avatarUrl by mutableStateOf<String?>(null)
    var streamSubtitle by mutableStateOf<String?>(null)
    
    private var metadataJob: Job? = null

    fun updateChannel(newChannel: String?) {
        if (channel == newChannel) return
        channel = newChannel
        if (newChannel != null) {
            startMetadataFetch(newChannel)
        } else {
            stopMetadataFetch()
        }
    }

    private fun startMetadataFetch(channel: String) {
        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            while (true) {
                Log.d("PlayerViewModel", "Fetching periodic metadata for $channel")
                val metadata = TwitchGqlService.getStreamMetadata(channel)
                
                val timestampedMetadata = metadata?.copy(
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
                
                timestampedMetadata?.user?.let { user ->
                    avatarUrl = user.profileImageUrl
                    streamSubtitle = user.stream?.title
                }
                
                delay(1.minutes)
            }
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
