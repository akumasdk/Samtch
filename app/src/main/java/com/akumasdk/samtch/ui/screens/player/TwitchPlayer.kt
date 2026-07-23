package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.google.common.util.concurrent.MoreExecutors
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.akumasdk.samtch.service.PlaybackService
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.service.TwitchGqlService
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.ui.components.MiniPlayer
import com.akumasdk.samtch.ui.components.WebViewContainer
import com.akumasdk.samtch.ui.components.createTwitchPlayerUrl
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

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
    onRefreshRequested: () -> Unit = {},
    onMetadataUpdated: (String?, String?) -> Unit = { _, _ -> },
    onAudioOnlyModeChanged: (Boolean) -> Unit = {},
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {}
) {
    val context = LocalContext.current
    var isAudioOnly by remember { mutableStateOf(false) }
    var isUiLoading by remember { mutableStateOf(true) }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var streamSubtitle by remember { mutableStateOf<String?>(null) }
    var streamMetadata by remember { mutableStateOf<TwitchStreamMetadata?>(null) }

    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentAvatarUrl by rememberUpdatedState(avatarUrl)

    val isAudioOnlyBackgroundEnabled by SettingsManager.isAudioOnlyBackgroundEnabled(context).collectAsState(initial = false)

    LaunchedEffect(channel, refreshTrigger) {
        isUiLoading = true
        while (true) {
            // Fetch detailed metadata via GraphQL
            android.util.Log.d("TwitchPlayer", "Fetching periodic metadata for $channel")
            val metadata = TwitchGqlService.getStreamMetadata(channel)
            streamMetadata = metadata
            
            // Update UI-facing metadata
            metadata?.user?.let { user ->
                avatarUrl = user.profileImageUrl
                streamSubtitle = user.stream?.title
                onMetadataUpdated(user.profileImageUrl, user.stream?.title)
            }
            
            delay(1.minutes)
        }
    }

    LaunchedEffect(isAudioOnly) {
        onAudioOnlyModeChanged(isAudioOnly)
    }

    // Connect to service only when Audio Only mode is manually enabled
    LaunchedEffect(isAudioOnly) {
        if (!isAudioOnly) {
            mediaController?.release()
            mediaController = null
            // If the background setting is also off, ensure the service is killed
            if (!isAudioOnlyBackgroundEnabled) {
                context.stopService(android.content.Intent(context, PlaybackService::class.java))
            }
            return@LaunchedEffect
        }

        if (mediaController == null) {
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
    }

    // Handle initial playback when switching to Audio Only mode
    LaunchedEffect(mediaController, isAudioOnly) {
        if (isAudioOnly && mediaController != null && !isPlaying) {
            val previewUri = streamMetadata?.user?.stream?.previewImageUrl?.toUri()
            val metadata = MediaMetadata.Builder()
                .setTitle(streamMetadata?.user?.stream?.title ?: channel)
                .setArtist(streamMetadata?.user?.displayName ?: channel)
                .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
                .setArtworkUri(previewUri)
                .build()
            
            mediaController?.setMediaItem(
                MediaItem.Builder()
                    .setMediaId(channel)
                    .setMediaMetadata(metadata)
                    .build()
            )
            mediaController?.prepare()
            mediaController?.play()
        }
    }

    // Periodically update controller metadata if it's connected
    LaunchedEffect(streamMetadata, mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        val stream = streamMetadata?.user?.stream ?: return@LaunchedEffect
        
        android.util.Log.d("TwitchPlayer", "Updating controller metadata for $channel")
        val metadata = MediaMetadata.Builder()
            .setTitle(stream.title)
            .setArtist(streamMetadata?.user?.displayName ?: channel)
            .setAlbumTitle(stream.game?.name)
            .setArtworkUri(stream.previewImageUrl?.toUri())
            .build()
            
        // Use replaceMediaItem to update metadata without disrupting the stream
        controller.replaceMediaItem(
            0,
            MediaItem.Builder()
                .setMediaId(channel)
                .setMediaMetadata(metadata)
                .build()
        )
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
            mediaController?.release()
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
                },
                onUiCleanFinish = {
                    isUiLoading = false
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .then(if (isMinimized) Modifier.wrapContentHeight() else Modifier.fillMaxSize())
            .animateContentSize()
    ) {
        val playerContent = remember(channel, isAudioOnly) {
            movableContentOf { modifier: Modifier, onToggleChat: () -> Unit ->
                if (isAudioOnly) {
                    AudioOnlyPlayer(
                        channel = channel,
                        avatarUrl = avatarUrl,
                        subtitle = streamSubtitle,
                        displayName = streamMetadata?.user?.displayName,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        isPlaying = isPlaying,
                        onTogglePlayback = {
                            if (currentIsPlaying) mediaController?.pause() else mediaController?.play()
                        },
                        onCloseAudioOnly = {
                            isAudioOnly = false
                            mediaController?.stop()
                            // Trigger the global refresh (adds &refresh=N to URL)
                            onRefreshRequested()
                        },
                        onRefresh = {
                            mediaController?.stop()
                            val avatarUri = currentAvatarUrl?.let { it.toUri() }
                            mediaController?.setMediaItem(
                                MediaItem.Builder()
                                    .setMediaId(channel)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(streamMetadata?.user?.stream?.title ?: channel)
                                            .setArtist(streamMetadata?.user?.displayName ?: channel)
                                            .setAlbumTitle(streamMetadata?.user?.stream?.game?.name)
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
                    Box(modifier = modifier.background(Color.Black)) {
                        webView(Modifier.fillMaxSize(), onToggleChat)
                        
                        // Loading Overlay constrained to video player area
                        AnimatedVisibility(
                            visible = isUiLoading,
                            enter = fadeIn(),
                            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                            modifier = Modifier.matchParentSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF9146FF), // Twitch Purple
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isMinimized) {
            Box(
                modifier = Modifier.fillMaxWidth().height(92.dp), // Height adjusted for 80dp pill + shadows
                contentAlignment = Alignment.Center
            ) {
                MiniPlayer(
                    channel = channel,
                    displayName = streamMetadata?.user?.displayName,
                    streamTitle = streamMetadata?.user?.stream?.title,
                    playerContent = { modifier -> playerContent(modifier) {} },
                    onClick = onExpand,
                    onClose = {
                        mediaController?.stop()
                        onClose()
                    }
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
                        displayName = streamMetadata?.user?.displayName,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        webView = { modifier, onToggleChat -> playerContent(modifier, onToggleChat) }
                    )
                } else {
                    PortraitPlayer(
                        channel = channel,
                        displayName = streamMetadata?.user?.displayName,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        isAudioOnly = isAudioOnly,
                        onToggleFullscreen = onToggleFullscreen,
                        webView = { modifier, onToggleChat -> playerContent(modifier, onToggleChat) }
                    )
                }
            }
        }
    }
}
