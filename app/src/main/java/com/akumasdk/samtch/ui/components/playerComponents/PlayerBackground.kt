package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants

@Composable
fun PlayerBackground(
    channel: String,
    previewUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.4f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.background(SamtchTheme.colors.rootBackground)
    ) {
        val finalUrl = previewUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())
        AsyncImage(
            model = finalUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = alpha
        )
        content()
    }
}
