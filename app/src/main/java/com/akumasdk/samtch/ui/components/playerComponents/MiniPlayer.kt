package com.akumasdk.samtch.ui.components.playerComponents

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MiniPlayer(
    channel: String,
    displayName: String? = null,
    streamTitle: String? = null,
    playerContent: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    onClose: () -> Unit,
    showHint: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                onClose()
                true
            } else {
                false
            }
        }
    )

    // Nudge animation for first-time users
    val nudgeOffset = remember { Animatable(0f) }
    LaunchedEffect(showHint) {
        if (showHint) {
            delay(500.milliseconds)
            // Nudge right
            nudgeOffset.animateTo(
                targetValue = 40f,
                animationSpec = SamtchAnimation.springBouncy()
            )
            // Back to center
            nudgeOffset.animateTo(
                targetValue = 0f,
                animationSpec = SamtchAnimation.springInteractive()
            )
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.offset { IntOffset(nudgeOffset.value.roundToInt(), 0) },
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isSwiping = direction != SwipeToDismissBoxValue.Settled
            val progress = if (isSwiping) dismissState.progress else 0f
            
            val color by animateColorAsState(
                if (isSwiping) SamtchTheme.colors.error.copy(alpha = (0.1f + (0.3f * progress)).coerceIn(0f, 0.4f)) else Color.Transparent,
                animationSpec = SamtchAnimation.ColorTween,
                label = "DismissBackground"
            )

            val iconScale by animateFloatAsState(
                if (isSwiping) 0.8f + (0.4f * progress) else 0.5f,
                animationSpec = SamtchAnimation.springBouncy(),
                label = "TrashIconScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(color),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) 
                    Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = SamtchTheme.colors.primaryText.copy(alpha = (0.2f + progress).coerceIn(0f, 1f)),
                        modifier = Modifier
                            .padding(horizontal = 28.dp)
                            .size(28.dp)
                            .scale(iconScale)
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp) // Slight margin from screen edges
                .shadow(12.dp, RoundedCornerShape(40.dp))
                .clip(RoundedCornerShape(40.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    RoundedCornerShape(40.dp)
                )
                .clickable(onClick = onClick),
            color = SamtchTheme.colors.miniPlayerBackground.copy(alpha = 0.98f),
            tonalElevation = 8.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                // Player Preview - Filling more height
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    playerContent(Modifier.fillMaxSize())
                    // Touch sink
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .pointerInput(Unit) { /* consume touches */ }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Channel Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayName ?: channel,
                        color = SamtchTheme.colors.miniPlayerTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = streamTitle ?: "Live",
                        transitionSpec = {
                            (slideInVertically(animationSpec = SamtchAnimation.springInteractive()) { height -> height / 2 } + fadeIn())
                                .togetherWith(slideOutVertically(animationSpec = SamtchAnimation.springInteractive()) { height -> -height / 2 } + fadeOut())
                        },
                        label = "MiniTitleAnimation"
                    ) { targetTitle ->
                        Text(
                            text = targetTitle,
                            color = SamtchTheme.colors.miniPlayerSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Close Button - Large and easy to tap
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Player",
                        tint = SamtchTheme.colors.miniPlayerTitle.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
