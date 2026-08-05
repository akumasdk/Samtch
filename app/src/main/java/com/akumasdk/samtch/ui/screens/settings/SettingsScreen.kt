package com.akumasdk.samtch.ui.screens.settings

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.BuildConfig
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.model.GitHubRelease
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.util.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showChatModeDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isBttvSettingsOpen by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val chatMode by SettingsManager.getChatMode(context).collectAsState(initial = SettingsManager.ChatMode.NATIVE)
    val themeMode by SettingsManager.getThemeMode(context).collectAsState(initial = SettingsManager.ThemeMode.SYSTEM)
    val adBlockMode by SettingsManager.getAdBlockMode(context).collectAsState(initial = SettingsManager.AdBlockMode.VIDEO_SWAP)
    val isPipEnabled by SettingsManager.isPipEnabled(context).collectAsState(initial = true)
    val isAudioBackgroundEnabled by SettingsManager.isAudioOnlyBackgroundEnabled(context).collectAsState(initial = false)

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
            item { ThemeModeItem(themeMode, onClick = { showThemeModeDialog = true }) }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            item { SettingSectionHeader(stringResource(R.string.settings_category_player)) }
            item { PipToggleItem(isPipEnabled, onToggle = { scope.launch { SettingsManager.setPipEnabled(context, it) } }) }
            item { AudioBackgroundToggleItem(isAudioBackgroundEnabled, onToggle = { scope.launch { SettingsManager.setAudioOnlyBackgroundEnabled(context, it) } }) }
            item { AdBlockModeItem(adBlockMode, onClick = { showAdBlockDialog = true }) }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            item { SettingSectionHeader(stringResource(R.string.settings_category_chat)) }
            item { ChatModeItem(chatMode, onClick = { showChatModeDialog = true }) }
            item { BttvSettingsItem(chatMode == SettingsManager.ChatMode.NATIVE, onClick = { isBttvSettingsOpen = true }) }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            item { SettingSectionHeader(stringResource(R.string.settings_category_account)) }
            item { LogoutItem(onClick = { showLogoutDialog = true }) }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

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
            onDismiss = { showChatModeDialog = false }
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
private fun ThemeModeItem(themeMode: SettingsManager.ThemeMode, onClick: () -> Unit) {
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
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun PipToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.pip_enabled_title)) },
        supportingContent = { Text(stringResource(R.string.pip_enabled_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_pip_mode), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.clickable { onToggle(!enabled) }
    )
}

@Composable
private fun AudioBackgroundToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.audio_only_background_title)) },
        supportingContent = { Text(stringResource(R.string.audio_only_background_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_headset), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.clickable { onToggle(!enabled) }
    )
}

@Composable
private fun AdBlockModeItem(mode: SettingsManager.AdBlockMode, onClick: () -> Unit) {
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
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun ChatModeItem(mode: SettingsManager.ChatMode, onClick: () -> Unit) {
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
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun BttvSettingsItem(isNativeChat: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { 
            Text(
                text = stringResource(R.string.bttv_settings_title),
                color = if (isNativeChat) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
            ) 
        },
        supportingContent = { 
            Text(
                text = if (isNativeChat) stringResource(R.string.chat_mode_notice) else stringResource(R.string.bttv_settings_summary),
                color = if (isNativeChat) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else Color.Unspecified
            ) 
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_bttv),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isNativeChat) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
            )
        },
        modifier = Modifier.clickable(enabled = !isNativeChat) { onClick() }
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

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(R.string.about_dialog_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                Text(text = stringResource(R.string.about_dialog_description))
                HorizontalDivider(thickness = 0.5.dp)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.github_repo)) },
                    supportingContent = { Text(stringResource(R.string.github_repo_summary)) },
                    leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/akumasdk/samtch") }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.support_project)) },
                    supportingContent = { Text(stringResource(R.string.support_project_summary)) },
                    leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_donation), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    modifier = Modifier.clickable { uriHandler.openUri("https://ko-fi.com/akumasdk") }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_button)) } }
    )
}
