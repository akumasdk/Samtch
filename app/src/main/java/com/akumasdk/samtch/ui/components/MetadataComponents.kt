package com.akumasdk.samtch.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import java.time.Duration
import java.time.Instant

/**
 * Formats large viewer counts into human-readable strings (e.g., 1.2k, 1.5M).
 */
fun formatViewerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000 -> "%.1fk".format(count / 1_000f)
        else -> count.toString()
    }
}

/**
 * Formats ISO 8601 timestamp into a human-readable duration string (e.g., 2h 15m).
 */
fun formatStreamDuration(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val start = Instant.parse(createdAt)
        val now = Instant.now()
        val duration = Duration.between(start, now)
        
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        
        when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "Just started"
        }
    } catch (e: Exception) {
        ""
    }
}

/**
 * A viewer count component with a red dot and a vertical scroll animation when the value updates.
 */
@Composable
fun AnimatedViewerCount(
    count: Int,
    textColor: Color = Color.LightGray,
    fontSize: TextUnit = 10.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Person Icon for Viewers
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        
        // Animated text content
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn(animationSpec = SamtchAnimation.FastTween))
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = SamtchAnimation.FastTween))
                } else {
                    (slideInVertically { height -> -height } + fadeIn(animationSpec = SamtchAnimation.FastTween))
                        .togetherWith(slideOutVertically { height -> height } + fadeOut(animationSpec = SamtchAnimation.FastTween))
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "ViewerCountAnimation",
            contentAlignment = Alignment.CenterStart
        ) { targetCount ->
            Text(
                text = formatViewerCount(targetCount),
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 10.sp
                )
            )
        }
    }
}

/**
 * A unified metadata bar for displaying streamer name, title, category and viewers.
 */
@Composable
fun StreamMetadataBar(
    channel: String,
    displayName: String? = null,
    avatarUrl: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    streamStartedAt: String? = null,
    expandTrigger: Int = 0,
    forceExpanded: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var isSlim by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isSlim && !forceExpanded) 38.dp else 68.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "MetadataBarHeight"
    )

    // Manual expansion / Title change / Force expansion
    LaunchedEffect(expandTrigger, streamTitle, forceExpanded) {
        if (forceExpanded) {
            isSlim = false
        } else {
            isSlim = false
        }
    }

    // Auto-shrink timer (Resets on expansion/trigger/title)
    LaunchedEffect(isSlim, expandTrigger, streamTitle, forceExpanded) {
        if (!isSlim && !forceExpanded) {
            delay(15.seconds)
            isSlim = true
        }
    }

    if (showInfoDialog) {
        StreamInfoDialog(
            channel = channel,
            displayName = displayName,
            avatarUrl = avatarUrl,
            streamTitle = streamTitle,
            gameName = gameName,
            viewersCount = viewersCount,
            streamStartedAt = streamStartedAt,
            onDismiss = { showInfoDialog = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clickable { 
                if (isSlim) {
                    isSlim = false
                } else {
                    showInfoDialog = true
                }
            },
        color = Color(0xFF1F1F23), // Twitch dark gray
        tonalElevation = 2.dp
    ) {
        AnimatedContent(
            targetState = isSlim,
            transitionSpec = {
                SamtchAnimation.FadeIn togetherWith SamtchAnimation.FadeOut
            },
            label = "MetadataBarStyleTransition"
        ) { slimMode ->
            if (slimMode) {
                // SLIM LAYOUT (Single line)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName ?: channel,
                        color = Color(0xFFBF94FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    
                    Text(text = ": ", color = Color.Gray, fontSize = 13.sp)
                    
                    Text(
                        text = streamTitle ?: "Stream Offline",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    if (viewersCount > 0) {
                        AnimatedViewerCount(
                            count = viewersCount,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                // STANDARD LAYOUT (3 Column Design)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // COLUMN 1: Streamer Avatar (40dp)
                    Box(modifier = Modifier.size(40.dp)) {
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
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF9146FF),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (displayName ?: channel).take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // COLUMN 2: Info Column (Flexible weight)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName ?: channel,
                            color = Color(0xFFBF94FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = streamTitle ?: "Stream Offline",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!gameName.isNullOrEmpty()) {
                            Text(
                                text = gameName,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // COLUMN 3: Stats Column
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val duration = formatStreamDuration(streamStartedAt)
                        if (duration.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Text(
                                    text = duration,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (viewersCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedViewerCount(
                                count = viewersCount,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamInfoDialog(
    channel: String,
    displayName: String? = null,
    avatarUrl: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    streamStartedAt: String? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(text = displayName ?: channel)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title
                InfoItem(
                    label = stringResource(R.string.stream_title_label),
                    value = streamTitle ?: "Offline",
                    icon = Icons.Default.Schedule
                )

                // Category
                if (!gameName.isNullOrEmpty()) {
                    InfoItem(
                        label = stringResource(R.string.category_label),
                        value = gameName,
                        icon = Icons.Default.Gamepad
                    )
                }

                // Stats
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(
                            label = stringResource(R.string.viewers_label),
                            value = formatViewerCount(viewersCount),
                            icon = Icons.Default.Person
                        )
                    }
                    val duration = formatStreamDuration(streamStartedAt)
                    if (duration.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoItem(
                                label = stringResource(R.string.uptime_label),
                                value = duration,
                                icon = Icons.Default.Schedule
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * A banner that shows adblock status, styled similarly to the metadata bar.
 */
@Composable
fun AdblockBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text.isNotEmpty(),
        enter = expandVertically(animationSpec = SamtchAnimation.springInteractive()) + fadeIn(),
        exit = shrinkVertically(animationSpec = SamtchAnimation.springInteractive()) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
