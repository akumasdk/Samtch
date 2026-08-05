package com.akumasdk.samtch.ui.components.metadata

import android.annotation.SuppressLint
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.components.metadata.util.formatStreamDuration
import com.akumasdk.samtch.ui.components.metadata.util.formatViewerCount

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun StreamInfoDialog(
    channel: String,
    displayName: String? = null,
    avatarUrl: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    streamStartedAt: String? = null,
    previewImageUrl: String? = null,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            color = SamtchTheme.colors.dialogBackground,
            tonalElevation = 12.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background Preview Image with enhanced gradient overlay
                if (!previewImageUrl.isNullOrEmpty()) {
                    Box(modifier = Modifier.matchParentSize()) {
                        AsyncImage(
                            model = previewImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Layered gradient for maximum readability across light/dark themes
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            SamtchTheme.colors.dialogBackground.copy(alpha = 0.6f),
                                            SamtchTheme.colors.dialogBackground.copy(alpha = 0.85f),
                                            SamtchTheme.colors.dialogBackground
                                        )
                                    )
                                )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    // Header Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, SamtchTheme.colors.twitchPurple),
                            modifier = Modifier.size(56.dp),
                            color = Color.Transparent
                        ) {
                            if (!avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(SamtchTheme.colors.twitchPurple),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (displayName ?: channel).take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName ?: channel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = SamtchTheme.colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = SamtchTheme.colors.accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "twitch.tv/$channel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SamtchTheme.colors.accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Stream Information Grid
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        // Detailed Info Cards
                        InfoCard(
                            label = stringResource(R.string.stream_title_label),
                            value = streamTitle ?: "Offline",
                            icon = Icons.Default.SmartDisplay,
                            maxLines = 4
                        )

                        if (!gameName.isNullOrEmpty()) {
                            InfoCard(
                                label = stringResource(R.string.category_label),
                                value = gameName,
                                icon = Icons.Default.Gamepad
                            )
                        }

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                InfoCard(
                                    label = stringResource(R.string.viewers_label),
                                    value = formatViewerCount(viewersCount),
                                    icon = Icons.Default.Person
                                )
                            }
                            
                            val duration = formatStreamDuration(streamStartedAt)
                            if (duration.isNotEmpty()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    InfoCard(
                                        label = stringResource(R.string.uptime_label),
                                        value = duration,
                                        icon = Icons.Default.Schedule
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Close Button - High emphasis
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SamtchTheme.colors.twitchPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.close_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
