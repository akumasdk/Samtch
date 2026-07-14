(function() {
    'use strict';
    console.log('[Samtch] controls_injector.js active');

    function injectStyles() {
        const styleId = 'samtch-controls-styles';
        if (document.getElementById(styleId)) return;
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            .samtch-control-btn {
                background: transparent; border: none; color: white; cursor: pointer;
                display: flex; align-items: center; justify-content: center;
                padding: 0 10px; height: 100%;
            }
            .samtch-control-btn:hover { background: rgba(255, 255, 255, 0.1); }
            .samtch-control-btn svg { fill: currentColor; }
        `;
        document.head.appendChild(style);
    }

    function injectButtons() {
        // Try multiple selectors for the control group to be more resilient
        const rightGroup = document.querySelector('.player-controls__right-control-group') ||
                           document.querySelector('[data-a-target="player-controls"] .tw-justify-content-end');

        if (!rightGroup) return;

        // 1. Chat Toggle Button
        if (!document.getElementById('samtch-chat-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-chat-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Toggle Chat';
            btn.innerHTML = '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M12 2C6.477 2 2 6.477 2 12c0 1.821.487 3.53 1.338 5L2.1 21.9l4.9-1.238A9.956 9.956 0 0 0 12 22c5.523 0 10-4.477 10-10S17.523 2 12 2zm0 18c-1.477 0-2.872-.37-4.1-.1.023l-3.324.84.84-3.324c-.63-1.228-1-2.623-1-4.1 0-4.411 3.589-8 8-8s8 3.589 8 8-3.589 8-8 8z"></path></svg>';
            btn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (window.TwitchPlayerBridge) window.TwitchPlayerBridge.toggleChat();
            };
            rightGroup.appendChild(btn);
            console.log('[Samtch] Chat button injected');
        }

        // 2. Fullscreen Toggle Button
        if (!document.getElementById('samtch-fullscreen-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-fullscreen-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Toggle Fullscreen';
            btn.innerHTML = '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M8 3v2H3.996v4H2V3h6ZM2 15v6h6v-2H4v-4H2Zm18.002 0-.024 4H16v2h6v-6h-1.998ZM22 9V3h-5.993v2H20l.002 4H22Z"></path></svg>';
            btn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (window.TwitchPlayerBridge) window.TwitchPlayerBridge.toggleFullscreen();
            };
            rightGroup.appendChild(btn);
            console.log('[Samtch] Fullscreen button injected');
        }
    }

    injectStyles();
    // Use an interval in addition to MutationObserver for extra persistence
    setInterval(injectButtons, 2000);

    const observer = new MutationObserver(injectButtons);
    observer.observe(document.documentElement, { childList: true, subtree: true });

    injectButtons();
})();
