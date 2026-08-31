package com.akumasdk.samtchtv.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akumasdk.samtch.data.api.gql.TwitchGqlService
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.data.settings.SettingsManager
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LandingScreen(onChannelSelected: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var streamerName by remember { mutableStateOf("") }
    val recentStreamers by SettingsManager.getRecentStreamers(context).collectAsState(initial = emptyList())
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F0F), Color(0xFF050505))
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
                    color = Color.Magenta,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = streamerName,
                    onValueChange = { streamerName = it.replace(" ", "") },
                    label = { Text("Streamer Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Magenta,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color.Magenta,
                        unfocusedLabelColor = Color.Gray,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val sanitized = streamerName.trim()
                        if (sanitized.isNotBlank()) {
                            scope.launch {
                                SettingsManager.addRecentStreamer(context, sanitized)
                                onChannelSelected(sanitized)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Magenta,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        focusedElevation = 16.dp
                    )
                ) {
                    Text("Start Watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(recentStreamers) { streamer ->
                            HistoryItem(streamer = streamer, onClick = {
                                scope.launch {
                                    SettingsManager.addRecentStreamer(context, streamer)
                                    onChannelSelected(streamer)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(streamer: String, onClick: () -> Unit) {
    var metadata by remember { mutableStateOf<TwitchStreamMetadata?>(null) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(streamer) {
        metadata = TwitchGqlService.getStreamMetadata(streamer)
    }

    val stream = metadata?.user?.stream
    val isLive = stream != null

    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f)
    val borderAlpha by animateFloatAsState(if (isFocused) 1f else 0f)
    val containerColor by animateColorAsState(
        if (isFocused) Color(0xFF3D3D3D) 
        else if (isLive) Color(0xFF252525) 
        else Color(0xFF181818)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .clickable { onClick() }
            .focusable(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Color.Magenta.copy(alpha = borderAlpha))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Glow if Live
            Box(contentAlignment = Alignment.Center) {
                if (isLive) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Color.Red.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, Color.Red)
                    ) {}
                }
                AsyncImage(
                    model = metadata?.user?.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.user?.displayName ?: streamer,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isLive) {
                    Text(
                        text = stream?.game?.name ?: "Streaming",
                        color = Color.Magenta.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Offline",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (isLive) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(6.dp),
                            shape = CircleShape,
                            color = Color.Red
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatViewers(stream?.viewersCount ?: 0),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatUptime(stream?.createdAt),
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatViewers(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000 -> "%.1fK".format(count / 1_000f)
        else -> count.toString()
    }
}

private fun formatUptime(createdAt: String?): String {
    if (createdAt == null) return "00:00"
    return try {
        val start = Instant.parse(createdAt)
        val now = Instant.now()
        val duration = Duration.between(start, now)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        if (hours > 0) {
            "%dh %dm".format(hours, minutes)
        } else {
            "%dm".format(minutes)
        }
    } catch (e: Exception) {
        "0m"
    }
}
