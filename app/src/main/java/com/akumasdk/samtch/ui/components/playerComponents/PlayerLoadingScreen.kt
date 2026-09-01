package com.akumasdk.samtch.ui.components.playerComponents

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.Constants

@Composable
fun PlayerLoadingScreen(
    channel: String,
    previewUrl: String?,
    loadingMessage: String,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null
) {
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

    val baseUrl = previewUrl ?: Constants.Twitch.Templates.PREVIEW_URL.format(channel.lowercase())
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
        // 1. Animated Background Layer
        Crossfade(
            targetState = finalUrl,
            animationSpec = tween(1000, easing = SamtchAnimation.EmphasizedEasing),
            label = "BackgroundCrossfade"
        ) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(5.dp)
                    .scale(1.1f), // Scale up slightly to hide blur edges
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
        }

        // 2. Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        // 3. Central Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(pulseScale)
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

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = loadingMessage,
                color = Color.White,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 12f
                    )
                )
            )
        }
    }
}
