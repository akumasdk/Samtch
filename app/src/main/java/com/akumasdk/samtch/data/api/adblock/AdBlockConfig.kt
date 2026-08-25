package com.akumasdk.samtch.data.api.adblock

object AdBlockConfig {
    const val LOG_TAG = "AdBlockOrchestrator"
    const val AD_SIGNIFIER = "stitched"
    const val CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
    
    val BACKUP_PLAYER_TYPES = listOf(
        "embed",   // Source
        "popout",  // Source
        "autoplay" // 360p
    )
    
    const val FALLBACK_PLAYER_TYPE = "embed"
    const val FORCE_ACCESS_TOKEN_PLAYER_TYPE = "popout"
}
