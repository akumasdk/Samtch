package com.akumasdk.samtch.ui.components.chat

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.components.chat.emote.EmoteInfoDialog
import com.akumasdk.samtch.ui.components.chat.emote.BadgeInfoDialog
import com.akumasdk.samtch.ui.components.chat.user.UserInfoDialog
import com.akumasdk.samtch.ui.components.chat.emotemenu.EmoteMenu
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchAnimation
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TwitchChat(
    channel: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    showInput: Boolean = true,
    refreshTrigger: Int = 0,
    viewModel: ChatViewModel,
    portraitMode: PortraitMode? = null,
    onToggleMode: (() -> Unit)? = null,
    onLoginRequested: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    previewImageUrl: String? = null,
) {
    val context = LocalContext.current
    val chatMode by SettingsManager.getChatMode(context).collectAsState(initial = SettingsManager.ChatMode.NATIVE)

    if (chatMode == SettingsManager.ChatMode.NATIVE) {
        NativeChatContainer(
            channel = channel,
            modifier = modifier,
            isCompact = isCompact,
            showInput = showInput,
            refreshTrigger = refreshTrigger,
            viewModel = viewModel,
            portraitMode = portraitMode,
            onToggleMode = onToggleMode,
            onLoginRequested = onLoginRequested,
            onSettingsClick = onSettingsClick,
            previewImageUrl = previewImageUrl
        )
    } else {
        WebChatContainer(
            channel = channel,
            modifier = modifier,
            refreshTrigger = refreshTrigger
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NativeChatContainer(
    channel: String,
    modifier: Modifier,
    isCompact: Boolean,
    showInput: Boolean,
    refreshTrigger: Int,
    viewModel: ChatViewModel,
    portraitMode: PortraitMode?,
    onToggleMode: (() -> Unit)?,
    onLoginRequested: () -> Unit,
    onSettingsClick: () -> Unit,
    previewImageUrl: String?,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImmersiveEnabled by SettingsManager.isImmersiveBackgroundEnabled(context).collectAsState(initial = true)
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val isEmoteMenuVisible by viewModel.isEmoteMenuVisible.collectAsState()
    val keyboardHeightPx by viewModel.keyboardHeightPx.collectAsState()
    
    val navBars = WindowInsets.navigationBars
    val imeTarget = WindowInsets.imeAnimationTarget
    val targetImeHeightPx = (imeTarget.getBottom(density) - navBars.getBottom(density)).coerceAtLeast(0)
    
    var inputAreaHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetImeHeightPx, isLandscape) {
        if (targetImeHeightPx > with(density) { 100.dp.toPx() }) {
            viewModel.updateKeyboardHeight(context, targetImeHeightPx, isLandscape)
        }
    }

    LaunchedEffect(isLandscape) {
        viewModel.initKeyboardHeight(context, isLandscape)
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val emoteSuggestions by viewModel.emoteSuggestions.collectAsState()
    val emoteMenuTabs by viewModel.emoteMenuTabs.collectAsState()
    val isEmoteLoading by viewModel.isEmoteLoading.collectAsState()
    val systemNotice by viewModel.systemNotice.collectAsState()
    
    val chatLoadingText = stringResource(R.string.chat_connecting)
    val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
    val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.connect(context, channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate, forceRefresh = true)
        }
    }

    LaunchedEffect(channel) {
        viewModel.connect(context, channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (isImmersiveEnabled) {
            ChatImmersiveBackground(channel, previewImageUrl)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            NativeTwitchChat(
                channel = channel,
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.changedToUp() }) {
                                    if (isEmoteMenuVisible) {
                                        viewModel.setEmoteMenuVisible(visible = false)
                                    }
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            }
                        }
                    },
                isCompact = isCompact,
                isImmersiveEnabled = isImmersiveEnabled,
                viewModel = viewModel,
                onEmoteClick = { viewModel.showEmoteInfo(it) },
                onEmoteLongClick = { viewModel.showEmoteInfo(it) },
                onBadgeClick = { viewModel.showBadgeInfo(it) },
                onUserClick = { viewModel.showUserInfo(it) },
                contentPadding = PaddingValues(top = 88.dp, bottom = 8.dp)
            )
            
            if (showInput) {
                ChatInputArea(
                    viewModel = viewModel,
                    channel = channel,
                    isLoggedIn = isLoggedIn,
                    isImmersiveEnabled = isImmersiveEnabled,
                    isEmoteMenuVisible = isEmoteMenuVisible,
                    emoteSuggestions = emoteSuggestions,
                    emoteMenuTabs = emoteMenuTabs,
                    isEmoteLoading = isEmoteLoading,
                    systemNotice = systemNotice,
                    keyboardHeightPx = keyboardHeightPx,
                    isLandscape = isLandscape,
                    portraitMode = portraitMode,
                    onToggleMode = onToggleMode,
                    onLoginRequested = onLoginRequested,
                    onSettingsClick = onSettingsClick,
                    previewImageUrl = previewImageUrl,
                    onSizeChanged = { inputAreaHeightPx = it.height }
                )
            }
        }

        ChatDialogs(viewModel)
    }
}

