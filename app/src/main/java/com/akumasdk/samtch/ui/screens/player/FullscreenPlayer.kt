package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.components.AdblockBanner
import com.akumasdk.samtch.ui.components.StreamMetadataBar
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
    isChatVisible: Boolean = false,
    expandTrigger: Int = 0,
    refreshTrigger: Int = 0,
    isPip: Boolean = false,
    onToggleChat: () -> Unit = {},
    chatContent: @Composable (isCompact: Boolean, showInput: Boolean, refreshTrigger: Int, Modifier) -> Unit,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

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

        // Optional Side Chat with Metadata Bar
        AnimatedVisibility(
            visible = isChatVisible,
            enter = slideInHorizontally(animationSpec = SamtchAnimation.springInteractive()) { it } + fadeIn(),
            exit = slideOutHorizontally(animationSpec = SamtchAnimation.springInteractive()) { it } + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(SamtchTheme.colors.chatBackground)
                    .systemBarsPadding()
                    .displayCutoutPadding()
            ) {
                // Adblock Banner in the same space as portrait (between video and metadata/chat)
                AdblockBanner(text = adblockText)

                // Metadata space above chat (Only visible when chat is open)
                AnimatedVisibility(
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
                        isPip = isPip,
                        modifier = Modifier.padding(horizontal = 4.dp) // Subtle extra padding for side panel
                    )
                }

                chatContent(
                    true,
                    true,
                    refreshTrigger,
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}


