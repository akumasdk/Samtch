package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.TwitchPurpleLight
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * An ultra-slim floating indicator attached directly to the top-left edge of the video player
 * when ad-blocking is active. Automatically collapses and hides text into the top-left corner
 * after a few seconds.
 */
@Composable
fun AdBlockIndicator(
    adblockText: String,
    modifier: Modifier = Modifier,
    isMinimized: Boolean = false
) {
    val isVisible = adblockText.isNotEmpty() && !isMinimized
    var isCollapsed by remember { mutableStateOf(false) }

    // Auto-collapse towards top-left edge after 3.5 seconds
    LaunchedEffect(adblockText, isMinimized) {
        if (adblockText.isNotEmpty() && !isMinimized) {
            isCollapsed = false
            delay(3.5.seconds)
            isCollapsed = true
        } else {
            isCollapsed = false
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "AdBlockContentAlpha"
    )

    val surfaceColor by animateColorAsState(
        targetValue = if (isCollapsed) Color.Black.copy(alpha = 0.25f) else Color(0xEE0E0E10),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "AdBlockBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isCollapsed) TwitchPurpleLight.copy(alpha = 0.15f) else TwitchPurpleLight.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "AdBlockBorderColor"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + slideOutVertically(targetOffsetY = { -it }) + fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "AdBlockPulse")

        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseAlpha"
        )

        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseScale"
        )

        Surface(
            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 0.dp, bottomEnd = 10.dp),
            color = surfaceColor,
            border = BorderStroke(width = 1.dp, color = borderColor),
            tonalElevation = if (isCollapsed) 0.dp else 4.dp,
            shadowElevation = if (isCollapsed) 0.dp else 4.dp,
            modifier = Modifier
                .animateContentSize(animationSpec = SamtchAnimation.springInteractive())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tapping toggles expansion
                    isCollapsed = !isCollapsed
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = if (isCollapsed) 6.dp else 8.dp,
                        end = if (isCollapsed) 6.dp else 10.dp,
                        top = if (isCollapsed) 2.dp else 4.dp,
                        bottom = if (isCollapsed) 2.dp else 4.dp
                    )
                    .alpha(contentAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pulsing glowing status dot
                Box(
                    modifier = Modifier.size(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha * 0.5f)
                            .background(TwitchPurpleLight, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(TwitchPurpleLight, CircleShape)
                    )
                }

                // AdBlock Icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_ad_block),
                    contentDescription = null,
                    tint = TwitchPurpleLight,
                    modifier = Modifier.size(12.dp)
                )

                // Label Text - Collapses towards top-left edge after delay
                AnimatedVisibility(
                    visible = !isCollapsed,
                    enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally()
                ) {
                    Text(
                        text = adblockText,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
