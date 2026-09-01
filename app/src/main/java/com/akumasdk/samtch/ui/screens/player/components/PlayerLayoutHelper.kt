package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode

data class PlayerLayoutDimensions(
    val height: State<Dp>,
    val width: State<Dp>,
    val paddingStart: State<Dp>,
    val paddingBottom: State<Dp>,
    val cornerRadius: State<Dp>,
    val elevation: State<Dp>
)

@Composable
fun rememberPlayerLayoutDimensions(
    isMinimized: Boolean,
    isAudioOnly: Boolean,
    isFullscreen: Boolean,
    portraitMode: PortraitMode,
    isPip: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    isChatVisible: Boolean,
    chatRatio: Float = 0.28f
): PlayerLayoutDimensions {
    val isChatOnly = portraitMode == PortraitMode.CHAT_ONLY && !isPip
    val animationSpec = if (isChatOnly) snap<Dp>() else SamtchAnimation.morphSpring()

    val height = animateDpAsState(
        targetValue = when {
            isMinimized -> 64.dp
            isAudioOnly -> 240.dp
            isFullscreen -> screenHeight
            isChatOnly -> 0.dp
            else -> (screenWidth * 9 / 16)
        },
        animationSpec = animationSpec,
        label = "PlayerHeight"
    )

    val width = animateDpAsState(
        targetValue = when {
            isMinimized -> 120.dp
            isFullscreen && isChatVisible -> screenWidth * (1f - chatRatio)
            isChatOnly -> 0.dp
            else -> screenWidth
        },
        animationSpec = animationSpec,
        label = "PlayerWidth"
    )

    val paddingStart = animateDpAsState(
        targetValue = if (isMinimized) 24.dp else 0.dp,
        animationSpec = animationSpec,
        label = "PlayerPaddingStart"
    )

    val paddingBottom = animateDpAsState(
        targetValue = if (isMinimized) 20.dp else 0.dp,
        animationSpec = animationSpec,
        label = "PlayerPaddingBottom"
    )

    val cornerRadius = animateDpAsState(
        targetValue = if (isMinimized) 40.dp else 0.dp,
        animationSpec = animationSpec,
        label = "PlayerCornerRadius"
    )

    val elevation = animateDpAsState(
        targetValue = if (isMinimized) 12.dp else 0.dp,
        animationSpec = animationSpec,
        label = "PlayerElevation"
    )

    return PlayerLayoutDimensions(height, width, paddingStart, paddingBottom, cornerRadius, elevation)
}
