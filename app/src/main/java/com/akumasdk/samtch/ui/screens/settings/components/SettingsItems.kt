package com.akumasdk.samtch.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.settings.SettingsManager

@Composable
fun ThemeModeItem(themeMode: SettingsManager.ThemeMode, onClick: () -> Unit, onReset: () -> Unit) {
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
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onReset)
    )
}

@Composable
fun ImmersiveBackgroundToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.immersive_background_title)) },
        supportingContent = { Text(stringResource(R.string.immersive_background_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_radial_blur), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.combinedClickable(onClick = { onToggle(!enabled) }, onLongClick = onReset)
    )
}

@Composable
fun PipToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.pip_enabled_title)) },
        supportingContent = { Text(stringResource(R.string.pip_enabled_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_pip_mode), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.combinedClickable(onClick = { onToggle(!enabled) }, onLongClick = onReset)
    )
}

@Composable
fun AudioBackgroundToggleItem(enabled: Boolean, onToggle: (Boolean) -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.audio_only_background_title)) },
        supportingContent = { Text(stringResource(R.string.audio_only_background_summary)) },
        leadingContent = { Icon(painter = painterResource(id = R.drawable.ic_headset), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        modifier = Modifier.combinedClickable(onClick = { onToggle(!enabled) }, onLongClick = onReset)
    )
}

@Composable
fun AdBlockModeItem(mode: SettingsManager.AdBlockMode, onClick: () -> Unit, onReset: () -> Unit) {
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
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onReset)
    )
}

@Composable
fun ChatModeItem(mode: SettingsManager.ChatMode, onClick: () -> Unit, onReset: () -> Unit) {
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
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onReset)
    )
}

@Composable
fun ChatFontSizeItem(size: Int, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_settings_font_size)) },
        supportingContent = { Text("${size}sp") },
        leadingContent = { Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onReset)
    )
}

@Composable
fun ChatEmoteSizeItem(size: Int, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_settings_emote_size)) },
        supportingContent = { Text("${size}dp") },
        leadingContent = { Icon(imageVector = Icons.Default.EmojiEmotions, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onReset)
    )
}

@Composable
fun ChatBadgeSizeItem(size: Int, onClick: () -> Unit, onReset: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.chat_settings_badge_size)) },
        supportingContent = { Text("${size}dp") },
        leadingContent = { Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onReset)
    )
}

@Composable
fun BttvSettingsItem(onClick: () -> Unit) {
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
fun LogoutItem(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.logout_title)) },
        supportingContent = { Text(stringResource(R.string.logout_summary)) },
        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun AboutItem(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.about_title)) },
        supportingContent = { Text(stringResource(R.string.about_summary)) },
        leadingContent = { Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable { onClick() }
    )
}
