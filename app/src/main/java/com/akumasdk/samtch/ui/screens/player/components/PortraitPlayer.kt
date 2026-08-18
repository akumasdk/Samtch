package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.components.metadata.StatusBanner
import com.akumasdk.samtch.ui.components.metadata.StreamInfoDialog
import com.akumasdk.samtch.ui.components.metadata.StreamMetadataBar
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchAnimation

@Composable
fun PortraitPlayer(
    channel: String,
    displayName: String? = null,
    avatarUrl: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    isAudioOnly: Boolean = false,
    adblockText: String = "",
    streamStartedAt: String? = null,
    previewImageUrl: String? = null,
    portraitMode: PortraitMode = PortraitMode.VIDEO_AND_CHAT,
    expandTrigger: Int = 0,
    forceSlimMetadata: Boolean = false,
    isImmersiveEnabled: Boolean = true,
    onToggleMode: () -> Unit = {},
    chatContent: @Composable (isCompact: Boolean, showInput: Boolean, PortraitMode, () -> Unit, Modifier) -> Unit,
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
            onDismiss = { showInfoDialog = false }
        )
    }

    val bgAlpha = if (isImmersiveEnabled) 0.3f else 0.2f
    val bgBlur = if (isImmersiveEnabled) 60.dp else 0.dp

    PlayerBackground(
        channel = channel,
        previewUrl = previewImageUrl,
        modifier = Modifier.fillMaxSize(),
        alpha = bgAlpha,
        blurRadius = bgBlur
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .animateContentSize(animationSpec = SamtchAnimation.springInteractive()),
            verticalArrangement = Arrangement.Top
        ) {
            // Dynamic height container (Placeholder for the stable WebView)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isAudioOnly) {
                            Modifier.height(240.dp)
                        } else if (portraitMode == PortraitMode.CHAT_ONLY) {
                            Modifier.height(0.dp)
                        } else {
                            Modifier.aspectRatio(16f / 9f)
                        }
                    )
                    .onSizeChanged { size ->
                        playerSize = size
                    }
            ) {
                webView(Modifier.fillMaxSize()) {
                    // Internal toggle ignored as per request to drop injected toggle
                }
            }

            // Space for Status Banner between player and metadata
            StatusBanner(text = adblockText)

            // Wrap metadata and chat in a Box to allow metadata bar to overlay chat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Twitch Chat
                Box(modifier = Modifier.fillMaxSize()) {
                    chatContent(
                        false,
                        true,
                        portraitMode,
                        onToggleMode,
                        Modifier.fillMaxSize()
                    )
                }

                // Metadata space above chat (Overlay)
                this@Column.AnimatedVisibility(
                    visible = !isAudioOnly && (!streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty()),
                    enter = SamtchAnimation.FadeIn,
                    exit = SamtchAnimation.FadeOut,
                    modifier = Modifier.align(Alignment.TopCenter)
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
                        forceExpanded = portraitMode == PortraitMode.CHAT_ONLY,
                        forceSlim = forceSlimMetadata,
                        isImmersiveEnabled = isImmersiveEnabled,
                        onClick = { showInfoDialog = true }
                    )
                }
            }
        }
    }
}
