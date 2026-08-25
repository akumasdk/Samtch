package com.akumasdk.samtch.data.api.vaft

object VaftConfig {
    const val LOG_TAG = "VaftOrchestrator"
    const val AD_SIGNIFIER = "stitched"
    const val CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
    
    val BACKUP_PLAYER_TYPES = listOf(
        "embed",   // Source
        "popout",  // Source
        "autoplay" // 360p
    )
    
    const val FALLBACK_PLAYER_TYPE = "embed"
    const val FORCE_ACCESS_TOKEN_PLAYER_TYPE = "popout"
    
    const val PLAYER_RELOAD_MINIMAL_REQUESTS_TIME = 1500L
    const val PLAYER_RELOAD_MINIMAL_REQUESTS_PLAYER_INDEX = 2 // autoplay
    
    const val PLAYER_BUFFERING_DELAY = 600L
    const val PLAYER_BUFFERING_SAME_STATE_COUNT = 3
    const val PLAYER_BUFFERING_DANGER_ZONE = 1 // seconds
    const val PLAYER_BUFFERING_MIN_REPEAT_DELAY = 8000L
}
