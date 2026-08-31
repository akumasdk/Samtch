package com.akumasdk.samtch.ui.components.metadata

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.model.TwitchUser
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.metadata.formatDate
import com.akumasdk.samtch.util.metadata.formatStreamDuration
import com.akumasdk.samtch.util.metadata.formatViewerCount
import com.akumasdk.samtch.util.Constants

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
    user: TwitchUser? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.akumasdk.samtch.util.MaintainFullscreenEffect()
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
                PlayerBackground(
                    previewUrl = previewImageUrl,
                    alpha = 1.0f, // Using gradient for transparency control
                    blurRadius = 150.dp,
                    containerColor = Color.Transparent,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                ) {
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
                            val finalAvatarUrl = avatarUrl ?: user?.profileImageUrl
                            if (!finalAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = finalAvatarUrl,
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
                                        text = (displayName ?: user?.displayName ?: channel).take(1).uppercase(),
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
                                text = displayName ?: user?.displayName ?: channel,
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
                            isScrollable = true
                        )

                        if (!gameName.isNullOrEmpty()) {
                            InfoCard(
                                label = stringResource(R.string.category_label),
                                value = gameName,
                                icon = Icons.Default.Gamepad
                            )
                        }

                        val description = user?.description?.trim()
                        if (!description.isNullOrEmpty()) {
                            InfoCard(
                                label = stringResource(R.string.description_label),
                                value = description,
                                icon = Icons.Default.Person,
                                isScrollable = true
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

                        // User Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (user != null && user.followersTotal > 0) {
                                Box(modifier = Modifier.weight(1f)) {
                                    InfoCard(
                                        label = stringResource(R.string.followers_label),
                                        value = formatViewerCount(user.followersTotal),
                                        icon = Icons.Default.Group
                                    )
                                }
                            }

                            val created = formatDate(user?.createdAt)
                            if (created.isNotEmpty()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    InfoCard(
                                        label = stringResource(R.string.created_at_label),
                                        value = created,
                                        icon = Icons.Default.History
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Open in External Browser
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "${Constants.Twitch.BASE_URL}/$channel".toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SamtchTheme.colors.accentColor.copy(alpha = 0.1f),
                                contentColor = SamtchTheme.colors.accentColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, SamtchTheme.colors.accentColor.copy(alpha = 0.2f))
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.open_external_button),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

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
}
