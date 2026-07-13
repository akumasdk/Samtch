(function() {
    'use strict';

    function injectStyles() {
        const styleId = 'samtch-tv-cleaner';
        if (document.getElementById(styleId)) return;

        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            /* Hide all player controls for a clean TV experience */
            .video-player__controls,
            [data-a-target="player-controls"],
            .video-player__overlay,
            .player-controls__right-control-group,
            .player-controls__left-control-group,
            .tw-tower {
                display: none !important;
            }

            /* Ensure video takes full screen */
            .video-player__container, .video-player__default-player {
                background: black !important;
            }

            /* Hide unnecessary buttons and banners */
            [data-a-target="player-fullscreen-button"],
            [data-a-target="player-clip-button"],
            [data-a-target="player-theatre-mode-button"],
            .ad-banner,
            .tw-upsell-banner {
                display: none !important;
            }
        `;
        document.head.appendChild(style);
    }

    function removeControls() {
        const controls = document.querySelector('.video-player__controls');
        if (controls) {
            controls.remove();
        }
    }

    function clean() {
        injectStyles();
        removeControls();
    }

    const observer = new MutationObserver(clean);
    observer.observe(document.body, { childList: true, subtree: true });
    clean();
})();
