(function() {
    'use strict';
    const CONFIG = {
        appKeywords: ['open in app', 'get the app', 'use the app', 'continue in browser', 'open the app', 'accept cookies', 'who\'s watching'],
        checkTags: ['DIV', 'SECTION', 'ASIDE', 'A', 'BUTTON', 'SPAN'],
        removeSelectors: [
            '.stream-info-social-panel',
            'div[class*="overlay"]'
        ]
    };

    function isAppPromotion(el) {
        if (!el || el.nodeType !== 1) return false;

        // Hard safety: NEVER remove anything inside the player or functional UI components
        if (el.id === 'root' ||
            el.classList.contains('video-player') ||
            el.classList.contains('video-player__overlay') ||
            el.closest('.video-player') ||
            el.closest('[data-a-target="video-player"]') ||
            el.closest('[data-a-target="player-controls"]') ||
            el.closest('[data-a-target="player-settings-menu"]') ||
            el.closest('[data-a-target="player-settings-balloon"]') ||
            el.closest('.tw-balloon') ||
            el.closest('.tw-dialog') ||
            el.closest('.video-player__container') ||
            (el.hasAttribute('data-a-target') && el.getAttribute('data-a-target').includes('settings'))) {
            return false;
        }

        // If it's a backdrop or overlay, only remove it if it contains app-promotion keywords
        // or if it's explicitly in our blacklist (like upsell banners)
        // AND ONLY if we are NOT in the player subdomain
        const isPlayerSubdomain = window.location.hostname === 'player.twitch.tv';
        const isGenericOverlay = el.matches('.tw-backdrop, .tw-modal-backdrop, div[class*="backdrop"], div[class*="overlay-background"]');
        if (isGenericOverlay && !isPlayerSubdomain) {
            const text = (el.textContent || '').toLowerCase();
            const hasAppKeyword = CONFIG.appKeywords.some(k => text.includes(k));
            const isUpsell = el.matches('.tw-upsell-banner, [class*="AppUpsell"]');
            return hasAppKeyword || isUpsell;
        }

        if (CONFIG.removeSelectors.some(s => el.matches && el.matches(s))) return true;

        if (CONFIG.checkTags.includes(el.tagName)) {
            const text = (el.textContent || '').toLowerCase();
            if (CONFIG.appKeywords.some(k => text.includes(k))) {
                const style = window.getComputedStyle(el);
                const isOverlay = style.position === 'fixed' || style.position === 'absolute' || el.classList.contains('tw-modal');

                if (isOverlay && el.offsetWidth < window.innerWidth * 0.95) {
                    return true;
                }
            }
        }
        return false;
    }

    function removePromotions() {
        document.querySelectorAll('*').forEach(el => {
            if (isAppPromotion(el)) {
                console.log('[Samtch] Removing promotion/backdrop:', el.tagName, el.className);
                el.remove();
            }
        });

        const patterns = ['desktop-redirect=true', 'mweb_upsell', 'top_nav_open_in_app'];

        // Regex for navigation items we want to hide
        // 1. /home
        // 2. /something/home
        // 3. /activity
        const navHidingRegex = /^\/home\/?$|^\/[^/]+\/home\/?$|^\/activity\/?$/;

        document.querySelectorAll('a[href]').forEach(link => {
            const href = link.getAttribute('href');

            // Original app promotion patterns
            if (patterns.some(p => href.includes(p))) {
                link.style.setProperty('display', 'none', 'important');
                return;
            }

            // New navigation removal patterns - only target elements inside .tw-transition container
            // This is usually the side/top menu that slides in
            if (link.closest('.tw-transition')) {
                try {
                    const url = new URL(href, window.location.origin);
                    if (navHidingRegex.test(url.pathname)) {
                        console.log('[Samtch] Removing navigation link inside .tw-transition:', href);
                        link.style.setProperty('display', 'none', 'important');
                    }
                } catch (e) {
                    // Handle relative paths or invalid URLs
                    if (navHidingRegex.test(href)) {
                        console.log('[Samtch] Removing relative navigation link inside .tw-transition:', href);
                        link.style.setProperty('display', 'none', 'important');
                    }
                }
            }
        });
    }

    const observer = new MutationObserver(removePromotions);
    observer.observe(document.documentElement, { childList: true, subtree: true });
    removePromotions();
})();
