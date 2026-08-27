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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.akumasdk.samtch.data.api.PreviewImageService
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import coil.request.CachePolicy

@Composable
fun PlayerBackground(
    channel: String = "",
    previewUrl: String? = null,
    refreshKey: Any? = null,
    modifier: Modifier = Modifier,
    alpha: Float = 0.65f, // Increased default alpha
    blurRadius: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop,
    containerColor: Color = SamtchTheme.colors.rootBackground,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val previewInfo = LocalStreamPreview.current
    
    val targetChannel = channel.ifEmpty { previewInfo.channel }
    val targetUrl = if (!previewUrl.isNullOrBlank()) previewUrl else previewInfo.previewUrl
    val targetRefreshKey = refreshKey ?: previewInfo.refreshKey
    
    val baseUrl = remember(targetUrl, targetChannel) {
        val url = PreviewImageService.getProcessedUrl(targetUrl, targetChannel)
        Log.d("PlayerBackground", "Processing Base URL: $url (Source: $targetUrl, Channel: $targetChannel)")
        url
    }
    
    val finalUrl = remember(baseUrl, targetRefreshKey) {
        val url = if (targetRefreshKey != null) {
            val connector = if (baseUrl.contains("?")) "&" else "?"
            "$baseUrl${connector}t=${System.currentTimeMillis()}"
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
                    durationMillis = 1000,
                    easing = SamtchAnimation.StandardEasing
                ),
                label = "PlayerBackgroundCrossfade"
            ) { url ->
                var isLoaded by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
                    contentScale = contentScale,
                    alpha = alpha,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            isLoaded = true
                        }
                    }
                )

                // Very subtle wash to ensure text readability on light images
                if (isLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.12f))
                    )
                }
            }
        }
        
        content()
    }
}

/**
 * The official immersive background implementation for the player.
 * Includes sophisticated gradients and dynamic radial glow.
 */
@Composable
fun ImmersivePlayerBackground(
    channel: String = "",
    previewUrl: String? = null,
    refreshKey: Any? = null,
    modifier: Modifier = Modifier,
    isChatVisible: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    PlayerBackground(
        channel = channel,
        previewUrl = previewUrl,
        refreshKey = refreshKey,
        modifier = modifier,
        alpha = 0.9f, // High alpha to ensure the preview image is the dominant visual element
        blurRadius = 40.dp, // Balanced blur: hides UI details but preserves large color masses and shapes
        contentScale = ContentScale.Crop // Use Crop to maintain aspect ratio and fill the screen naturally
    ) {
        // Immersive gradient overlay - very subtle to avoid washing out the image colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        
        // Dynamic ambient glow
        val glowScale by animateFloatAsState(
            targetValue = if (isChatVisible) 1.6f else 1.0f,
            animationSpec = SamtchAnimation.StandardTween,
            label = "GlowScale"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SamtchTheme.colors.twitchPurple.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 1500f * glowScale
                    )
                )
        )
        
        content()
    }
}
