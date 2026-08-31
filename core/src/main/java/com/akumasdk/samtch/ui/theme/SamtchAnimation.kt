package com.akumasdk.samtch.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Standard animation specifications for Samtch.
 * Provides a unified look and feel across the app based on Material 3 motion.
 */
object SamtchAnimation {
    // Material 3 Easing
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    // Springs
    fun <T> springLowBouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springInteractive() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = 500f
    )

    fun <T> springBouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val InteractiveSpring = springInteractive<Float>()
    val DpSpring = springInteractive<androidx.compose.ui.unit.Dp>()

    // Tweens
    val StandardDuration = 300
    val FastDuration = 150
    val SlowDuration = 500

    val EmphasizedTween = tween<Float>(durationMillis = SlowDuration, easing = EmphasizedEasing)
    val StandardTween = tween<Float>(durationMillis = StandardDuration, easing = StandardEasing)
    val FastTween = tween<Float>(durationMillis = FastDuration, easing = StandardEasing)
    val ColorTween = tween<androidx.compose.ui.graphics.Color>(StandardDuration, easing = StandardEasing)

    // Transitions
    val ScreenEnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.9f)
    ) + fadeIn(animationSpec = tween(StandardDuration))

    val ScreenExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.9f)
    ) + fadeOut(animationSpec = tween(StandardDuration))

    val PlayerEnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = springInteractive<IntOffset>()
    ) + fadeIn(animationSpec = StandardTween)

    val PlayerExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(StandardDuration, easing = StandardEasing)
    ) + fadeOut(animationSpec = tween(StandardDuration, easing = StandardEasing))

    val FadeIn = fadeIn(animationSpec = tween(StandardDuration))
    val FadeOut = fadeOut(animationSpec = tween(StandardDuration))

    // Player transitions (Mini <-> Full)
    val MinimizeTransition = (fadeIn(animationSpec = StandardTween) + scaleIn(initialScale = 0.92f, animationSpec = StandardTween)) togetherWith 
            (slideOutVertically(animationSpec = springInteractive()) { it } + fadeOut(animationSpec = FastTween))

    val MaximizeTransition = (slideInVertically(animationSpec = springInteractive()) { it } + fadeIn(animationSpec = StandardTween)) togetherWith 
            (fadeOut(animationSpec = FastTween) + scaleOut(targetScale = 0.92f, animationSpec = FastTween))

    // Helper for staggered items
    fun staggeredEnter(index: Int) = slideInVertically(
        initialOffsetY = { 20 * (index + 1) },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
    ) + fadeIn(animationSpec = tween(300, delayMillis = 50 * index))
}
