(function() {
    'use strict';

    const CONFIG = {
        appKeywords: ['open in app', 'get the app', 'use the app', 'continue in browser', 'open the app', 'accept cookies', 'who\'s watching'],
        // Targeted selectors for CSS-based permanent hiding - ONLY high-confidence ones
        selectorsToHide: [
            'div[class*="AppUpsell"]',
            'div[class*="app-upsell"]',
            '.tw-upsell-banner',
            '.disclosure-card',
            '.stream-info-social-panel'
        ]
    };

    function injectPersistentStyles() {
        const styleId = 'samtch-permanent-cleanup';
        if (document.getElementById(styleId)) return;

        console.log('[Samtch] Injecting persistent cleanup styles');
        const style = document.createElement('style');
        style.id = styleId;

        const css = CONFIG.selectorsToHide.map(s => `${s} { display: none !important; }`).join('\n');
        style.textContent = css;

        document.documentElement.appendChild(style);
    }

    function removePromotions(attempt) {
        console.log(`[Samtch] app_banners_remover.js: Pass #${attempt}`);

        injectPersistentStyles();

        const isInsideStreamCard = (el) => !!el.closest('[class*="streamCard"], [class*="stream-card-horizontal"]');

        // 1. Surgical "Open in App" Banner Removal
        const TARGET_CONTENT = "light_upsell_bottom_sheet_open_in_app";
        const targetLink = document.querySelector(`a[href*="${TARGET_CONTENT}"]`);

        if (targetLink) {
            console.log('[Samtch] Targeted "Open in App" link detected.');

            // Find the modal/dialog container.
            // We look for the provided style pattern or standard Twitch modal classes.
            const container = targetLink.closest('.tw-modal, .tw-dialog, div[style*="position: relative"][style*="flex"]') || targetLink.parentElement;

            if (container) {
                // Find the backdrop: the highest parent that is fixed or absolute and covers most of the screen
                let current = container;
                let backdrop = null;
                while (current && current !== document.body) {
                    const style = window.getComputedStyle(current);
                    if (style.position === 'fixed' || style.position === 'absolute') {
                        backdrop = current;
                        // If it's a fullscreen-ish backdrop, we found it
                        if (current.offsetWidth > window.innerWidth * 0.8) break;
                    }
                    current = current.parentElement;
                }

                console.log('[Samtch] Removing confirmed promotion container/backdrop.');
                if (backdrop) backdrop.remove();
                else container.remove();

                // Restore scrolling
                document.body.style.setProperty('overflow', 'auto', 'important');
                document.documentElement.style.setProperty('overflow', 'auto', 'important');
            }
        }

        // 2. Text-based Safety Net (for variants without the specific link)
        document.querySelectorAll('div, section, aside').forEach(el => {
            if (isInsideStreamCard(el)) return;

            const style = window.getComputedStyle(el);
            if (style.position === 'fixed' || style.position === 'absolute') {
                const text = (el.textContent || '').toLowerCase();
                // If it contains "Open in app" AND it's a large overlay
                if (CONFIG.appKeywords.some(k => text.includes(k)) && el.offsetWidth > window.innerWidth * 0.5) {
                    console.log('[Samtch] Removing dynamic promotion via keyword safety net');
                    el.remove();
                    document.body.style.setProperty('overflow', 'auto', 'important');
                }
            }
        });

        // 3. Hide specific navigation links
        const navHidingRegex = /^\/home\/?$|^\/[^/]+\/home\/?$|^\/activity\/?$/;
        const patterns = ['desktop-redirect=true', 'mweb_upsell', 'top_nav_open_in_app'];

        document.querySelectorAll('a[href]').forEach(link => {
            if (isInsideStreamCard(link)) return;
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
    }

    // Initial run
    injectPersistentStyles();
    removePromotions(1);

    // Staggered execution for late-appearing dynamic elements
    [1500, 4000, 8000].forEach((delay, index) => {
        setTimeout(() => removePromotions(index + 2), delay);
    });
})();
