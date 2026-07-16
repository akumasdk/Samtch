package com.akumasdk.samtch.ui.screens.settings

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.BuildConfig
import com.akumasdk.samtch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    // Intercept system back button
    BackHandler {
        if (showAboutDialog) {
            showAboutDialog = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                    headlineContent = { Text("About") },
                    supportingContent = { Text("App information and version") },
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

    if (showAboutDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Samtch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Text("An enhanced Twitch experience for Android.")
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ListItem(
                        headlineContent = { Text("GitHub Repository") },
                        supportingContent = { Text("View source code and releases") },
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
                        headlineContent = { Text("Support the Project") },
                        supportingContent = { Text("Donations via Ko-fi") },
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
                    Text("Close")
                }
            }
        )
    }
}
