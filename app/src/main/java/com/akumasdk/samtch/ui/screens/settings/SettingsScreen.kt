package com.akumasdk.samtch.ui.screens.settings

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    onBack: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showChatModeDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
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

    // Intercept system back button
    BackHandler {
        Log.d("SettingsScreen", "BackHandler triggered. showAboutDialog=$showAboutDialog, isBttvSettingsOpen=$isBttvSettingsOpen")
        if (showAboutDialog) {
            showAboutDialog = false
        } else if (isBttvSettingsOpen) {
            isBttvSettingsOpen = false
        } else {
            onBack()
        }
    }

    // Check for updates on screen launch
    LaunchedEffect(Unit) {
        isCheckingUpdate = true
        latestRelease = UpdateManager.checkForUpdate()
        isCheckingUpdate = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
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
            // --- APPEARANCE ---
            item { SettingSectionHeader(stringResource(R.string.settings_category_appearance)) }
            
            item {
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
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        showThemeModeDialog = true
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            // --- PLAYER ---
            item { SettingSectionHeader(stringResource(R.string.settings_category_player)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pip_enabled_title)) },
                    supportingContent = { Text(stringResource(R.string.pip_enabled_summary)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pip_mode),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isPipEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    SettingsManager.setPipEnabled(context, enabled)
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch {
                            SettingsManager.setPipEnabled(context, !isPipEnabled)
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.audio_only_background_title)) },
                    supportingContent = { Text(stringResource(R.string.audio_only_background_summary)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_headset),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isAudioBackgroundEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    SettingsManager.setAudioOnlyBackgroundEnabled(context, enabled)
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch {
                            SettingsManager.setAudioOnlyBackgroundEnabled(context, !isAudioBackgroundEnabled)
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.ad_block_mode_title)) },
                    supportingContent = {
                        Text(
                            when (adBlockMode) {
                                SettingsManager.AdBlockMode.VAFT -> stringResource(R.string.ad_block_mode_vaft)
                                SettingsManager.AdBlockMode.VIDEO_SWAP -> stringResource(R.string.ad_block_mode_video_swap)
                            }
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ad_block),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        showAdBlockDialog = true
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            // --- CHAT ---
            item { SettingSectionHeader(stringResource(R.string.settings_category_chat)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.chat_mode_title)) },
                    supportingContent = {
                        Text(
                            when (chatMode) {
                                SettingsManager.ChatMode.NATIVE -> stringResource(R.string.chat_mode_native)
                                SettingsManager.ChatMode.LEGACY -> stringResource(R.string.chat_mode_legacy)
                            }
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        showChatModeDialog = true
                    }
                )
            }

            item {
                val isNative = chatMode == SettingsManager.ChatMode.NATIVE
                
                ListItem(
                    headlineContent = { 
                        Text(
                            text = stringResource(R.string.bttv_settings_title),
                            color = if (isNative) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
                        ) 
                    },
                    supportingContent = { 
                        Text(
                            text = if (isNative) stringResource(R.string.chat_mode_notice) else stringResource(R.string.bttv_settings_summary),
                            color = if (isNative) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else Color.Unspecified
                        ) 
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bttv),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isNative) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
                        )
                    },
                    modifier = Modifier.clickable(enabled = !isNative) {
                        isBttvSettingsOpen = true
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), thickness = 0.5.dp) }

            // --- APP ---
            item { SettingSectionHeader(stringResource(R.string.settings_category_app)) }

            if (BuildConfig.UPDATES_ENABLED) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.check_for_updates)) },
                        supportingContent = {
                            if (isCheckingUpdate) {
                                Text(stringResource(R.string.checking_updates))
                            } else if (isDownloading) {
                                Text(stringResource(R.string.update_download_description))
                            } else if (latestRelease != null) {
                                Text(stringResource(R.string.new_version_available,
                                    latestRelease?.tagName ?: ""))
                            } else {
                                Text(stringResource(R.string.app_up_to_date, BuildConfig.VERSION_NAME))
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            if (latestRelease != null) {
                                Button(
                                    onClick = {
                                        latestRelease?.let { release ->
                                            if (release.assets.any { it.name.endsWith("full.apk") }) {
                                                isDownloading = true
                                                UpdateManager.downloadAndInstall(context, release)
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("No Full APK found in release")
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text(stringResource(R.string.update_button))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.clickable(
                            enabled = !isCheckingUpdate && !isDownloading
                        ) {
                            scope.launch {
                                isCheckingUpdate = true
                                latestRelease = UpdateManager.checkForUpdate()
                                if (latestRelease == null) {
                                    snackbarHostState.showSnackbar("No updates found or error occurred")
                                }
                                isCheckingUpdate = false
                            }
                        }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about_title)) },
                    supportingContent = { Text(stringResource(R.string.about_summary)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        showAboutDialog = true
                    }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = isBttvSettingsOpen,
        enter = SamtchAnimation.ScreenEnterTransition,
        exit = SamtchAnimation.ScreenExitTransition
    ) {
        BttvSettingsScreen(
            onBack = { isBttvSettingsOpen = false }
        )
    }

    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text(stringResource(R.string.theme_mode_title)) },
            text = {
                Column {
                    SettingsManager.ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        SettingsManager.setThemeMode(context, mode)
                                        showThemeModeDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    scope.launch {
                                        SettingsManager.setThemeMode(context, mode)
                                        showThemeModeDialog = false
                                    }
                                }
                            )
                            Text(
                                text = when (mode) {
                                    SettingsManager.ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                    SettingsManager.ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                    SettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeModeDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (showAboutDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.about_dialog_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                    Text(stringResource(R.string.about_dialog_description))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.github_repo)) },
                        supportingContent = { Text(stringResource(R.string.github_repo_summary)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/akumasdk/Samtch")
                        }
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.support_project)) },
                        supportingContent = { Text(stringResource(R.string.support_project_summary)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_donation),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://ko-fi.com/akumasdk")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close_button))
                }
            }
        )
    }

    if (showChatModeDialog) {
        AlertDialog(
            onDismissRequest = { showChatModeDialog = false },
            title = { Text(stringResource(R.string.chat_mode_title)) },
            text = {
                Column {
                    SettingsManager.ChatMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        SettingsManager.setChatMode(context, mode)
                                        showChatModeDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = chatMode == mode,
                                onClick = {
                                    scope.launch {
                                        SettingsManager.setChatMode(context, mode)
                                        showChatModeDialog = false
                                    }
                                }
                            )
                            Text(
                                text = when (mode) {
                                    SettingsManager.ChatMode.NATIVE -> stringResource(R.string.chat_mode_native)
                                    SettingsManager.ChatMode.LEGACY -> stringResource(R.string.chat_mode_legacy)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = stringResource(R.string.chat_mode_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatModeDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    if (showAdBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAdBlockDialog = false },
            title = { Text(stringResource(R.string.ad_block_mode_title)) },
            text = {
                Column {
                    SettingsManager.AdBlockMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        SettingsManager.setAdBlockMode(context, mode)
                                        showAdBlockDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = adBlockMode == mode,
                                onClick = {
                                    scope.launch {
                                        SettingsManager.setAdBlockMode(context, mode)
                                        showAdBlockDialog = false
                                    }
                                }
                            )
                            Text(
                                text = when (mode) {
                                    SettingsManager.AdBlockMode.VAFT -> stringResource(R.string.ad_block_mode_vaft)
                                    SettingsManager.AdBlockMode.VIDEO_SWAP -> stringResource(R.string.ad_block_mode_video_swap)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text(
                        text = stringResource(R.string.ad_block_mode_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdBlockDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            .fillMaxWidth(),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}
