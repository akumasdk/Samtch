package com.akumasdk.samtch.ui.screens.player

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

/**
 * A custom WebView that ignores window visibility changes to keep audio playing in the background.
 */
class BackgroundAudioWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    override fun onWindowVisibilityChanged(visibility: Int) {
        // By always passing VISIBLE, we trick the internal WebView logic into
        // thinking it's still visible, preventing it from pausing JS and media.
        super.onWindowVisibilityChanged(VISIBLE)
    }

    override fun onPause() {
        // Many systems call onPause when the app goes background.
        // We do nothing here to keep the engine running.
        // Note: super.onPause() is what actually stops the timers.
    }

    override fun onResume() {
        // Do nothing or call super if needed, but we want to avoid state changes.
        super.onResume()
    }
}
