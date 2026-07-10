// Refactored Auto-Close/Hide Dialog script for Twitch
// Focuses on DOM manipulation and removal instead of simulated clicks
(function() {
    'use strict';

    const CONFIG = {
        // Selectors that are always safe to remove or hide (app banners, modals, and backdrops)
        removeSelectors: [
            'div[class*="overlay"]',
        ],
        // Keywords that identify "Open in App" or "App Upsell" elements
        appKeywords: ['open in app', 'get the app', 'use the app', 'continue in browser', 'open the app', 'accept cookies', 'who\'s watching'],
        // Elements to check for keywords
        checkTags: ['DIV', 'SECTION', 'ASIDE', 'A', 'BUTTON', 'SPAN']
    };

    /**
     * Checks if an element is an app promotion or blocking dialog
     */
    function isAppPromotion(el) {
        if (!el || el.nodeType !== 1) return false;

        // Check matching selectors
        if (CONFIG.removeSelectors.some(s => {
            try { return el.matches(s); } catch(e) { return false; }
        })) return true;

        // Check text content for specific keywords in dialog-like elements
        if (CONFIG.checkTags.includes(el.tagName)) {
            const text = (el.textContent || '').toLowerCase();
            if (CONFIG.appKeywords.some(k => text.includes(k))) {
                const style = window.getComputedStyle(el);
                const isOverlay = style.position === 'fixed' || style.position === 'absolute' || el.classList.contains('tw-modal');
                const isAction = el.tagName === 'A' || el.tagName === 'BUTTON' || el.getAttribute('role') === 'button';

                if (isOverlay || isAction) {
                    // Avoid removing the entire main content
                    if (el.offsetWidth < window.innerWidth * 0.95) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes an element and its associated wrappers if they are now empty/useless
     */
    function removePromotion(el) {
        if (!el || el === document.body || el === document.documentElement) return false;

        if (isAppPromotion(el)) {
            console.log('[Samtch] Removing app promotion:', el.tagName, '.' + (el.className || '').split(' ').join('.'));

            // If it's a modal or backdrop, we definitely want it gone
            const parent = el.parentElement;
            el.remove();

            // Optional: check if parent is now empty and was a wrapper
            if (parent && parent.children.length === 0 && (parent.className || '').toLowerCase().includes('wrapper')) {
                parent.remove();
            }
            return true;
        }
        return false;
    }

    /**
     * Twitch often disables scrolling on body/html when a modal is "active"
     * This function forces the page to be scrollable.
     */
    function fixPageState() {
        const elements = [document.body, document.documentElement];
        let fixed = false;
        elements.forEach(el => {
            if (el) {
                const style = window.getComputedStyle(el);
                if (style.overflow === 'hidden' || el.style.overflow === 'hidden') {
                    el.style.setProperty('overflow', 'auto', 'important');
                    el.style.setProperty('position', 'static', 'important');
                    fixed = true;
                }
            }
        });
        if (fixed) console.log('[Samtch] Restored page scrolling');
    }

    /**
     * Injects CSS to hide known selectors immediately and prevent layout shifts
     */
    function injectStyles() {
        if (document.getElementById('samtch-hide-css')) return;

        const style = document.createElement('style');
        style.id = 'samtch-hide-css';
        style.textContent = `
            ${CONFIG.removeSelectors.join(', ')} {
                display: none !important;
                visibility: hidden !important;
                pointer-events: none !important;
                height: 0 !important;
                width: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
            }
            /* Ensure the player container is always visible */
            .video-player, [data-a-target="video-player"] {
                display: block !important;
                visibility: visible !important;
            }
            /* Unlock scrolling */
            body, html {
                overflow: auto !important;
                position: static !important;
                height: 100% !important;
            }
        `;
        document.head.appendChild(style);
    }

    /**
     * Specifically targets and hides the "Open in App" or "Desktop Redirect" buttons
     * that use complex tracking URLs.
     */
    function hideTargetedButtons() {
        const patterns = ['desktop-redirect=true', 'mweb_upsell', 'top_nav_open_in_app'];
        document.querySelectorAll('a[href]').forEach(link => {
            const href = link.getAttribute('href');
            if (patterns.some(p => href.includes(p))) {
                if (link.style.display !== 'none') {
                    console.log('[Samtch] Hiding targeted redirect link');
                    link.style.setProperty('display', 'none', 'important');
                }
            }
        });
    }

    // --- Execution ---

    injectStyles();

    // Initial pass: scan existing elements
    hideTargetedButtons();
    const allElements = document.querySelectorAll('*');
    allElements.forEach(removePromotion);
    fixPageState();

    const observer = new MutationObserver(mutations => {
        let needsFix = false;
        hideTargetedButtons();
        for (const mutation of mutations) {
            for (const node of mutation.addedNodes) {
                if (node.nodeType === 1) {
                    if (removePromotion(node)) {
                        needsFix = true;
                    } else {
                        // Scan children for text-based matches
                        const children = node.querySelectorAll('div, a, button, span');
                        children.forEach(child => {
                            if (removePromotion(child)) needsFix = true;
                        });
                    }
                }
            }
        }
        if (needsFix) fixPageState();
    });

    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    // Periodic check to counteract late-running Twitch scripts that lock the body
    setInterval(fixPageState, 1500);

    console.log('[Samtch] Auto-hide script active (Removal mode)');
})();
