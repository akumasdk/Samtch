(function() {
    'use strict';

    // Self-cleanup: Clear existing intervals and observers from previous runs
    if (window.samtch_controls_init_int) clearInterval(window.samtch_controls_init_int);
    if (window.samtch_controls_maint_int) clearInterval(window.samtch_controls_maint_int);
    if (window.samtch_controls_obs) window.samtch_controls_obs.disconnect();

    console.log('[Samtch] controls_injector.js starting fresh session...');

    function injectStyles() {
        const styleId = 'samtch-controls-styles';
        if (document.getElementById(styleId)) return;
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            .samtch-control-btn {
                background: transparent; border: none; color: white; cursor: pointer;
                display: flex; align-items: center; justify-content: center;
                padding: 0 8px; height: 100%;
                opacity: 0.9; transition: opacity 0.2s;
            }
            .samtch-control-btn:hover { opacity: 1; background: rgba(255, 255, 255, 0.15); }
            .samtch-control-btn svg { fill: currentColor; }

            /* Ensure the control group is visible enough */
            .player-controls__right-control-group { overflow: visible !important; }
        `;
        document.head.appendChild(style);
    }

    function injectButtons() {
        // Broad range of selectors to find the control container
        const selectors = [
            '.player-controls__right-control-group',
            '[data-a-target="player-controls"] .tw-justify-content-end',
            '.video-player__controls .tw-justify-content-end',
            '.video-player__controls .tw-align-items-center.tw-flex-row'
        ];

        let rightGroup = null;
        for (const s of selectors) {
            rightGroup = document.querySelector(s);
            if (rightGroup) break;
        }

        if (!rightGroup) return false;

        let injectedCount = 0;

        // 1. Chat Toggle Button
        if (!document.getElementById('samtch-chat-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-chat-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Toggle Chat';
            btn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>';
            btn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (window.TwitchPlayerBridge) {
                    window.TwitchPlayerBridge.toggleChat();
                } else {
                    console.error('[Samtch] Bridge not found for chat toggle');
                }
            };
            rightGroup.appendChild(btn);
            injectedCount++;
        }

        // 2. Fullscreen Toggle Button
        if (!document.getElementById('samtch-fullscreen-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-fullscreen-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Toggle Fullscreen';
            btn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24"><path d="M8 3v2H3.996v4H2V3h6ZM2 15v6h6v-2H4v-4H2Zm18.002 0-.024 4H16v2h6v-6h-1.998ZM22 9V3h-5.993v2H20l.002 4H22Z"></path></svg>';
            btn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (window.TwitchPlayerBridge) {
                    window.TwitchPlayerBridge.toggleFullscreen();
                } else {
                    console.error('[Samtch] Bridge not found for fullscreen toggle');
                }
            };
            rightGroup.appendChild(btn);
            injectedCount++;
        }

        if (injectedCount > 0) {
            console.log('[Samtch] Buttons injected successfully (' + injectedCount + ')');
        }
        return true;
    }

    injectStyles();

    // Aggressive polling for the first 10 seconds
    const startTime = Date.now();
    window.samtch_controls_init_int = setInterval(() => {
        const success = injectButtons();
        if (success || Date.now() - startTime > 10000) {
            clearInterval(window.samtch_controls_init_int);
            // Switch to low-frequency maintenance polling
            window.samtch_controls_maint_int = setInterval(injectButtons, 3000);
        }
    }, 500);

    // Watch for dynamic UI updates (React transitions)
    window.samtch_controls_obs = new MutationObserver(injectButtons);
    window.samtch_controls_obs.observe(document.documentElement, { childList: true, subtree: true });

    injectButtons();
})();
