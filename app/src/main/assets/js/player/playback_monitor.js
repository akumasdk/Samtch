(function() {
    'use strict';

    if (window.samtch_playback_monitor_int) clearInterval(window.samtch_playback_monitor_int);

    console.log('[Samtch] playback_monitor.js starting...');

    let signalSent = false;
    let playingStartTime = null;

    function checkPlayback() {
        if (signalSent) return;

        // Look for all video elements, including those that might be in iframes
        const videos = Array.from(document.querySelectorAll('video'));

        // Also check same-origin iframes recursively
        try {
            const iframes = document.querySelectorAll('iframe');
            iframes.forEach(iframe => {
                try {
                    if (iframe.contentDocument) {
                        const iframeVideos = iframe.contentDocument.querySelectorAll('video');
                        videos.push(...Array.from(iframeVideos));
                    }
                } catch (e) {
                    // Cross-origin iframe, ignore
                }
            });
        } catch (e) {}

        if (videos.length === 0) return;

        for (const video of videos) {
            // Check if video is actually playing
            // readyState 2 (HAVE_CURRENT_DATA) is often enough to start hearing/seeing something
            const isPlaying = !video.paused && !video.ended && video.readyState >= 2;

            if (isPlaying) {
                if (!playingStartTime) playingStartTime = Date.now();

                const isAudible = !video.muted && video.volume > 0;
                const timeSinceStart = Date.now() - playingStartTime;

                // Trigger if:
                // 1. Sound is detected
                // 2. Or video has been playing for > 3 seconds even if muted (fallback)
                if (isAudible || timeSinceStart > 3000) {
                    if (window.TwitchPlayerBridge && window.TwitchPlayerBridge.onPlaybackStarted) {
                        console.log(`[Samtch] Playback detected (Audible: ${isAudible}, Fallback: ${timeSinceStart > 3000}). Signaling bridge.`);
                        window.TwitchPlayerBridge.onPlaybackStarted();
                        signalSent = true;
                        clearInterval(window.samtch_playback_monitor_int);
                        break;
                    }
                }
            }
        }
    }

    // High frequency polling initially
    window.samtch_playback_monitor_int = setInterval(checkPlayback, 300);
})();
