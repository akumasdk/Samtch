package com.akumasdk.samtch.ui.screens.player.components

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun NativePlayer(
    player: Player?,
    modifier: Modifier = Modifier,
    useController: Boolean = true, // Temporarily true for debugging
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    Log.d("NativePlayer", "NativePlayer composed with player: $player")
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        
        Log.d("NativePlayer", "Adding listener to player: $player")
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("NativePlayer", "Player error: ${error.message}", error)
            }
            
            override fun onPlaybackStateChanged(state: Int) {
                Log.d("NativePlayer", "Playback state changed: $state")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                this.useController = useController
                this.resizeMode = resizeMode
                this.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.player = player
            playerView.resizeMode = resizeMode
        },
        modifier = modifier.fillMaxSize()
    )
}
