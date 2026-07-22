package com.akumasdk.samtch.util

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.akumasdk.samtch.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val ACTION_REFRESH = "com.akumasdk.samtch.ACTION_REFRESH"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Constants.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)

        val trackSelector = DefaultTrackSelector(this).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                .build()
        }

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(30_000, 120_000, 2_000, 5_000)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Wrap player to disable seeking for live streams
        val forwardingPlayer = object : ForwardingPlayer(exoPlayer!!) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_BACK)
                    .remove(Player.COMMAND_SEEK_FORWARD)
                    .remove(Player.COMMAND_SEEK_TO_NEXT)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }
        }

        val refreshCommand = SessionCommand(ACTION_REFRESH, Bundle.EMPTY)
        val refreshButton = CommandButton.Builder()
            .setSessionCommand(refreshCommand)
            .setDisplayName(getString(R.string.pip_action_refresh))
            .setIconResId(R.drawable.ic_refresh)
            .build()

        val intent = Intent(this, com.akumasdk.samtch.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(CustomCallback())
            .setCustomLayout(ImmutableList.of(refreshButton))
            .setSessionActivity(pendingIntent)
            .build()

        val provider = DefaultMediaNotificationProvider(this)
        provider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            mediaSession?.player?.stop()
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
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
            if (customCommand.customAction == ACTION_REFRESH) {
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
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }

        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val item = mediaSession.player.currentMediaItem
            return if (item != null) {
                serviceScope.async(Dispatchers.IO) {
                    val resolved = resolveMediaItem(item)
                    MediaSession.MediaItemsWithStartPosition(resolved, 0, 0L)
                }.asListenableFuture()
            } else {
                super.onPlaybackResumption(mediaSession, controller)
            }
        }

        @OptIn(UnstableApi::class)
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val item = mediaItems.firstOrNull() ?: return super.onAddMediaItems(mediaSession, controller, mediaItems)
            
            // If it already has a URI, it's ready
            if (item.localConfiguration?.uri != null) {
                return Futures.immediateFuture(mediaItems)
            }

            return serviceScope.async(Dispatchers.IO) {
                resolveMediaItem(item)
            }.asListenableFuture()
        }

        private suspend fun resolveMediaItem(item: MediaItem): MutableList<MediaItem> {
            val channelName = item.mediaId
            
            // Parallel fetch for token and metadata
            val tokenPairDeferred = serviceScope.async { TwitchGqlService.getPlaybackAccessToken(channelName) }
            val metadataDeferred = serviceScope.async { TwitchGqlService.getStreamMetadata(channelName) }
            
            val tokenPair = tokenPairDeferred.await()
            val detailedMetadata = metadataDeferred.await()
            
            return if (tokenPair != null) {
                val hlsUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
                val audioOnlyUrl = fetchAudioOnlyUrl(hlsUrl)
                
                val finalUrl = audioOnlyUrl ?: hlsUrl
                
                val user = detailedMetadata?.user
                val stream = user?.stream
                
                val newItem = item.buildUpon()
                    .setUri(Uri.parse(finalUrl))
                    .setMediaMetadata(
                        item.mediaMetadata.buildUpon()
                            .setTitle(stream?.title ?: item.mediaMetadata.title ?: channelName)
                            .setArtist(user?.displayName ?: item.mediaMetadata.artist ?: channelName)
                            .setAlbumTitle(stream?.game?.name)
                            .setArtworkUri(item.mediaMetadata.artworkUri)
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                            .build()
                    )
                    .build()
                mutableListOf(newItem)
            } else {
                mutableListOf(item)
            }
        }
    }

    private suspend fun fetchAudioOnlyUrl(masterUrl: String): String? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(masterUrl)
                .header("User-Agent", Constants.USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val entries = ExtM3UParser().parse(body)
            entries.firstOrNull { it.name == "audio_only" }?.playlistUrl
        } catch (e: Exception) {
            null
        }
    }
}
