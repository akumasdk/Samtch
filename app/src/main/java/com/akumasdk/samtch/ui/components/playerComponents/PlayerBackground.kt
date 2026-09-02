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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.akumasdk.samtch.ui.theme.LocalStreamPreview
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.ui.components.metadata.util.getAlternatingPreviewUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                var imageLuminance by remember { mutableFloatStateOf(0f) }
                var successState by remember { mutableStateOf<AsyncImagePainter.State.Success?>(null) }
                
                // Offload CPU-intensive luminance analysis to a background thread
                LaunchedEffect(successState) {
                    val state = successState ?: return@LaunchedEffect
                    withContext(Dispatchers.Default) {
                        try {
                            // toBitmap() and Palette generation are heavy operations
                            val bitmap = state.result.drawable.toBitmap()
                            val palette = Palette.from(bitmap).generate()
                            
                            val dom = Color(palette.getDominantColor(0)).luminance()
                            val muted = Color(palette.getMutedColor(0)).luminance()
                            
                            // Return to main thread to update UI state
                            imageLuminance = (dom + muted) / 2f
                            isLoaded = true
                            Log.d("PlayerBackground", "Luminance Analyzed: $imageLuminance")
                        } catch (e: Exception) {
                            Log.e("PlayerBackground", "Background Palette analysis failed", e)
                            isLoaded = true // Still show the image even if analysis fails
                        }
                    }
                }

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
                        .allowHardware(false) // Required for Palette to read the bitmap
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
                    contentScale = contentScale,
                    alpha = animatedAlpha,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            successState = state
                        }
                    }
                )

                // Dynamic dark wash: apply more darkness if the image is bright
                // This ensures content readability regardless of image brightness.
                if (isLoaded && imageLuminance > 0.25f) {
                    val washAlpha = ((imageLuminance - 0.25f) * 0.95f).coerceIn(0f, 0.75f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = washAlpha))
                    )
                }
            }
        }
        
        content()
    }
}
