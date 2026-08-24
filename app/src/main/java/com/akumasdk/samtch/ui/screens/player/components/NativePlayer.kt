package com.akumasdk.samtch.ui.screens.player.components

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(UnstableApi::class)
@Composable
fun NativePlayer(
    player: Player?,
    modifier: Modifier = Modifier,
    useController: Boolean = false,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    var isReady by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(1000),
        label = "NativePlayerAlpha"
    )

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("NativePlayer", "Player error: ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
            }
            
            override fun onPlaybackStateChanged(state: Int) {
                Log.d("NativePlayer", "Playback state changed: $state")
                when (state) {
                    Player.STATE_READY -> {
                        isReady = true
                        isBuffering = false
                    }
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    else -> {
                        isBuffering = false
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    this.useController = useController
                    this.resizeMode = resizeMode
                    this.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) 
                    this.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Make the internal video surface container transparent if possible
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize().alpha(alpha)
        )

        if (isBuffering && isReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SamtchTheme.colors.loadingIndicator,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
