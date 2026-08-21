package com.akumasdk.samtch.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager
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
    showLogoutDialog: Boolean,
    showAboutDialog: Boolean,
    themeMode: SettingsManager.ThemeMode,
    adBlockMode: SettingsManager.AdBlockMode,
    chatMode: SettingsManager.ChatMode,
    chatFontSize: Int,
    chatEmoteSize: Int,
    chatBadgeSize: Int,
    onDismissTheme: () -> Unit,
    onDismissAdBlock: () -> Unit,
    onDismissChatMode: () -> Unit,
    onDismissFontSize: () -> Unit,
    onDismissEmoteSize: () -> Unit,
    onDismissBadgeSize: () -> Unit,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Samtch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Version ${com.akumasdk.samtch.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.about_dialog_description))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button))
            }
        }
    )
}
