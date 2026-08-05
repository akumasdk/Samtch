package com.akumasdk.samtch.ui.screens.player

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun AudioServiceEffects(
    channel: String,
    shouldUseAudioService: Boolean,
    isAudioOnlyBackgroundEnabled: Boolean,
    playerViewModel: PlayerViewModel,
    context: android.content.Context
) {
    val isPlaying = playerViewModel.isPlaying
    
    LaunchedEffect(shouldUseAudioService) {
        if (!shouldUseAudioService) {
            playerViewModel.disconnectMediaController()
            if (!isAudioOnlyBackgroundEnabled) {
                context.stopService(android.content.Intent(context, com.akumasdk.samtch.service.PlaybackService::class.java))
            }
            return@LaunchedEffect
        }
        playerViewModel.connectMediaController(context)
    }

    LaunchedEffect(playerViewModel.mediaController, shouldUseAudioService) {
        if (shouldUseAudioService && playerViewModel.mediaController != null && !isPlaying) {
            playerViewModel.updateMediaItem(channel)
        }
    }

    LaunchedEffect(playerViewModel.streamMetadata, playerViewModel.mediaController) {
        val controller = playerViewModel.mediaController ?: return@LaunchedEffect
        val stream = playerViewModel.streamMetadata?.user?.stream ?: return@LaunchedEffect
        
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(stream.title)
            .setArtist(playerViewModel.streamMetadata?.user?.displayName ?: channel)
            .setAlbumTitle(stream.game?.name)
            .setArtworkUri(stream.previewImageUrl?.let { android.net.Uri.parse(it) })
            .build()
            
        controller.replaceMediaItem(
            0,
            androidx.media3.common.MediaItem.Builder()
                .setMediaId(channel)
                .setMediaMetadata(metadata)
                .build()
        )
    }
}

@Composable
fun PlayerLifecycleEffects(
    channel: String,
    isPip: Boolean,
    lifecycleState: Lifecycle.State,
    portraitMode: PortraitMode,
    chatViewModel: com.akumasdk.samtch.ui.components.chat.ChatViewModel,
    chatLoadingText: String,
    chatWelcomeTemplate: String,
    chatLoginTemplate: String,
    isUiLoading: Boolean,
    onLoadingTimeout: () -> Unit
) {
    // Manage chat connection lifecycle
    LaunchedEffect(channel, isPip, lifecycleState, portraitMode) {
        val isForeground = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        val shouldBeConnected = isForeground && (!isPip || portraitMode == PortraitMode.CHAT_ONLY)
        
        if (shouldBeConnected) {
            chatViewModel.connect(channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate)
        } else {
            chatViewModel.disconnect()
        }
    }

    // Safety timeout for loading screen
    LaunchedEffect(isUiLoading, channel) {
        if (isUiLoading) {
            delay(12.seconds)
            if (isUiLoading) {
                onLoadingTimeout()
            }
        }
    }
}
