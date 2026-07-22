package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AudioOnlyPlayer(
    channel: String,
    avatarUrl: String?,
    subtitle: String?,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onCloseAudioOnly: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF18181B)),
        contentAlignment = Alignment.Center
    ) {
        val availableHeight = maxHeight
        val isSmallSpace = availableHeight < 300.dp
        
        val avatarSize = if (isSmallSpace) 60.dp else 100.dp
        val titleSize = if (isSmallSpace) 18.sp else 22.sp
        val subtitleSize = if (isSmallSpace) 12.sp else 14.sp
        val controlSize = if (isSmallSpace) 48.dp else 64.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isSmallSpace) 8.dp else 16.dp)
        ) {
            // Avatar
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(if (isSmallSpace) 8.dp else 16.dp))
            }

            // Channel Name
            Text(
                text = channel,
                color = Color.White,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Subtitle / Stream Description
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = subtitleSize,
                    textAlign = TextAlign.Center,
                    maxLines = if (isSmallSpace) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallSpace) 4.dp else 8.dp))
            Text(
                text = "Audio Only Mode",
                color = Color(0xFF9146FF), // Twitch Purple
                fontSize = if (isSmallSpace) 10.sp else 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(if (isSmallSpace) 12.dp else 24.dp))
            
            // Playback Controls Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isSmallSpace) 16.dp else 32.dp)
            ) {
                // Refresh Button
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Stream",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(if (isSmallSpace) 24.dp else 32.dp)
                    )
                }

                // Play/Pause Button
                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(controlSize)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Spacer or additional control if needed
                Box(modifier = Modifier.size(if (isSmallSpace) 24.dp else 32.dp))
            }
        }
        
        // Close Button (Return to Video)
        IconButton(
            onClick = onCloseAudioOnly,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Return to Video",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
