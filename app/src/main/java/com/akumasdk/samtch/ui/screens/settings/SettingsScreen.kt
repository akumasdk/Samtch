package com.akumasdk.samtch.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.BuildConfig
import com.akumasdk.samtch.ui.screens.settings.components.AboutDialog
import com.akumasdk.samtch.ui.screens.settings.components.BttvSettingsChat
import com.akumasdk.samtch.ui.screens.settings.components.LogoutDialog
import com.akumasdk.samtch.ui.screens.settings.components.SelectionDialog
import com.akumasdk.samtch.ui.screens.settings.components.SettingSectionHeader
import com.akumasdk.samtch.ui.screens.settings.components.UpdateItem
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.model.GitHubRelease
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.UpdateManager
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
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isBttvSettingsOpen by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val chatMode by remember(context) { SettingsManager.getChatMode(context) }.collectAsState(initial = SettingsManager.ChatMode.NATIVE)
    val chatFontSize by remember(context) { SettingsManager.getChatFontSize(context) }.collectAsState(initial = 14)
    val chatEmoteSize by remember(context) { SettingsManager.getChatEmoteSize(context) }.collectAsState(initial = 28)
    val chatBadgeSize by remember(context) { SettingsManager.getChatBadgeSize(context) }.collectAsState(initial = 18)
    val themeMode by remember(context) { SettingsManager.getThemeMode(context) }.collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
    val adBlockMode by remember(context) { SettingsManager.getAdBlockMode(context) }.collectAsState(initial = SettingsManager.AdBlockMode.VIDEO_SWAP)
    val isPipEnabled by remember(context) { SettingsManager.isPipEnabled(context) }.collectAsState(initial = true)
    val isAudioBackgroundEnabled by remember(context) { SettingsManager.isAudioOnlyBackgroundEnabled(context) }.collectAsState(initial = false)
    val isImmersiveBackgroundEnabled by remember(context) { SettingsManager.isImmersiveBackgroundEnabled(context) }.collectAsState(initial = true)
    val isLoggedIn by remember(context) { SettingsManager.isLoggedIn(context) }.collectAsState(initial = false)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
            item { SettingSectionHeader(stringResource(R.string.settings_category_appearance)) }
            item { 
                ThemeModeItem(
                    themeMode, 
                    onClick = { showThemeModeDialog = true },
                    onReset = { scope.launch { SettingsManager.setThemeMode(context, SettingsManager.ThemeMode.SYSTEM) } }
                ) 
            }
            item { 
                ImmersiveBackgroundToggleItem(
                    isImmersiveBackgroundEnabled,
                    onToggle = { scope.launch { SettingsManager.setImmersiveBackgroundEnabled(context, it) } },
                    onReset = { scope.launch { SettingsManager.setImmersiveBackgroundEnabled(context, true) } }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

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
                    isAudioBackgroundEnabled, 
                    onToggle = { scope.launch { SettingsManager.setAudioOnlyBackgroundEnabled(context, it) } },
                    onReset = { scope.launch { SettingsManager.setAudioOnlyBackgroundEnabled(context, false) } }
                ) 
            }
            item { 
                AdBlockModeItem(
                    adBlockMode, 
                    onClick = { showAdBlockDialog = true },
                    onReset = { scope.launch { SettingsManager.setAdBlockMode(context, SettingsManager.AdBlockMode.VAFT) } }
                ) 
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            item { SettingSectionHeader(stringResource(R.string.settings_category_chat)) }
            item { 
                ChatModeItem(
                    chatMode, 
                    onClick = { showChatModeDialog = true },
                    onReset = { scope.launch { SettingsManager.setChatMode(context, SettingsManager.ChatMode.NATIVE) } }
                ) 
            }
            
            if (chatMode == SettingsManager.ChatMode.NATIVE) {
                item { 
                    ChatFontSizeItem(
                        chatFontSize, 
                        onClick = { showChatFontSizeDialog = true },
                        onReset = { scope.launch { SettingsManager.setChatFontSize(context, 14) } }
                    ) 
                }
                item { 
                    ChatEmoteSizeItem(
                        chatEmoteSize, 
                        onClick = { showChatEmoteSizeDialog = true },
                        onReset = { scope.launch { SettingsManager.setChatEmoteSize(context, 28) } }
                    ) 
                }
                item { 
                    ChatBadgeSizeItem(
                        chatBadgeSize, 
                        onClick = { showChatBadgeSizeDialog = true },
                        onReset = { scope.launch { SettingsManager.setChatBadgeSize(context, 18) } }
                    ) 
                }
            } else {
                item { BttvSettingsItem(onClick = { isBttvSettingsOpen = true }) }
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            if (isLoggedIn) {
                item { SettingSectionHeader(stringResource(R.string.settings_category_account)) }
                item { LogoutItem(onClick = { showLogoutDialog = true }) }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }
            }

            item { SettingSectionHeader(stringResource(R.string.settings_category_app)) }
            if (BuildConfig.UPDATES_ENABLED) {
                item {
                    UpdateItem(
                        latestRelease = latestRelease,
                        isChecking = isCheckingUpdate,
                        isDownloading = isDownloading,
                        onClick = {
                            latestRelease?.let { release ->
                                isDownloading = true
                                UpdateManager.downloadAndInstall(context, release)
                            }
                        }
                    )
                }
            }
            item { AboutItem(onClick = { showAboutDialog = true }) }
        }
    }

    // Dialogs
    if (showThemeModeDialog) {
        SelectionDialog(
            title = stringResource(R.string.theme_mode_title),
            options = SettingsManager.ThemeMode.entries.map { mode ->
                val label = when (mode) {
                    SettingsManager.ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                    SettingsManager.ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                    SettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                }
                label to { scope.launch { SettingsManager.setThemeMode(context, mode) } }
            },
            selectedIndex = SettingsManager.ThemeMode.entries.indexOf(themeMode),
            onReset = { scope.launch { SettingsManager.setThemeMode(context, SettingsManager.ThemeMode.SYSTEM) } },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    if (showAdBlockDialog) {
        SelectionDialog(
            title = stringResource(R.string.ad_block_mode_title),
            options = SettingsManager.AdBlockMode.entries.map { mode ->
                val label = when (mode) {
                    SettingsManager.AdBlockMode.VAFT -> stringResource(R.string.ad_block_mode_vaft)
                    SettingsManager.AdBlockMode.VIDEO_SWAP -> stringResource(R.string.ad_block_mode_video_swap)
                }
                label to { scope.launch { SettingsManager.setAdBlockMode(context, mode) } }
            },
            selectedIndex = SettingsManager.AdBlockMode.entries.indexOf(adBlockMode),
            onReset = { scope.launch { SettingsManager.setAdBlockMode(context, SettingsManager.AdBlockMode.VAFT) } },
            onDismiss = { showAdBlockDialog = false }
        )
    }

    if (showChatModeDialog) {
        SelectionDialog(
            title = stringResource(R.string.chat_mode_title),
            options = SettingsManager.ChatMode.entries.map { mode ->
                val label = when (mode) {
                    SettingsManager.ChatMode.NATIVE -> stringResource(R.string.chat_mode_native)
                    SettingsManager.ChatMode.LEGACY -> stringResource(R.string.chat_mode_legacy)
                }
                label to { scope.launch { SettingsManager.setChatMode(context, mode) } }
            },
            selectedIndex = SettingsManager.ChatMode.entries.indexOf(chatMode),
            onReset = { scope.launch { SettingsManager.setChatMode(context, SettingsManager.ChatMode.NATIVE) } },
            onDismiss = { showChatModeDialog = false }
        )
    }

    if (showChatFontSizeDialog) {
        val fontSizeOptions = listOf(12, 13, 14, 15, 16, 17, 18, 20, 22)
        SelectionDialog(
            title = stringResource(R.string.chat_settings_font_size),
            options = fontSizeOptions.map { size ->
                "${size}sp" to { scope.launch { SettingsManager.setChatFontSize(context, size) } }
            },
            selectedIndex = fontSizeOptions.indexOf(chatFontSize),
            onReset = { scope.launch { SettingsManager.setChatFontSize(context, 14) } },
            onDismiss = { showChatFontSizeDialog = false }
        )
    }

    if (showChatEmoteSizeDialog) {
        val emoteSizeOptions = listOf(20, 24, 28, 32, 36, 40, 44, 48)
        SelectionDialog(
            title = stringResource(R.string.chat_settings_emote_size),
            options = emoteSizeOptions.map { size ->
                "${size}dp" to { scope.launch { SettingsManager.setChatEmoteSize(context, size) } }
            },
            selectedIndex = emoteSizeOptions.indexOf(chatEmoteSize),
            onReset = { scope.launch { SettingsManager.setChatEmoteSize(context, 28) } },
            onDismiss = { showChatEmoteSizeDialog = false }
        )
    }

    if (showChatBadgeSizeDialog) {
        val badgeSizeOptions = listOf(14, 16, 18, 20, 22, 24, 28, 32)
        SelectionDialog(
            title = stringResource(R.string.chat_settings_badge_size),
            options = badgeSizeOptions.map { size ->
                "${size}dp" to { scope.launch { SettingsManager.setChatBadgeSize(context, size) } }
            },
            selectedIndex = badgeSizeOptions.indexOf(chatBadgeSize),
            onReset = { scope.launch { SettingsManager.setChatBadgeSize(context, 18) } },
            onDismiss = { showChatBadgeSizeDialog = false }
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(onConfirm = onLogout, onDismiss = { showLogoutDialog = false })
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (isBttvSettingsOpen) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BttvSettingsChat(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ThemeModeItem(themeMode: SettingsManager.ThemeMode, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.theme_mode_title)) },
        supportingContent = {
            Text(
                when (themeMode) {
                    SettingsManager.ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                    SettingsManager.ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                    SettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                }
            )
        },
        leadingContent = { Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onReset
        )
    )
}

@Composable
private fun ImmersiveBackgroundToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.immersive_background_title)) },
        supportingContent = { Text(stringResource(R.string.immersive_background_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_radial_blur), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.combinedClickable(
            onClick = { onToggle(!enabled) },
            onLongClick = onReset
        )
    )
}

