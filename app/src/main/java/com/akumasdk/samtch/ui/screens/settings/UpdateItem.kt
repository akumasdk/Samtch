package com.akumasdk.samtch.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.model.GitHubRelease

@Composable
fun UpdateItem(
    latestRelease: GitHubRelease?,
    isChecking: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.check_for_updates)) },
        supportingContent = {
            Text(
                when {
                    isDownloading -> stringResource(R.string.update_download_description)
                    isChecking -> stringResource(R.string.checking_updates)
                    latestRelease != null -> stringResource(R.string.new_version_available, latestRelease.tagName)
                    else -> stringResource(R.string.app_up_to_date, com.akumasdk.samtch.BuildConfig.VERSION_NAME)
                }
            )
        },
        leadingContent = {
            if (isChecking || isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingContent = {
            if (latestRelease != null && !isDownloading && !isChecking) {
                Button(onClick = onClick) {
                    Text(stringResource(R.string.update_button))
                }
            }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
