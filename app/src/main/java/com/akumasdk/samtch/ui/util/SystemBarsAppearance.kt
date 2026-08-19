package com.akumasdk.samtch.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.akumasdk.samtch.ui.theme.LocalSystemBarsAppearance
import com.akumasdk.samtch.ui.theme.SystemBarsAppearance

@Composable
fun SystemBarsAppearance(
    lightStatusBars: Boolean? = null,
    lightNavigationBars: Boolean? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSystemBarsAppearance provides SystemBarsAppearance(
            lightStatusBars = lightStatusBars,
            lightNavigationBars = lightNavigationBars
        )
    ) {
        content()
    }
}
