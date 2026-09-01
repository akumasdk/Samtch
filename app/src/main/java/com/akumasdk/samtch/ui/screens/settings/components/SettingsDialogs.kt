package com.akumasdk.samtch.ui.screens.settings.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsDialogs(
    showThemeDialog: Boolean,
    showAdBlockDialog: Boolean,
    showChatModeDialog: Boolean,
    showChatFontSizeDialog: Boolean,
    showChatEmoteSizeDialog: Boolean,
    showChatBadgeSizeDialog: Boolean,
    showChatRatioDialog: Boolean,
    showLogoutDialog: Boolean,
    showAboutDialog: Boolean,
    themeMode: SettingsManager.ThemeMode,
    adBlockMode: SettingsManager.AdBlockMode,
    chatMode: SettingsManager.ChatMode,
    chatFontSize: Int,
    chatEmoteSize: Int,
    chatBadgeSize: Int,
    chatRatio: Int,
    onDismissTheme: () -> Unit,
    onDismissAdBlock: () -> Unit,
    onDismissChatMode: () -> Unit,
    onDismissFontSize: () -> Unit,
    onDismissEmoteSize: () -> Unit,
    onDismissBadgeSize: () -> Unit,
    onDismissChatRatio: () -> Unit,
    onDismissLogout: () -> Unit,
    onDismissAbout: () -> Unit,
    onLogout: () -> Unit,
    scope: CoroutineScope,
    context: android.content.Context
) {
    if (showThemeDialog) {
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
            onDismiss = onDismissTheme
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
            onDismiss = onDismissAdBlock
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
            onDismiss = onDismissChatMode
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
            onDismiss = onDismissFontSize
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
            onDismiss = onDismissEmoteSize
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
            onDismiss = onDismissBadgeSize
        )
    }

    if (showChatRatioDialog) {
        val ratioOptions = listOf(0, 15, 20, 25, 30, 35, 40, 45, 50)
        SelectionDialog(
            title = stringResource(R.string.fullscreen_chat_ratio_title),
            options = ratioOptions.map { ratio ->
                val label = if (ratio == 0) stringResource(R.string.fullscreen_chat_ratio_auto) else "$ratio%"
                label to { scope.launch { SettingsManager.setFullscreenChatRatio(context, ratio) } }
            },
            selectedIndex = ratioOptions.indexOf(chatRatio),
            onReset = { scope.launch { SettingsManager.setFullscreenChatRatio(context, 0) } },
            onDismiss = onDismissChatRatio
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(onConfirm = onLogout, onDismiss = onDismissLogout)
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = onDismissAbout)
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<Pair<String, () -> Unit>>,
    selectedIndex: Int,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                itemsIndexed(options) { index, (label, onClick) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClick()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onReset()
                onDismiss()
            }) {
                Text(stringResource(R.string.reset_default))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logout_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.logout_dialog_message)) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.logout_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Samtch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        stringResource(R.string.about_dialog_version, com.akumasdk.samtch.BuildConfig.VERSION_NAME, com.akumasdk.samtch.BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.about_dialog_description))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.github_repo)) },
                    supportingContent = { Text(stringResource(R.string.github_repo_summary)) },
                    leadingContent = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Constants.Links.GITHUB_REPO.toUri())
                        context.startActivity(intent)
                    }
                )
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.support_project)) },
                    supportingContent = { Text(stringResource(R.string.support_project_summary)) },
                    leadingContent = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_donation),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Constants.Links.DONATION_KOFI.toUri())
                        context.startActivity(intent)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button))
            }
        }
    )
}
