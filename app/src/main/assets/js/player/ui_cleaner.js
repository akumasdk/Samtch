(function() {
    'use strict';

    if (window.samtch_ui_cleaner_active) return;
    window.samtch_ui_cleaner_active = true;

    console.log('[Samtch] ui_cleaner.js starting...');

    function injectStyles() {
        const styleId = 'samtch-player-cleaner';
        if (document.getElementById(styleId)) return;

        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            /* Hide distracting elements */
            [data-a-target="player-fullscreen-button"],
            [data-a-target="player-clip-button"],
            [data-a-target="player-forward-button"],
            [data-a-target="player-rewind-button"],
            [data-a-target="player-theatre-mode-button"],
            button[aria-label*="Clip"],
            button[title*="Clip"],
            .stream-info-social-panel,
            .samtch-hidden-button,
            .ad-banner,
            .disclosure-card,
            .tw-upsell-banner {
                display: none !important;
            }
        `;
        document.head.appendChild(style);
    }

    function extractMetadata() {
        const avatarImg = document.querySelector('img.tw-image-avatar');
        const subtitleEl = document.querySelector('p[data-test-selector="stream-info-card-component__subtitle"]');

        const avatarUrl = avatarImg ? avatarImg.src : null;
        const subtitle = subtitleEl ? subtitleEl.textContent.trim() : null;

        if ((avatarUrl && avatarUrl !== window.samtch_last_avatar) ||
            (subtitle && subtitle !== window.samtch_last_subtitle)) {

            if (window.TwitchPlayerBridge) {
                window.TwitchPlayerBridge.updateMetadata(avatarUrl || '', subtitle || '');
                window.samtch_last_avatar = avatarUrl;
                window.samtch_last_subtitle = subtitle;
            }
        }
    }

    function clean() {
        const playerExists = document.querySelector('.video-player') ||
                           document.querySelector('[data-a-target="video-player"]');

        if (!playerExists) return;

        injectStyles();
        extractMetadata();

        // Remove "Watch on Twitch" button manually if it persists
        document.querySelectorAll('.tw-svg').forEach(container => {
            const svg = container.querySelector('svg');
            if (svg && svg.getAttribute('viewBox') === '0 0 65 16') {
                const button = container.closest('button');
                if (button) button.remove();
            }
        });
    }

    // High frequency cleanup during initial load
    const startTime = Date.now();
    const cleanInterval = setInterval(() => {
        clean();
        if (Date.now() - startTime > 8000) {
            clearInterval(cleanInterval);
            // Switch to maintenance cleaning
            setInterval(clean, 2500);
        }
    }, 500);

    const observer = new MutationObserver(clean);
    observer.observe(document.documentElement, { childList: true, subtree: true });

    clean();
})();
