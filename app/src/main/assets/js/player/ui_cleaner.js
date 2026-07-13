(function() {
    'use strict';

    function injectStyles() {
        const styleId = 'samtch-player-cleaner';
        if (document.getElementById(styleId)) return;

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
                if (button) button.remove(); else container.remove();
            }
        });
    }

    function removeClipButtons() {
        const selectors = [
            '[data-a-target="player-clip-button"]',
            'button[aria-label*="Clip"]',
            'button[title*="Clip"]',
            '.clipping-button'
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
    }

    function clean() {
        injectStyles();
        removeSocialPanel();
        removeWatchOnTwitch();
        removeClipButtons();
    }

    const observer = new MutationObserver(clean);
    observer.observe(document.body, { childList: true, subtree: true });
    clean();
})();
