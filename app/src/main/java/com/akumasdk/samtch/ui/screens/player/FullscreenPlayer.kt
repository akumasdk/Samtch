package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.components.AdblockBanner
import com.akumasdk.samtch.ui.components.StreamMetadataBar
import com.akumasdk.samtch.ui.components.TwitchChat
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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


