package com.akumasdk.samtch.data.api.vaft

import com.akumasdk.samtch.util.ExtMediaEntry

data class StreamInfo(
    val channelName: String,
    var isShowingAd: Boolean = false,
    var lastPlayerReload: Long = 0,
    var encodingsM3U8: String? = null,
    var modifiedM3U8: String? = null,
    var isUsingModifiedM3U8: Boolean = false,
    var usherParams: String = "",
    val requestedAds: MutableSet<String> = mutableSetOf(),
    val urls: MutableMap<String, ExtMediaEntry> = mutableMapOf(),
    val resolutionList: MutableList<ExtMediaEntry> = mutableListOf(),
    val backupEncodingsM3U8Cache: MutableMap<String, String?> = mutableMapOf(),
    var activeBackupPlayerType: String? = null,
    var isMidroll: Boolean = false,
    var isStrippingAdSegments: Boolean = false,
    var numStrippedAdSegments: Int = 0,
    var cleanManifestStreak: Int = 0,
    var isInitialized: Boolean = false
)

data class AdStatus(
    val hasAds: Boolean,
    val isMidroll: Boolean = false,
    val isStrippingAdSegments: Boolean = false,
    val numStrippedAdSegments: Int = 0,
    val playerType: String? = null
)
