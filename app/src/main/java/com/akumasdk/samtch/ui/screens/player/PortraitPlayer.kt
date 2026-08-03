package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.SmartDisplay
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
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.components.AdblockBanner
import com.akumasdk.samtch.ui.components.PlayerBackground
import com.akumasdk.samtch.ui.components.StreamMetadataBar
import com.akumasdk.samtch.ui.components.TwitchChat
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import kotlin.math.abs

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
    onToggleMode: () -> Unit = {},
    chatContent: @Composable (isCompact: Boolean, showInput: Boolean, Modifier) -> Unit,
    webView: @Composable (Modifier, () -> Unit) -> Unit
) {
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

    PlayerBackground(
        channel = channel,
        previewUrl = previewImageUrl,
        modifier = Modifier.fillMaxSize(),
        alpha = 0.2f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            // Tiny metadata space above chat
            AnimatedVisibility(
                visible = !isAudioOnly && (!streamTitle.isNullOrEmpty() || !gameName.isNullOrEmpty()),
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
                    forceExpanded = portraitMode == PortraitMode.CHAT_ONLY
                )
            }

            // Twitch Chat & Mode Toggle Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                chatContent(
                    false,
                    true,
                    Modifier.fillMaxSize()
                )

                // Floating Toggle Mode Circle
                if (!isAudioOnly) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 100.dp, end = 16.dp) // Elevated further to avoid overlapping with message input
                            .navigationBarsPadding()
                    ) {
                        Surface(
                            onClick = onToggleMode,
                            shape = CircleShape,
                            color = SamtchTheme.colors.twitchPurpleLight.copy(alpha = 0.8f),
                            contentColor = Color.White,
                            tonalElevation = 6.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (portraitMode) {
                                        PortraitMode.VIDEO_AND_CHAT -> Icons.Default.SmartDisplay
                                        PortraitMode.CHAT_ONLY -> Icons.AutoMirrored.Filled.Chat
                                    },
                                    contentDescription = "Toggle Mode",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
