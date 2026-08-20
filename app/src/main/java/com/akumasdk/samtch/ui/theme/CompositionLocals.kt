package com.akumasdk.samtch.ui.theme

import androidx.compose.runtime.compositionLocalOf

data class StreamPreviewInfo(
    val channel: String = "",
    val previewUrl: String? = null,
    val refreshKey: Any? = null
)

val LocalStreamPreview = compositionLocalOf { StreamPreviewInfo() }
