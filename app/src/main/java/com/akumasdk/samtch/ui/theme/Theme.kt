package com.akumasdk.samtch.ui.theme

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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class SamtchColors(
    val twitchPurple: Color = TwitchPurple,
    val twitchPurpleLight: Color = TwitchPurpleLight,
    val twitchDarkGray: Color = TwitchDarkGray,
    val twitchBlack: Color = TwitchBlack,
    val chatBackground: Color = TwitchChatBackground,
    val miniPlayerBackground: Color,
    val miniPlayerTitle: Color,
    val miniPlayerSubtitle: Color
)

val LocalSamtchColors = staticCompositionLocalOf {
    SamtchColors(
        miniPlayerBackground = Color.White,
        miniPlayerTitle = Color.Black,
        miniPlayerSubtitle = TwitchPurple
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
            miniPlayerBackground = TwitchDarkGray,
            miniPlayerTitle = Color.White,
            miniPlayerSubtitle = TwitchPurpleLight
        )
    } else {
        SamtchColors(
            miniPlayerBackground = Color.White,
            miniPlayerTitle = Color.Black,
            miniPlayerSubtitle = TwitchPurple
        )
    }

    CompositionLocalProvider(LocalSamtchColors provides samtchColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
