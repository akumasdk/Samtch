package com.akumasdk.samtch.ui.components.chat

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun NativeTwitchChat(
    channel: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    // Hide the catch-up button only when very close to the bottom
    val showJumpToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100
        }
    }

    // Disable auto-scroll when user drags up, re-enable when they return to absolute bottom
    LaunchedEffect(isDragged) {
        if (isDragged) {
            shouldAutoScroll = false
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !isDragged) {
            shouldAutoScroll = true
        }
    }

    LaunchedEffect(channel) {
        viewModel.connect(channel)
    }

    DisposableEffect(channel) {
        onDispose {
            viewModel.disconnect()
        }
    }

    // Auto-scroll when new messages arrive if enabled
    // Key on messages.size for most reliable trigger
    LaunchedEffect(messages.size, shouldAutoScroll) {
        if (messages.isNotEmpty() && shouldAutoScroll) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF18181B))) {
        Box(modifier = Modifier.weight(1f)) {
            val reversedMessages = remember(messages) { messages.asReversed() }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                reverseLayout = true
            ) {
                itemsIndexed(
                    items = reversedMessages,
                    key = { _, it -> it.id },
                    contentType = { _, it -> it.contentType }
                ) { _, msg ->
                    key(msg.id, isCompact) {
                        ChatMessageRow(message = msg, isCompact = isCompact)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showJumpToBottom,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        shouldAutoScroll = true
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9146FF),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Jump to bottom",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1F1F23),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Send a message", style = MaterialTheme.typography.bodyMedium) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFBF94FF)
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            scope.launch {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }
                    })
                )
                
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            scope.launch {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color(0xFFBF94FF) else Color.Gray
                    )
                }
            }
        }
    }
}
