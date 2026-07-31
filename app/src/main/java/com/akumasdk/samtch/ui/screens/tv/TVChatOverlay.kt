package com.akumasdk.samtch.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.akumasdk.samtch.ui.components.chat.ChatMessageRow
import com.akumasdk.samtch.ui.components.chat.ChatViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVChatOverlay(
    channel: String,
    viewModel: ChatViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    
    // Connect to chat
    LaunchedEffect(channel) {
        viewModel.connect(channel, "Connecting to chat...", "Welcome to %s's chat!", "Logged in as %s")
    }

    // Auto-scroll logic (simplified for TV)
    // We use messages directly as a key because size becomes constant after 500 messages
    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex <= 5) {
            listState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF18181B))) {
        val reversedMessages = remember(messages) { messages.asReversed() }
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            itemsIndexed(
                items = reversedMessages,
                key = { _, it -> it.id }
            ) { _, msg ->
                // Wrapping ChatMessageRow with some TV specific padding/text size if needed
                // For now, let's use it as is, it's already quite readable
                ChatMessageRow(message = msg, isCompact = false)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
