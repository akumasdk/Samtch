package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants

@Composable
fun PlayerBackground(
    channel: String,
    previewUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.4f,
    blurRadius: Dp = 0.dp,
    containerColor: Color = SamtchTheme.colors.rootBackground,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val finalUrl = previewUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())

    Box(
        modifier = modifier.background(containerColor)
    ) {
        Crossfade(
            targetState = finalUrl,
            animationSpec = tween(durationMillis = 1200),
            label = "PlayerBackgroundCrossfade"
        ) { url ->
            var isLoaded by remember { mutableStateOf(false) }
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isLoaded) alpha else 0f,
                animationSpec = tween(800),
                label = "ImageFadeAnimation"
            )

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
                contentScale = ContentScale.Crop,
                alpha = animatedAlpha,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        isLoaded = true
                    }
                }
            )
        }
        
        content()
    }
}
