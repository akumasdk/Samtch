package com.akumasdk.samtch.data.badge

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TwitchBadgeDto(
    val setID: String = "",
    val version: String = "",
    val title: String = "",
    val description: String = "",
    val image1x: String? = null,
    val image2x: String? = null,
    val image4x: String? = null
) {
    val bestUrl: String? get() = image4x ?: image2x ?: image1x
}