@Composable
private fun PipToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.pip_enabled_title)) },
        supportingContent = { Text(stringResource(R.string.pip_enabled_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_pip_mode), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.combinedClickable(
            onClick = { onToggle(!enabled) },
            onLongClick = onReset
        )
    )
}

@Composable
private fun AudioBackgroundToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.audio_only_background_title)) },
        supportingContent = { Text(stringResource(R.string.audio_only_background_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_headset), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.combinedClickable(
            onClick = { onToggle(!enabled) },
            onLongClick = onReset
        )
    )
}

@Composable
private fun AdBlockModeItem(mode: SettingsManager.AdBlockMode, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.ad_block_mode_title)) },
        supportingContent = {
            Text(
                when (mode) {
                    SettingsManager.AdBlockMode.VAFT -> stringResource(R.string.ad_block_mode_vaft)
                    SettingsManager.AdBlockMode.VIDEO_SWAP -> stringResource(R.string.ad_block_mode_video_swap)
                }
            )
        },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_ad_block), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onReset
        )
    )
}

@Composable
private fun ChatModeItem(mode: SettingsManager.ChatMode, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_mode_title)) },
        supportingContent = {
            Text(
                when (mode) {
                    SettingsManager.ChatMode.NATIVE -> stringResource(R.string.chat_mode_native)
                    SettingsManager.ChatMode.LEGACY -> stringResource(R.string.chat_mode_legacy)
                }
            )
        },
        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onReset
        )
    )
}

@Composable
private fun ChatFontSizeItem(size: Int, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_settings_font_size)) },
        supportingContent = { Text("${size}sp") },
        leadingContent = { Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onReset
        )
    )
}

@Composable
private fun ChatEmoteSizeItem(size: Int, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_settings_emote_size)) },
        supportingContent = { Text("${size}dp") },
        leadingContent = { Icon(imageVector = Icons.Default.EmojiEmotions, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onReset
        )
    )
}

@Composable
private fun ChatBadgeSizeItem(size: Int, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_settings_badge_size)) },
        supportingContent = { Text("${size}dp") },
        leadingContent = { Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onReset
        )
    )
}

@Composable
private fun BttvSettingsItem(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = stringResource(R.string.bttv_settings_title)) },
        supportingContent = { Text(text = stringResource(R.string.bttv_settings_summary)) },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_bttv),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun LogoutItem(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.logout_title)) },
        supportingContent = { Text(stringResource(R.string.logout_summary)) },
        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun AboutItem(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.about_title)) },
        supportingContent = { Text(stringResource(R.string.about_summary)) },
        leadingContent = { Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable { onClick() }
    )
}
