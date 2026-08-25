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
import com.akumasdk.samtch.util.ExtM3UParser
import com.akumasdk.samtch.util.ExtMediaEntry
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val httpClient = OkHttpClient()
    private val m3u8Parser = ExtM3UParser()
    
    var channel by mutableStateOf<String?>(null)
        private set
        
    var portraitMode by mutableStateOf(PortraitMode.VIDEO_AND_CHAT)
    var isAudioOnly by mutableStateOf(false)
    
    var masterStreamUrl: String? = null
        private set

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
    var availableQualities by mutableStateOf<List<ExtMediaEntry>>(emptyList())
        private set
    var selectedQuality by mutableStateOf<ExtMediaEntry?>(null)
        private set
    var isAdActive by mutableStateOf(false)
        private set
    var isHoldingLoader by mutableStateOf(false)
        private set
    var isQualityChanging by mutableStateOf(false)
        private set
        
    private var hasAppliedCleanStreamDuringAd = false
    private var lastUrlUpdateTime = 0L
    private var metadataJob: Job? = null
    private var loaderHoldJob: Job? = null

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
            masterStreamUrl = null
            isAdActive = false
            isHoldingLoader = false
            hasAppliedCleanStreamDuringAd = false
            lastUrlUpdateTime = 0L
            metadataJob?.cancel()
            loaderHoldJob?.cancel()
            
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
                
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        // When transitioning to READY for a new stream, 
                        // ensure we are as close to the live edge as possible
                        if (currentStreamUrl != null) {
                            controller.seekToDefaultPosition()
                            
                            if (isQualityChanging) {
                                Log.d("PlayerViewModel", "Quality change catch-up: performing secondary live seek")
                                viewModelScope.launch {
                                    delay(600.milliseconds)
                                    controller.seekToDefaultPosition()
                                    isQualityChanging = false
                                }
                            }
                        }
                    }
                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Sync our local stream URL if it was changed by the service or elsewhere
                    val newUri = mediaItem?.localConfiguration?.uri?.toString()
                    Log.d("PlayerViewModel", "MediaItem Transition: $newUri (reason=$reason)")
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
        if (controller.isPlaying) {
            controller.pause()
        } else {
            // Catch up to live edge when resuming
            controller.seekToDefaultPosition()
            controller.play()
        }
    }

    fun updateMediaItem(channelName: String) {
        val controller = mediaController ?: return
        
        // If we already have a clean URL, don't trigger the service resolution
        if (currentStreamUrl != null) {
            Log.d("PlayerViewModel", "updateMediaItem: Already have a clean URL ($currentStreamUrl), skipping service resolution.")
            return
        }

        Log.d("PlayerViewModel", "updateMediaItem: Triggering background resolution for $channelName")
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
        if (isAdActive) {
            if (!isValidated && source != "main") {
                Log.d("PlayerViewModel", "Ignoring unvalidated stream during ad block: source=$source, validated=$isValidated -> $url")
                return
            }
            
            if (hasAppliedCleanStreamDuringAd) { 
                Log.d("PlayerViewModel", "Lock active. Ignoring subsequent clean stream (source=$source, current=$currentStreamUrl): $url")
                return
            }
        } else {
            if (source != "main" && currentStreamUrl != null && now - lastUrlUpdateTime < 8000) {
                return
            }
        }
        
        currentStreamUrl = url
        lastUrlUpdateTime = now
        
        if (isAdActive && isValidated) {
            hasAppliedCleanStreamDuringAd = true
        }

        // Cache the master URL if it's main or from usher
        if (url.contains("usher.ttvnw.net") || source == "main") {
            masterStreamUrl = url
        }

        // If it's the master playlist, fetch and select best quality
        if (url.contains("usher.ttvnw.net") || url.contains("m3u8")) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val entries = m3u8Parser.parse(body)
                        
                        // Only update available qualities if we are not in an active ad session
                        // or if this is the first time we get them for this channel.
                        if (!isAdActive || availableQualities.isEmpty()) {
                            availableQualities = entries.filter { !it.playlistUrl.isNullOrEmpty() }
                        }
                        
                        // Filter for video variants and sort by bandwidth/resolution
                        val variants = entries.filter { !it.playlistUrl.isNullOrEmpty() }
                        
                        val bestEntry = if (isAudioOnly) {
                            variants.find { it.name?.contains("audio", ignoreCase = true) == true || it.resolution == null }
                                ?: variants.minByOrNull { it.bandwidth ?: Long.MAX_VALUE }
                        } else {
                            variants.filter { it.resolution != null }
                                .maxByOrNull { (it.bandwidth ?: 0L) + (parseResolution(it.resolution) * 1000L) }
                        }

                        val finalUrl = bestEntry?.playlistUrl ?: url
                        selectedQuality = bestEntry
                        Log.d("PlayerViewModel", "Best quality found (isAudioOnly=$isAudioOnly): ${bestEntry?.resolution ?: bestEntry?.name ?: "original"} -> $finalUrl")
                        
                        withContext(Dispatchers.Main) {
                            applyStreamToController(finalUrl, source)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            applyStreamToController(url, source)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error fetching/parsing M3U8", e)
                    withContext(Dispatchers.Main) {
                        applyStreamToController(url, source)
                    }
                }
            }
        } else {
            applyStreamToController(url, source)
        }
    }

    private fun parseResolution(resolution: String?): Int {
        if (resolution == null) return 0
        return try {
            val parts = resolution.split('x')
            if (parts.size == 2) parts[0].toInt() * parts[1].toInt() else 0
        } catch (_: Exception) {
            0
        }
    }

    fun selectQuality(entry: ExtMediaEntry) {
        val url = entry.playlistUrl ?: return
        if (currentStreamUrl == url) return
        
        Log.d("PlayerViewModel", "Manual quality selection: ${entry.resolution ?: entry.name}")
        selectedQuality = entry
        isQualityChanging = true
        
        // Smart catch up: show a brief loader to cover the buffer swap
        isHoldingLoader = true
        loaderHoldJob?.cancel()
        loaderHoldJob = viewModelScope.launch {
            delay(2.seconds) // Increased delay to ensure stable playback before showing video
            isHoldingLoader = false
        }
        
        applyStreamToController(url, "manual_selection")
    }

    private fun applyStreamToController(url: String, source: String) {
        val controller = mediaController ?: run {
            Log.w("PlayerViewModel", "applyStreamToController: mediaController is null!")
            return
        }
        val channelName = channel ?: return

        Log.d("PlayerViewModel", "Applying stream swap [$source]: $url")
        val metadata = MediaMetadata.Builder()
            .setTitle(streamMetadata?.user?.stream?.title ?: channelName)
            .setArtist(streamMetadata?.user?.displayName ?: channelName)
            .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
            .setArtworkUri(avatarUrl?.toUri())
            .build()
            
        // Use a balanced live configuration to minimize delay without causing stutters
        val liveConfig = MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(3000) // Aim for 3 seconds latency (balanced)
            .setMaxOffsetMs(10000)
            .setMinOffsetMs(1500)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(channelName)
            .setUri(url.toUri())
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(metadata)
            .setLiveConfiguration(liveConfig)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        // Explicitly seek to the very end (live edge) to remove any initial buffer delay
        controller.seekToDefaultPosition()
        controller.play()
        Log.d("PlayerViewModel", "MediaItem applied to controller (seeking to live): $url")
    }

    fun onAdStatusChanged(isAd: Boolean, message: String) {
        if (isAdActive == isAd) {
            Log.d("PlayerViewModel", "Ad status redundancy check: isAd=$isAd")
            return
        }
        
        Log.d("PlayerViewModel", "Ad status changed: isAd=$isAd, type=$message. RESETTING LOCKS.")
        isAdActive = isAd
        
        if (!isAd) {
            // Reset for next ad session
            hasAppliedCleanStreamDuringAd = false
            // Reset URL update time to allow immediate switch back to main stream
            lastUrlUpdateTime = 0L 
        }
    }

    fun onAdblocked(text: String) {
        if (text.contains("autoplay", ignoreCase = true) && isAdActive) {
            Log.d("PlayerViewModel", "Autoplay ad detected, holding loader.")
            isHoldingLoader = true
            loaderHoldJob?.cancel()
            loaderHoldJob = viewModelScope.launch {
                delay(3.5.seconds) // Hold for 3.5 seconds to ensure clean stream swap
                isHoldingLoader = false
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
