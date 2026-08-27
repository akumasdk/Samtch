package com.akumasdk.samtch.ui.components.metadata

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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
    forceSlim: Boolean = false,
    isImmersiveEnabled: Boolean = true,
    onClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var isSlimManual by rememberSaveable { mutableStateOf(false) }
    // forceSlim (keyboard/emotes) takes absolute precedence over forceExpanded (Chat Only mode)
    val isSlim = forceSlim || (isSlimManual && !forceExpanded)

    val animatedHeight by animateDpAsState(
        targetValue = if (isSlim) 38.dp else 68.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "MetadataBarHeight"
    )

    val animatedTopPadding by animateDpAsState(
        targetValue = if (isSlim) 0.dp else 8.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "MetadataTopPadding"
    )

    val animatedHorizontalPadding by animateDpAsState(
        targetValue = if (isSlim) 0.dp else 12.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "MetadataHorizontalPadding"
    )

    val animatedTopRadius by animateDpAsState(
        targetValue = if (isSlim) 0.dp else 16.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "MetadataTopRadius"
    )

    // Manual expansion / Title change / Force expansion
    LaunchedEffect(expandTrigger, streamTitle, forceExpanded) {
        isSlimManual = false
    }

    // Auto-shrink timer (Resets on expansion/trigger/title)
    LaunchedEffect(isSlimManual, expandTrigger, streamTitle, forceExpanded, forceSlim) {
        if (!isSlimManual && !forceExpanded && !forceSlim) {
            delay(15.seconds)
            isSlimManual = true
        }
    }

    val isLightMode = SamtchTheme.colors.dialogBackground.luminance() > 0.5f
    // Even if immersive is enabled globally, we force standard colors for the metadata bar in light theme
    val isImmersiveActuallyEnabled = isImmersiveEnabled && !isLightMode

    val surfaceAlpha = if (isImmersiveActuallyEnabled) 0.82f else 1.0f
    
    // Minimal image alpha for very subtle color accents
    val imageAlpha = 0.7f

    Surface(
        modifier = modifier
            .padding(
                start = animatedHorizontalPadding.coerceAtLeast(0.dp),
                top = animatedTopPadding.coerceAtLeast(0.dp),
                end = animatedHorizontalPadding.coerceAtLeast(0.dp),
                bottom = 8.dp
            )
            .fillMaxWidth()
            .height(animatedHeight),
        shape = RoundedCornerShape(
            topStart = animatedTopRadius.coerceAtLeast(0.dp),
            topEnd = animatedTopRadius.coerceAtLeast(0.dp),
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        ),
        color = SamtchTheme.colors.dialogBackground.copy(alpha = surfaceAlpha),
        border = if (isImmersiveActuallyEnabled) {
            BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f))
        } else {
            BorderStroke(0.5.dp, SamtchTheme.colors.divider.copy(alpha = 0.3f))
        },
        tonalElevation = if (isImmersiveActuallyEnabled) 0.dp else 2.dp
    ) {
        if (isImmersiveActuallyEnabled) {
            PlayerBackground(
                previewUrl = previewImageUrl,
                alpha = imageAlpha,
                blurRadius = 150.dp,
                contentScale = ContentScale.FillBounds,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        AnimatedContent(
            targetState = isSlim,
            modifier = Modifier.clickable { 
                if (isSlim) {
                    isSlimManual = false
                } else {
                    onClick()
                }
            },
            transitionSpec = {
                SamtchAnimation.FadeIn togetherWith SamtchAnimation.FadeOut
            },
            label = "MetadataBarStyleTransition",
            contentAlignment = Alignment.TopStart
        ) { slimMode ->
            if (slimMode) {
                SlimMetadataBar(
                    channel = channel,
                    displayName = displayName,
                    streamTitle = streamTitle,
                    viewersCount = viewersCount,
                    streamStartedAt = streamStartedAt
                )
            } else {
                StandardMetadataBar(
                    channel = channel,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    streamTitle = streamTitle,
                    gameName = gameName,
                    viewersCount = viewersCount,
                    streamStartedAt = streamStartedAt
                )
            }
        }
    }
}
