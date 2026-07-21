(function() {
    'use strict';

    const CONFIG = {
        appKeywords: ['open in app', 'get the app', 'use the app', 'continue in browser', 'open the app', 'accept cookies', 'who\'s watching'],
        targetedSelectors: [
            'div[class*="AppUpsell"]',
            'div[class*="app-upsell"]',
            '.tw-upsell-banner',
            '.disclosure-card',
            '.stream-info-social-panel'
        ],
        backdropSelectors: [
            'div[class*="overlay"]',
            'div[class*="overlay-background"]',
            'div[class*="backdrop"]',
            '.tw-backdrop',
            '.tw-modal-backdrop'
        ]
    };

    function removePromotions(attempt) {
        console.log(`[Samtch] app_banners_remover.js: Pass #${attempt}`);

        let promotionFound = false;

        // 1. Remove targeted promotion elements
        CONFIG.targetedSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(el => {
                console.log('[Samtch] Removing targeted promotion:', selector);
                el.remove();
                promotionFound = true;
            });
        });

        // 2. Scan for dynamic promotion overlays via text
        document.querySelectorAll('div, section, aside, span, button').forEach(el => {
            // Only check visible elements that are likely modals
            if (el.offsetWidth > 0 && (el.classList.contains('tw-modal') || el.classList.contains('tw-dialog'))) {
                const text = (el.textContent || '').toLowerCase();
                if (CONFIG.appKeywords.some(k => text.includes(k))) {
                     console.log('[Samtch] Removing dynamic promotion modal via text match');
                     el.remove();
                     promotionFound = true;
                }
            }
        });

        // 3. Remove backdrops/overlays if a promotion was found OR if they contain keywords
        CONFIG.backdropSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(el => {
                const text = (el.textContent || '').toLowerCase();
                const hasKeyword = CONFIG.appKeywords.some(k => text.includes(k));

                // If we found a promotion elsewhere, or this backdrop has a keyword, or it's a fixed fullscreen backdrop
                if (promotionFound || hasKeyword || (el.offsetWidth >= window.innerWidth && el.offsetHeight >= window.innerHeight)) {
                    // Check if it's actually a backdrop (fixed/absolute)
                    const style = window.getComputedStyle(el);
                    if (style.position === 'fixed' || style.position === 'absolute') {
                         console.log('[Samtch] Removing backdrop/overlay:', selector);
                         el.remove();
                    }
                }
            });
        });

        // 4. Hide specific navigation links and cleanup body lock
        const navHidingRegex = /^\/home\/?$|^\/[^/]+\/home\/?$|^\/activity\/?$/;
        const patterns = ['desktop-redirect=true', 'mweb_upsell', 'top_nav_open_in_app'];

        document.querySelectorAll('a[href]').forEach(link => {
            const href = link.getAttribute('href');
            if (patterns.some(p => href.includes(p))) {
                link.style.setProperty('display', 'none', 'important');
                return;
            }
            try {
                const url = new URL(href, window.location.origin);
                if (navHidingRegex.test(url.pathname)) {
                    link.style.setProperty('display', 'none', 'important');
                }
            } catch (e) {
                if (navHidingRegex.test(href)) {
                    link.style.setProperty('display', 'none', 'important');
                }
            }
        });

        // Ensure body scrolling is not locked (Twitch often does this)
        if (promotionFound) {
            document.body.style.overflow = 'auto';
            document.documentElement.style.overflow = 'auto';
        }
    }

    // Staggered execution to catch SPA dynamic elements
    removePromotions(1); // Immediate
    setTimeout(() => removePromotions(2), 1000);
    setTimeout(() => removePromotions(3), 2500);
    setTimeout(() => removePromotions(4), 5000);
    setTimeout(() => removePromotions(5), 8000); // Late sweep
})();
