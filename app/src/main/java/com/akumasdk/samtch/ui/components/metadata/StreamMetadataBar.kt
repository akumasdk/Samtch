package com.akumasdk.samtch.ui.components.metadata

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    val surfaceAlpha = if (isImmersiveEnabled) 0.8f else 1.0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clickable { 
                if (isSlim) {
                    isSlimManual = false
                } else {
                    onClick()
                }
            },
        color = SamtchTheme.colors.dialogBackground.copy(alpha = surfaceAlpha),
        border = if (isImmersiveEnabled) BorderStroke(0.5.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.2f)) else null,
        tonalElevation = 0.dp
    ) {
        // No extra lighting effects for a cleaner Telegram-like look
        AnimatedContent(
            targetState = isSlim,
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
                    viewersCount = viewersCount
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
