package com.akumasdk.samtch.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.BuildConfig
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.model.GitHubRelease
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.screens.settings.components.*
import com.akumasdk.samtch.util.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showChatModeDialog by remember { mutableStateOf(false) }
    var showChatFontSizeDialog by remember { mutableStateOf(false) }
    var showChatEmoteSizeDialog by remember { mutableStateOf(false) }
    var showChatBadgeSizeDialog by remember { mutableStateOf(false) }
    var showChatRatioDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isBttvSettingsOpen by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val chatMode by remember(context) { SettingsManager.getChatMode(context) }.collectAsState(initial = SettingsManager.ChatMode.NATIVE)
    val chatFontSize by remember(context) { SettingsManager.getChatFontSize(context) }.collectAsState(initial = 14)
    val chatEmoteSize by remember(context) { SettingsManager.getChatEmoteSize(context) }.collectAsState(initial = 28)
    val chatBadgeSize by remember(context) { SettingsManager.getChatBadgeSize(context) }.collectAsState(initial = 18)
    val chatRatio by remember(context) { SettingsManager.getFullscreenChatRatio(context) }.collectAsState(initial = 0)
    val themeMode by remember(context) { SettingsManager.getThemeMode(context) }.collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
    val adBlockMode by remember(context) { SettingsManager.getAdBlockMode(context) }.collectAsState(initial = SettingsManager.AdBlockMode.VIDEO_SWAP)
    val isPipEnabled by remember(context) { SettingsManager.isPipEnabled(context) }.collectAsState(initial = true)
    val isAudioBackgroundEnabled by remember(context) { SettingsManager.isAudioOnlyBackgroundEnabled(context) }.collectAsState(initial = false)
    val isImmersiveBackgroundEnabled by remember(context) { SettingsManager.isImmersiveBackgroundEnabled(context) }.collectAsState(initial = true)
    val isLoggedIn by remember(context) { SettingsManager.isLoggedIn(context) }.collectAsState(initial = false)

    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val isActuallyDark = when (themeMode) {
        SettingsManager.ThemeMode.DARK -> true
        SettingsManager.ThemeMode.LIGHT -> false
        SettingsManager.ThemeMode.SYSTEM -> isSystemInDarkTheme
    }

    BackHandler {
        if (showAboutDialog) {
            showAboutDialog = false
        } else if (isBttvSettingsOpen) {
            isBttvSettingsOpen = false
        } else {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (BuildConfig.UPDATES_ENABLED) {
            isCheckingUpdate = true
            latestRelease = UpdateManager.checkForUpdate()
            isCheckingUpdate = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            appearanceSection(
                themeMode = themeMode,
                isImmersiveEnabled = isImmersiveBackgroundEnabled,
                isActuallyDark = isActuallyDark,
                scope = scope,
                context = context,
                onThemeClick = { showThemeModeDialog = true }
            )

            playerSection(
                isPipEnabled = isPipEnabled,
                isAudioEnabled = isAudioBackgroundEnabled,
                adBlockMode = adBlockMode,
                scope = scope,
                context = context,
                onAdBlockClick = { showAdBlockDialog = true }
            )

            chatSection(
                chatMode = chatMode,
                chatFontSize = chatFontSize,
                chatEmoteSize = chatEmoteSize,
                chatBadgeSize = chatBadgeSize,
                chatRatio = chatRatio,
                scope = scope,
                context = context,
                onChatModeClick = { showChatModeDialog = true },
                onFontSizeClick = { showChatFontSizeDialog = true },
                onEmoteSizeClick = { showChatEmoteSizeDialog = true },
                onBadgeSizeClick = { showChatBadgeSizeDialog = true },
                onChatRatioClick = { showChatRatioDialog = true },
                onBttvClick = { isBttvSettingsOpen = true }
            )

            if (isLoggedIn) {
                accountSection(onLogoutClick = { showLogoutDialog = true })
            }

            appSection(
                latestRelease = latestRelease,
                isCheckingUpdate = isCheckingUpdate,
                isDownloading = isDownloading,
                context = context,
                onAboutClick = { showAboutDialog = true },
                onDownloadClick = { release ->
                    isDownloading = true
                    UpdateManager.downloadAndInstall(context, release)
                }
            )
        }
    }

    SettingsDialogs(
        showThemeDialog = showThemeModeDialog,
        showAdBlockDialog = showAdBlockDialog,
        showChatModeDialog = showChatModeDialog,
        showChatFontSizeDialog = showChatFontSizeDialog,
        showChatEmoteSizeDialog = showChatEmoteSizeDialog,
        showChatBadgeSizeDialog = showChatBadgeSizeDialog,
        showChatRatioDialog = showChatRatioDialog,
        showLogoutDialog = showLogoutDialog,
        showAboutDialog = showAboutDialog,
        themeMode = themeMode,
        adBlockMode = adBlockMode,
        chatMode = chatMode,
        chatFontSize = chatFontSize,
        chatEmoteSize = chatEmoteSize,
        chatBadgeSize = chatBadgeSize,
        chatRatio = chatRatio,
        onDismissTheme = { showThemeModeDialog = false },
        onDismissAdBlock = { showAdBlockDialog = false },
        onDismissChatMode = { showChatModeDialog = false },
        onDismissFontSize = { showChatFontSizeDialog = false },
        onDismissEmoteSize = { showChatEmoteSizeDialog = false },
        onDismissBadgeSize = { showChatBadgeSizeDialog = false },
        onDismissChatRatio = { showChatRatioDialog = false },
        onDismissLogout = { showLogoutDialog = false },
        onDismissAbout = { showAboutDialog = false },
        onLogout = onLogout,
        scope = scope,
        context = context
    )

    if (isBttvSettingsOpen) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BttvSettingsChat(modifier = Modifier.fillMaxSize())
        }
    }
}

