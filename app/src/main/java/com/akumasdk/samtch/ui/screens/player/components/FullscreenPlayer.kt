package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding

@OptIn(ExperimentalLayoutApi::class)
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
    isFoldableInnerScreen: Boolean = false,
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        if (isFoldableInnerScreen) {
            val videoHeight = if (isChatVisible) {
                if (forceSlimMetadata) {
                    (screenWidth * 9f / 16f).coerceAtMost(screenHeight * 0.3f)
                } else {
                    (screenWidth * 9f / 16f).coerceAtMost(screenHeight * 0.65f)
                }
            } else {
                screenHeight
            }

            // Foldable inner screen / square layout: Video on TOP, Chat on BOTTOM
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Video Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(videoHeight)
                        .onSizeChanged { size -> playerSize = size }
                ) {
                    webView(Modifier.fillMaxSize(), onToggleChat)
                }

                // Bottom Chat
                AnimatedVisibility(
                    visible = isChatVisible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight - videoHeight),
                    enter = slideInVertically(animationSpec = SamtchAnimation.layoutSpring()) { it } + 
                            fadeIn(animationSpec = tween(400, easing = SamtchAnimation.EmphasizedEasing)),
                    exit = slideOutVertically(animationSpec = SamtchAnimation.layoutSpring()) { it } + 
                           fadeOut(animationSpec = tween(300))
                ) {
                val isActuallyDark = SamtchTheme.colors.dialogBackground.luminance() < 0.5f
                val surfaceAlpha = if (isImmersiveEnabled && isActuallyDark) 0.65f else 1.0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SamtchTheme.colors.chatBackground.copy(alpha = surfaceAlpha))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Chat area
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

                            this@Column.AnimatedVisibility(
                                visible = displayName == null && streamTitle == null && gameName == null || !streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty(),
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
                                    loading = displayName == null && streamTitle == null && gameName == null,
                                    isImmersiveEnabled = isImmersiveEnabled,
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Standard widescreen landscape layout: Video on LEFT, Chat on RIGHT
        Row(modifier = Modifier.fillMaxSize()) {
            // Video Player
            Box(
                modifier = Modifier
                    .weight(if (isChatVisible) (1f - chatRatio).coerceAtLeast(0.01f) else 1f)
                    .onSizeChanged { size ->
                        playerSize = size
                    }
            ) {
                webView(Modifier.fillMaxSize(), onToggleChat)
            }

            AnimatedVisibility(
                visible = isChatVisible,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(chatRatio),
                enter = slideInHorizontally(animationSpec = SamtchAnimation.layoutSpring()) { it } + 
                        fadeIn(animationSpec = tween(400, easing = SamtchAnimation.EmphasizedEasing)),
                exit = slideOutHorizontally(animationSpec = SamtchAnimation.layoutSpring()) { it } + 
                       fadeOut(animationSpec = tween(300))
            ) {
                val isActuallyDark = SamtchTheme.colors.dialogBackground.luminance() < 0.5f
                val surfaceAlpha = if (isImmersiveEnabled && isActuallyDark) 0.65f else 1.0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SamtchTheme.colors.chatBackground.copy(alpha = surfaceAlpha))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                    ) {
                        // 1. Chat area
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
                                visible = displayName == null && streamTitle == null && gameName == null || !streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty(),
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
                                    loading = displayName == null && streamTitle == null && gameName == null,
                                    isImmersiveEnabled = isImmersiveEnabled,
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
