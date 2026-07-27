package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.service.TwitchGqlService
import com.akumasdk.samtch.ui.components.MiniPlayer
import com.akumasdk.samtch.ui.components.NativePlayer
import com.akumasdk.samtch.data.settings.SettingsManager
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
    @Suppress("UNUSED_PARAMETER") onRefreshRequested: () -> Unit = {},
    onMetadataUpdated: (String?, String?) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onAudioOnlyModeChanged: (Boolean) -> Unit = {},
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit = {}
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    var isUiLoading by remember { mutableStateOf(true) }
    var streamMetadata by remember { mutableStateOf<TwitchStreamMetadata?>(null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var streamSubtitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(channel, refreshTrigger) {
        isUiLoading = true
        while (true) {
            val metadata = TwitchGqlService.getStreamMetadata(channel)
            streamMetadata = metadata
            metadata?.user?.let { user ->
                avatarUrl = user.profileImageUrl
                streamSubtitle = user.stream?.title
                onMetadataUpdated(user.profileImageUrl, user.stream?.title)
            }
            delay(1.minutes)
        }
    }

    if (!isPip && !isMinimized) {
        androidx.activity.compose.BackHandler {
            onBack?.invoke()
        }
    }

    val nativePlayer = remember(channel) {
        movableContentOf { modifier: Modifier ->
            NativePlayer(
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
                channel = channel,
                onPlaybackStarted = {
                    isUiLoading = false
                }
            )
        }
    }

    val playerContent = remember(channel) {
        movableContentOf { modifier: Modifier, _: () -> Unit ->
            Box(modifier = modifier.background(Color.Black)) {
                nativePlayer(Modifier.fillMaxSize())
            }
        }
    }

    Box(
        modifier = Modifier
            .then(if (isMinimized) Modifier.wrapContentHeight() else Modifier.fillMaxSize())
            .animateContentSize()
    ) {
        if (isMinimized) {
            Box(
                modifier = Modifier.fillMaxWidth().height(92.dp),
                contentAlignment = Alignment.Center
            ) {
                MiniPlayer(
                    channel = channel,
                    displayName = streamMetadata?.user?.displayName,
                    streamTitle = streamMetadata?.user?.stream?.title,
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
                    playerContent(Modifier.fillMaxSize()) {}
                } else if (isFullscreen) {
                    FullscreenPlayer(
                        channel = channel,
                        displayName = streamMetadata?.user?.displayName,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        playerContent = { modifier, onToggleChat -> playerContent(modifier, onToggleChat) }
                    )
                } else {
                    PortraitPlayer(
                        channel = channel,
                        displayName = streamMetadata?.user?.displayName,
                        streamTitle = streamMetadata?.user?.stream?.title,
                        gameName = streamMetadata?.user?.stream?.game?.name,
                        viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                        isAudioOnly = false, // TODO: Re-add audio only support if needed
                        onToggleFullscreen = onToggleFullscreen,
                        playerContent = { modifier, onToggleChat -> playerContent(modifier, onToggleChat) }
                    )
                }
            }
        }
    }
}
