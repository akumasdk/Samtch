package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Legacy banner component. Ceased in favor of sleek floating [com.akumasdk.samtch.ui.components.playerComponents.AdBlockIndicator].
 */
@Deprecated("Ceased using full-width banner component in favor of sleek floating AdBlockIndicator HUD pill")
@Composable
fun StatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    isImmersiveEnabled: Boolean = false,
    channel: String = "",
    previewImageUrl: String? = null
) {
    // Deprecated banner component - no-op
}
