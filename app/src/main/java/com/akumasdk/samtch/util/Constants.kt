package com.akumasdk.samtch.util

object Constants {
    const val ABOUT_BLANK = "about:blank"

    object UserAgents {
        const val DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        const val MOBILE = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
    }

    object Twitch {
        const val BASE_URL = "https://www.twitch.tv"
        const val MOBILE_URL = "https://m.twitch.tv/"
        const val DOMAIN = "twitch.tv"
        const val CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        const val LOGIN_CLIENT_ID = "1eljwhgtfbalfkugdmf9zdevcusjiz"
        const val REDIRECT_URL = "https://akumasdk.github.io/Samtch/"
        const val DEFAULT_CHANNEL = "forsen"

        object Api {
            const val GQL = "https://gql.twitch.tv/gql"
            const val AUTH_BASE = "https://id.twitch.tv/oauth2/authorize?response_type=token"
            const val HLS_BASE = "https://usher.ttvnw.net/api/channel/hls/"
            const val INTEGRITY = "https://gql.twitch.tv/integrity"
            const val HELIX_VALIDATE = "https://id.twitch.tv/oauth2/validate"
            const val HELIX_USERS = "https://api.twitch.tv/helix/users"
            const val HELIX_STREAMS = "https://api.twitch.tv/helix/streams"
            const val HELIX_GLOBAL_BADGES = "https://api.twitch.tv/helix/chat/badges/global"
            const val HELIX_CHANNEL_BADGES = "https://api.twitch.tv/helix/chat/badges"
        }

        object Templates {
            const val PLAYER_URL = "https://player.twitch.tv/?channel=%s&parent=twitch.tv&muted=false&autoplay=true&enableExtensions=false&player=mobile"
            const val CHAT_URL = "https://www.twitch.tv/embed/%s/chat?parent=twitch.tv&darkpopout"
            const val PREVIEW_URL = "https://static-cdn.jtvnw.net/previews-ttv/live_user_%s-853x480.jpg"
            const val EMOTE_CDN = "https://static-cdn.jtvnw.net/emoticons/v2/%s/default/dark/3.0"
        }
    }

    object ThirdParty {
        object BTTV {
            const val API_GLOBAL = "https://api.betterttv.net/3/cached/emotes/global"
            const val API_USER = "https://api.betterttv.net/3/cached/users/twitch/%s"
            const val CDN_EMOTE = "https://cdn.betterttv.net/emote/%s/3x"
        }

        object FFZ {
            const val API_GLOBAL = "https://api.frankerfacez.com/v1/set/global"
            const val API_USER = "https://api.frankerfacez.com/v1/room/id/%s"
        }

        object SevenTV {
            const val API_GLOBAL = "https://7tv.io/v3/emote-sets/global"
            const val API_USER = "https://7tv.io/v3/users/twitch/%s"
        }
    }

    object Actions {
        const val REFRESH = "com.akumasdk.samtch.REFRESH"
        const val STOP_PLAYER = "com.akumasdk.samtch.STOP_PLAYER"
        const val STOP = "STOP"
    }

    object Extras {
        const val CHANNEL = "CHANNEL"
        const val ACTION = "ACTION"
    }

    object Bridges {
        const val PLAYER = "TwitchPlayerBridge"
        const val CHAT = "TwitchChatBridge"
        const val BROWSER = "TwitchBrowserBridge"
        const val BTTV_SETTINGS = "BttvSettingsBridge"
    }

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
