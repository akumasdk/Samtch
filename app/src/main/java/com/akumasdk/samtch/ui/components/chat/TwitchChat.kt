package com.akumasdk.samtch.ui.components.chat

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.components.chat.emote.EmoteInfoDialog
import com.akumasdk.samtch.ui.components.chat.emotemenu.EmoteMenu
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.ScriptLoader
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("JavascriptInterface")
@Composable
fun TwitchChat(
    channel: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    showInput: Boolean = true,
    refreshTrigger: Int = 0,
    viewModel: ChatViewModel,
    portraitMode: PortraitMode? = null,
    onToggleMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatMode by SettingsManager.getChatMode(context).collectAsState(initial = SettingsManager.ChatMode.NATIVE)

    val density = LocalDensity.current
    
    // Determine orientation
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Emote menu state is now persistent until back press
    val isEmoteMenuVisible by viewModel.isEmoteMenuVisible.collectAsState()
    val keyboardHeightPx by viewModel.keyboardHeightPx.collectAsState()
    
    // Track IME insets
    val ime = WindowInsets.ime
    val navBars = WindowInsets.navigationBars
    
    // Current IME height state
    val currentImeHeightPx = remember(density) {
        derivedStateOf { (ime.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0) }
    }
    
    val isImeVisible = WindowInsets.isImeVisible

    // Persist keyboard height when it changes
    val imeTarget = WindowInsets.imeAnimationTarget
    val targetImeHeightPx = (imeTarget.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
    
    LaunchedEffect(targetImeHeightPx, isLandscape) {
        if (targetImeHeightPx > with(density) { 100.dp.toPx() }) {
            viewModel.updateKeyboardHeight(context, targetImeHeightPx, isLandscape)
        }
    }

    // Initialize keyboard height on start
    LaunchedEffect(isLandscape) {
        viewModel.initKeyboardHeight(context, isLandscape)
    }

    if (chatMode == SettingsManager.ChatMode.NATIVE) {
        val isLoggedIn by viewModel.isLoggedIn.collectAsState()
        val emoteSuggestions by viewModel.emoteSuggestions.collectAsState()
        val selectedEmoteForInfo by viewModel.selectedEmoteForInfo.collectAsState()
        val emoteMenuTabs by viewModel.emoteMenuTabs.collectAsState()
        
        // Native chat refresh logic
        val chatLoadingText = stringResource(R.string.chat_connecting)
        val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
        val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

        BackHandler(enabled = isEmoteMenuVisible) {
            viewModel.setEmoteMenuVisible(false)
        }

        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger > 0) {
                viewModel.disconnect()
                viewModel.connect(channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate)
            }
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                NativeTwitchChat(
                    channel = channel,
                    modifier = Modifier.weight(1f),
                    isCompact = isCompact,
                    viewModel = viewModel,
                    onEmoteLongClick = { viewModel.showEmoteInfo(it) }
                )
                
                if (showInput) {
                    HorizontalDivider(color = SamtchTheme.colors.divider, thickness = 1.dp)
                    ChatInputBox(
                        isLoggedIn = isLoggedIn,
                        onSendMessage = { text ->
                            coroutineScope.launch {
                                viewModel.sendMessage(text)
                            }
                        },
                        onEmoteToggle = { viewModel.setEmoteMenuVisible(!isEmoteMenuVisible) },
                        isEmoteMenuVisible = isEmoteMenuVisible,
                        suggestions = emoteSuggestions,
                        onEmoteSelected = { /* Logic handled in ChatInputBox for now */ },
                        onEmoteLongClick = { viewModel.showEmoteInfo(it) },
                        onTextChange = { text, pos -> viewModel.updateSuggestions(text, pos) },
                        emoteInsertFlow = viewModel.emoteInsertFlow,
                        portraitMode = portraitMode,
                        onToggleMode = onToggleMode
                    )

                    // Emote menu area
                    val menuHeight = if (keyboardHeightPx > 0) {
                        with(density) { keyboardHeightPx.toDp() }
                    } else {
                        if (isLandscape) 200.dp else 350.dp
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // This space animates with the keyboard
                        Spacer(modifier = Modifier.imePadding())

                        if (isEmoteMenuVisible) {
                            EmoteMenu(
                                tabs = emoteMenuTabs,
                                onEmoteClick = { emote ->
                                    viewModel.insertEmote(emote)
                                },
                                onEmoteLongClick = { viewModel.showEmoteInfo(it) },
                                height = menuHeight
                            )
                        }
                    }
                }
            }

            selectedEmoteForInfo?.let { emote ->
                EmoteInfoDialog(
                    emote = emote,
                    onDismiss = { viewModel.dismissEmoteInfo() }
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
