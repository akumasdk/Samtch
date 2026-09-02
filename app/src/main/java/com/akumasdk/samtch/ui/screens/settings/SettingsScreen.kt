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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsManager = viewModel.settingsManager
    var showAboutDialog by remember { mutableStateOf(false) }
    // ... rest of the state
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

    val chatMode by settingsManager.getChatMode().collectAsState(initial = SettingsManager.ChatMode.NATIVE)
    val chatFontSize by settingsManager.getChatFontSize().collectAsState(initial = 14)
    val chatEmoteSize by settingsManager.getChatEmoteSize().collectAsState(initial = 28)
    val chatBadgeSize by settingsManager.getChatBadgeSize().collectAsState(initial = 18)
    val chatRatio by settingsManager.getFullscreenChatRatio().collectAsState(initial = 0)
    val themeMode by settingsManager.getThemeMode().collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
    val adBlockMode by settingsManager.getAdBlockMode().collectAsState(initial = SettingsManager.AdBlockMode.VIDEO_SWAP)
    val isPipEnabled by settingsManager.isPipEnabled().collectAsState(initial = true)
    val isAudioBackgroundEnabled by settingsManager.isAudioOnlyBackgroundEnabled().collectAsState(initial = false)
    val isImmersiveBackgroundEnabled by settingsManager.isImmersiveBackgroundEnabled().collectAsState(initial = true)
    val isLoggedIn by settingsManager.isLoggedIn().collectAsState(initial = false)

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
                onThemeClick = { showThemeModeDialog = true },
                settingsManager = settingsManager
            )

            playerSection(
                isPipEnabled = isPipEnabled,
                isAudioEnabled = isAudioBackgroundEnabled,
                adBlockMode = adBlockMode,
                scope = scope,
                onAdBlockClick = { showAdBlockDialog = true },
                settingsManager = settingsManager
            )

            chatSection(
                chatMode = chatMode,
                chatFontSize = chatFontSize,
                chatEmoteSize = chatEmoteSize,
                chatBadgeSize = chatBadgeSize,
                chatRatio = chatRatio,
                scope = scope,
                onChatModeClick = { showChatModeDialog = true },
                onFontSizeClick = { showChatFontSizeDialog = true },
                onEmoteSizeClick = { showChatEmoteSizeDialog = true },
                onBadgeSizeClick = { showChatBadgeSizeDialog = true },
                onChatRatioClick = { showChatRatioDialog = true },
                onBttvClick = { isBttvSettingsOpen = true },
                settingsManager = settingsManager
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
        settingsManager = settingsManager,
        scope = scope
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
    settingsManager: SettingsManager,
    onThemeClick: () -> Unit
) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_appearance)) }
    item { 
        ThemeModeItem(
            themeMode, 
            onClick = onThemeClick,
            onReset = { scope.launch { settingsManager.setThemeMode(SettingsManager.ThemeMode.SYSTEM) } }
        ) 
    }
    if (isActuallyDark) {
        item { 
            ImmersiveBackgroundToggleItem(
                enabled = isImmersiveEnabled,
                onToggle = { scope.launch { settingsManager.setImmersiveBackgroundEnabled(it) } },
                onReset = { scope.launch { settingsManager.setImmersiveBackgroundEnabled(true) } }
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
    settingsManager: SettingsManager,
    onAdBlockClick: () -> Unit
) {
    item { SettingSectionHeader(stringResource(R.string.settings_category_player)) }
    item { 
        PipToggleItem(
            isPipEnabled, 
            onToggle = { scope.launch { settingsManager.setPipEnabled(it) } },
            onReset = { scope.launch { settingsManager.setPipEnabled(true) } }
        ) 
    }
    item { 
        AudioBackgroundToggleItem(
            isAudioEnabled, 
            onToggle = { scope.launch { settingsManager.setAudioOnlyBackgroundEnabled(it) } },
            onReset = { scope.launch { settingsManager.setAudioOnlyBackgroundEnabled(false) } }
        ) 
    }
    item { 
        AdBlockModeItem(
            adBlockMode, 
            onClick = onAdBlockClick,
            onReset = { scope.launch { settingsManager.setAdBlockMode(SettingsManager.AdBlockMode.VAFT) } }
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
    settingsManager: SettingsManager,
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
            onReset = { scope.launch { settingsManager.setChatMode(SettingsManager.ChatMode.NATIVE) } }
        ) 
    }
    
    if (chatMode == SettingsManager.ChatMode.NATIVE) {
        item { 
            ChatFontSizeItem(
                chatFontSize, 
                onClick = onFontSizeClick,
                onReset = { scope.launch { settingsManager.setChatFontSize(14) } }
            ) 
        }
        item { 
            ChatEmoteSizeItem(
                chatEmoteSize, 
                onClick = onEmoteSizeClick,
                onReset = { scope.launch { settingsManager.setChatEmoteSize(28) } }
            ) 
        }
        item { 
            ChatBadgeSizeItem(
                chatBadgeSize, 
                onClick = onBadgeSizeClick,
                onReset = { scope.launch { settingsManager.setChatBadgeSize(18) } }
            ) 
        }
        item { 
            FullscreenChatRatioItem(
                chatRatio, 
                onClick = onChatRatioClick,
                onReset = { scope.launch { settingsManager.setFullscreenChatRatio(0) } }
            ) 
        }
    }
else {
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
