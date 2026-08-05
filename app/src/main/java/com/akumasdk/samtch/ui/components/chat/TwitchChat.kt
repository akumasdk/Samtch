package com.akumasdk.samtch.ui.components.chat

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("JavascriptInterface")
@Composable
fun TwitchChat(
    channel: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    showInput: Boolean = true,
    refreshTrigger: Int = 0,
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatMode by SettingsManager.getChatMode(context).collectAsState(initial = SettingsManager.ChatMode.NATIVE)

    if (chatMode == SettingsManager.ChatMode.NATIVE) {
        val isLoggedIn by viewModel.isLoggedIn.collectAsState()
        
        // Native chat refresh logic
        val chatLoadingText = stringResource(R.string.chat_connecting)
        val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
        val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger > 0) {
                viewModel.disconnect()
                viewModel.connect(channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate)
            }
        }
        
        Column(modifier = modifier.fillMaxSize()) {
            NativeTwitchChat(
                channel = channel,
                modifier = Modifier.weight(1f),
                isCompact = isCompact,
                viewModel = viewModel
            )
            
            if (showInput) {
                HorizontalDivider(color = SamtchTheme.colors.divider, thickness = 1.dp)
                ChatInputBox(
                    isLoggedIn = isLoggedIn,
                    onSendMessage = { text ->
                        coroutineScope.launch {
                            viewModel.sendMessage(text)
                        }
                    }
                )
            }
        }
        return
    }

    val chatUrl = Constants.Twitch.Templates.CHAT_URL.format(channel)
    val state = rememberSaveableWebViewState(chatUrl)
    val navigator = rememberWebViewNavigator()

    // Track if chat is fully loaded (chat-input element is present)
    var isChatFullyLoaded by remember { mutableStateOf(false) }

    // Reload WebView chat on theme change (Reverted)

    val chatBridge = remember(coroutineScope) {
        TwitchChatBridge(
            onChatLoadedCallback = {
                Log.d("TwitchChat", "Chat input element detected, waiting to prevent flash...")
                coroutineScope.launch {
                    delay(300.milliseconds)
                    isChatFullyLoaded = true
                    Log.d("TwitchChat", "Chat fully loaded - hiding loading screen")
                }
            }
        )
    }

    // Update URL when channel changes or refresh is triggered
    LaunchedEffect(channel, refreshTrigger) {
        isChatFullyLoaded = false
        navigator.loadUrl(chatUrl)
    }

    // Inject scripts when page is loaded
    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            try {
                listOf(
                    Constants.Scripts.CHAT_LOADER_OBSERVER,
                    Constants.Scripts.CHAT_UI_CLEANER,
                    Constants.Scripts.CHAT_BTTV
                ).forEach { path ->
                    val script = ScriptLoader.getScript(context, path)
                    if (script.isNotEmpty()) {
                        navigator.evaluateJavaScript(script)
                    }
                }
            } catch (e: Exception) {
                Log.e("TwitchChat", "Error injecting scripts", e)
                isChatFullyLoaded = true
            }
        }
    }

    Box(modifier = modifier) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = navigator,
            captureBackPresses = false,
            onCreated = { webView ->
                state.webSettings.apply {
                    isJavaScriptEnabled = true
                    androidWebSettings.domStorageEnabled = true
                }
                webView.apply {
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    addJavascriptInterface(chatBridge, Constants.Bridges.CHAT)
                }
            }
        )

        if (!isChatFullyLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SamtchTheme.colors.chatBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SamtchTheme.colors.twitchPurple)
            }
        }
    }
}
