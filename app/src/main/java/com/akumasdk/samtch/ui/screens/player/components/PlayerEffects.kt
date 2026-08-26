package com.akumasdk.samtch.ui.screens.player.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import com.akumasdk.samtch.data.api.PreviewImageService
import com.akumasdk.samtch.ui.screens.player.viewmodel.PlayerViewModel
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
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
        val controller = playerViewModel.mediaController
        if (shouldUseAudioService && controller != null) {
            // Only trigger resolution if the controller doesn't have an active media item
            // or if it's in IDLE state (freshly created).
            // We use force = false to allow reuse of existing playing items.
            val currentItem = controller.currentMediaItem
            if (currentItem == null || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                Log.d("AudioServiceEffects", "Triggering initial resolution for $channel")
                playerViewModel.updateMediaItem(channel, force = false)
            } else {
                Log.d("AudioServiceEffects", "Controller already playing ${currentItem.mediaId}. Skipping resolution.")
            }
        }
    }

    LaunchedEffect(playerViewModel.streamMetadata, playerViewModel.mediaController) {
        val controller = playerViewModel.mediaController ?: return@LaunchedEffect
        val stream = playerViewModel.streamMetadata?.user?.stream ?: return@LaunchedEffect
        
        val previewUri = PreviewImageService.getProcessedUrl(
            stream.previewImageUrl, 
            channel,
            PreviewImageService.NOTIFICATION_WIDTH,
            PreviewImageService.NOTIFICATION_HEIGHT
        ).toUri()

        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(stream.title)
            .setArtist(playerViewModel.streamMetadata?.user?.displayName ?: channel)
            .setAlbumTitle(stream.game?.name)
            .setArtworkUri(previewUri) // No fallback to avatar
            .build()
            
        // Update metadata of the current item without replacing it if possible, 
        // to avoid clearing the stream URL set by the orchestrator.
        val currentItem = controller.currentMediaItem
        if (currentItem != null && currentItem.mediaId == channel) {
            // Check if metadata is actually different to avoid redundant updates
            // We check artwork URI too now
            if (currentItem.mediaMetadata.title != stream.title || 
                currentItem.mediaMetadata.artworkUri != previewUri) {
                
                Log.d("AudioServiceEffects", "Updating metadata with preview: $previewUri")
                controller.replaceMediaItem(
                    controller.currentMediaItemIndex,
                    currentItem.buildUpon()
                        .setMediaMetadata(metadata)
                        .build()
                )
            }
        }
    }
}

@Composable
fun PlayerLifecycleEffects(
    channel: String,
    isPip: Boolean,
    refreshTrigger: Int,
    lifecycleState: Lifecycle.State,
    portraitMode: PortraitMode,
    chatViewModel: com.akumasdk.samtch.ui.components.chat.ChatViewModel,
    chatLoadingText: String,
    chatWelcomeTemplate: String,
    chatLoginTemplate: String,
    isUiLoading: Boolean,
    onLoadingTimeout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }

    // Manage chat connection lifecycle
    LaunchedEffect(channel, isPip, lifecycleState, portraitMode, refreshTrigger) {
        val isForeground = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        val shouldBeConnected = isForeground && (!isPip || portraitMode == PortraitMode.CHAT_ONLY)
        val isManualRefresh = refreshTrigger > lastRefreshTrigger
        lastRefreshTrigger = refreshTrigger

        if (shouldBeConnected) {
            chatViewModel.connect(context, channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate, forceRefresh = isManualRefresh)
        } else {
            chatViewModel.disconnect()
        }
    }

    // Safety timeout for loading screen
    LaunchedEffect(isUiLoading, channel) {
        if (isUiLoading) {
            delay(12.seconds)
            onLoadingTimeout()
        }
    }
}
