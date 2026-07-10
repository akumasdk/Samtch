(function() {
    'use strict';
    console.log('[Samtch] clean_ui.js active');

    function injectStyles() {
        if (document.getElementById('samtch-ui-styles')) return;

        const style = document.createElement('style');
        style.id = 'samtch-ui-styles';
        style.textContent = `
            /* 1. Ocultar botones NO deseados dentro de los grupos de control */
            [data-a-target="player-fullscreen-button"],
            [data-a-target="player-clip-button"],
            [data-a-target="player-forward-button"],
            [data-a-target="player-rewind-button"],
            [data-a-target="player-theatre-mode-button"],
            .samtch-hidden-button {
                display: none !important;
            }

            /* 2. Asegurar visibilidad de controles esenciales */
            [data-a-target="player-play-pause-button"],
            [data-a-target="player-mute-unmute-button"],
            .volume-slider__slider-container,
            [data-a-target="player-settings-button"] {
                display: flex !important;
            }

            /* 3. Limpieza de elementos que podrían reaparecer */
            .stream-info-social-panel {
                display: none !important;
            }

            /* Estilos para el botón personalizado */
            .samtch-fullscreen-btn {
                background: transparent;
                border: none;
                color: white;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 0 10px;
                height: 100%;
            }
            .samtch-fullscreen-btn:hover {
                background: rgba(255, 255, 255, 0.1);
            }
            .samtch-fullscreen-btn svg {
                fill: currentColor;
            }
        `;
        document.head.appendChild(style);
    }

    function removeSocialPanel() {
        const socialPanel = document.querySelector('.stream-info-social-panel');
        if (socialPanel) {
            socialPanel.remove();
        }
    }

    function removeWatchOnTwitch() {
        const containers = document.querySelectorAll('.tw-svg');
        containers.forEach(container => {
            const svg = container.querySelector('svg');
            if (svg && svg.getAttribute('viewBox') === '0 0 65 16') {
                const button = container.closest('button');
                if (button) {
                    button.remove();
                } else {
                    container.remove();
                }
            }
        });
    }

    function removeClipButton() {
        const selectors = [
            '[data-a-target="player-clip-button"]',
            'button[aria-label^="Clip"]',
            'button[title^="Clip"]'
        ];
        selectors.forEach(selector => {
            const btn = document.querySelector(selector);
            if (btn) {
                console.log('[Samtch] Removing clip button via JS');
                btn.remove();
            }
        });
    }

    function injectCustomFullscreenButton() {
        const rightGroup = document.querySelector('.player-controls__right-control-group');
        if (rightGroup && !document.getElementById('samtch-fullscreen-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-fullscreen-btn';
            btn.className = 'samtch-fullscreen-btn';
            btn.innerHTML = `
                <svg width="24" height="24" viewBox="0 0 24 24">
                    <path d="M8 3v2H3.996v4H2V3h6ZM2 15v6h6v-2H4v-4H2Zm18.002 0-.024 4H16v2h6v-6h-1.998ZM22 9V3h-5.993v2H20l.002 4H22Z"></path>
                </svg>
            `;
            btn.onclick = function() {
                if (window.SamtchBridge) {
                    window.SamtchBridge.toggleFullscreen();
                }
            };
            rightGroup.appendChild(btn);
        }
    }

    function disableTwitchLinks() {
        const twLinks = document.querySelectorAll('.tw-link');
        twLinks.forEach(link => {
            if (!link.hasAttribute('data-samtch-disabled')) {
                link.setAttribute('data-samtch-disabled', 'true');
                link.style.pointerEvents = 'none';
                link.style.cursor = 'default';
                link.style.textDecoration = 'none';
                link.onclick = (e) => e.preventDefault();
            }
        });
    }

    let lastTitle = "";
    function syncVisibility() {
        const controls = document.querySelector('[data-a-target="player-controls"]');
        if (controls) {
            const isVisible = controls.getAttribute('data-a-visible') === 'true';
            const signal = isVisible ? 'samtch:ui:show' : 'samtch:ui:hide';
            if (document.title !== signal) {
                document.title = signal;
                if (lastTitle !== signal) {
                    console.log('[Samtch] Visibility signal: ' + signal);
                    lastTitle = signal;
                }
            }
        } else {
            if (document.title !== 'samtch:ui:hide') {
                document.title = 'samtch:ui:hide';
            }
        }
    }

    function runCleaning() {
        injectStyles();
        removeSocialPanel();
        removeWatchOnTwitch();
        removeClipButton();
        injectCustomFullscreenButton();
        disableTwitchLinks();
        syncVisibility();
    }

    // Intervalo para visibilidad
    setInterval(syncVisibility, 500);

    // Observador para cambios dinámicos en el DOM
    const observer = new MutationObserver(() => {
        runCleaning();
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['data-a-visible', 'class', 'style']
    });

    // Ejecución inicial
    runCleaning();
})();
