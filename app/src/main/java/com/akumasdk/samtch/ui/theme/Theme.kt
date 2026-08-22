package com.akumasdk.samtch.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class SamtchColors(
    val twitchPurple: Color = TwitchPurple,
    val twitchPurpleLight: Color = TwitchPurpleLight,
    val twitchDarkGray: Color = TwitchDarkGray,
    val twitchBlack: Color = TwitchBlack,
    val chatBackground: Color,
    val miniPlayerBackground: Color,
    val miniPlayerTitle: Color,
    val miniPlayerSubtitle: Color,
    val loadingIndicator: Color = TwitchPurple,
    val adblockBackground: Color,
    val divider: Color,
    val liveDot: Color = Color.Red,
    val primaryText: Color,
    val secondaryText: Color,
    val error: Color = Color.Red,
    val tooltipBackground: Color,
    val tabButtonBackground: Color,
    val dialogBackground: Color,
    val rootBackground: Color,
    val cardBackground: Color,
    val textFieldBackground: Color,
    val loadingOverlay: Color,
    val defaultUserColor: Color,
    val accentColor: Color,
    val glassBorder: Color,
    val audioPlayerBackgroundStart: Color,
    val audioPlayerBackgroundEnd: Color
)

@Composable
fun animateSamtchColorsAsState(target: SamtchColors): SamtchColors {
    val animationSpec = tween<Color>(durationMillis = 400, easing = FastOutSlowInEasing)
    
    return SamtchColors(
        twitchPurple = animateColorAsState(target.twitchPurple, animationSpec, label = "twitchPurple").value,
        twitchPurpleLight = animateColorAsState(target.twitchPurpleLight, animationSpec, label = "twitchPurpleLight").value,
        twitchDarkGray = animateColorAsState(target.twitchDarkGray, animationSpec, label = "twitchDarkGray").value,
        twitchBlack = animateColorAsState(target.twitchBlack, animationSpec, label = "twitchBlack").value,
        chatBackground = animateColorAsState(target.chatBackground, animationSpec, label = "chatBackground").value,
        miniPlayerBackground = animateColorAsState(target.miniPlayerBackground, animationSpec, label = "miniPlayerBackground").value,
        miniPlayerTitle = animateColorAsState(target.miniPlayerTitle, animationSpec, label = "miniPlayerTitle").value,
        miniPlayerSubtitle = animateColorAsState(target.miniPlayerSubtitle, animationSpec, label = "miniPlayerSubtitle").value,
        loadingIndicator = animateColorAsState(target.loadingIndicator, animationSpec, label = "loadingIndicator").value,
        adblockBackground = animateColorAsState(target.adblockBackground, animationSpec, label = "adblockBackground").value,
        divider = animateColorAsState(target.divider, animationSpec, label = "divider").value,
        liveDot = animateColorAsState(target.liveDot, animationSpec, label = "liveDot").value,
        primaryText = animateColorAsState(target.primaryText, animationSpec, label = "primaryText").value,
        secondaryText = animateColorAsState(target.secondaryText, animationSpec, label = "secondaryText").value,
        error = animateColorAsState(target.error, animationSpec, label = "error").value,
        tooltipBackground = animateColorAsState(target.tooltipBackground, animationSpec, label = "tooltipBackground").value,
        tabButtonBackground = animateColorAsState(target.tabButtonBackground, animationSpec, label = "tabButtonBackground").value,
        dialogBackground = animateColorAsState(target.dialogBackground, animationSpec, label = "dialogBackground").value,
        rootBackground = animateColorAsState(target.rootBackground, animationSpec, label = "rootBackground").value,
        cardBackground = animateColorAsState(target.cardBackground, animationSpec, label = "cardBackground").value,
        textFieldBackground = animateColorAsState(target.textFieldBackground, animationSpec, label = "textFieldBackground").value,
        loadingOverlay = animateColorAsState(target.loadingOverlay, animationSpec, label = "loadingOverlay").value,
        defaultUserColor = animateColorAsState(target.defaultUserColor, animationSpec, label = "defaultUserColor").value,
        accentColor = animateColorAsState(target.accentColor, animationSpec, label = "accentColor").value,
        glassBorder = animateColorAsState(target.glassBorder, animationSpec, label = "glassBorder").value,
        audioPlayerBackgroundStart = animateColorAsState(target.audioPlayerBackgroundStart, animationSpec, label = "audioPlayerBackgroundStart").value,
        audioPlayerBackgroundEnd = animateColorAsState(target.audioPlayerBackgroundEnd, animationSpec, label = "audioPlayerBackgroundEnd").value
    )
}

