package com.akumasdk.samtch.ui.components.playerComponents

import android.util.Log
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.ui.components.metadata.util.getAlternatingPreviewUrl

@Composable
fun PlayerBackground(
    channel: String = "",
    previewUrl: String? = null,
    refreshKey: Any? = null,
    modifier: Modifier = Modifier,
    alpha: Float = 0.55f,
    blurRadius: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop,
    containerColor: Color = SamtchTheme.colors.rootBackground,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val previewInfo = LocalStreamPreview.current
    
    val targetChannel = channel.ifEmpty { previewInfo.channel }
    val targetUrl = previewUrl ?: previewInfo.previewUrl 
    val targetRefreshKey = refreshKey ?: previewInfo.refreshKey
    
    val baseUrl = remember(targetUrl, targetChannel, targetRefreshKey) {
        val url = targetUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(targetChannel.lowercase())
        getAlternatingPreviewUrl(url, targetRefreshKey) ?: ""
    }
    
    val finalUrl = remember(baseUrl, targetRefreshKey) {
        val url = if (targetRefreshKey != null) {
            val connector = if (baseUrl.contains("?")) "&" else "?"
            "$baseUrl${connector}v=$targetRefreshKey"
        } else {
            baseUrl
        }
        Log.d("PlayerBackground", "Preview URL: $url (RefreshKey: $targetRefreshKey)")
        url
    }

    Box(
        modifier = modifier.background(containerColor)
    ) {
        if (targetChannel.isNotEmpty() || targetUrl != null) {
            Crossfade(
                targetState = finalUrl,
                animationSpec = tween(
                    durationMillis = 1600,
                    easing = SamtchAnimation.EmphasizedEasing
                ),
                label = "PlayerBackgroundCrossfade"
            ) { url ->
                var isLoaded by remember { mutableStateOf(false) }

                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isLoaded) alpha else 0f,
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = SamtchAnimation.StandardEasing
                    ),
                    label = "ImageFadeAnimation"
                )

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
                    contentScale = contentScale,
                    alpha = animatedAlpha,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            isLoaded = true
                        }
                    }
                )
            }
        }
        
        content()
    }
}
