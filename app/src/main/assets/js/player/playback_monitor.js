(function() {
    'use strict';

    if (window.samtch_playback_monitor_int) clearInterval(window.samtch_playback_monitor_int);

    console.log('[Samtch] playback_monitor.js starting...');

    let signalSent = false;
    let playingStartTime = null;

    function checkPlayback() {
        if (signalSent) return;

        const video = document.querySelector('video');
        if (!video) return;

        // Check if video is actually playing (currentTime is moving)
        const isPlaying = video.currentTime > 0 && !video.paused && !video.ended && video.readyState >= 3;

        if (isPlaying) {
            if (!playingStartTime) playingStartTime = Date.now();

            const isAudible = !video.muted && video.volume > 0;
            const timeSinceStart = Date.now() - playingStartTime;

            // Trigger if:
            // 1. Sound is detected
            // 2. Or video has been playing for > 5 seconds even if muted (fallback)
            if (isAudible || timeSinceStart > 5000) {
                if (window.TwitchPlayerBridge && window.TwitchPlayerBridge.onPlaybackStarted) {
                    console.log(`[Samtch] Playback detected (Audible: ${isAudible}, Fallback: ${timeSinceStart > 5000}). Signaling bridge.`);
                    window.TwitchPlayerBridge.onPlaybackStarted();
                    signalSent = true;
                    clearInterval(window.samtch_playback_monitor_int);
                }
            }
        } else {
            playingStartTime = null;
        }
    }

    // Poll frequently during startup
    window.samtch_playback_monitor_int = setInterval(checkPlayback, 500);
})();
