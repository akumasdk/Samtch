package com.akumasdk.samtch.ui.components.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.akumasdk.samtch.ui.components.chat.emote.BadgeInfoDialog
import com.akumasdk.samtch.ui.components.chat.emote.EmoteInfoDialog
import com.akumasdk.samtch.ui.components.chat.gif.GifInfoDialog
import com.akumasdk.samtch.ui.components.chat.user.UserInfoDialog

@Composable
fun ChatDialogs(viewModel: ChatViewModel, isFullscreen: Boolean = false) {
    val selectedEmoteForInfo by viewModel.selectedEmoteForInfo.collectAsState()
    val selectedBadgeForInfo by viewModel.selectedBadgeForInfo.collectAsState()
    val selectedUserForInfo by viewModel.selectedUserForInfo.collectAsState()
    val selectedGifForInfo by viewModel.selectedGifForInfo.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    selectedGifForInfo?.let { gifInfo ->
        GifInfoDialog(
            url = gifInfo.url,
            gifId = gifInfo.id,
            gifDescription = gifInfo.description,
            isFullscreen = isFullscreen,
            onDismiss = { viewModel.dismissGifInfo() }
        )
    }

    selectedEmoteForInfo?.let { emote ->
        EmoteInfoDialog(
            emote = emote,
            isLoggedIn = isLoggedIn,
            isFullscreen = isFullscreen,
            onDismiss = { viewModel.dismissEmoteInfo() },
            onUseEmote = { 
                viewModel.insertEmote(it)
            }
        )
    }

    selectedBadgeForInfo?.let { badge ->
        BadgeInfoDialog(
            badge = badge,
            isFullscreen = isFullscreen,
            onDismiss = { viewModel.dismissBadgeInfo() }
        )
    }

    selectedUserForInfo?.let { user ->
        UserInfoDialog(
            user = user,
            isFullscreen = isFullscreen,
            onDismiss = { viewModel.dismissUserInfo() }
        )
    }
}
