package com.akumasdk.samtch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

val LocalSamtchColors = staticCompositionLocalOf {
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
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val samtchColors = if (darkTheme) {
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
            textFieldBackground = Color.Black.copy(alpha = 0.4f),
            defaultUserColor = TwitchPurpleLight,
            cardBackground = Color.Black.copy(alpha = 0.4f),
            accentColor = TwitchPurpleLight,
            glassBorder = Color.White.copy(alpha = 0.12f),
            loadingOverlay = TwitchBlack.copy(alpha = 0.6f),
            audioPlayerBackgroundStart = TwitchDarkGray,
            audioPlayerBackgroundEnd = TwitchBlack
        )
    } else {
        SamtchColors(
            chatBackground = Color.White,
            miniPlayerBackground = Color.White,
            miniPlayerTitle = Color.Black,
            miniPlayerSubtitle = TwitchPurple,
            primaryText = Color.Black,
            secondaryText = Color.DarkGray,
            dialogBackground = Color.White,
            rootBackground = Color.White,
            adblockBackground = Color.White.copy(alpha = 0.9f),
            tooltipBackground = Color.White.copy(alpha = 0.9f),
            tabButtonBackground = Color.White.copy(alpha = 0.8f),
            divider = Color.Black.copy(alpha = 0.1f),
            textFieldBackground = Color.Black.copy(alpha = 0.05f),
            defaultUserColor = Color(0xFF6441A5), // Darker Twitch-like purple for light mode
            cardBackground = Color(0xFFEFEEF1), // Twitch light mode UI background
            accentColor = TwitchPurple, // Standard Twitch Purple for brand consistency
            glassBorder = Color.Black.copy(alpha = 0.08f),
            loadingOverlay = Color.White.copy(alpha = 0.35f),
            audioPlayerBackgroundStart = Color(0xFFF7F7F8),
            audioPlayerBackgroundEnd = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalSamtchColors provides samtchColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
