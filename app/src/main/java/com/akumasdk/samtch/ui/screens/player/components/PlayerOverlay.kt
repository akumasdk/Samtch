package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.akumasdk.samtch.ui.screens.player.models.ChatContentConfig
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchAnimation

@Composable
fun PlayerOverlay(
    isMinimized: Boolean,
    isFullscreen: Boolean,
    channel: String,
    streamMetadata: com.akumasdk.samtch.data.model.TwitchStreamMetadata?,
    avatarUrl: String?,
    isAudioOnly: Boolean,
    adblockText: String,
    portraitMode: PortraitMode,
    metadataExpandTrigger: Int,
    isPip: Boolean,
    isChatVisible: Boolean,
    refreshTrigger: Int,
    forceSlimMetadata: Boolean = false,
    onToggleChat: () -> Unit,
    onToggleMode: () -> Unit,
    chatContent: @Composable (ChatContentConfig, PortraitMode?, (() -> Unit)?, Modifier) -> Unit
) {
    AnimatedVisibility(
        visible = !isMinimized,
        enter = fadeIn(animationSpec = SamtchAnimation.EmphasizedTween),
        exit = fadeOut(animationSpec = SamtchAnimation.FastTween)
    ) {
        if (isFullscreen && !isAudioOnly) {
            FullscreenPlayer(
                channel = channel,
                displayName = streamMetadata?.user?.displayName,
                avatarUrl = avatarUrl,
                streamTitle = streamMetadata?.user?.stream?.title,
                gameName = streamMetadata?.user?.stream?.game?.name,
                viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                adblockText = adblockText,
                refreshTrigger = refreshTrigger,
                streamStartedAt = streamMetadata?.user?.stream?.createdAt,
                previewImageUrl = streamMetadata?.user?.stream?.previewImageUrl,
                isChatVisible = isChatVisible,
                expandTrigger = metadataExpandTrigger,
                isPip = isPip,
                forceSlimMetadata = forceSlimMetadata,
                onToggleChat = onToggleChat,
                chatContent = { isCompact, showInput, rTrigger, modifier ->
                    chatContent(ChatContentConfig(isCompact, showInput, rTrigger), null, null, modifier)
                },
                webView = { modifier, _ ->
                    Box(modifier = modifier)
                }
            )
        } else {
            PortraitPlayer(
                channel = channel,
                displayName = streamMetadata?.user?.displayName,
                avatarUrl = avatarUrl,
                streamTitle = streamMetadata?.user?.stream?.title,
                gameName = streamMetadata?.user?.stream?.game?.name,
                viewersCount = streamMetadata?.user?.stream?.viewersCount ?: 0,
                isAudioOnly = isAudioOnly,
                adblockText = adblockText,
                streamStartedAt = streamMetadata?.user?.stream?.createdAt,
                previewImageUrl = streamMetadata?.user?.stream?.previewImageUrl,
                portraitMode = portraitMode,
                expandTrigger = metadataExpandTrigger,
                isPip = isPip,
                forceSlimMetadata = forceSlimMetadata,
                onToggleMode = onToggleMode,
                chatContent = { isCompact, showInput, pMode, onToggle, modifier ->
                    chatContent(ChatContentConfig(isCompact, showInput, refreshTrigger), pMode, onToggle, modifier)
                },
                webView = { modifier, _ ->
                    Box(modifier = modifier)
                }
            )
        }
    }
}
