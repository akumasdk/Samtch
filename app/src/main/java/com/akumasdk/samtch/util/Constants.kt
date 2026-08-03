package com.akumasdk.samtch.util

object Constants {
    // User Agents
    const val USER_AGENT_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    const val USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
    
    // Twitch Endpoints
    const val TWITCH_BASE_URL = "https://www.twitch.tv"
    const val TWITCH_MOBILE_URL = "https://m.twitch.tv/"
    const val TWITCH_GQL_ENDPOINT = "https://gql.twitch.tv/gql"
    const val TWITCH_HLS_BASE = "https://usher.ttvnw.net/api/channel/hls/"
    const val TWITCH_GRAPHQL_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
    const val TWITCH_DOMAIN = "twitch.tv"
    
    const val TWITCH_PLAYER_URL_TEMPLATE = "https://player.twitch.tv/?channel=%s&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
    const val TWITCH_CHAT_URL_TEMPLATE = "https://www.twitch.tv/embed/%s/chat?parent=twitch.tv&darkpopout"
    const val TWITCH_PREVIEW_URL_TEMPLATE = "https://static-cdn.jtvnw.net/previews-ttv/live_user_%s-853x480.jpg"
    
    // API Endpoints
    const val INTEGRITY_URL = "https://gql.twitch.tv/integrity"
    const val HELIX_VALIDATE_URL = "https://id.twitch.tv/oauth2/validate"
    const val HELIX_GLOBAL_BADGES_URL = "https://api.twitch.tv/helix/chat/badges/global"
    const val HELIX_CHANNEL_BADGES_URL = "https://api.twitch.tv/helix/chat/badges"
    
    // Third-party API Endpoints
    const val BTTV_API_GLOBAL = "https://api.betterttv.net/3/cached/emotes/global"
    const val BTTV_API_USER = "https://api.betterttv.net/3/cached/users/twitch/%s"
    const val BTTV_CDN_EMOTE = "https://cdn.betterttv.net/emote/%s/3x"
    
    const val FFZ_API_GLOBAL = "https://api.frankerfacez.com/v1/set/global"
    const val FFZ_API_USER = "https://api.frankerfacez.com/v1/room/id/%s"
    
    const val SEVENTV_API_GLOBAL = "https://7tv.io/v3/emote-sets/global"
    const val SEVENTV_API_USER = "https://7tv.io/v3/users/twitch/%s"
    
    const val TWITCH_EMOTE_CDN_TEMPLATE = "https://static-cdn.jtvnw.net/emoticons/v2/%s/default/dark/3.0"
    
    // Actions
    const val ACTION_REFRESH = "com.akumasdk.samtch.REFRESH"
    const val ACTION_STOP_PLAYER = "com.akumasdk.samtch.STOP_PLAYER"
    const val ACTION_STOP = "STOP"
    
    // Extras
    const val EXTRA_CHANNEL = "CHANNEL"
    const val EXTRA_ACTION = "ACTION"
    
    // Common Strings
    const val ABOUT_BLANK = "about:blank"
    const val DEFAULT_CHANNEL = "forsen"
    
    // Bridges
    const val BRIDGE_PLAYER = "TwitchPlayerBridge"
    const val BRIDGE_CHAT = "TwitchChatBridge"
    const val BRIDGE_BROWSER = "TwitchBrowserBridge"
    const val BRIDGE_BTTV_SETTINGS = "BttvSettingsBridge"

    object Scripts {
        const val COMMON_SCROLL_UNLOCKER = "js/common/scroll_unlocker.js"
        const val COMMON_SPLASH_CONTROLLER = "js/common/splash_controller.js"
        const val COMMON_APP_BANNERS_REMOVER = "js/common/app_banners_remover.js"
        const val COMMON_BROWSER_NAV_INJECTOR = "js/common/browser_nav_injector.js"
        const val COMMON_PULL_TO_REFRESH = "js/common/pull_to_refresh.js"
        const val COMMON_SPA_DETECTOR = "js/common/spa_detector.js"
        
        const val CHAT_BTTV = "js/chat/bttv.js"
        const val CHAT_UI_CLEANER = "js/chat/ui_cleaner.js"
        const val CHAT_LOADER_OBSERVER = "js/chat/chat_loader_observer.js"
        
        const val PLAYER_UI_CLEANER = "js/player/ui_cleaner.js"
        const val PLAYER_PLAYBACK_MONITOR = "js/player/playback_monitor.js"
        const val PLAYER_VIDEO_SWAP = "js/player/video_swap.js"
        const val PLAYER_LINK_DISABLER = "js/player/link_disabler.js"
        const val PLAYER_CONTROLS_INJECTOR = "js/player/controls_injector.js"
        const val PLAYER_VISIBILITY_MONITOR = "js/player/visibility_monitor.js"
        const val PLAYER_VAFT = "js/player/vaft.js"
        const val PLAYER_EARLY_HIDER = "js/player/early_hider.js"
    }
}
