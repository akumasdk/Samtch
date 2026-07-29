(function() {
    'use strict';
    console.log('[Samtch] Aggressive Sentinel & Suppressor active');

    const SAFE_ROOTS = new Set([
        'directory', 'search', 'following', 'browse', 'p',
        'settings', 'inventory', 'wallet', 'drops', 'turbo',
        'friends', 'activity', 'bits', 'about', 'jobs', 'security', 'home'
    ]);

    // CSS Suppressor: Aggressively hides any player elements to prevent background leaks
    const style = document.createElement('style');
    style.id = 'samtch-suppressor';
    style.innerHTML = `
        .video-player,
        .player-core-main,
        .persistent-player,
        [data-a-target="video-player"],
        .video-player__container,
        .video-player__overlay,
        video {
            display: none !important;
            visibility: hidden !important;
            opacity: 0 !important;
            height: 0 !important;
            width: 0 !important;
            pointer-events: none !important;
        }
    `;

    function isSafeUrl(url) {
        if (!url || url.includes('about:blank')) return true;
        try {
            const uri = new URL(url, window.location.href);
            if (!uri.hostname.includes('twitch.tv')) return true;

            const path = uri.pathname || '/';
            const segments = path.split('/').filter(s => s.length > 0);

            if (segments.length === 0 || path === '/' || path === '/home') return true;
            if (SAFE_ROOTS.has(segments[0].toLowerCase())) return true;

            return false;
        } catch (e) {
            return true;
        }
    }

    function notifyUrlChange(url, blocked = false, requestBack = false) {
        if (window.TwitchBrowserBridge) {
            window.TwitchBrowserBridge.onUrlChange(url, blocked, requestBack);
        }
    }

    function killVideoElements() {
        const videos = document.getElementsByTagName('video');
        for (let i = 0; i < videos.length; i++) {
            try {
                videos[i].pause();
                videos[i].src = "";
                videos[i].load();
                videos[i].remove();
                console.log('[Samtch] Video element terminated');
            } catch (e) {}
        }
    }

    function handleUnsafe(url, source) {
        console.log('[Samtch] Aggressive Block (' + source + '):', url);
        window.stop();
        notifyUrlChange(url, true);
    }

    // Hook into pushState
    const originalPushState = history.pushState;
    history.pushState = function(state, title, url) {
        const fullUrl = url ? new URL(url, window.location.href).href : window.location.href;
        if (!isSafeUrl(fullUrl)) {
            handleUnsafe(fullUrl, 'pushState');
            return;
        }
        originalPushState.apply(this, arguments);
        notifyUrlChange(fullUrl, false);
    };

    // Hook into replaceState
    const originalReplaceState = history.replaceState;
    history.replaceState = function(state, title, url) {
        const fullUrl = url ? new URL(url, window.location.href).href : window.location.href;
        if (!isSafeUrl(fullUrl)) {
            handleUnsafe(fullUrl, 'replaceState');
            return;
        }
        originalReplaceState.apply(this, arguments);
        notifyUrlChange(fullUrl, false);
    };

    // Global Click Interceptor
    window.addEventListener('click', function(e) {
        const target = e.target.closest('a, button, [role="button"]');
        if (!target) return;

        const url = target.href || '';
        if (url && url.includes('twitch.tv') && !isSafeUrl(url)) {
            e.preventDefault();
            e.stopImmediatePropagation();
            handleUnsafe(url, 'click');
            return;
        }

        if (!isSafeUrl(window.location.href)) {
            notifyUrlChange(window.location.href, false, true);
        }
    }, true);

    // Mutation Observer: Actively kills video elements that appear (e.g. featured streams)
    const observer = new MutationObserver(function(mutations) {
        killVideoElements();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });

    // Periodic cleanup as a fail-safe
    setInterval(killVideoElements, 1000);

    // Heartbeat Sentinel
    let lastReportedUrl = window.location.href;
    setInterval(function() {
        const currentUrl = window.location.href;
        if (currentUrl !== lastReportedUrl) {
            lastReportedUrl = currentUrl;
            if (!isSafeUrl(currentUrl)) {
                handleUnsafe(currentUrl, 'heartbeat');
            } else {
                notifyUrlChange(currentUrl, false);
            }
        }
    }, 250);

    // Initial injection
    document.head.appendChild(style);
    killVideoElements();
    notifyUrlChange(window.location.href, false);
})();
