package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.akumasdk.samtch.ui.screens.player.models.ChatContentConfig
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchAnimation

@OptIn(ExperimentalAnimationApi::class)
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
    isImmersiveEnabled: Boolean = true,
    onToggleChat: () -> Unit,
    onToggleMode: () -> Unit,
    chatContent: @Composable (ChatContentConfig, PortraitMode?, (() -> Unit)?, Modifier) -> Unit
) {
    AnimatedVisibility(
        visible = !isMinimized && !isPip,
        enter = fadeIn(animationSpec = SamtchAnimation.EmphasizedTween),
        exit = fadeOut(animationSpec = SamtchAnimation.FastTween)
    ) {
        AnimatedContent(
            targetState = isFullscreen && !isAudioOnly,
            transitionSpec = {
                if (targetState) {
                    // Entering Fullscreen: Scale up + Slide from below + Fade
                    (fadeIn(animationSpec = SamtchAnimation.EmphasizedTween) + 
                     scaleIn(initialScale = 0.92f, animationSpec = SamtchAnimation.EmphasizedTween) +
                     slideInVertically(animationSpec = androidx.compose.animation.core.tween(SamtchAnimation.SlowDuration, easing = SamtchAnimation.EmphasizedEasing)) { it / 10 }) togetherWith
                            fadeOut(animationSpec = SamtchAnimation.FastTween)
                } else {
                    // Returning to Portrait: Fade in + Scale down old + Slide down old
                    fadeIn(animationSpec = SamtchAnimation.StandardTween) togetherWith
                            (fadeOut(animationSpec = SamtchAnimation.FastTween) +
                             scaleOut(targetScale = 0.92f, animationSpec = SamtchAnimation.FastTween) +
                             slideOutVertically(animationSpec = androidx.compose.animation.core.tween(SamtchAnimation.FastDuration, easing = SamtchAnimation.StandardEasing)) { it / 10 })
                }
            },
            label = "PlayerOverlayMode"
        ) { isFull ->
            if (isFull) {
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
                    user = streamMetadata?.user,
                    isChatVisible = isChatVisible,
                    expandTrigger = metadataExpandTrigger,
                    forceSlimMetadata = forceSlimMetadata,
                    isImmersiveEnabled = isImmersiveEnabled,
                    onToggleChat = onToggleChat,
                    chatContent = { isCompact, showInput, rTrigger, modifier ->
                        chatContent(ChatContentConfig(isCompact, showInput, rTrigger, isFullscreen = true), null, null, modifier)
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
                    user = streamMetadata?.user,
                    portraitMode = portraitMode,
                    expandTrigger = metadataExpandTrigger,
                    forceSlimMetadata = forceSlimMetadata,
                    isImmersiveEnabled = isImmersiveEnabled,
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
}
