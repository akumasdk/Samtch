(function() {
    'use strict';

    function log(msg) {
        console.log("[StreamDetector] " + msg);
    }

    function reportUrl(url) {
        if (!url || url.startsWith('blob:') || url.startsWith('data:')) return;

        if (typeof TwitchPlayerBridge !== 'undefined' && TwitchPlayerBridge.onStreamUrlFound) {
            log("Reporting found stream URL: " + url);
            TwitchPlayerBridge.onStreamUrlFound(url);
        }
    }

    function scanForStreams() {
        // 1. Scan <video> tags
        const videos = document.getElementsByTagName('video');
        for (let i = 0; i < videos.length; i++) {
            const v = videos[i];
            if (v.src) reportUrl(v.src);

            const sources = v.getElementsByTagName('source');
            for (let j = 0; j < sources.length; j++) {
                if (sources[j].src) reportUrl(sources[j].src);
            }
        }

        // 2. Scan <iframe> tags (might be nested players)
        const iframes = document.getElementsByTagName('iframe');
        for (let i = 0; i < iframes.length; i++) {
            const src = iframes[i].src;
            if (src && (src.includes('.m3u8') || src.includes('.mp4'))) {
                reportUrl(src);
            }
        }

        // 3. Scan for common JS player variables if they exist
        if (window.player && window.player.source) reportUrl(window.player.source);
        if (window.jwplayer) {
            try {
                const jw = window.jwplayer();
                if (jw && jw.getPlaylist) {
                    const pl = jw.getPlaylist();
                    if (pl && pl[0] && pl[0].file) reportUrl(pl[0].file);
                }
            } catch(e) {}
        }
    }

    // Start periodic scanning
    log("Stream Detector initialized");
    setInterval(scanForStreams, 5000);
    scanForStreams();
})();
