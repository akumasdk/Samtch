package com.akumasdk.samtch.ui.screens.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.akumasdk.samtch.data.api.adblock.AdBlockConfig
import com.akumasdk.samtch.data.api.adblock.AdBlockOrchestrator
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
    private val adBlockOrchestrator = AdBlockOrchestrator(httpClient, m3u8Parser) { adStatus ->
        viewModelScope.launch(Dispatchers.Main) {
            val message = if (adStatus.hasAds) {
                val type = adStatus.playerType ?: if (adStatus.isStrippingAdSegments) "stripping" else "unknown"
                if (adStatus.isMidroll) "midroll ($type)" else "preroll ($type)"
            } else ""
            onAdStatusChanged(adStatus.hasAds, message)
        }
    }
    
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
    var adblockMessage by mutableStateOf("")
        private set
    var isHoldingLoader by mutableStateOf(false)
        private set
    var isQualityChanging by mutableStateOf(false)
        private set
        
    private var hasAppliedCleanStreamDuringAd = false
    private var lastUrlUpdateTime = 0L
    private var metadataJob: Job? = null
    private var loaderHoldJob: Job? = null
    private var adBlockJob: Job? = null

    fun updateChannel(newChannel: String?, forceRefresh: Boolean = false) {
        val isNewChannel = channel != newChannel
        if (!isNewChannel && !forceRefresh) return
        
        // STOP CURRENT PLAYBACK IMMEDIATELY on channel change
        if (isNewChannel) {
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
        
        if (isNewChannel || forceRefresh) {
            newChannel?.let { adBlockOrchestrator.resetChannel(it) }
            hasBackgroundReloaded = false
            metadataRefreshTrigger = 0
            isAdActive = false
            isHoldingLoader = false
            hasAppliedCleanStreamDuringAd = false
            lastUrlUpdateTime = 0L
            metadataJob?.cancel()
            loaderHoldJob?.cancel()
            adBlockJob?.cancel()
            
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
            }
        }
        
        if (newChannel != null) {
            startMetadataFetch(newChannel)
            startAdBlockOrchestrator(newChannel)
        } else {
            stopMetadataFetch()
            adBlockJob?.cancel()
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
    }

    private fun startAdBlockOrchestrator(channelName: String) {
        adBlockJob?.cancel()
        adBlockJob = viewModelScope.launch {
            while (true) {
                val updateUrl = adBlockOrchestrator.getCleanStreamUrl(
                    channelName = channelName,
                    targetResolution = selectedQuality?.resolution
                )
                if (updateUrl != null) {
                    Log.d(AdBlockConfig.LOG_TAG, "AdBlock state change detected: $updateUrl")
                    withContext(Dispatchers.Main) {
                        onStreamUrlFound(updateUrl, isValidated = true, source = "adblock_sync")
                    }
                }
                delay(4.seconds) // Check every 4 seconds for ad changes (High precision)
            }
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

        Log.d(AdBlockConfig.LOG_TAG, "updateMediaItem: Triggering AdBlock resolution for $channelName")
        viewModelScope.launch(Dispatchers.Default) {
            val cleanUrl = adBlockOrchestrator.getCleanStreamUrl(channelName, selectedQuality?.resolution)
            
            withContext(Dispatchers.Main) {
                if (cleanUrl != null) {
                    onStreamUrlFound(cleanUrl, isValidated = true, source = "adblock_init")
                } else {
                    // Fallback to service resolution if AdBlock fails for some reason
                    triggerServiceResolution(channelName, controller)
                }
            }
        }
    }

    private fun triggerServiceResolution(channelName: String, controller: MediaController) {
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
                if (currentStreamUrl == null) {
                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                }
            }
        }
    }

    fun onStreamUrlFound(url: String, isValidated: Boolean = false, source: String = "unknown") {
        // If we already have this URL as our master or current source, ignore unless it's a manual override
        if (availableQualities.isNotEmpty() && (currentStreamUrl == url || masterStreamUrl == url) && 
            source != "manual_selection" && source != "mode_change") {
            return
        }
        
        val now = System.currentTimeMillis()
        
        // ORCHESTRATION LOGIC
        if (isAdActive) {
            if (!isValidated) {
                Log.d(AdBlockConfig.LOG_TAG, "Ignoring unvalidated stream during ad block: source=$source -> $url")
                return
            }
            
            if (hasAppliedCleanStreamDuringAd) { 
                Log.d(AdBlockConfig.LOG_TAG, "Lock active. Ignoring subsequent clean stream (source=$source, current=$currentStreamUrl): $url")
                return
            }
        } else {
            if (currentStreamUrl != null && now - lastUrlUpdateTime < 8000) {
                return
            }
        }
        
        currentStreamUrl = url
        lastUrlUpdateTime = now
        
        if (isAdActive && isValidated) {
            hasAppliedCleanStreamDuringAd = true
        }

        // Cache the master URL
        if (url.contains("usher.ttvnw.net") || source.contains("adblock")) {
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
                        val variants = entries.filter { !it.playlistUrl.isNullOrEmpty() }
                        
                        // Only update available qualities if we found variant stream info (master manifest)
                        if (variants.any { it.resolution != null || it.bandwidth != null }) {
                            if (!isAdActive || availableQualities.isEmpty()) {
                                availableQualities = variants
                                Log.d("PlayerViewModel", "Populated available qualities from master: ${variants.size} options")
                            }
                        }
                        
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
                            applyStreamToController(finalUrl, source, isAd = isAdActive)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            applyStreamToController(url, source, isAd = isAdActive)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error fetching/parsing M3U8", e)
                    withContext(Dispatchers.Main) {
                        applyStreamToController(url, source, isAd = isAdActive)
                    }
                }
            }
        } else {
            applyStreamToController(url, source, isAd = isAdActive)
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
        if (isAdActive) {
            Log.d(AdBlockConfig.LOG_TAG, "Manual quality selection ignored during active ad session.")
            return
        }
        val url = entry.playlistUrl ?: return
        if (currentStreamUrl == url) return
        
        Log.d("PlayerViewModel", "Manual quality selection: ${entry.resolution ?: entry.name}")
        selectedQuality = entry
        isQualityChanging = true
        
        applyStreamToController(url, "manual_selection", isAd = false)
    }

    private fun applyStreamToController(url: String, source: String, isAd: Boolean) {
        val controller = mediaController ?: run {
            Log.w("PlayerViewModel", "applyStreamToController: mediaController is null!")
            return
        }
        val channelName = channel ?: return

        val isSafeBuffer = isAd || currentStreamUrl == null
        Log.d(AdBlockConfig.LOG_TAG, "Applying stream swap [$source] (isAd=$isAd, isSafeBuffer=$isSafeBuffer): $url")
        
        val metadata = MediaMetadata.Builder()
            .setTitle(streamMetadata?.user?.stream?.title ?: channelName)
            .setArtist(streamMetadata?.user?.displayName ?: channelName)
            .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
            .setArtworkUri(avatarUrl?.toUri())
            .build()
            
        // "Safe Buffer" vs "Low Latency" Live Configuration:
        val targetOffset = if (isSafeBuffer) 5000L else 3000L
        val minOffset = if (isSafeBuffer) 2000L else 1500L
        val maxOffset = if (isSafeBuffer) 15000L else 10000L

        Log.d(AdBlockConfig.LOG_TAG, "Configuring Live Latency: target=${targetOffset}ms, min=${minOffset}ms")

        val liveConfig = MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(targetOffset)
            .setMaxOffsetMs(maxOffset)
            .setMinOffsetMs(minOffset)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(channelName)
            .setUri(url.toUri())
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(metadata)
            .setLiveConfiguration(liveConfig)
            .build()

        // Mask the transition during the buffer build-up
        isHoldingLoader = true
        loaderHoldJob?.cancel()
        loaderHoldJob = viewModelScope.launch {
            // Wait for buffer to stabilize
            delay(if (isAd) 2500.milliseconds else 1500.milliseconds) 
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

    fun onAdStatusChanged(isAd: Boolean, message: String) {
        if (isAdActive == isAd && adblockMessage == message) {
            return
        }
        
        Log.d(AdBlockConfig.LOG_TAG, "Ad status changed: isAd=$isAd, type=$message. RESETTING LOCKS.")
        isAdActive = isAd
        adblockMessage = message
        
        if (!isAd) {
            // Reset for next ad session
            hasAppliedCleanStreamDuringAd = false
            // Reset URL update time to allow immediate switch back to main stream
            lastUrlUpdateTime = 0L 
        }
    }

    fun onAdblocked(text: String) {
        adblockMessage = text
        if (text.contains("autoplay", ignoreCase = true) && isAdActive) {
            Log.d(AdBlockConfig.LOG_TAG, "Autoplay ad detected, holding loader.")
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
