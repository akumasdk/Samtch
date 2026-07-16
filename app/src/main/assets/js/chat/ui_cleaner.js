(function() {
    'use strict';

    function clean() {
        // 1. Remove transition groups that aren't part of the chat input
        // This targets the dynamic containers Twitch uses for highlights and banners
        const transitions = document.querySelectorAll('.tw-transition-group');
        transitions.forEach(element => {
            if (!element.closest('.chat-input')) {
                element.style.display = 'none';
            }
        });

        // 2. Directly target and hide community highlights and their containers
        const highlightSelectors = [
            '.sticky-community-highlight',
            '.community-highlight-stack__card',
            '.community-highlight-stack__card--wide',
            '.community-highlight-stack',
            '.community-highlight-stack__nav-target',
            '.highlight',
            '.highlight__collapsed',
            '.pinned-chat__highlight-card',
            '.pinned-chat__highlight-card__collapsed',
            '.community-highlight-stack__backlog-card'
        ];

        highlightSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(el => {
                // Try to hide the most relevant parent container to avoid leaving empty spaces
                // We look for transition groups or specific stack containers
                const container = el.closest('.community-highlight-stack__scroll-area--disable') ||
                                  el.closest('.community-highlight-stack__scroll-content--disable') ||
                                  el.closest('.scrollable-area') ||
                                  el.closest('.tw-transition-group') ||
                                  el.closest('.community-highlight-stack') ||
                                  el.parentElement;

                if (container && container !== document.body) {
                    container.style.display = 'none';
                } else {
                    el.style.display = 'none';
                }
            });
        });
    }

    // Use MutationObserver to keep the chat clean as Twitch dynamically adds elements
    const observer = new MutationObserver((mutations) => {
        let added = false;
        for (const mutation of mutations) {
            if (mutation.addedNodes.length > 0) {
                added = true;
                break;
            }
        }
        if (added) {
            clean();
        }
    });

    observer.observe(document.body, { childList: true, subtree: true });

    // Initial execution
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', clean);
    } else {
        clean();
    }
})();
