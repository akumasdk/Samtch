package com.akumasdk.samtch.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.akumasdk.samtch.ui.screens.player.AudioOnlyPlayer
import com.akumasdk.samtch.ui.screens.player.FullscreenPlayer
import com.akumasdk.samtch.ui.screens.player.MiniPlayer
import com.akumasdk.samtch.ui.screens.player.PortraitPlayer
import com.akumasdk.samtch.ui.screens.player.WebViewContainer
import com.akumasdk.samtch.ui.screens.player.createTwitchPlayerUrl
import com.akumasdk.samtch.util.PlaybackService
import com.akumasdk.samtch.util.SettingsManager

@Composable
fun TwitchPlayer(
    channel: String = "forsen",
    isFullscreen: Boolean = false,
    isPip: Boolean = false,
    isMinimized: Boolean = false,
    refreshTrigger: Int = 0,
    onToggleFullscreen: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onClose: () -> Unit = {},
    onExpand: () -> Unit = {},
    onMetadataUpdated: (String?, String?) -> Unit = { _, _ -> },
    onAudioOnlyModeChanged: (Boolean) -> Unit = {},
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {}
) {
    val context = LocalContext.current
    var isAudioOnly by remember { mutableStateOf(false) }

    LaunchedEffect(isAudioOnly) {
        onAudioOnlyModeChanged(isAudioOnly)
    }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var streamSubtitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
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
            })
        }, MoreExecutors.directExecutor())
    }

    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()

    android.util.Log.d("TwitchPlayer", "Creating player for channel: $channel (isPip: $isPip, isMinimized: $isMinimized)")

    // Handle back button to return to browser (minimize)
    if (!isPip && !isMinimized) {
        androidx.activity.compose.BackHandler {
            android.util.Log.d("TwitchPlayer", "BackHandler triggered for $channel")
            onBack?.invoke()
        }
    }

    // Handle URL loading and refresh logic
    LaunchedEffect(channel, refreshTrigger) {
        val baseUrl = createTwitchPlayerUrl(channel)
        val finalUrl = if (refreshTrigger > 0) {
            "$baseUrl&refresh=$refreshTrigger"
        } else {
            baseUrl
        }
        android.util.Log.d("TwitchPlayer", "Loading URL: $finalUrl (trigger: $refreshTrigger)")
        navigator.loadUrl(finalUrl)
    }

    DisposableEffect(channel) {
        onDispose {
            android.util.Log.d("TwitchPlayer", "Disposing player for channel: $channel")
            // Clean up WebView resources aggressively
            try {
                state.nativeWebView.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearCache(true)
                    clearHistory()
                    clearFormData()
                    // Remove all views to prevent overlap
                    removeAllViews()
                }
            } catch (e: Exception) {
                android.util.Log.e("TwitchPlayer", "Error disposing WebView", e)
            }
        }
    }

    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentAvatarUrl by rememberUpdatedState(avatarUrl)
    val currentSubtitle by rememberUpdatedState(streamSubtitle)

    // Stable WebView content that won't be recreated when moving in the tree
    val webView = remember(channel) {
        movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
            WebViewContainer(
                modifier = modifier.onGloballyPositioned { layoutCoordinates ->
                    val rect = layoutCoordinates.boundsInWindow()
                    onVideoBoundsChanged(
                        android.graphics.Rect(
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.right.toInt(),
                            rect.bottom.toInt()
                        )
                    )
                },
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = onToggleFullscreen,
                onToggleChat = onToggleChat,
                onToggleAudioOnly = {
                    isAudioOnly = true
                    val avatarUri = currentAvatarUrl?.let { android.net.Uri.parse(it) }
                    val metadata = MediaMetadata.Builder()
                        .setTitle(channel)
                        .setArtist(currentSubtitle)
                        .setArtworkUri(avatarUri)
                        .build()
                    mediaController?.setMediaItem(
                        MediaItem.Builder()
                            .setMediaId(channel)
                            .setMediaMetadata(metadata)
                            .build()
                    )
                    mediaController?.prepare()
                    mediaController?.play()
                },
                onMetadataDetected = { avatar, subtitle ->
                    avatarUrl = avatar
                    streamSubtitle = subtitle
                    onMetadataUpdated(avatar, subtitle)
                }
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val playerContent = remember(channel, isAudioOnly) {
            movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
                if (isAudioOnly) {
                    AudioOnlyPlayer(
                        channel = channel,
                        avatarUrl = avatarUrl,
                        subtitle = streamSubtitle,
                        isPlaying = isPlaying,
                        onTogglePlayback = {
                            if (currentIsPlaying) mediaController?.pause() else mediaController?.play()
                        },
                        onCloseAudioOnly = {
                            isAudioOnly = false
                            mediaController?.stop()
                            // Explicitly reload video player when returning to trigger script injection
                            navigator.loadUrl(createTwitchPlayerUrl(channel))
                        },
                        onRefresh = {
                            mediaController?.stop()
                            val avatarUri = currentAvatarUrl?.let { android.net.Uri.parse(it) }
                            mediaController?.setMediaItem(
                                MediaItem.Builder()
                                    .setMediaId(channel)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(channel)
                                            .setArtist(currentSubtitle)
                                            .setArtworkUri(avatarUri)
                                            .build()
                                    )
                                    .build()
                            )
                            mediaController?.prepare()
                            mediaController?.play()
                        },
                        modifier = modifier
                    )
                } else {
                    webView(modifier, onToggleChat)
                }
            }
        }

        if (isMinimized) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                MiniPlayer(
                    channel = channel,
                    playerContent = { modifier -> playerContent(modifier) {} },
                    onClick = onExpand,
                    onClose = onClose
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (isPip) {
                    // Simplified view for PiP: Just the WebView container
                    playerContent(Modifier.fillMaxSize()) {}
                } else if (isFullscreen) {
                    FullscreenPlayer(
                        channel = channel,
                        webView = { modifier, onToggleChat -> playerContent(modifier, onToggleChat) }
                    )
                } else {
                    PortraitPlayer(
                        channel = channel,
                        onToggleFullscreen = onToggleFullscreen,
                        webView = { modifier, onToggleChat -> playerContent(modifier, onToggleChat) }
                    )
                }
            }
        }
    }
}
