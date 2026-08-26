package com.akumasdk.samtch.service

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.api.PreviewImageService
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.api.helix.TwitchHelixMapper
import com.akumasdk.samtch.util.Constants
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var metadataRefreshJob: Job? = null

    private val ACTION_DISMISS_NOTIFICATION = "com.akumasdk.samtch.DISMISS_NOTIFICATION"

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.Actions.STOP_PLAYER, ACTION_DISMISS_NOTIFICATION -> {
                    Log.d("PlaybackService", "Stop/Dismiss requested via Broadcast. Action: ${intent.action}")
                    terminatePlayback()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.akumasdk.samtch.ACTION_REFRESH"
        const val ACTION_STOP_PLAYBACK = "com.akumasdk.samtch.ACTION_STOP_PLAYBACK"
    }

    private var errorRetryCount = 0
    private var lastErrorTime = 0L

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 1. Lifecycle: Register dismissal/stop receiver
        val filter = IntentFilter().apply {
            addAction(Constants.Actions.STOP_PLAYER)
            addAction(ACTION_DISMISS_NOTIFICATION)
        }
        ContextCompat.registerReceiver(this, stopReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // 2. Networking & Data Sources
        val dataSourceFactory = com.akumasdk.samtch.util.StreamingPlayerFactory.getDataSourceFactory()
        
        val loadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val exception = loadErrorInfo.exception
                if (exception is HttpDataSource.InvalidResponseCodeException) {
                    if (exception.responseCode == 403 || exception.responseCode == 404 || exception.responseCode == 410) {
                        return 0 // Immediate retry
                    }
                }
                return super.getRetryDelayMsFor(loadErrorInfo)
            }
        }

        val hlsFactory = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

        // 3. Player Configuration
        val trackSelector = DefaultTrackSelector(this)
        val speedControl = DefaultLivePlaybackSpeedControl.Builder()
            .setFallbackMaxPlaybackSpeed(1.10f) // Matches BufferingManager max speed
            .setFallbackMinPlaybackSpeed(0.95f)
            .setTargetLiveOffsetIncrementOnRebufferMs(1000) // Increase offset more significantly on rebuffer to find stability
            .build()

        exoPlayer = com.akumasdk.samtch.util.StreamingPlayerFactory.createLowLatencyPlayerBuilder(this)
            .setMediaSourceFactory(hlsFactory)
            .setTrackSelector(trackSelector)
            .setLivePlaybackSpeedControl(speedControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlaybackService", "ExoPlayer Error: ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
                
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    Log.w("PlaybackService", "Behind live window, seeking to default and re-preparing")
                    exoPlayer?.seekToDefaultPosition()
                    exoPlayer?.prepare()
                    return
                }

                val now = System.currentTimeMillis()
                if (now - lastErrorTime > 30000) {
                    errorRetryCount = 0
                }
                lastErrorTime = now
                
                // Automatic Recovery for network/timeout errors
                if (errorRetryCount < 3 && (
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)) {
                    
                    errorRetryCount++
                    Log.d("PlaybackService", "Attempting automatic recovery (retry $errorRetryCount)...")
                    exoPlayer?.let {
                        it.prepare()
                        it.play()
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val stateName = when(state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d("PlaybackService", "Playback state changed: $stateName")
                
                if (state == Player.STATE_READY) {
                    startMetadataRefreshLoop()
                } else if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                    metadataRefreshJob?.cancel()
                }
            }
        })

        // 4. Session & UI Actions
        val forwardingPlayer = object : ForwardingPlayer(exoPlayer!!) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .remove(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .remove(COMMAND_SEEK_BACK)
                    .remove(COMMAND_SEEK_FORWARD)
                    .remove(COMMAND_SEEK_TO_PREVIOUS)
                    .remove(COMMAND_SEEK_TO_NEXT)
                    .remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
            }
        }

        val refreshCommand = SessionCommand(ACTION_REFRESH, Bundle.EMPTY)
        val refreshButton = CommandButton.Builder()
            .setSessionCommand(refreshCommand)
            .setDisplayName(getString(R.string.pip_action_refresh))
            .setIconResId(R.drawable.ic_refresh)
            .build()

        val stopCommand = SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY)
        val stopButton = CommandButton.Builder()
            .setSessionCommand(stopCommand)
            .setDisplayName("Stop")
            .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
            .build()

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, com.akumasdk.samtch.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 5. Custom Notification Provider to inject DeleteIntent
        val defaultProvider = DefaultMediaNotificationProvider(this)
        defaultProvider.setSmallIcon(R.drawable.ic_notification)
        
        val customProvider = object : MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val mediaNotification = defaultProvider.createNotification(
                    session, customLayout, actionFactory, onNotificationChangedCallback
                )
                
                val dismissIntent = Intent(ACTION_DISMISS_NOTIFICATION).setPackage(packageName)
                val deleteIntent = PendingIntent.getBroadcast(
                    this@PlaybackService, 1001, dismissIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Recover the builder from the existing notification to preserve all its data
                val builder = android.app.Notification.Builder.recoverBuilder(this@PlaybackService, mediaNotification.notification)
                builder.setDeleteIntent(deleteIntent)
                
                return MediaNotification(mediaNotification.notificationId, builder.build())
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean {
                return defaultProvider.handleCustomCommand(session, action, extras)
            }
        }
        setMediaNotificationProvider(customProvider)

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(CustomCallback())
            .setCustomLayout(ImmutableList.of(refreshButton, stopButton))
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun terminatePlayback() {
        Log.d("PlaybackService", "Terminating playback flow.")
        metadataRefreshJob?.cancel()
        
        // 1. Notify UI
        val stopIntent = Intent(Constants.Actions.STOP_PLAYER).setPackage(packageName)
        sendBroadcast(stopIntent)

        // 2. Stop Service and Release
        mediaSession?.player?.stop()
        mediaSession?.release()
        mediaSession = null
        exoPlayer = null
        stopSelf()

        // 3. App Kill Logic: if background, kill process
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        val isForeground = appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        
        if (!isForeground) {
            Log.d("PlaybackService", "App in background. Executing process kill.")
            android.os.Process.killProcess(android.os.Process.myPid())
        } else {
            Log.d("PlaybackService", "App in foreground. Keeping process alive.")
        }
    }

    private fun startMetadataRefreshLoop() {
        if (metadataRefreshJob?.isActive == true) return
        
        metadataRefreshJob = serviceScope.launch {
            while (true) {
                // Wait 2 minutes between refreshes
                kotlinx.coroutines.delay(2.minutes)
                
                val currentItem = exoPlayer?.currentMediaItem ?: break
                val channelName = currentItem.mediaId
                
                Log.d("PlaybackService", "Refreshing live metadata for $channelName")
                val metadata = TwitchGqlService.getStreamMetadata(channelName)
                val stream = metadata?.user?.stream
                
                if (stream != null) {
                    val previewUri = PreviewImageService.getProcessedUrl(
                        stream.previewImageUrl, 
                        channelName,
                        PreviewImageService.NOTIFICATION_WIDTH,
                        PreviewImageService.NOTIFICATION_HEIGHT
                    ).toUri()
                    
                    val newMetadata = currentItem.mediaMetadata.buildUpon()
                        .setTitle(stream.title)
                        .setArtist(metadata.user.displayName)
                        .setAlbumTitle(stream.game?.name)
                        .setArtworkUri(previewUri)
                        .build()

                    withContext(Dispatchers.Main) {
                        exoPlayer?.replaceMediaItem(
                            exoPlayer?.currentMediaItemIndex ?: 0,
                            currentItem.buildUpon().setMediaMetadata(newMetadata).build()
                        )
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("PlaybackService", "Task removed. Terminating.")
        terminatePlayback()
        super.onTaskRemoved(rootIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.Actions.STOP) {
            terminatePlayback()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }

    private inner class CustomCallback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_REFRESH, Bundle.EMPTY))
                .add(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        @OptIn(UnstableApi::class)
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_REFRESH -> {
                    val player = session.player
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        serviceScope.launch {
                            val resolvedItems = resolveMediaItem(currentItem)
                            val resolvedItem = resolvedItems.firstOrNull()
                            if (resolvedItem != null) {
                                player.setMediaItem(resolvedItem)
                                player.prepare()
                                player.play()
                            }
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_STOP_PLAYBACK -> {
                    terminatePlayback()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }

        @OptIn(UnstableApi::class)
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val item = mediaItems.firstOrNull() ?: return super.onAddMediaItems(mediaSession, controller, mediaItems)
            
            if (item.localConfiguration?.uri != null) return Futures.immediateFuture(mediaItems)

            return serviceScope.async(Dispatchers.IO) {
                resolveMediaItem(item)
            }.asListenableFuture()
        }

        private suspend fun resolveMediaItem(item: MediaItem): MutableList<MediaItem> {
            val channelName = item.mediaId
            val auth = com.akumasdk.samtch.data.auth.TwitchAuthManager.getAuthState(this@PlaybackService)

            val tokenPairDeferred = serviceScope.async { TwitchGqlService.getPlaybackAccessToken(channelName) }
            val metadataDeferred = serviceScope.async { 
                if (auth.isLoggedIn) {
                    try {
                        val helixUser = HelixApiClient.getUsers(this@PlaybackService, logins = listOf(channelName)).getOrNull()?.firstOrNull()
                        val helixStream = HelixApiClient.getStreamMetadata(this@PlaybackService, channelName).getOrNull()
                        if (helixUser != null) {
                            return@async TwitchHelixMapper.mapHelixToMetadata(helixUser, helixStream)
                        }
                    } catch (_: Exception) {}
                }
                TwitchGqlService.getStreamMetadata(channelName)
            }
            
            val tokenPair = tokenPairDeferred.await()
            val detailedMetadata = metadataDeferred.await()
            
            return if (tokenPair != null) {
                val hlsUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
                val user = detailedMetadata?.user
                val stream = user?.stream
                
                val previewUri = PreviewImageService.getProcessedUrl(
                    stream?.previewImageUrl, 
                    channelName,
                    PreviewImageService.NOTIFICATION_WIDTH,
                    PreviewImageService.NOTIFICATION_HEIGHT
                ).toUri()
                
                val newItem = item.buildUpon()
                    .setUri(hlsUrl.toUri())
                    .setLiveConfiguration(com.akumasdk.samtch.util.BufferingManager.getLiveConfiguration())
                    .setMediaMetadata(
                        item.mediaMetadata.buildUpon()
                            .setTitle(stream?.title ?: item.mediaMetadata.title ?: channelName)
                            .setArtist(user?.displayName ?: item.mediaMetadata.artist ?: channelName)
                            .setAlbumTitle(stream?.game?.name)
                            .setArtworkUri(previewUri)
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                            .build()
                    )
                    .build()
                mutableListOf(newItem)
            } else {
                mutableListOf(item)
            }
        }
    }
}
