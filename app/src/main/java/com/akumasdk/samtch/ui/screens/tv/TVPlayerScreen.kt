package com.akumasdk.samtch.ui.screens.tv

import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.akumasdk.samtch.MainActivity
import com.akumasdk.samtch.ui.components.KWebView
import com.akumasdk.samtch.ui.components.chat.ChatViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVPlayerScreen(
    channel: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Completely disable soft input activity-wide while on the player screen
    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.setSoftInputDisabled(true)
        onDispose {
            activity?.setSoftInputDisabled(false)
        }
    }

    var isUiLoading by remember { mutableStateOf(true) }
    var showOverlay by remember { mutableStateOf(true) }
    var overlayVisibleTick by remember { mutableLongStateOf(0L) }
    var isChatOpen by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val chatViewModel: ChatViewModel = viewModel()
    
    val playerUrl = remember(channel, refreshTrigger) {
        val baseUrl = "https://player.twitch.tv/?channel=$channel&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
        if (refreshTrigger > 0) "$baseUrl&refresh=$refreshTrigger" else baseUrl
    }
    
    // Reset loading state whenever the URL changes (e.g. on refresh)
    LaunchedEffect(playerUrl) {
        isUiLoading = true
    }

    val rootFocusRequester = remember { FocusRequester() }
    val refreshFocusRequester = remember { FocusRequester() }
    val chatFocusRequester = remember { FocusRequester() }
    var lastPressedButton by remember { mutableStateOf("refresh") }

    // Initial focus
    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    // Safety timeout for loading screen
    LaunchedEffect(isUiLoading) {
        if (isUiLoading) {
            delay(10.seconds)
            if (isUiLoading) {
                isUiLoading = false
            }
        }
    }

    // Auto-hide overlay
    LaunchedEffect(showOverlay, overlayVisibleTick) {
        if (showOverlay) {
            delay(5.seconds)
            showOverlay = false
        } else {
            // Ensure focus returns to root to catch next key
            rootFocusRequester.requestFocus()
        }
    }

    // Request focus for buttons when overlay shown
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            // Tiny delay to ensure buttons are ready to receive focus after animation start
            delay(100.milliseconds)
            if (lastPressedButton == "refresh") {
                refreshFocusRequester.requestFocus()
            } else {
                chatFocusRequester.requestFocus()
            }
        }
    }

    // Handle Back Button
    androidx.activity.compose.BackHandler {
        if (isChatOpen) {
            isChatOpen = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    val isInteractionKey = when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> true
                        else -> false
                    }

                    if (isInteractionKey) {
                        overlayVisibleTick = System.currentTimeMillis()
                        if (!showOverlay) {
                            showOverlay = true
                            true // consume to show overlay
                        } else {
                            false // allow navigation/click on buttons
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main content (Player)
            Box(modifier = Modifier.weight(if (isChatOpen) 0.75f else 1f).fillMaxHeight()) {
                KWebView(
                    url = playerUrl,
                    modifier = Modifier.fillMaxSize(),
                    enableJavaScript = true,
                    enableDomStorageForAndroid = true,
                    isLoading = { loading ->
                        if (!loading) isUiLoading = false
                    }
                )

                if (isUiLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF9146FF))
                    }
                }

                // Overlay Buttons
                androidx.compose.animation.AnimatedVisibility(
                    visible = showOverlay,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), androidx.tv.material3.MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        androidx.tv.material3.Button(
                            onClick = { 
                                refreshTrigger++ 
                                overlayVisibleTick = System.currentTimeMillis()
                                lastPressedButton = "refresh"
                            },
                            modifier = Modifier.focusRequester(refreshFocusRequester),
                            colors = androidx.tv.material3.ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color(0xFF9146FF)
                            )
                        ) {
                            androidx.tv.material3.Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            Spacer(Modifier.width(8.dp))
                            androidx.tv.material3.Text("Refresh")
                        }

                        androidx.tv.material3.Button(
                            onClick = { 
                                isChatOpen = !isChatOpen 
                                overlayVisibleTick = System.currentTimeMillis()
                                lastPressedButton = "chat"
                            },
                            modifier = Modifier.focusRequester(chatFocusRequester),
                            colors = androidx.tv.material3.ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color(0xFF9146FF)
                            )
                        ) {
                            androidx.tv.material3.Icon(Icons.Default.Chat, contentDescription = "Chat")
                            Spacer(Modifier.width(8.dp))
                            androidx.tv.material3.Text(if (isChatOpen) "Hide Chat" else "Show Chat")
                        }
                    }
                }
            }

            // Side Chat
            androidx.compose.animation.AnimatedVisibility(
                visible = isChatOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.weight(0.25f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF18181B))
                ) {
                    TVChatOverlay(channel = channel, viewModel = chatViewModel)
                }
            }
        }
    }
}
