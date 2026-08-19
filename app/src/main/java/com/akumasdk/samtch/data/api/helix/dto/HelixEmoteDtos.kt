package com.akumasdk.samtch.data.api.helix.dto

import kotlinx.serialization.Serializable

@Serializable
data class HelixEmoteDto(
    val id: String,
    val name: String,
    val images: HelixEmoteImagesDto,
    val format: List<String>? = null,
    val scale: List<String>? = null,
    val theme_mode: List<String>? = null,
    val emote_type: String? = null,
    val emote_set_id: String? = null,
    val owner_id: String? = null
)

@Serializable
data class HelixEmoteImagesDto(
    val url_1x: String,
    val url_2x: String,
    val url_4x: String
)

@Serializable
data class HelixEmoteResponse(
    val data: List<HelixEmoteDto>,
    val template: String? = null
)
