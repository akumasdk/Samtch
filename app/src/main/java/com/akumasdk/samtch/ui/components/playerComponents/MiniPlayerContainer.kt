package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.MiniPlayerContainer(
    visible: Boolean,
    channel: String,
    displayName: String?,
    streamTitle: String?,
    elevation: Dp,
    nudgeOffset: Float,
    dismissState: SwipeToDismissBoxState,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = SamtchAnimation.StandardTween) + scaleIn(initialScale = 0.92f, animationSpec = SamtchAnimation.StandardTween),
        exit = fadeOut(animationSpec = SamtchAnimation.FastTween) + scaleOut(targetScale = 0.92f, animationSpec = SamtchAnimation.FastTween),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .offset { IntOffset(nudgeOffset.roundToInt(), 0) }
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.padding(horizontal = 8.dp),
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val isSwiping = direction != SwipeToDismissBoxValue.Settled
                val progress = if (isSwiping) dismissState.progress else 0f
                
                val color by animateColorAsState(
                    if (isSwiping) SamtchTheme.colors.error.copy(alpha = (0.1f + (0.3f * progress)).coerceIn(0f, 0.4f)) else Color.Transparent,
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
                    .shadow(elevation, RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
                    .clickable(onClick = onExpand),
                color = SamtchTheme.colors.miniPlayerBackground.copy(alpha = 0.98f),
                tonalElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    Box(modifier = Modifier.size(width = 120.dp, height = 64.dp)) {
                        content()
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = displayName ?: channel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            color = SamtchTheme.colors.miniPlayerTitle
                        )
                        Text(
                            text = streamTitle ?: "Live",
                            color = SamtchTheme.colors.miniPlayerSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = SamtchTheme.colors.miniPlayerTitle.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
