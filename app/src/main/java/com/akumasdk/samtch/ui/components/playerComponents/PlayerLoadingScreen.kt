package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.ui.components.metadata.util.unifyPreviewUrl
import com.akumasdk.samtch.ui.components.loading.PlayerSurfaceSkeleton

@Composable
fun PlayerLoadingScreen(
    channel: String,
    previewUrl: String?,
    loadingMessage: String,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val baseUrl = remember(previewUrl, channel) {
        val url = previewUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())
        unifyPreviewUrl(url, width = "853", height = "480") ?: ""
    }
    
    val finalUrl = remember(baseUrl, refreshKey) {
        if (refreshKey != null) {
            val connector = if (baseUrl.contains("?")) "&" else "?"
            "$baseUrl${connector}v=$refreshKey"
        } else {
            baseUrl
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SamtchTheme.colors.rootBackground),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Gradient Background when preview image is not loaded yet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SamtchTheme.colors.twitchPurple.copy(alpha = 0.22f),
                            SamtchTheme.colors.rootBackground,
                            Color.Black
                        )
                    )
                )
        )

        // 2. Preview Image Layer with smooth fade-in
        Crossfade(
            targetState = finalUrl,
            animationSpec = tween(1000, easing = SamtchAnimation.EmphasizedEasing),
            label = "BackgroundCrossfade"
        ) { url ->
            var isLoaded by remember { mutableStateOf(false) }

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.04f),
                contentScale = ContentScale.Crop,
                alpha = if (isLoaded) 0.78f else 0f,
                onSuccess = { isLoaded = true }
            )
        }

        // Layered scrims keep controls readable while preserving the preview artwork.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.34f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
        )

        // Player controls skeleton filling the video space
        PlayerSurfaceSkeleton(
            modifier = Modifier.fillMaxSize()
        )

        // 3. Central Progress Wheel Aligned with Skeleton Center
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(pulseScale)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = SamtchTheme.colors.twitchPurple,
                strokeWidth = 4.dp,
                trackColor = SamtchTheme.colors.twitchPurple.copy(alpha = 0.1f)
            )
            
            // Subtle inner glow
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SamtchTheme.colors.twitchPurple.copy(alpha = 0.15f), MaterialTheme.shapes.extraLarge)
            )
        }

        // Loading message text placed below the centered spinning wheel
        Text(
            text = loadingMessage,
            color = Color.White,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    blurRadius = 12f
                )
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 104.dp, start = 32.dp, end = 32.dp)
        )
    }
}
