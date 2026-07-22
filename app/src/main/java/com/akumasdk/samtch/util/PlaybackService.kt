package com.akumasdk.samtch.util

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(CustomCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

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

            // Otherwise, it's a channel name in the mediaId, we need to fetch the HLS URL
            return serviceScope.async(Dispatchers.IO) {
                val channelName = item.mediaId
                val tokenPair = TwitchGqlService.getPlaybackAccessToken(channelName, Constants.TWITCH_GRAPHQL_CLIENT_ID)
                
                if (tokenPair != null) {
                    val hlsUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second)
                    val audioOnlyUrl = fetchAudioOnlyUrl(hlsUrl)
                    
                    val finalUrl = audioOnlyUrl ?: hlsUrl
                    
                    val newItem = item.buildUpon()
                        .setUri(finalUrl)
                        .setMediaMetadata(
                            item.mediaMetadata.buildUpon()
                                .setArtist(channelName)
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .build()
                        )
                        .build()
                    mutableListOf(newItem)
                } else {
                    mediaItems
                }
            }.asListenableFuture()
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
