package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onToggleChat: () -> Unit = {},
    chatContent: @Composable (isCompact: Boolean, showInput: Boolean, Modifier) -> Unit,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var showTooltip by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000.milliseconds)
        showTooltip = false
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

            // Adblock status banner at the top
            AdblockBanner(
                text = adblockText,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Double tap hint tooltip
            TapTooltip(
                visible = showTooltip,
                modifier = Modifier.align(Alignment.Center)
            )
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
                        modifier = Modifier.padding(horizontal = 4.dp) // Subtle extra padding for side panel
                    )
                }

                chatContent(
                    true,
                    true,
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TapTooltip(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.fullscreen_double_tap_hint),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
