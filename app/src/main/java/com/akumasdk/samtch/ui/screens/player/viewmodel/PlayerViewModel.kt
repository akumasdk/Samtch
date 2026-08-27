package com.akumasdk.samtch.ui.screens.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.akumasdk.samtch.util.BufferingManager
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
    private val httpClient = com.akumasdk.samtch.util.StreamingPlayerFactory.okHttpClient
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
    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set

    var currentStreamUrl by mutableStateOf<String?>(null)
        private set
    var availableQualities by mutableStateOf<List<ExtMediaEntry>>(emptyList())
        private set
    var selectedQuality by mutableStateOf<ExtMediaEntry?>(null)
        private set
    var isAdActive by mutableStateOf(false)
        private set
    var adblockMessage by mutableStateOf("")
        private set
    var isHoldingLoader by mutableStateOf(false)
        private set
    var isQualityChanging by mutableStateOf(false)
        private set
        
    private var hasAppliedCleanStreamDuringAd = false
    private var lastUrlUpdateTime = 0L
    private var lastVideoQuality: ExtMediaEntry? = null
    private var metadataJob: Job? = null
    private var loaderHoldJob: Job? = null
    private var watchdogJob: Job? = null

    private var zeroFpsCount = 0

    fun updateChannel(newChannel: String?, forceRefresh: Boolean = false) {
        val isNewChannel = channel != newChannel
        val isSameActiveStream = mediaController?.currentMediaItem?.mediaId == newChannel

        if (!isNewChannel && !forceRefresh) return
        
        // STOP CURRENT PLAYBACK IMMEDIATELY on channel change
        if (isNewChannel && !isSameActiveStream) {
            Log.d("PlayerViewModel", "Channel changing to $newChannel. Stopping current playback.")
            mediaController?.let {
                it.stop()
                it.clearMediaItems()
            }
            currentStreamUrl = null
            masterStreamUrl = null
        }

        // Reset state for the new channel or forced refresh
        channel = newChannel
        
        if ((isNewChannel && !isSameActiveStream) || forceRefresh) {
            hasBackgroundReloaded = false
            metadataRefreshTrigger = 0
            isAdActive = false
            isPlaying = false
            isHoldingLoader = false
            hasAppliedCleanStreamDuringAd = false
            lastUrlUpdateTime = 0L
            zeroFpsCount = 0
            metadataJob?.cancel()
            loaderHoldJob?.cancel()
            
            if (isNewChannel) {
                // Always reset UI mode to standard when changing channels to avoid "breaking logic"
                portraitMode = PortraitMode.VIDEO_AND_CHAT
                isAudioOnly = false

                // Clear metadata for new channel
                streamMetadata = null
                avatarUrl = null
                streamSubtitle = null
                availableQualities = emptyList()
                selectedQuality = null
                lastVideoQuality = null
            }
        }
        
        if (newChannel != null) {
            startMetadataFetch(newChannel)
        } else {
            stopMetadataFetch()
        }
    }

    fun stopAndDisconnect() {
        Log.d("PlayerViewModel", "Force stopping player and disconnecting controller.")
        
        // 1. Tell the service to stop immediately
        val context = getApplication<Application>().applicationContext
        val stopIntent = Intent(context, PlaybackService::class.java).apply {
            action = com.akumasdk.samtch.util.Constants.Actions.STOP
        }
        context.startService(stopIntent)

        // 2. Cleanup controller
        mediaController?.let {
            it.stop()
            it.clearMediaItems()
            it.release()
        }
        mediaController = null
        isPlaying = false
        currentStreamUrl = null
        masterStreamUrl = null
        channel = null
        lastVideoQuality = null
    }

    fun connectMediaController(context: Context) {
        if (mediaController != null) return
        
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            isPlaying = controller.isPlaying
            playbackState = controller.playbackState
            
            // Sync current URL if controller already has one to prevent re-resolution on resume
            controller.currentMediaItem?.localConfiguration?.uri?.toString()?.let { uri ->
                if (currentStreamUrl == null) {
                    Log.d("PlayerViewModel", "Syncing currentStreamUrl from connected controller: $uri")
                    currentStreamUrl = uri
                }
            }

            // Sync audio-only state from controller constraints if applicable
            val currentBitrate = controller.trackSelectionParameters.maxVideoBitrate
            if (currentBitrate <= 250_000 && !isAudioOnly) {
                Log.d("PlayerViewModel", "Detected audio-only constraints on controller. Syncing state.")
                isAudioOnly = true
                portraitMode = PortraitMode.AUDIO_AND_CHAT
            }

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    Log.d("PlayerViewModel", "Player isPlaying changed: $playing")
                    isPlaying = playing
                }
                
                override fun onPlaybackStateChanged(state: Int) {
                    val stateName = when (state) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN ($state)"
                    }
                    Log.d("PlayerViewModel", "Player playbackState changed: $stateName")
                    
                    playbackState = state
                    if (state == Player.STATE_READY) {
                        // Quality change catch-up: 
                        // Only perform an explicit sync seek if we are manually changing quality.
                        if (currentStreamUrl != null && isQualityChanging) {
                            Log.d("PlayerViewModel", "Quality change: performing sync seek to live edge")
                            controller.seekToDefaultPosition()
                            isQualityChanging = false
                        }
                    }
                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Sync our local stream URL if it was changed by the service or elsewhere
                    val newUri = mediaItem?.localConfiguration?.uri?.toString()
                    val reasonName = when (reason) {
                        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
                        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
                        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
                        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
                        else -> "UNKNOWN ($reason)"
                    }
                    Log.d("PlayerViewModel", "MediaItem Transition: $newUri (reason=$reasonName)")
                    
                    if (newUri != null && currentStreamUrl != newUri) {
                        currentStreamUrl = newUri
                    }
                }
            })
            
            // Sync current channel if already playing
            channel?.let { updateMediaItem(it) }
            
            startWatchdog(controller)
        }, MoreExecutors.directExecutor())
    }

    private fun startWatchdog(controller: Player) {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch {
            while (true) {
                delay(1.seconds) // Frequent health checks
                
                if (!isHoldingLoader && !isQualityChanging) {
                    val currentFps = com.akumasdk.samtch.util.FpsMonitor.currentFps.value
                    val state = controller.playbackState
                    val isActuallyPlaying = controller.isPlaying

                    if (isActuallyPlaying && state == Player.STATE_READY) {
                        if (currentFps <= 0f) {
                            zeroFpsCount++
                            // If FPS is 0 for 2 seconds while playing, force refresh
                            if (zeroFpsCount >= 2) {
                                Log.w("PlayerViewModel", "Watchdog: Zero FPS detected for 2s. Refreshing.")
                                triggerRefresh()
                                zeroFpsCount = 0
                                continue
                            }
                        } else {
                            zeroFpsCount = 0
                        }
                    } else if (state == Player.STATE_BUFFERING) {
                        // Keep counting if buffering for too long too
                        zeroFpsCount++
                        if (zeroFpsCount >= 10) {
                            Log.w("PlayerViewModel", "Watchdog: Infinite buffering (>10s). Refreshing.")
                            triggerRefresh()
                            zeroFpsCount = 0
                            continue
                        }
                    } else {
                        zeroFpsCount = 0
                    }
                } else {
                    zeroFpsCount = 0
                }
            }
        }
    }

    private fun triggerRefresh() {
        viewModelScope.launch(Dispatchers.Main) {
            channel?.let { updateMediaItem(it, force = true) }
        }
        zeroFpsCount = 0
    }

    fun disconnectMediaController() {
        mediaController?.release()
        mediaController = null
        // We do NOT reset isPlaying or currentStreamUrl here 
        // to allow for smooth UI transitions when re-connecting.
    }

    fun togglePlayback() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            // "Play" button now triggers a full refresh to get back to live edge
            channel?.let { updateMediaItem(it, force = true) }
        }
    }

    fun toggleAudioOnly() {
        val nextMode = !isAudioOnly
        isAudioOnly = nextMode

        // Apply hardware-level track selection constraints
        updateTrackSelectionForAudioMode(nextMode)

        if (nextMode) {
            // Save current quality if it's a video quality before switching to audio
            if (selectedQuality?.resolution != null) {
                lastVideoQuality = selectedQuality
            }

            portraitMode = PortraitMode.AUDIO_AND_CHAT
            // Use existing qualities to instantly switch to the lowest resolution
            val lowest = availableQualities
                .filter { it.resolution != null }
                .minByOrNull { it.bandwidth ?: Long.MAX_VALUE }
            
            if (lowest != null) {
                Log.d("PlayerViewModel", "toggleAudioOnly: Switching to lowest quality: ${lowest.name ?: lowest.resolution}")
                selectQuality(lowest, isManual = false)
                return
            }
        } else {
            portraitMode = PortraitMode.VIDEO_AND_CHAT
            // Reverting to video: try to restore last saved video quality, otherwise find the highest
            val targetQuality = lastVideoQuality ?: availableQualities.filter { it.resolution != null }
                .maxByOrNull { (it.bandwidth ?: 0L) + (parseResolution(it.resolution) * 1000L) }
            
            if (targetQuality != null) {
                Log.d("PlayerViewModel", "toggleAudioOnly: Restoring quality: ${targetQuality.name ?: targetQuality.resolution}")
                selectQuality(targetQuality, isManual = false)
                lastVideoQuality = null // Clear after restore
                return
            }
        }
        
        // Fallback: If qualities aren't loaded yet, trigger a full resolution refresh
        channel?.let { updateMediaItem(it, force = true) }
    }

    private fun updateTrackSelectionForAudioMode(audioOnly: Boolean) {
        val controller = mediaController ?: return
        val currentParams = controller.trackSelectionParameters
        val newParams = currentParams.buildUpon().apply {
            if (audioOnly) {
                // Force lowest possible bandwidth and size to ensure "Audio Only" behavior
                // regardless of what manifest updates or re-resolutions occur.
                setMaxVideoBitrate(250_000)
                setMaxVideoSize(160, 120)
            } else {
                // Restore defaults for normal video playback
                setMaxVideoBitrate(Int.MAX_VALUE)
                setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            }
        }.build()
        
        if (currentParams != newParams) {
            Log.d("PlayerViewModel", "Updating track selection parameters: isAudioOnly=$audioOnly")
            controller.trackSelectionParameters = newParams
        }
    }

    fun updateMediaItem(channelName: String, force: Boolean = false) {
        val controller = mediaController ?: return

        // 1. PROACTIVE REUSE: If the controller is already playing the right channel,
        // don't even start the resolution unless forced.
        val currentItem = controller.currentMediaItem
        if (!force && currentItem?.mediaId == channelName && 
            (controller.playbackState == Player.STATE_READY || controller.playbackState == Player.STATE_BUFFERING)) {
            Log.d("PlayerViewModel", "updateMediaItem: Controller already has active item for $channelName. Reusing.")
            
            // Sync local URL if missing
            if (currentStreamUrl == null) {
                currentStreamUrl = currentItem.localConfiguration?.uri?.toString()
            }
            return
        }

        Log.d("PlayerViewModel", "updateMediaItem: Triggering resolution for $channelName (force=$force)")
        triggerServiceResolution(channelName, force = force)
    }

    private fun triggerServiceResolution(channelName: String, force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val tokenPair = TwitchGqlService.getPlaybackAccessToken(channelName)
            if (tokenPair == null) {
                Log.e("PlayerViewModel", "Failed to get playback access token for $channelName")
                return@launch
            }

            val masterUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
            masterStreamUrl = masterUrl

            try {
                val request = Request.Builder().url(masterUrl).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val entries = m3u8Parser.parse(body)
                    val variants = entries.filter { !it.playlistUrl.isNullOrEmpty() }
                    
                    if (variants.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            availableQualities = variants
                            Log.d("PlayerViewModel", "Populated ${variants.size} qualities for $channelName")
                        }

                        val bestEntry = if (isAudioOnly) {
                            variants.find { it.name?.contains("audio", ignoreCase = true) == true || it.resolution == null }
                                ?: variants.minByOrNull { it.bandwidth ?: Long.MAX_VALUE }
                        } else {
                            variants.filter { it.resolution != null }
                                .maxByOrNull { (it.bandwidth ?: 0L) + (parseResolution(it.resolution) * 1000L) }
                        }

                        val finalUrl = bestEntry?.playlistUrl ?: masterUrl
                        selectedQuality = bestEntry

                        withContext(Dispatchers.Main) {
                            if (force || currentStreamUrl == null) {
                                applyStreamToController(finalUrl, "service_resolution", isAd = false)
                            }
                        }
                    } else {
                        // Fallback to master URL if no variants found
                        withContext(Dispatchers.Main) {
                            applyStreamToController(masterUrl, "service_resolution_fallback", isAd = false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error resolving master manifest", e)
                withContext(Dispatchers.Main) {
                    applyStreamToController(masterUrl, "service_resolution_error", isAd = false)
                }
            }
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

    fun selectQuality(entry: ExtMediaEntry, isManual: Boolean = true) {
        if (isAdActive) {
            Log.d("PlayerViewModel", "Manual quality selection ignored during active ad session.")
            return
        }
        val url = entry.playlistUrl ?: return
        if (currentStreamUrl == url) return
        
        Log.d("PlayerViewModel", "Manual quality selection: ${entry.resolution ?: entry.name}")
        selectedQuality = entry
        
        // Auto-disable audio-only mode if a video quality is manually selected
        if (isManual && isAudioOnly && entry.resolution != null) {
            Log.d("PlayerViewModel", "Video quality selected manually, disabling audio-only mode.")
            isAudioOnly = false
        }

        isQualityChanging = true
        
        applyStreamToController(url, "manual_selection", isAd = false)
    }

    private fun applyStreamToController(url: String, source: String, isAd: Boolean) {
        val controller = mediaController ?: run {
            Log.w("PlayerViewModel", "applyStreamToController: mediaController is null!")
            return
        }
        val channelName = channel ?: return

        // Prevent redundant reloads if the URL is already set and playing
        if (controller.currentMediaItem?.localConfiguration?.uri?.toString() == url && 
            controller.playbackState != Player.STATE_IDLE && controller.playbackState != Player.STATE_ENDED) {
            Log.d("PlayerViewModel", "applyStreamToController: URL $url is already playing. Skipping reset.")
            return
        }

        val isSafeBuffer = isAd || currentStreamUrl == null
        Log.d("PlayerViewModel", "Applying stream swap [$source] (isAd=$isAd, isSafeBuffer=$isSafeBuffer): $url")
        
        val metadata = MediaMetadata.Builder()
            .setTitle(streamMetadata?.user?.stream?.title ?: channelName)
            .setArtist(streamMetadata?.user?.displayName ?: channelName)
            .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
            .setArtworkUri(avatarUrl?.toUri())
            .build()
            
        // Use BufferingManager for optimized live configuration
        val liveConfig = BufferingManager.getLiveConfiguration(isLowLatencyEnabled = !isSafeBuffer)
        Log.d("PlayerViewModel", "Configuring Live Latency (isLowLatency=${!isSafeBuffer}): target=${liveConfig.targetOffsetMs}ms")

        val mediaItem = MediaItem.Builder()
            .setMediaId(channelName)
            .setUri(url.toUri())
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(metadata)
            .setLiveConfiguration(liveConfig)
            .build()

        // Reset watchdog state to prevent false positives during transition
        zeroFpsCount = 0
        
        // Mask the transition during the buffer build-up
        isHoldingLoader = true
        loaderHoldJob?.cancel()
        loaderHoldJob = viewModelScope.launch {
            // Bleeding edge snappiness
            delay(if (isAd) 1000.milliseconds else 200.milliseconds) 
            isHoldingLoader = false
        }

        // AGGRESSIVE PURGE: Stop and clear before applying the new item
        // This ensures no stale ad segments remain in the buffer.
        controller.stop()
        controller.clearMediaItems()
        
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.seekToDefaultPosition()
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
