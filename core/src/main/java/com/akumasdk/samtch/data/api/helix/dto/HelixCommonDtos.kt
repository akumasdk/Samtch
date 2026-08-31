package com.akumasdk.samtch.data.api.helix.dto

import kotlinx.serialization.Serializable

@Serializable
data class DataListDto<T>(
    val data: List<T> = emptyList()
)
