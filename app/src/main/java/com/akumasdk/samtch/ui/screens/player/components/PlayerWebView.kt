package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.akumasdk.samtch.ui.components.playerComponents.WebViewContainer
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState

@Composable
fun PlayerWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    channel: String,
    isMinimized: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleChat: () -> Unit,
    onToggleAudioOnly: () -> Unit,
    onPlaybackStarted: () -> Unit,
    onLoadingStatus: (String) -> Unit,
    onAdblocked: (String) -> Unit,
    onVideoBoundsChanged: (android.graphics.Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    WebViewContainer(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                if (!isMinimized) {
                    val rect = layoutCoordinates.boundsInWindow()
                    onVideoBoundsChanged(
                        android.graphics.Rect(
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.right.toInt(),
                            rect.bottom.toInt()
                        )
                    )
                }
            },
        state = state,
        navigator = navigator,
        channel = channel,
        onToggleFullscreen = onToggleFullscreen,
        onToggleChat = onToggleChat,
        onToggleAudioOnly = onToggleAudioOnly,
        onPlaybackStarted = onPlaybackStarted,
        onLoadingStatus = onLoadingStatus,
        onAdblocked = onAdblocked
    )
}
