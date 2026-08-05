package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
internal fun SlimMetadataBar(
    channel: String,
    displayName: String?,
    streamTitle: String?,
    viewersCount: Int
) {
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
}
