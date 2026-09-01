package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    chatRatio: Float = 0.28f,
    forceSlimMetadata: Boolean = false,
    isImmersiveEnabled: Boolean = true,
    onToggleChat: () -> Unit,
    onToggleMode: () -> Unit,
    onChatInteraction: () -> Unit = {},
    chatContent: @Composable (ChatContentConfig, Modifier) -> Unit
) {
    AnimatedVisibility(
        visible = !isMinimized && !isPip,
        enter = fadeIn(animationSpec = SamtchAnimation.EmphasizedTween),
        exit = fadeOut(animationSpec = SamtchAnimation.FastTween)
    ) {
        val previewUrl = streamMetadata?.user?.stream?.previewImageUrl
        
        AnimatedContent(
            targetState = isFullscreen && !isAudioOnly,
            transitionSpec = {
                val duration = 550
                (fadeIn(animationSpec = tween(duration, easing = SamtchAnimation.EmphasizedEasing)) + 
                 scaleIn(initialScale = 0.96f, animationSpec = tween(duration, easing = SamtchAnimation.EmphasizedEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + 
                                  scaleOut(targetScale = 0.96f, animationSpec = tween(300)))
            },
            label = "FullscreenContentTransition",
            modifier = Modifier.fillMaxSize()
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
                    previewImageUrl = previewUrl,
                    user = streamMetadata?.user,
                    isChatVisible = isChatVisible,
                    expandTrigger = metadataExpandTrigger,
                    forceSlimMetadata = forceSlimMetadata,
                    isImmersiveEnabled = isImmersiveEnabled,
                    chatRatio = chatRatio,
                    onToggleChat = onToggleChat,
                    chatContent = { config, modifier ->
                        chatContent(
                            config.copy(onInteraction = onChatInteraction),
                            modifier
                        )
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
                    previewImageUrl = previewUrl,
                    user = streamMetadata?.user,
                    portraitMode = portraitMode,
                    expandTrigger = metadataExpandTrigger,
                    forceSlimMetadata = forceSlimMetadata,
                    isImmersiveEnabled = isImmersiveEnabled,
                    refreshTrigger = refreshTrigger,
                    onToggleMode = onToggleMode,
                    chatContent = { config, modifier ->
                        chatContent(
                            config.copy(onInteraction = onChatInteraction),
                            modifier
                        )
                    },
                    webView = { modifier, _ ->
                        Box(modifier = modifier)
                    }
                )
            }
        }
    }
}
