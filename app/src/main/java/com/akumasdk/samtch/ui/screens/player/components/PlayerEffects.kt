package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerLifecycleEffects(
    channel: String,
    isPip: Boolean,
    refreshTrigger: Int,
    lifecycleState: Lifecycle.State,
    portraitMode: PortraitMode,
    chatViewModel: ChatViewModel,
    chatLoadingText: String,
    chatWelcomeTemplate: String,
    chatLoginTemplate: String,
    isUiLoading: Boolean,
    onLoadingTimeout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }
    LaunchedEffect(channel, isPip, lifecycleState, portraitMode, refreshTrigger) {
        val visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED) || isPip
        val connected = visible && (!isPip || portraitMode == PortraitMode.CHAT_ONLY)
        val manualRefresh = refreshTrigger > lastRefreshTrigger
        lastRefreshTrigger = refreshTrigger
        if (connected) chatViewModel.connect(context, channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate, forceRefresh = manualRefresh)
        else chatViewModel.disconnect()
    }
    LaunchedEffect(isUiLoading, channel) {
        if (isUiLoading) {
            delay(12.seconds)
            onLoadingTimeout()
        }
    }
}
