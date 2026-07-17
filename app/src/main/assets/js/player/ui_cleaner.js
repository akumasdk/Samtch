(function() {
    'use strict';
    console.log('[Samtch] ui_cleaner.js active');

    function injectStyles() {
        const styleId = 'samtch-player-cleaner';
        if (document.getElementById(styleId)) return;

        console.log('[Samtch] Injecting player cleaner styles');
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            /* Ocultar elementos innecesarios */
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

    function removeSocialPanel() {
        const panel = document.querySelector('.stream-info-social-panel');
        if (panel) {
            console.log('[Samtch] Removing social panel');
            panel.remove();
        }
    }

    function removeWatchOnTwitch() {
        document.querySelectorAll('.tw-svg').forEach(container => {
            const svg = container.querySelector('svg');
            if (svg && svg.getAttribute('viewBox') === '0 0 65 16') {
                const button = container.closest('button');
                if (button) {
                    console.log('[Samtch] Removing Watch on Twitch button');
                    button.remove();
                } else {
                    container.remove();
                }
            }
        });
    }

    function removeClipButtons() {
        const selectors = [
            '[data-a-target="player-clip-button"]',
            'button[aria-label*="Clip"]',
            'button[title*="Clip"]',
            '.clipping-button',
        ];
        selectors.forEach(s => {
            document.querySelectorAll(s).forEach(el => {
                // Double check it's not a settings button or something we want to keep
                const target = el.getAttribute('data-a-target') || '';
                if (!target.includes('settings') && !target.includes('quality') && !target.includes('play')) {
                    console.log('[Samtch] Removing clip button element:', s);
                    el.remove();
                }
            });
        });

        // Remove by label text
        document.querySelectorAll('[data-a-target="tw-core-button-label-text"]').forEach(el => {
            if (el.textContent.trim() === 'Clip') {
                const button = el.closest('button');
                if (button) {
                    console.log('[Samtch] Removing clip button via label text');
                    button.remove();
                }
            }
        });
    }

    let reloadAttempts = 0;
    const maxReloadAttempts = 20; // ~40 seconds total at 2s interval

    function clean() {
        // Check if player exists before executing clean functions
        const playerExists = document.querySelector('.video-player') ||
                           document.querySelector('[data-a-target="video-player"]');

        if (!playerExists) {
            if (!window.samtch_player_wait_logged) {
                console.log('[Samtch] UI Cleaner: Waiting for player...');
                window.samtch_player_wait_logged = true;
            }

            reloadAttempts++;
            if (reloadAttempts > maxReloadAttempts) {
                console.log('[Samtch] UI Cleaner: Player not detected after timeout, performing refresh...');
                reloadAttempts = 0;
                window.location.reload();
            }
            return;
        }

        if (window.samtch_player_wait_logged) {
            console.log('[Samtch] UI Cleaner: Player detected! Starting cleaning...');
            window.samtch_player_wait_logged = false;
        }

        reloadAttempts = 0;

        injectStyles();
        removeSocialPanel();
        removeWatchOnTwitch();
        removeClipButtons();
    }

    // Persistent cleaning and reload check
    setInterval(clean, 2000);

    const observer = new MutationObserver(clean);
    observer.observe(document.documentElement, { childList: true, subtree: true });
    clean();
})();
