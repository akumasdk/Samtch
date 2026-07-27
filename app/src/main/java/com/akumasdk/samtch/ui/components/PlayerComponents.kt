package com.akumasdk.samtch.ui.components

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.akumasdk.samtch.player.SamtchDataSourceFactory

@OptIn(UnstableApi::class)
@Composable
fun NativePlayer(
    modifier: Modifier,
    channel: String,
    onPlaybackStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    var masterPlaylistUrl by remember { mutableStateOf<String?>(null) }
    
    // Fetch initial stream URL via GraphQL directly
    LaunchedEffect(channel) {
        masterPlaylistUrl = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val tokenPair = com.akumasdk.samtch.service.TwitchGqlService.getPlaybackAccessToken(channel)
            if (tokenPair != null) {
                masterPlaylistUrl = com.akumasdk.samtch.service.TwitchGqlService.buildHlsUrl(
                    channel, tokenPair.first, tokenPair.second
                )
            }
        }
    }

    Box(modifier = modifier) {
        if (masterPlaylistUrl != null) {
            val exoPlayer = remember {
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(3_000, 10_000, 1_500, 2_500)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()

                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(
                        HlsMediaSource.Factory(
                            SamtchDataSourceFactory(DefaultHttpDataSource.Factory())
                        ).setAllowChunklessPreparation(true)
                    )
                    .setLoadControl(loadControl)
                    .build().apply {
                        playWhenReady = true
                    }
            }

            val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)

            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            currentOnPlaybackStarted()
                        }
                    }
                }
                exoPlayer.addListener(listener)
                onDispose { 
                    exoPlayer.removeListener(listener)
                    exoPlayer.release() 
                }
            }

            LaunchedEffect(masterPlaylistUrl) {
                val mediaItem = MediaItem.Builder()
                    .setUri(masterPlaylistUrl!!)
                    .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setTargetOffsetMs(2_500)
                            .setMinOffsetMs(1_000)
                            .setMaxOffsetMs(5_000)
                            .setMinPlaybackSpeed(0.97f)
                            .setMaxPlaybackSpeed(1.03f)
                            .build()
                    )
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        keepScreenOn = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
