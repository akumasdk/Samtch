package com.akumasdk.samtch.ui.screens.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

@Composable
fun FullscreenPlayer(
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    onToggleFullscreen: () -> Unit
) {
    WebViewContainer(
        modifier = Modifier.fillMaxSize(),
        state = state,
        navigator = navigator,
        channel = channel,
        onToggleFullscreen = onToggleFullscreen
    )
}