@Composable
fun animateColorSchemeAsState(target: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(400, easing = FastOutSlowInEasing)
    return target.copy(
        primary = animateColorAsState(target.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(target.inversePrimary, animationSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(target.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(target.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(target.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(target.surfaceTint, animationSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(target.inverseSurface, animationSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, animationSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(target.error, animationSpec, label = "error").value,
        onError = animateColorAsState(target.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(target.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(target.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(target.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(target.scrim, animationSpec, label = "scrim").value
    )
}

val LocalSamtchColors = compositionLocalOf {
    SamtchColors(
        chatBackground = TwitchChatBackground,
        miniPlayerBackground = Color.White,
        miniPlayerTitle = Color.Black,
        miniPlayerSubtitle = TwitchPurple,
        primaryText = Color.Black,
        secondaryText = Color.Gray,
        rootBackground = Color.White,
        adblockBackground = Color.Black.copy(alpha = 0.7f),
        tooltipBackground = Color.Black.copy(alpha = 0.7f),
        tabButtonBackground = Color.Black.copy(alpha = 0.6f),
        divider = Color.Black.copy(alpha = 0.1f),
        textFieldBackground = Color.Black.copy(alpha = 0.3f),
        defaultUserColor = TwitchPurple,
        dialogBackground = Color(0xFF1F1F23),
        cardBackground = Color.Black.copy(alpha = 0.4f),
        accentColor = TwitchPurpleLight,
        glassBorder = Color.Black.copy(alpha = 0.08f),
        loadingOverlay = Color.Black.copy(alpha = 0.6f),
        audioPlayerBackgroundStart = TwitchDarkGray,
        audioPlayerBackgroundEnd = TwitchBlack
    )
}

object SamtchTheme {
    val colors: SamtchColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSamtchColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black,
    surface = TwitchDarkGray,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black
)

@Composable
fun SamtchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val targetColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val targetSamtchColors = if (darkTheme) {
        SamtchColors(
            chatBackground = TwitchChatBackground,
            miniPlayerBackground = TwitchDarkGray,
            miniPlayerTitle = Color.White,
            miniPlayerSubtitle = TwitchPurpleLight,
            primaryText = Color.White,
            secondaryText = Color.LightGray,
            dialogBackground = Color(0xFF1F1F23),
            rootBackground = TwitchBlack,
            adblockBackground = Color.Black.copy(alpha = 0.7f),
            tooltipBackground = Color.Black.copy(alpha = 0.7f),
            tabButtonBackground = Color.Black.copy(alpha = 0.6f),
            divider = Color.White.copy(alpha = 0.15f),
            textFieldBackground = Color.Black.copy(alpha = 0.15f),
            defaultUserColor = TwitchPurpleLight,
            cardBackground = Color.Black.copy(alpha = 0.4f),
            accentColor = TwitchPurpleLight,
            glassBorder = Color.White.copy(alpha = 0.12f),
            loadingOverlay = TwitchBlack.copy(alpha = 0.6f),
            audioPlayerBackgroundStart = TwitchDarkGray,
            audioPlayerBackgroundEnd = TwitchBlack
        )
    } else {
        val darkPurple = Color(0xFF6441A5) // Deep purple for high contrast on light backgrounds
        val offWhite = Color(0xFFF7F7F8) // Twitch light mode background
        SamtchColors(
            chatBackground = Color.White,
            miniPlayerBackground = Color.White,
            miniPlayerTitle = Color.Black,
            miniPlayerSubtitle = darkPurple,
            twitchPurpleLight = darkPurple,
            primaryText = Color.Black,
            secondaryText = Color.DarkGray,
            dialogBackground = Color.White,
            rootBackground = offWhite,
            adblockBackground = Color.White.copy(alpha = 0.9f),
            tooltipBackground = Color.White.copy(alpha = 0.9f),
            tabButtonBackground = Color.White.copy(alpha = 0.8f),
            divider = Color.Black.copy(alpha = 0.1f),
            textFieldBackground = Color.Black.copy(alpha = 0.05f),
            defaultUserColor = darkPurple,
            cardBackground = Color(0xFFEFEEF1), // Twitch light mode UI background
            accentColor = darkPurple, 
            glassBorder = Color.Black.copy(alpha = 0.08f),
            loadingOverlay = Color.White.copy(alpha = 0.35f),
            audioPlayerBackgroundStart = Color(0xFFF7F7F8),
            audioPlayerBackgroundEnd = Color.White
        )
    }
    
    val animatedSamtchColors = animateSamtchColorsAsState(targetSamtchColors)
    val animatedColorScheme = animateColorSchemeAsState(targetColorScheme)

    CompositionLocalProvider(LocalSamtchColors provides animatedSamtchColors) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            content = content
        )
    }
}
