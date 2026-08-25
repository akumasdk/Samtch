package com.akumasdk.samtch.data.api.adblock

data class StreamInfo(
    val channelName: String,
    var isShowingAd: Boolean = false,
    var activeBackupPlayerType: String? = null,
    var isMidroll: Boolean = false,
    var isStrippingAdSegments: Boolean = false,
    var cleanManifestStreak: Int = 0,
    var isInitialized: Boolean = false
)

data class AdStatus(
    val hasAds: Boolean,
    val isMidroll: Boolean = false,
    val isStrippingAdSegments: Boolean = false,
    val playerType: String? = null
)
