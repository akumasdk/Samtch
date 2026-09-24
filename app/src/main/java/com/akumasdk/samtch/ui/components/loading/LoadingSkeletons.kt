package com.akumasdk.samtch.ui.components.loading

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.ui.theme.SamtchTheme

private const val LoadingDescription = "Loading video details"

@Composable
fun Modifier.skeletonLoading(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    description: String = LoadingDescription,
    baseColor: Color? = null,
    highlightColor: Color? = null
): Modifier {
    val context = LocalContext.current
    val animationsEnabled = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
    val colors = SamtchTheme.colors
    val base = baseColor ?: colors.secondaryText.copy(alpha = 0.24f)
    val highlight = highlightColor ?: colors.primaryText.copy(alpha = 0.14f)
    val transition = rememberInfiniteTransition(label = "SkeletonShimmer")
    val progress by if (animationsEnabled) {
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SkeletonShimmerProgress"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    return this
        .clearAndSetSemantics { contentDescription = description }
        .graphicsLayer()
        .clip(shape)
        .drawWithCache {
            val brush = if (animationsEnabled) {
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(size.width * progress, 0f),
                    end = Offset(size.width * (progress + 1f), size.height),
                    tileMode = TileMode.Clamp
                )
            } else {
                Brush.linearGradient(listOf(base, base))
            }
            onDrawBehind { drawRect(brush) }
        }
}

@Composable
fun PlayerSurfaceSkeleton(modifier: Modifier = Modifier) {
    val playerBase = Color.White.copy(alpha = 0.10f)
    val playerHighlight = Color.White.copy(alpha = 0.22f)
    val iconTint = Color.White.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = "Loading video player" }
    ) {
        // Top Controls Scrim & Skeleton
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .skeletonLoading(CircleShape, baseColor = playerBase, highlightColor = playerHighlight)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(12.dp)
                                .skeletonLoading(RoundedCornerShape(4.dp), baseColor = playerBase, highlightColor = playerHighlight)
                        )
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(10.dp)
                                .skeletonLoading(RoundedCornerShape(4.dp), baseColor = playerBase, highlightColor = playerHighlight)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .skeletonLoading(CircleShape, baseColor = playerBase, highlightColor = playerHighlight)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .skeletonLoading(CircleShape, baseColor = playerBase, highlightColor = playerHighlight)
                    )
                }
            }
        }

        // Center Play Control Skeleton
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .skeletonLoading(CircleShape, baseColor = playerBase, highlightColor = playerHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
        }

        // Bottom Controls Scrim & Skeleton
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(24.dp)
                        .skeletonLoading(CircleShape, baseColor = playerBase, highlightColor = playerHighlight)
                )
                Box(
                    modifier = Modifier
                        .size(42.dp, 18.dp)
                        .skeletonLoading(RoundedCornerShape(4.dp), baseColor = playerBase, highlightColor = playerHighlight)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .skeletonLoading(RoundedCornerShape(50), baseColor = playerBase, highlightColor = playerHighlight)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp, 14.dp)
                        .skeletonLoading(RoundedCornerShape(4.dp), baseColor = playerBase, highlightColor = playerHighlight)
                )
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(24.dp)
                        .skeletonLoading(CircleShape, baseColor = playerBase, highlightColor = playerHighlight)
                )
            }
        }
    }
}

@Composable
fun MetadataBarSkeleton(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 38.dp else 80.dp)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 0.dp else 4.dp)
            .clearAndSetSemantics { contentDescription = "Loading video details" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 24.dp else 46.dp)
                .skeletonLoading(CircleShape)
        )
        Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(if (compact) 0.45f else 0.32f).height(12.dp).skeletonLoading())
            Box(modifier = Modifier.fillMaxWidth(if (compact) 0.82f else 0.72f).height(if (compact) 13.dp else 16.dp).skeletonLoading())
            if (!compact) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(11.dp).skeletonLoading())
            }
        }
        if (!compact) {
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(82.dp, 36.dp).skeletonLoading(RoundedCornerShape(18.dp)))
        }
    }
}

@Composable
fun PlayerDialogSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
    includeComments: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Loading dialog content" }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.42f).height(22.dp).skeletonLoading())
        if (includeComments) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).skeletonLoading(CircleShape))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp).skeletonLoading())
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).skeletonLoading())
                        Box(modifier = Modifier.fillMaxWidth(0.72f).height(12.dp).skeletonLoading())
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items((0 until itemCount).toList()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(24.dp).skeletonLoading(CircleShape))
                        Box(modifier = Modifier.weight(1f).height(20.dp).skeletonLoading())
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
