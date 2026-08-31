package com.akumasdk.samtchtv.ui

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.painter.ColorPainter
import coil.compose.AsyncImage
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.data.settings.SettingsManager
import kotlinx.coroutines.launch
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.metadata.formatStreamDuration
import com.akumasdk.samtch.util.metadata.formatViewerCount

@Composable
fun LandingScreen(onChannelSelected: (String) -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var streamerName by remember { mutableStateOf("") }
    var isCheckingStreamer by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val recentStreamers by SettingsManager.getRecentStreamers(context).collectAsState(initial = emptyList())
    val focusRequester = remember { FocusRequester() }

    val startWatching = {
        val sanitized = streamerName.trim()
        if (sanitized.isNotBlank() && !isCheckingStreamer) {
            keyboardController?.hide()
            scope.launch {
                isCheckingStreamer = true
                val metadata = TwitchGqlService.getStreamMetadata(sanitized)
                val user = metadata?.user
                if (user?.stream != null) {
                    // Stream is live, start playing
                    SettingsManager.addRecentStreamer(context, sanitized)
                    onChannelSelected(sanitized)
                } else if (user != null) {
                    // Streamer exists but offline
                    SettingsManager.addRecentStreamer(context, sanitized)
                    errorMessage = "${user.displayName} is currently offline"
                } else {
                    // GQL returned null user or error
                    errorMessage = "Streamer not found or GQL error"
                }
                isCheckingStreamer = false
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SamtchTheme.colors.audioPlayerBackgroundStart, SamtchTheme.colors.rootBackground)
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Side: Search & Branding
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Samtch TV",
                    style = MaterialTheme.typography.displayMedium,
                    color = SamtchTheme.colors.accentColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = streamerName,
                    onValueChange = { 
                        streamerName = it.replace(" ", "")
                        errorMessage = null 
                    },
                    label = { Text("Streamer Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { startWatching() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SamtchTheme.colors.primaryText,
                        unfocusedTextColor = SamtchTheme.colors.primaryText,
                        focusedBorderColor = SamtchTheme.colors.accentColor,
                        unfocusedBorderColor = SamtchTheme.colors.divider,
                        focusedLabelColor = SamtchTheme.colors.accentColor,
                        unfocusedLabelColor = SamtchTheme.colors.secondaryText,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        errorBorderColor = SamtchTheme.colors.error,
                        errorLabelColor = SamtchTheme.colors.error,
                        errorSupportingTextColor = SamtchTheme.colors.error
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = startWatching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isCheckingStreamer,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamtchTheme.colors.accentColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        focusedElevation = 16.dp
                    )
                ) {
                    if (isCheckingStreamer) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Start Watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Right Side: History
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(horizontal = 48.dp, vertical = 64.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "RECENT CHANNELS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recentStreamers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No history yet", color = Color.DarkGray, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recentStreamers) { streamer ->
                            HistoryItem(
                                streamer = streamer, 
                                onClick = {
                                    keyboardController?.hide()
                                    scope.launch {
                                        SettingsManager.addRecentStreamer(context, streamer)
                                        onChannelSelected(streamer)
                                    }
                                },
                                onRemove = {
                                    scope.launch {
                                        SettingsManager.removeRecentStreamer(context, streamer)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(streamer: String, onClick: () -> Unit, onRemove: () -> Unit) {
    var metadata by remember { mutableStateOf<TwitchStreamMetadata?>(null) }
    var isPillFocused by remember { mutableStateOf(false) }
    var isRemoveFocused by remember { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(streamer) {
        val result = TwitchGqlService.getStreamMetadata(streamer)
        if (result != null) {
            metadata = result
        }
        hasLoaded = true
    }

    val stream = metadata?.user?.stream
    val isLive = stream != null

    val pillScale by animateFloatAsState(if (isPillFocused) 1.03f else 1f, label = "PillScale")
    val removeScale by animateFloatAsState(if (isRemoveFocused) 1.1f else 1f, label = "RemoveScale")
    
    val pillContainerColor by animateColorAsState(
        if (isPillFocused) SamtchTheme.colors.accentColor.copy(alpha = 0.15f) 
        else if (isLive) SamtchTheme.colors.cardBackground.copy(alpha = 0.8f)
        else SamtchTheme.colors.miniPlayerBackground.copy(alpha = 0.1f), // Dim for offline
        label = "PillColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Info Pill
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .scale(pillScale)
                .onFocusChanged { isPillFocused = it.isFocused }
                .clickable { onClick() }
                .focusable(),
            shape = RoundedCornerShape(36.dp), // Full pill shape
            color = pillContainerColor,
            border = if (isPillFocused) BorderStroke(2.dp, SamtchTheme.colors.accentColor) else null
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = metadata?.user?.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SamtchTheme.colors.cardBackground),
                        contentScale = ContentScale.Crop
                    )
                    if (isLive) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp),
                            shape = CircleShape,
                            color = SamtchTheme.colors.liveDot,
                            border = BorderStroke(2.dp, Color.White)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata?.user?.displayName ?: streamer,
                        color = if (isPillFocused) Color.White else SamtchTheme.colors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val gameName = stream?.game?.name
                    if (gameName != null) {
                        Text(
                            text = gameName,
                            color = if (isLive) SamtchTheme.colors.accentColor else SamtchTheme.colors.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (!hasLoaded) {
                        Text(
                            text = "Loading...",
                            color = SamtchTheme.colors.secondaryText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else if (metadata == null) {
                        Text(
                            text = "GQL Error",
                            color = SamtchTheme.colors.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else if (!isLive) {
                        Text(
                            text = "Offline",
                            color = SamtchTheme.colors.secondaryText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (isLive) {
                    Column(
                        horizontalAlignment = Alignment.End, 
                        modifier = Modifier.padding(end = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatViewerCount(stream?.viewersCount ?: 0),
                            color = SamtchTheme.colors.primaryText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = formatStreamDuration(stream?.createdAt),
                            color = SamtchTheme.colors.accentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Side Remove Button
        Surface(
            modifier = Modifier
                .size(72.dp)
                .scale(removeScale)
                .onFocusChanged { isRemoveFocused = it.isFocused }
                .clickable { onRemove() }
                .focusable(),
            shape = CircleShape,
            color = if (isRemoveFocused) SamtchTheme.colors.error.copy(alpha = 0.2f) else SamtchTheme.colors.tabButtonBackground,
            border = if (isRemoveFocused) BorderStroke(2.dp, SamtchTheme.colors.error) else null
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = if (isRemoveFocused) SamtchTheme.colors.error else SamtchTheme.colors.primaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/* Shared metadata utils used instead */
