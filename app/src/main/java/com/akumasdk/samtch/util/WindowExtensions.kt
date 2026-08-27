package com.akumasdk.samtch.util

import android.view.View
import android.view.Window
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Ensures the dialog or bottom sheet window maintains the immersive fullscreen state.
 */
@Composable
fun MaintainFullscreenEffect() {
    val view = LocalView.current
    SideEffect {
        val window = findWindow(view)
        window?.let { 
            applyFullscreenFlags(it)
        }
    }
}

/**
 * Searches for a Window starting from a View, checking for DialogWindowProvider first.
 */
private fun findWindow(view: View): Window? {
    var current: Any? = view
    while (current != null) {
        if (current is DialogWindowProvider) {
            return current.window
        }
        current = if (current is View) current.parent else null
    }
    
    // Fallback to Activity window if no DialogWindowProvider is found
    var context = view.context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context.window
        }
        context = context.baseContext
    }
    return null
}

/**
 * Applies the necessary flags and behavior to a window to hide system bars.
 */
fun applyFullscreenFlags(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
