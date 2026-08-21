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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants

@Composable
fun PlayerBackground(
    channel: String = "",
    previewUrl: String? = null,
    refreshKey: Any? = null,
    modifier: Modifier = Modifier,
    alpha: Float = 0.55f,
    blurRadius: Dp = 0.dp,
    containerColor: Color = SamtchTheme.colors.rootBackground,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val previewInfo = LocalStreamPreview.current
    
    val targetChannel = channel.ifEmpty { previewInfo.channel }
    val targetUrl = previewUrl ?: previewInfo.previewUrl 
    val targetRefreshKey = refreshKey ?: previewInfo.refreshKey
    
    val baseUrl = targetUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(targetChannel.lowercase())
    
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
                    durationMillis = 1600,
                    easing = SamtchAnimation.EmphasizedEasing
                ),
                label = "PlayerBackgroundCrossfade"
            ) { url ->
                var isLoaded by remember { mutableStateOf(false) }
                var imageLuminance by remember { mutableFloatStateOf(0.5f) }
                
                val isLightMode = SamtchTheme.colors.rootBackground.luminance() > 0.5f
                
                // If there's a clash, slightly reduce alpha, but keep it high enough for colors
                val effectiveAlpha = if (isLoaded) {
                    if (isLightMode && imageLuminance < 0.4f) {
                        (alpha * 0.7f).coerceIn(0.2f, 0.4f)
                    } else if (!isLightMode && imageLuminance > 0.6f) {
                        (alpha * 0.7f).coerceIn(0.2f, 0.4f)
                    } else {
                        alpha
                    }
                } else 0f

                val animatedAlpha by animateFloatAsState(
                    targetValue = effectiveAlpha,
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
                        .allowHardware(false) // Hardware bitmaps cannot be read by Palette
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
                    contentScale = ContentScale.Crop,
                    alpha = animatedAlpha,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            val bitmap = state.result.drawable.toBitmap()
                            // Analyze the image to detect its general brightness
                            Palette.from(bitmap).generate { palette ->
                                palette?.let {
                                    val dominantLuminance = Color(it.getDominantColor(0)).luminance()
                                    val mutedLuminance = Color(it.getMutedColor(0)).luminance()
                                    
                                    // Use a combination of swatches for a more accurate reading
                                    imageLuminance = (dominantLuminance + mutedLuminance) / 2f
                                    Log.d("PlayerBackground", "Luminance Analysis - Avg: $imageLuminance")
                                }
                            }
                            isLoaded = true
                        }
                    }
                )
            }
        }
        
        // Final safety wash: very subtle tint
        val washColor = if (SamtchTheme.colors.rootBackground.luminance() > 0.5f) Color.White else Color.Black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(washColor.copy(alpha = 0.02f))
        )

        content()
    }
}