@Composable
private fun ChatImmersiveBackground(channel: String, previewImageUrl: String?) {
    val isLightMode = SamtchTheme.colors.dialogBackground.luminance() > 0.5f
    val imageAlpha = if (isLightMode) 0.5f else 0.7f
    PlayerBackground(
        channel = channel,
        previewUrl = previewImageUrl,
        alpha = imageAlpha,
        blurRadius = 150.dp,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatInputArea(
    viewModel: ChatViewModel,
    channel: String,
    isLoggedIn: Boolean,
    isImmersiveEnabled: Boolean,
    isEmoteMenuVisible: Boolean,
    emoteSuggestions: List<com.akumasdk.samtch.data.emote.Emote>,
    emoteMenuTabs: Map<Int, List<com.akumasdk.samtch.data.emote.Emote>>,
    isEmoteLoading: Boolean,
    systemNotice: String?,
    keyboardHeightPx: Int,
    isLandscape: Boolean,
    portraitMode: PortraitMode?,
    onToggleMode: (() -> Unit)?,
    onLoginRequested: () -> Unit,
    onSettingsClick: () -> Unit,
    previewImageUrl: String?,
    modifier: Modifier = Modifier,
    onSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLightMode = SamtchTheme.colors.dialogBackground.luminance() > 0.5f
    val surfaceAlpha = if (isImmersiveEnabled) {
        if (isLightMode) 0.94f else 0.82f
    } else 1.0f
    val imageAlpha = if (isLightMode) 0.5f else 0.7f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged(onSizeChanged),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = SamtchTheme.colors.dialogBackground.copy(alpha = surfaceAlpha),
        border = if (isImmersiveEnabled) BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f)) else null,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isImmersiveEnabled) {
                PlayerBackground(
                    alpha = imageAlpha,
                    blurRadius = 150.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier.matchParentSize()
                )
            }

            Column {
                SystemNoticeBanner(
                    message = systemNotice,
                    onDismiss = { viewModel.dismissSystemNotice() },
                    isImmersiveEnabled = isImmersiveEnabled,
                    channel = channel,
                    previewImageUrl = previewImageUrl
                )
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
                    onEmoteSelected = { emote ->
                        viewModel.recordEmoteUsage(context, emote)
                    },
                    onEmoteLongClick = { viewModel.showEmoteInfo(it) },
                    onTextChange = { text, pos -> viewModel.updateSuggestions(text, pos) },
                    emoteInsertFlow = viewModel.emoteInsertFlow,
                    portraitMode = portraitMode,
                    onToggleMode = onToggleMode,
                    onFocusChanged = { focused ->
                        if (focused) {
                            viewModel.setEmoteMenuVisible(visible = true)
                        }
                    },
                    onLoginRequested = onLoginRequested
                )

                val menuHeight = if (keyboardHeightPx > 0) {
                    with(density) { keyboardHeightPx.toDp() }
                } else {
                    if (isLandscape) 200.dp else 350.dp
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime.union(WindowInsets.navigationBars)))
                    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEmoteMenuVisible,
                        enter = expandVertically(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = SamtchAnimation.EmphasizedEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 300)
                            ),
                        exit = shrinkVertically(
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = SamtchAnimation.EmphasizedEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 250)
                        ),
                        label = "EmoteMenuVisibility"
                    ) {
                        val currentImeBottom = WindowInsets.ime.getBottom(density)
                        val navBarBottom = WindowInsets.navigationBars.getBottom(density)
                        val currentKeyboardHeightPx = (currentImeBottom - navBarBottom).coerceAtLeast(0)
                        val menuHeightPx = with(density) { menuHeight.toPx() }
                        val isKeyboardCoveringMenu = currentKeyboardHeightPx >= (menuHeightPx * 0.98f).toInt()
                        
                        Column {
                            Box(modifier = Modifier.height(menuHeight)) {
                                if (!isKeyboardCoveringMenu) {
                                    val chatLoadingText = stringResource(R.string.chat_connecting)
                                    val chatWelcomeTemplate = stringResource(R.string.chat_welcome)
                                    val chatLoginTemplate = stringResource(R.string.chat_logged_in_as)

                                    EmoteMenu(
                                        tabs = emoteMenuTabs,
                                        onEmoteClick = { emote ->
                                            viewModel.insertEmote(emote)
                                            viewModel.recordEmoteUsage(context, emote)
                                        },
                                        onEmoteLongClick = { viewModel.showEmoteInfo(it) },
                                        onRefresh = {
                                            viewModel.connect(context, channel, chatLoadingText, chatWelcomeTemplate, chatLoginTemplate, forceRefresh = true)
                                            context.sendBroadcast(Intent(Constants.Actions.REFRESH).setPackage(context.packageName))
                                        },
                                        onSettingsClick = onSettingsClick,
                                        height = menuHeight,
                                        channel = channel,
                                        previewImageUrl = previewImageUrl,
                                        isImmersiveEnabled = isImmersiveEnabled,
                                        isLoading = isEmoteLoading
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebChatContainer(
    channel: String,
    modifier: Modifier,
    refreshTrigger: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatUrl = Constants.Twitch.Templates.CHAT_URL.format(channel)
    val state = rememberSaveableWebViewState(chatUrl)
    val navigator = rememberWebViewNavigator()

    var isChatFullyLoaded by remember { mutableStateOf(false) }

    val chatBridge = remember(coroutineScope) {
        TwitchChatBridge {
            Log.d("TwitchChat", "Chat input element detected, waiting to prevent flash...")
            coroutineScope.launch {
                delay(300.milliseconds)
                isChatFullyLoaded = true
                Log.d("TwitchChat", "Chat fully loaded - hiding loading screen")
            }
        }
    }

    LaunchedEffect(channel, refreshTrigger) {
        isChatFullyLoaded = false
        navigator.loadUrl(chatUrl)
    }

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

    Box(modifier = modifier.navigationBarsPadding()) {
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
