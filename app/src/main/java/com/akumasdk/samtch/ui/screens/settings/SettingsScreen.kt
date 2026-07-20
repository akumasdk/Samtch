package com.akumasdk.samtch.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.Info
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
import com.akumasdk.samtch.util.GitHubRelease
import com.akumasdk.samtch.util.SettingsManager
import com.akumasdk.samtch.util.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var isBttvSettingsOpen by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isBackgroundPlayEnabled by SettingsManager.isBackgroundPlayEnabled(context).collectAsState(initial = false)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                scope.launch {
                    SettingsManager.setBackgroundPlayEnabled(context, true)
                }
            }
        }
    )

    // Intercept system back button
    BackHandler {
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.bttv_settings_title)) },
                    supportingContent = { Text(stringResource(R.string.bttv_settings_summary)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bttv),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified // Keep original BetterTTV colors
                        )
                    },
                    modifier = Modifier.clickable {
                        isBttvSettingsOpen = true
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.bg_play_title)) },
                    supportingContent = { Text(stringResource(R.string.bg_play_summary)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh), // TODO: Better icon
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isBackgroundPlayEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scope.launch {
                                            SettingsManager.setBackgroundPlayEnabled(context, true)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        SettingsManager.setBackgroundPlayEnabled(context, enabled)
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        val newEnabled = !isBackgroundPlayEnabled
                        if (newEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                scope.launch {
                                    SettingsManager.setBackgroundPlayEnabled(context, true)
                                }
                            }
                        } else {
                            scope.launch {
                                SettingsManager.setBackgroundPlayEnabled(context, newEnabled)
                            }
                        }
                    }
                )
            }

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
                                contentDescription = null
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
                            contentDescription = null
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
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        BttvSettingsScreen(
            onBack = { isBttvSettingsOpen = false }
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
}
