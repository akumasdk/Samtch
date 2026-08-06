package com.akumasdk.samtch.ui.components.chat

import android.annotation.SuppressLint
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlinx.coroutines.launch

@SuppressLint("FrequentlyChangingValue")
@Composable
fun NativeTwitchChat(
    channel: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    viewModel: ChatViewModel = viewModel(),
    onEmoteLongClick: ((EmoteInfo) -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }
    var lastJumpTime by remember { mutableLongStateOf(0L) }

    // More lenient at-bottom detection to handle minor layout shifts from images
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 15
        }
    }

    var lastIndex by remember { mutableIntStateOf(0) }
    var lastOffset by remember { mutableIntStateOf(0) }

    // Precise scroll tracking to distinguish user intent from system layout shifts
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val isLocked = (System.currentTimeMillis() - lastJumpTime) < 500
        
        if (listState.isScrollInProgress && !isLocked) {
            val isScrollingUp = listState.firstVisibleItemIndex > lastIndex || 
                                (listState.firstVisibleItemIndex == lastIndex && listState.firstVisibleItemScrollOffset > lastOffset)
            
            // Only pause auto-scroll if the user deliberately scrolls AWAY from the bottom (up)
            if (isScrollingUp && shouldAutoScroll) {
                shouldAutoScroll = false
            }
        }
        
        // Auto-resume sticky scroll if the user manually returns to the bottom and stops scrolling
        if (isAtBottom && !listState.isScrollInProgress && !shouldAutoScroll) {
            shouldAutoScroll = true
        }
        
        if (isLocked) {
            shouldAutoScroll = true
        }
        
        lastIndex = listState.firstVisibleItemIndex
        lastOffset = listState.firstVisibleItemScrollOffset
    }

    // Process auto-scroll updates
    LaunchedEffect(messages.size, shouldAutoScroll) {
        if (messages.isNotEmpty() && shouldAutoScroll) {
            listState.scrollToItem(0)
        }
    }

    Box(modifier = modifier.background(SamtchTheme.colors.chatBackground)) {
        val reversedMessages = remember(messages) { messages.asReversed() }
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 0.dp),
            reverseLayout = true
        ) {
            // Floor spacer to provide consistent gap from input box
            item(key = "floor_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(
                items = reversedMessages,
                key = { _, it -> it.id },
                contentType = { _, it -> it.contentType }
            ) { _, msg ->
                key(msg.id, isCompact) {
                    ChatMessageRow(
                        message = msg,
                        isCompact = isCompact,
                        onEmoteLongClick = onEmoteLongClick
                    )
                }
            }
        }

        // Jump to bottom button only shown when auto-scroll is manually paused
        androidx.compose.animation.AnimatedVisibility(
            visible = !shouldAutoScroll && !isAtBottom,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Button(
                onClick = {
                    lastJumpTime = System.currentTimeMillis()
                    shouldAutoScroll = true
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamtchTheme.colors.twitchPurple,
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
                        text = stringResource(R.string.chat_jump_to_bottom),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
