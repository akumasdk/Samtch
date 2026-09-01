package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.data.model.TwitchUser
import com.akumasdk.samtch.ui.components.metadata.StatusBanner
import com.akumasdk.samtch.ui.components.metadata.StreamMetadataBar
import com.akumasdk.samtch.ui.components.metadata.StreamInfoDialog
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.screens.player.models.ChatContentConfig
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun FullscreenPlayer(
    channel: String,
    displayName: String? = null,
    avatarUrl: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    adblockText: String = "",
    streamStartedAt: String? = null,
    previewImageUrl: String? = null,
    user: TwitchUser? = null,
    isChatVisible: Boolean = false,
    expandTrigger: Int = 0,
    refreshTrigger: Int = 0,
    forceSlimMetadata: Boolean = false,
    isImmersiveEnabled: Boolean = true,
    chatRatio: Float = 0.28f,
    onToggleChat: () -> Unit = {},
    chatContent: @Composable (ChatContentConfig, Modifier) -> Unit,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        StreamInfoDialog(
            channel = channel,
            displayName = displayName,
            avatarUrl = avatarUrl,
            streamTitle = streamTitle,
            gameName = gameName,
            viewersCount = viewersCount,
            streamStartedAt = streamStartedAt,
            previewImageUrl = previewImageUrl,
            user = user,
            onDismiss = { showInfoDialog = false }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Video Player
        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { size ->
                    playerSize = size
                }
        ) {
            webView(Modifier.fillMaxSize(), onToggleChat)
        }

        AnimatedVisibility(
            visible = isChatVisible,
            enter = slideInHorizontally(animationSpec = SamtchAnimation.layoutSpring()) { it } + 
                    fadeIn(animationSpec = tween(400, easing = SamtchAnimation.EmphasizedEasing)),
            exit = slideOutHorizontally(animationSpec = SamtchAnimation.layoutSpring()) { it } + 
                   fadeOut(animationSpec = tween(300))
        ) {
            val isActuallyDark = SamtchTheme.colors.dialogBackground.luminance() < 0.5f
            val bgAlpha = if (isImmersiveEnabled && isActuallyDark) 0.35f else 0f
            val bgBlur = if (isImmersiveEnabled && isActuallyDark) 60.dp else 0.dp
            val surfaceAlpha = if (isImmersiveEnabled && isActuallyDark) 0.4f else 1.0f

            PlayerBackground(
                channel = channel,
                previewUrl = previewImageUrl,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(chatRatio),
                alpha = if (isActuallyDark) bgAlpha else 0f,
                blurRadius = bgBlur,
                contentScale = ContentScale.FillBounds
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SamtchTheme.colors.chatBackground.copy(alpha = surfaceAlpha))
                        .systemBarsPadding()
                        .displayCutoutPadding()
                ) {
                    // 1. Chat area (Background layer)
                    Box(modifier = Modifier.fillMaxSize()) {
                        chatContent(
                            ChatContentConfig(
                                isCompact = true,
                                showInput = true,
                                refreshTrigger = refreshTrigger,
                                isFullscreen = true
                            ),
                            Modifier.fillMaxSize()
                        )
                    }

                    // 2. Overlays (Banner + Metadata)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StatusBanner(
                            text = adblockText,
                            isImmersiveEnabled = isImmersiveEnabled,
                            channel = channel,
                            previewImageUrl = previewImageUrl
                        )

                        this@Row.AnimatedVisibility(
                            visible = !streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty(),
                            enter = SamtchAnimation.FadeIn,
                            exit = SamtchAnimation.FadeOut
                        ) {
                            StreamMetadataBar(
                                channel = channel,
                                displayName = displayName,
                                avatarUrl = avatarUrl,
                                streamTitle = streamTitle,
                                gameName = gameName,
                                viewersCount = viewersCount,
                                streamStartedAt = streamStartedAt,
                                previewImageUrl = previewImageUrl,
                                expandTrigger = expandTrigger,
                                forceSlim = forceSlimMetadata,
                                isImmersiveEnabled = isImmersiveEnabled,
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.padding(horizontal = 4.dp) // Subtle extra padding for side panel
                            )
                        }
                    }
                }
            }
        }
    }
}
