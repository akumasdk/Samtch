(function() {
    'use strict';

    const styleId = 'samtch-player-background-style';
    const channel = window.SamtchChannel;
    if (!channel) return;

    let isFirstLoad = true;
    let isTransitioning = false;
    let lastRefreshTime = 0;

    function getUrl() {
        return `https://static-cdn.jtvnw.net/previews-ttv/live_user_${channel.toLowerCase()}-853x480.jpg?t=${new Date().getTime()}`;
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

    window.refreshSamtchBackground = function() {
        const now = Date.now();
        // Prevent multiple simultaneous transitions or spamming (min 10s between refreshes)
        if (isTransitioning || (now - lastRefreshTime < 10000 && !isFirstLoad)) {
            return;
        }

        const container = document.querySelector('[data-a-target="video-ref"]');
        if (!container) return;

        const nextUrl = getUrl();
        const img = new Image();

        isTransitioning = true;

        img.onload = function() {
            lastRefreshTime = Date.now();
            if (isFirstLoad) {
                container.style.setProperty('--samtch-bg-prev', `url('${nextUrl}')`);
                isFirstLoad = false;
                isTransitioning = false;
                console.log('Samtch: Initial background set for ' + channel);
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
            isTransitioning = false;
        };

        img.src = nextUrl;
    };

    injectStyles();
    // Initial call
    setTimeout(() => window.refreshSamtchBackground(), 1000);

    if (!window.SamtchObserverActive) {
        const observer = new MutationObserver(() => {
            if (!document.getElementById(styleId)) injectStyles();
            const container = document.querySelector('[data-a-target="video-ref"]');
            // Only trigger if container exists and hasn't been initialized yet
            if (container && !container.style.getPropertyValue('--samtch-bg-prev') && !isTransitioning) {
                window.refreshSamtchBackground();
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
        window.SamtchObserverActive = true;
    }
})();
