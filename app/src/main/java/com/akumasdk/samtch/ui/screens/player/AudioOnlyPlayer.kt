package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.components.AnimatedViewerCount

@Composable
fun AudioOnlyPlayer(
    channel: String,
    avatarUrl: String?,
    subtitle: String?,
    displayName: String? = null,
    streamTitle: String? = null,
    gameName: String? = null,
    viewersCount: Int = 0,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onCloseAudioOnly: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F1F23), // Twitch dark gray
                        Color(0xFF0E0E10)  // Almost black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val availableHeight = maxHeight
        val isMini = availableHeight < 100.dp
        
        if (isMini) {
            // Simplified view for MiniPlayer bar
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (displayName ?: channel).take(1).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Table Design: Left (Avatar), Center (Info), Right (Controls)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT: Avatar
                Box(
                    modifier = Modifier
                        .size(if (availableHeight < 200.dp) 70.dp else 100.dp)
                        .border(2.dp, Color(0xFF9146FF), CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
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
                        Text(
                            text = (displayName ?: channel).take(1).uppercase(),
                            color = Color.White,
                            fontSize = if (availableHeight < 200.dp) 24.sp else 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // CENTER: Info (Flexible)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayName ?: channel,
                        color = Color(0xFFBF94FF), // Unified purple accent
                        fontSize = if (availableHeight < 200.dp) 18.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onCloseAudioOnly() }
                    )
                    
                    // Detailed Stream Title
                    val displayTitle = streamTitle ?: subtitle
                    if (!displayTitle.isNullOrEmpty()) {
                        Text(
                            text = displayTitle,
                            color = Color.White.copy(alpha = 0.9f), // Unified white/alpha title
                            fontSize = if (availableHeight < 200.dp) 12.sp else 14.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Game/Category Pill
                        if (!gameName.isNullOrEmpty()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = gameName,
                                    color = Color(0xFFBF94FF), // Unified light purple category
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Viewer Count (Animated)
                        if (viewersCount > 0) {
                            AnimatedViewerCount(
                                count = viewersCount,
                                fontSize = 10.sp,
                                dotSize = 6.dp
                            )
                        }

                        Surface(
                            color = Color(0xFF9146FF).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { onCloseAudioOnly() }
                        ) {
                            Text(
                                text = "AUDIO ONLY",
                                color = Color(0xFFBF94FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // RIGHT: Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Return to Video (Close)
                    IconButton(
                        onClick = onCloseAudioOnly,
                        modifier = Modifier.size(if (availableHeight < 200.dp) 32.dp else 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Return to Video",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (availableHeight < 200.dp) 20.dp else 24.dp)
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier.size(if (availableHeight < 200.dp) 56.dp else 64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Refresh
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(if (availableHeight < 200.dp) 32.dp else 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (availableHeight < 200.dp) 18.dp else 22.dp)
                        )
                    }
                }
            }
            
            // Subtle Hint at the very bottom if space allows
            if (availableHeight > 250.dp) {
                Text(
                    text = stringResource(R.string.audio_only_hint),
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}


