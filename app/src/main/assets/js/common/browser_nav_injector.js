(function() {
    'use strict';
    console.log('[Samtch] browser_nav_injector.js active');

    const NAV_SELECTOR = 'body > div > div:nth-child(1) > div:nth-child(1) > nav';
    const BTN_ID = 'samtch-settings-btn';
    const LOGIN_BTN_ID = 'samtch-login-btn';

    function inject() {
        const nav = document.querySelector(NAV_SELECTOR);
        if (!nav) return;

        const isDark = document.documentElement.classList.contains('tw-root--theme-dark');
        const loggedIn = document.cookie.includes('login=');

        // 1. Login Button (if not logged in)
        if (!loggedIn) {
            if (!document.getElementById(LOGIN_BTN_ID)) {
                const loginBtn = document.createElement('button');
                loginBtn.id = LOGIN_BTN_ID;
                loginBtn.textContent = 'Log In';
                loginBtn.style.cssText = 'margin-left: auto; margin-right: 8px; background: #9147ff; color: white; border: none; padding: 0 12px; height: 30px; font-size: 13px; font-weight: 600; border-radius: 4px; cursor: pointer; flex-shrink: 0;';
                loginBtn.onclick = () => {
                    if (window.TwitchBrowserBridge) window.TwitchBrowserBridge.openLogin();
                };
                nav.appendChild(loginBtn);
            }
        } else {
            const existingLogin = document.getElementById(LOGIN_BTN_ID);
            if (existingLogin) existingLogin.remove();
        }

        // 2. Settings Button
        let settingsBtn = document.getElementById(BTN_ID);
        if (!settingsBtn) {
            settingsBtn = document.createElement('button');
            settingsBtn.id = BTN_ID;
            settingsBtn.innerHTML = '<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>';
            settingsBtn.style.cssText = 'margin-right: 18px; background: transparent; border: none; padding: 0; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; width: 34px; height: 34px; border-radius: 4px; flex-shrink: 0;';
            settingsBtn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (window.TwitchBrowserBridge) window.TwitchBrowserBridge.openSettings();
            };
            nav.appendChild(settingsBtn);
        }

        // Apply theme color and update alignment
        settingsBtn.style.color = isDark ? 'white' : 'black';
        settingsBtn.style.marginLeft = document.getElementById(LOGIN_BTN_ID) ? '4px' : 'auto';
    }

    // Use interval for lower overhead than MutationObserver on large sites
    setInterval(inject, 2000);
    inject();
})();
