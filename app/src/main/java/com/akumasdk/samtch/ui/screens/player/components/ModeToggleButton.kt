package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode

@Composable
fun ModeToggleButton(
    visible: Boolean,
    portraitMode: PortraitMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .padding(bottom = 100.dp, end = 16.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = SamtchTheme.colors.accentColor.copy(alpha = 0.8f),
            contentColor = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (portraitMode) {
                        PortraitMode.VIDEO_AND_CHAT, PortraitMode.AUDIO_AND_CHAT -> Icons.AutoMirrored.Filled.Chat
                        PortraitMode.CHAT_ONLY -> Icons.Default.SmartDisplay
                    },
                    contentDescription = "Toggle Mode",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
