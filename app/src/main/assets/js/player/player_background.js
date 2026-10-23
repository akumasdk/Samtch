(function() {
    'use strict';

    const styleId = 'samtch-player-background-style';
    let isFirstLoad = true;
    let isTransitioning = false;
    let lastRefreshTime = 0;
    let currentPosterUrl = '';
    let lastChannel = '';

    function getChannel() {
        return (window.SamtchChannel || '').trim().toLowerCase();
    }

    function getUrl(key) {
        const currentChannel = getChannel();
        if (!currentChannel) return '';
        const trigger = parseInt(key) || Date.now();
        const res = (trigger % 2 === 0) ? '640x360' : '1280x720';
        const cacheBuster = `t=${Date.now()}`;
        return `https://static-cdn.jtvnw.net/previews-ttv/live_user_${currentChannel}-${res}.jpg?${cacheBuster}`;
    }

    function updatePosters(newPoster) {
        const poster = newPoster || currentPosterUrl || getUrl();
        if (!poster) return;
        if (newPoster) {
            currentPosterUrl = newPoster;
        }
        document.querySelectorAll('video').forEach(video => {
            if (newPoster || !video.poster || video.poster.startsWith('android-webview-video-poster:')) {
                video.poster = poster;
            }
        });
    }

    function injectStyles() {
        if (document.getElementById(styleId)) return;
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            [data-a-target="video-ref"] {
                background: transparent !important;
                position: relative !important;
                overflow: hidden !important;
                isolation: isolate !important;
            }
            [data-a-target="video-ref"]::before,
            [data-a-target="video-ref"]::after {
                content: ""; position: absolute; top: -100px; left: -100px; right: -100px; bottom: -100px;
                background-size: cover !important;
                background-position: center !important;
                background-repeat: no-repeat !important;
                filter: blur(45px) brightness(0.5) saturate(1.2) !important;
                z-index: -1 !important;
                pointer-events: none !important;
                transform: scale(1.1);
            }
            [data-a-target="video-ref"]::before {
                background-image: var(--samtch-bg-prev);
            }
            [data-a-target="video-ref"]::after {
                background-image: var(--samtch-bg-curr);
                opacity: 0;
                transition: opacity 2s ease-in-out !important;
            }
            [data-a-target="video-ref"].crossfading::after {
                opacity: 1;
            }
            .video-player__container { background: transparent !important; }
            video { background: transparent !important; }
        `;
        document.head.appendChild(style);
    }

    window.refreshSamtchBackground = function(key) {
        const now = Date.now();
        const activeChannel = getChannel();
        if (!activeChannel) return;

        if (lastChannel !== activeChannel) {
            isFirstLoad = true;
            isTransitioning = false;
            lastChannel = activeChannel;
            currentPosterUrl = '';
        }

        if (isTransitioning || (now - lastRefreshTime < 5000 && !isFirstLoad)) {
            return;
        }

        const container = document.querySelector('[data-a-target="video-ref"]');
        if (!container) return;

        const nextUrl = getUrl(key);
        if (!nextUrl) return;

        isTransitioning = true;
        const loadTimeout = setTimeout(() => {
            isTransitioning = false;
        }, 5000);

        const img = new Image();
        img.onload = function() {
            clearTimeout(loadTimeout);
            lastRefreshTime = Date.now();
            updatePosters(nextUrl);
            if (isFirstLoad) {
                container.style.setProperty('--samtch-bg-prev', `url('${nextUrl}')`);
                isFirstLoad = false;
                isTransitioning = false;
                console.log('Samtch: Initial background set for ' + activeChannel);
            } else {
                console.log('Samtch: Starting crossfade to new preview');
                const currentVal = container.style.getPropertyValue('--samtch-bg-curr') || container.style.getPropertyValue('--samtch-bg-prev');
                container.style.setProperty('--samtch-bg-prev', currentVal);

                container.classList.remove('crossfading');
                void container.offsetWidth;

                container.style.setProperty('--samtch-bg-curr', `url('${nextUrl}')`);
                container.classList.add('crossfading');

                setTimeout(() => {
                    container.style.setProperty('--samtch-bg-prev', `url('${nextUrl}')`);
                    container.classList.remove('crossfading');
                    isTransitioning = false;
                }, 2100);
            }
        };

        img.onerror = function() {
            clearTimeout(loadTimeout);
            isTransitioning = false;
        };

        img.src = nextUrl;
    };

    injectStyles();
    updatePosters();

    setTimeout(() => {
        window.refreshSamtchBackground();
        updatePosters();
    }, 1000);

    if (!window.SamtchObserverActive) {
        const observer = new MutationObserver(() => {
            if (!document.getElementById(styleId)) injectStyles();
            updatePosters();
            const container = document.querySelector('[data-a-target="video-ref"]');
            if (container && !container.style.getPropertyValue('--samtch-bg-prev') && !isTransitioning) {
                window.refreshSamtchBackground();
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
        window.SamtchObserverActive = true;
    }
})();
