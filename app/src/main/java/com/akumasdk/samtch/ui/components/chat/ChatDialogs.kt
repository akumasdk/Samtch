package com.akumasdk.samtch.ui.components.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.akumasdk.samtch.ui.components.chat.emote.BadgeInfoDialog
import com.akumasdk.samtch.ui.components.chat.emote.EmoteInfoDialog
import com.akumasdk.samtch.ui.components.chat.user.UserInfoDialog

@Composable
fun ChatDialogs(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val selectedEmoteForInfo by viewModel.selectedEmoteForInfo.collectAsState()
    val selectedBadgeForInfo by viewModel.selectedBadgeForInfo.collectAsState()
    val selectedUserForInfo by viewModel.selectedUserForInfo.collectAsState()

    selectedEmoteForInfo?.let { emote ->
        EmoteInfoDialog(
            emote = emote,
            onDismiss = { viewModel.dismissEmoteInfo() },
            onUseEmote = { 
                viewModel.insertEmote(it)
                viewModel.recordEmoteUsage(context, it)
            }
        )
    }

    selectedBadgeForInfo?.let { badge ->
        BadgeInfoDialog(
            badge = badge,
            onDismiss = { viewModel.dismissBadgeInfo() }
        )
    }

    selectedUserForInfo?.let { user ->
        UserInfoDialog(
            user = user,
            onDismiss = { viewModel.dismissUserInfo() }
        )
    }
}