private fun LazyListScope.appearanceSection(
    themeMode: SettingsManager.ThemeMode,
    isImmersiveEnabled: Boolean,
    isActuallyDark: Boolean,
    scope: CoroutineScope,
    context: android.content.Context,
    onThemeClick: () -> Unit
) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_appearance)) }
    item { 
        ThemeModeItem(
            themeMode, 
            onClick = onThemeClick,
            onReset = { scope.launch { SettingsManager.setThemeMode(context, SettingsManager.ThemeMode.SYSTEM) } }
        ) 
    }
    if (isActuallyDark) {
        item { 
            ImmersiveBackgroundToggleItem(
                enabled = isImmersiveEnabled,
                onToggle = { scope.launch { SettingsManager.setImmersiveBackgroundEnabled(context, it) } },
                onReset = { scope.launch { SettingsManager.setImmersiveBackgroundEnabled(context, true) } }
            )
        }
    }
    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }
}

private fun LazyListScope.playerSection(
    isPipEnabled: Boolean,
    isAudioEnabled: Boolean,
    adBlockMode: SettingsManager.AdBlockMode,
    scope: CoroutineScope,
    context: android.content.Context,
    onAdBlockClick: () -> Unit
) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_player)) }
    item { 
        PipToggleItem(
            isPipEnabled, 
            onToggle = { scope.launch { SettingsManager.setPipEnabled(context, it) } },
            onReset = { scope.launch { SettingsManager.setPipEnabled(context, true) } }
        ) 
    }
    item { 
        AudioBackgroundToggleItem(
            isAudioEnabled, 
            onToggle = { scope.launch { SettingsManager.setAudioOnlyBackgroundEnabled(context, it) } },
            onReset = { scope.launch { SettingsManager.setAudioOnlyBackgroundEnabled(context, false) } }
        ) 
    }
    item { 
        AdBlockModeItem(
            adBlockMode, 
            onClick = onAdBlockClick,
            onReset = { scope.launch { SettingsManager.setAdBlockMode(context, SettingsManager.AdBlockMode.VAFT) } }
        ) 
    }
    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }
}

private fun LazyListScope.chatSection(
    chatMode: SettingsManager.ChatMode,
    chatFontSize: Int,
    chatEmoteSize: Int,
    chatBadgeSize: Int,
    chatRatio: Int,
    scope: CoroutineScope,
    context: android.content.Context,
    onChatModeClick: () -> Unit,
    onFontSizeClick: () -> Unit,
    onEmoteSizeClick: () -> Unit,
    onBadgeSizeClick: () -> Unit,
    onChatRatioClick: () -> Unit,
    onBttvClick: () -> Unit
) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_chat)) }
    item { 
        ChatModeItem(
            chatMode, 
            onClick = onChatModeClick,
            onReset = { scope.launch { SettingsManager.setChatMode(context, SettingsManager.ChatMode.NATIVE) } }
        ) 
    }
    
    if (chatMode == SettingsManager.ChatMode.NATIVE) {
        item { 
            ChatFontSizeItem(
                chatFontSize, 
                onClick = onFontSizeClick,
                onReset = { scope.launch { SettingsManager.setChatFontSize(context, 14) } }
            ) 
        }
        item { 
            ChatEmoteSizeItem(
                chatEmoteSize, 
                onClick = onEmoteSizeClick,
                onReset = { scope.launch { SettingsManager.setChatEmoteSize(context, 28) } }
            ) 
        }
        item { 
            ChatBadgeSizeItem(
                chatBadgeSize, 
                onClick = onBadgeSizeClick,
                onReset = { scope.launch { SettingsManager.setChatBadgeSize(context, 18) } }
            ) 
        }
        item { 
            FullscreenChatRatioItem(
                chatRatio, 
                onClick = onChatRatioClick,
                onReset = { scope.launch { SettingsManager.setFullscreenChatRatio(context, 0) } }
            ) 
        }
    } else {
        item { BttvSettingsItem(onClick = onBttvClick) }
    }
    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }
}

private fun LazyListScope.accountSection(onLogoutClick: () -> Unit) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_account)) }
    item { LogoutItem(onClick = onLogoutClick) }
    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }
}

private fun LazyListScope.appSection(
    latestRelease: GitHubRelease?,
    isCheckingUpdate: Boolean,
    isDownloading: Boolean,
    context: android.content.Context,
    onAboutClick: () -> Unit,
    onDownloadClick: (GitHubRelease) -> Unit
) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_app)) }
    if (BuildConfig.UPDATES_ENABLED) {
        item {
            UpdateItem(
                latestRelease = latestRelease,
                isChecking = isCheckingUpdate,
                isDownloading = isDownloading,
                onClick = { latestRelease?.let { onDownloadClick(it) } }
            )
        }
    }
    item { AboutItem(onClick = onAboutClick) }
}
