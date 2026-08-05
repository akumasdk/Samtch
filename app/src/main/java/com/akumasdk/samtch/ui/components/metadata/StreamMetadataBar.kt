package com.akumasdk.samtch.ui.components.metadata

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var isSlim by rememberSaveable { mutableStateOf(false) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isSlim && !forceExpanded) 38.dp else 68.dp,
        animationSpec = SamtchAnimation.DpSpring,
        label = "MetadataBarHeight"
    )

    // Manual expansion / Title change / Force expansion
    LaunchedEffect(expandTrigger, streamTitle, forceExpanded) {
        isSlim = false
    }

    // Auto-shrink timer (Resets on expansion/trigger/title)
    LaunchedEffect(isSlim, expandTrigger, streamTitle, forceExpanded) {
        if (!isSlim && !forceExpanded) {
            delay(15.seconds)
            isSlim = true
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clickable { 
                if (isSlim) {
                    isSlim = false
                } else {
                    onClick()
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
