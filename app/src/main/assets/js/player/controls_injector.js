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
                flex-shrink: 0 !important;
            }
            .samtch-control-btn:hover { opacity: 1; background: rgba(255, 255, 255, 0.15); }
            .samtch-control-btn svg { fill: currentColor; }

            /* Ensure left & right control groups are visible and don't wrap */
            .player-controls__left-control-group,
            .player-controls__right-control-group {
                overflow: visible !important;
                flex-wrap: nowrap !important;
            }
        `;
        document.head.appendChild(style);
    }

    function injectButtons() {
        const rightSelectors = [
            '.player-controls__right-control-group',
            '[data-a-target="player-controls"] .tw-justify-content-end',
            '.video-player__controls .tw-justify-content-end',
            '.video-player__controls .tw-align-items-center.tw-flex-row'
        ];

        const leftSelectors = [
            '.player-controls__left-control-group',
            '[data-a-target="player-controls"] .tw-justify-content-start',
            '.video-player__controls .tw-justify-content-start'
        ];

        let rightGroup = null;
        for (const s of rightSelectors) {
            rightGroup = document.querySelector(s);
            if (rightGroup) break;
        }

        let leftGroup = null;
        for (const s of leftSelectors) {
            leftGroup = document.querySelector(s);
            if (leftGroup) break;
        }

        if (!rightGroup && !leftGroup) return false;

        let injectedCount = 0;

        // 1. Audio Only Toggle Button (Headset) - Right Group
        if (rightGroup && !document.getElementById('samtch-audio-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-audio-btn';
            btn.className = 'samtch-control-btn';
            btn.title = 'Audio Only Mode';
            btn.innerHTML = '<svg width="22" height="22" viewBox="0 0 640 640"><path d="M160 288C160 199.6 231.6 128 320 128C408.4 128 480 199.6 480 288L480 325.5C470 322 459.2 320 448 320L432 320C405.5 320 384 341.5 384 368L384 496C384 522.5 405.5 544 432 544L448 544C501 544 544 501 544 448L544 288C544 164.3 443.7 64 320 64C196.3 64 96 164.3 96 288L96 448C96 501 139 544 192 544L208 544C234.5 544 256 522.5 256 496L256 368C256 341.5 234.5 320 208 320L192 320C180.8 320 170 321.9 160 325.5L160 288z"></path></svg>';
            btn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (window.TwitchPlayerBridge) {
                    window.TwitchPlayerBridge.toggleAudioOnly();
                } else {
                    console.error('[Samtch] Bridge not found for audio toggle');
                }
            };
            rightGroup.prepend(btn);
            injectedCount++;
        }

        // 2. Fullscreen Toggle Button - Right Group
        if (rightGroup && !document.getElementById('samtch-fullscreen-btn')) {
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
            rightGroup.prepend(btn);
            injectedCount++;
        }

        // 3. Audio Compressor Toggle Button - Left Group (preferred)
        const compressorGroup = leftGroup || rightGroup;
        if (compressorGroup && !document.getElementById('samtch-compressor-btn')) {
            const btn = document.createElement('button');
            btn.id = 'samtch-compressor-btn';
            btn.className = 'samtch-control-btn';

            const isActive = (window.samtch_is_compressor_active && window.samtch_is_compressor_active());
            btn.title = isActive ? 'Audio Compressor (ON)' : 'Audio Compressor (OFF)';
            if (isActive) {
                btn.classList.add('active');
                btn.style.color = '#9147ff';
            }

            btn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24"><path d="M10 20h4V4h-4v16zm-6 0h4v-8H4v8zM16 9v11h4V9h-4z"></path></svg>';
            btn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                const toggleFn = window.toggleCompressor || window.samtch_toggle_compressor;
                if (toggleFn) {
                    const active = toggleFn();
                    if (window.TwitchPlayerBridge && window.TwitchPlayerBridge.onCompressorToggled) {
                        window.TwitchPlayerBridge.onCompressorToggled(active);
                    }
                } else {
                    console.error('[Samtch] Audio compressor module not loaded.');
                }
            };

            if (leftGroup) {
                if (leftGroup.lastElementChild) {
                    leftGroup.insertBefore(btn, leftGroup.lastElementChild);
                } else {
                    leftGroup.appendChild(btn);
                }
            } else {
                rightGroup.prepend(btn);
            }
            injectedCount++;
        }

        if (injectedCount > 0) {
            console.log('[Samtch] Buttons injected successfully (' + injectedCount + ')');
            document.documentElement.classList.add('samtch-ready');
        } else if (document.getElementById('samtch-compressor-btn') || document.getElementById('samtch-audio-btn')) {
            document.documentElement.classList.add('samtch-ready');
        }
        return injectedCount > 0;
    }

    injectStyles();

    const startTime = Date.now();
    window.samtch_controls_init_int = setInterval(() => {
        const success = injectButtons();
        if (success || Date.now() - startTime > 10000) {
            clearInterval(window.samtch_controls_init_int);
            window.samtch_controls_maint_int = setInterval(injectButtons, 3000);
        }
    }, 500);

    window.samtch_controls_obs = new MutationObserver(injectButtons);
    window.samtch_controls_obs.observe(document.documentElement, { childList: true, subtree: true });

    injectButtons();
})();
