package com.akumasdk.samtch.ui.screens.player

import android.view.KeyEvent as NativeKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun TvPlayer(
    channel: String,
    onBack: () -> Unit
) {
    val twitchUrl = createTwitchPlayerUrl(channel)
    val context = LocalContext.current
    val state = rememberSaveableWebViewState("")
    val navigator = rememberWebViewNavigator()
    
    var isChatVisible by remember { mutableStateOf(false) }
    var isChatButtonFocused by remember { mutableStateOf(false) }
    var isRefreshButtonFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun notifyInteraction() {
        areControlsVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    BackHandler {
        if (!areControlsVisible) {
            notifyInteraction()
        } else {
            onBack()
        }
    }

    LaunchedEffect(channel) {
        navigator.loadUrl(twitchUrl)
    }

    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(lastInteractionTime) {
        delay(5.seconds)
        // Force hide even if focused to allow focus to clear
        areControlsVisible = false
    }

    // Request focus when controls appear
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            // Wait slightly longer to ensure layout is ready after Crossfade
            delay(300.milliseconds)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Try again once more if failed
                delay(200.milliseconds)
                try { focusRequester.requestFocus() } catch (_: Exception) {}
            }
        }
    }

    // Basic script injection for TV (cleaner UI)
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            val scripts = listOf(
                "js/player/video_swap.js",
                "js/player/tv_ui_cleaner.js",
                "js/player/link_disabler.js"
            )
            scripts.forEach { path ->
                val script = ScriptLoader.loadAsset(context, path)
                if (script.isNotEmpty()) {
                    navigator.evaluateJavaScript(script)
                }
            }
        }
    }

    Row(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .onKeyEvent { event ->
            if (event.nativeKeyEvent.action == NativeKeyEvent.ACTION_DOWN) {
                when (event.nativeKeyEvent.keyCode) {
                    NativeKeyEvent.KEYCODE_DPAD_UP,
                    NativeKeyEvent.KEYCODE_DPAD_DOWN,
                    NativeKeyEvent.KEYCODE_DPAD_LEFT,
                    NativeKeyEvent.KEYCODE_DPAD_RIGHT,
                    NativeKeyEvent.KEYCODE_DPAD_CENTER,
                    NativeKeyEvent.KEYCODE_ENTER -> {
                        if (!areControlsVisible) {
                            notifyInteraction()
                            return@onKeyEvent true
                        }
                    }
                }
            }
            false
        }
        .focusable()
        .pointerInput(Unit) {
            detectTapGestures(onTap = { notifyInteraction() })
        }
    ) {
        Box(modifier = Modifier.weight(1f)) {
            WebViewContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = false },
                state = state,
                navigator = navigator,
                channel = channel,
                onToggleFullscreen = {}, // Always fullscreen on TV
                onToggleChat = { isChatVisible = !isChatVisible },
                isFocusable = false
            )

            // TV Controls Overlay
            Crossfade(
                targetState = areControlsVisible,
                modifier = Modifier.align(Alignment.TopEnd),
                label = "controls_fade"
            ) { visible ->
                if (visible) {
                    Row(
                        modifier = Modifier
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = ButtonDefaults.shape)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Refresh Button
                        Button(
                            onClick = { 
                                notifyInteraction()
                                navigator.reload() 
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .onFocusChanged { 
                                    if (it.isFocused) notifyInteraction()
                                    isRefreshButtonFocused = it.isFocused 
                                },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRefreshButtonFocused) 
                                    Color.White.copy(alpha = 0.9f) 
                                else 
                                    Color.Black.copy(alpha = 0.6f),
                                contentColor = if (isRefreshButtonFocused) Color.Black else Color.White
                            )
                        ) {
                            Text("Refresh", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // Toggle Chat Button
                        Button(
                            onClick = { 
                                notifyInteraction()
                                isChatVisible = !isChatVisible 
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { 
                                    if (it.isFocused) notifyInteraction()
                                    isChatButtonFocused = it.isFocused 
                                },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChatButtonFocused) 
                                    Color.White.copy(alpha = 0.9f) 
                                else 
                                    Color.Black.copy(alpha = 0.6f),
                                contentColor = if (isChatButtonFocused) Color.Black else Color.White
                            )
                        ) {
                            Text(
                                text = if (isChatVisible) "Hide Chat" else "Show Chat",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (isChatVisible) {
            Box(
                modifier = Modifier
                    .width(350.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF18181B))
                    .focusProperties { canFocus = false }
            ) {
                TwitchChat(
                    channel = channel,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF18181B)),
                    isFocusable = false
                )
            }
        }
    }
}
