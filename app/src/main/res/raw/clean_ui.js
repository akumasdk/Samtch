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

            /* Estilos para los botones personalizados */
            .samtch-control-btn {
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
            .samtch-control-btn:hover {
                background: rgba(255, 255, 255, 0.1);
            }
            .samtch-control-btn svg {
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

    function injectCustomControls() {
        const rightGroup = document.querySelector('.player-controls__right-control-group');
        if (!rightGroup) return;

        // 1. Botón de Chat
        if (!document.getElementById('samtch-chat-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-chat-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Toggle Chat';
            btn.innerHTML = `
                <svg width="24" height="24" viewBox="0 0 24 24">
                    <path d="M12 2C6.477 2 2 6.477 2 12c0 1.821.487 3.53 1.338 5L2.1 21.9l4.9-1.238A9.956 9.956 0 0 0 12 22c5.523 0 10-4.477 10-10S17.523 2 12 2zm0 18c-1.477 0-2.872-.37-4.1-.1.023l-3.324.84.84-3.324c-.63-1.228-1-2.623-1-4.1 0-4.411 3.589-8 8-8s8 3.589 8 8-3.589 8-8 8z"></path>
                </svg>
            `;
            btn.onclick = function() {
                if (window.SamtchBridge) {
                    window.SamtchBridge.toggleChat();
                }
            };
            // Insertar antes de cualquier otro botón personalizado o al final
            rightGroup.appendChild(btn);
        }

        // 2. Botón de Pantalla Completa
        if (!document.getElementById('samtch-fullscreen-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-fullscreen-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Toggle Fullscreen';
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
        injectCustomControls();
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
