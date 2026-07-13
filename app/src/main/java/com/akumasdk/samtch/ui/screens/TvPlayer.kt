package com.akumasdk.samtch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.screens.player.TwitchChat
import com.akumasdk.samtch.ui.screens.player.WebViewContainer
import com.akumasdk.samtch.ui.screens.player.createTwitchPlayerUrl
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
    var isButtonFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        onBack()
    }

    LaunchedEffect(channel) {
        navigator.loadUrl(twitchUrl)
    }

    // Autofocus the toggle button
    LaunchedEffect(Unit) {
        delay(500.milliseconds)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
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

    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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

            // TV Toggle Chat Button
            Button(
                onClick = { isChatVisible = !isChatVisible },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isButtonFocused = it.isFocused }
                    .border(
                        width = if (isButtonFocused) 2.dp else 0.dp,
                        color = if (isButtonFocused) Color.White else Color.Transparent,
                        shape = ButtonDefaults.shape
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isButtonFocused) 
                        Color.White.copy(alpha = 0.3f) 
                    else 
                        Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Text(if (isChatVisible) "Hide Chat" else "Show Chat")
            }
        }

        if (isChatVisible) {
            Box(
                modifier = Modifier
                    .width(300.dp)
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
