package com.akumasdk.samtch.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun FullscreenChatToggle(
    visible: Boolean,
    isChatVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            color = SamtchTheme.colors.tabButtonBackground,
            contentColor = SamtchTheme.colors.primaryText,
            tonalElevation = 4.dp,
            modifier = Modifier.height(80.dp).width(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isChatVisible) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                    contentDescription = "Toggle Chat",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
