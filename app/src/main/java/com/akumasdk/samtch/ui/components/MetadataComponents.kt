package com.akumasdk.samtch.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import com.akumasdk.samtch.util.formatStreamDuration
import com.akumasdk.samtch.util.formatViewerCount

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
    previewImageUrl: String? = null,
    expandTrigger: Int = 0,
    forceExpanded: Boolean = false,
    isPip: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var isSlim by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Close dialog when entering PiP
    LaunchedEffect(isPip) {
        if (isPip) {
            showInfoDialog = false
        }
    }

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
            previewImageUrl = previewImageUrl,
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
        color = SamtchTheme.colors.dialogBackground,
        tonalElevation = 2.dp
    ) {
        AnimatedContent(
            targetState = isSlim,
            transitionSpec = {
                SamtchAnimation.FadeIn togetherWith SamtchAnimation.FadeOut
            },
            label = "MetadataBarStyleTransition",
            contentAlignment = Alignment.TopStart
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
                        color = SamtchTheme.colors.twitchPurpleLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    
                    Text(text = ": ", color = SamtchTheme.colors.secondaryText, fontSize = 13.sp)
                    
                    Text(
                        text = streamTitle ?: "Stream Offline",
                        color = SamtchTheme.colors.primaryText,
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
                // REDESIGNED STANDARD LAYOUT (High information density)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Avatar Section
                    Box(modifier = Modifier.size(46.dp)) {
                        if (!avatarUrl.isNullOrEmpty()) {
                            SubcomposeAsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        color = SamtchTheme.colors.accentColor,
                                        strokeWidth = 2.dp
                                    )
                                }
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = SamtchTheme.colors.twitchPurple,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color.White.copy(alpha = 0.7f),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = (displayName ?: channel).take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 2. Multi-row Info Block
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                    ) {
                        // ROW 1: Name & Uptime
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayName ?: channel,
                                color = SamtchTheme.colors.twitchPurpleLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                modifier = Modifier.weight(1f)
                            )

                            val duration = formatStreamDuration(streamStartedAt)
                            if (duration.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(SamtchTheme.colors.liveDot, CircleShape)
                                    )
                                    Text(
                                        text = duration,
                                        color = SamtchTheme.colors.secondaryText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                }
                            }
                        }

                        // ROW 2: Stream Title (Main Focus)
                        Text(
                            text = streamTitle ?: "Stream Offline",
                            color = SamtchTheme.colors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )

                        // ROW 3: Game Category & Viewers
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!gameName.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = SamtchTheme.colors.twitchPurpleLight.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp) // Matched size to text optics
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = gameName,
                                    color = SamtchTheme.colors.twitchPurpleLight.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .basicMarquee(iterations = Int.MAX_VALUE)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            if (viewersCount > 0) {
                                Box(modifier = Modifier.padding(start = 8.dp)) {
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

@Composable
private fun InfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    maxLines: Int = 2
) {
    Surface(
        color = SamtchTheme.colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SamtchTheme.colors.primaryText.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = SamtchTheme.colors.accentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SamtchTheme.colors.primaryText.copy(alpha = 0.5f),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp
                ),
                fontWeight = FontWeight.Bold,
                color = SamtchTheme.colors.primaryText,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
