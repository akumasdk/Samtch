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
            btn.innerHTML = '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>';
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
