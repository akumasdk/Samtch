(function() {
    'use strict';
    console.log('[Samtch] browser_nav_injector.js active');

    const NAV_SELECTOR = 'body > div > div:nth-child(1) > div:nth-child(1) > nav';
    const BTN_ID = 'samtch-settings-btn';

    function injectSettingsButton() {
        const topNav = document.querySelector(NAV_SELECTOR);
        if (!topNav) return;

        // Detect dark mode from Twitch's root element
        const isDarkMode = document.documentElement.classList.contains('tw-root--theme-dark');

        let btn = document.getElementById(BTN_ID);
        if (btn) {
            btn.style.color = isDarkMode ? 'white' : 'black';
            return;
        }

        btn = document.createElement('button');
        btn.id = BTN_ID;
        btn.setAttribute('aria-label', 'Samtch Settings');

        // Styles matching Twitch's mobile UI
        btn.style.marginRight = '18px';
        btn.style.marginLeft = '4px';
        btn.style.color = isDarkMode ? 'white' : 'black';
        btn.style.background = 'transparent';
        btn.style.border = 'none';
        btn.style.padding = '0';
        btn.style.display = 'inline-flex';
        btn.style.alignItems = 'center';
        btn.style.justifyContent = 'center';
        btn.style.cursor = 'pointer';
        btn.style.width = '34px';
        btn.style.height = '34px';
        btn.style.borderRadius = '4px';
        btn.style.flexShrink = '0';

        // Hover effect adjusted for theme
        btn.onmouseenter = () => {
            const isDark = document.documentElement.classList.contains('tw-root--theme-dark');
            btn.style.background = isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)';
        };
        btn.onmouseleave = () => btn.style.background = 'transparent';

        // Cogwheel SVG
        btn.innerHTML = `
            <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="3"></circle>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
            </svg>
        `;

        btn.onclick = (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (window.TwitchBrowserBridge) {
                window.TwitchBrowserBridge.openSettings();
            }
        };

        topNav.appendChild(btn);
    }

    // Use MutationObserver for reliability (Twitch re-renders nav often)
    const observer = new MutationObserver(injectSettingsButton);
    observer.observe(document.documentElement, { childList: true, subtree: true });

    injectSettingsButton();
})();
