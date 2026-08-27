package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.theme.SamtchAnimation
import com.akumasdk.samtch.ui.theme.SamtchTheme

/**
 * A versatile banner used to display status messages (like Adblock status or specialized viewing modes).
 */
@Composable
fun StatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    isImmersiveEnabled: Boolean = false,
    channel: String = "",
    previewImageUrl: String? = null
) {
    AnimatedVisibility(
        visible = text.isNotEmpty(),
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = if (isImmersiveEnabled) SamtchTheme.colors.twitchPurple.copy(alpha = 0.75f)
                          else SamtchTheme.colors.twitchPurple,
            animationSpec = SamtchAnimation.ColorTween,
            label = "StatusBannerBgColor"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks */ }
                )
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
