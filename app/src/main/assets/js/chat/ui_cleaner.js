(function() {
    'use strict';

    function clean() {
        const elements = document.querySelectorAll('.tw-transition-group');
        elements.forEach(element => {
            if (!element.closest('.chat-input')) {
                element.remove();
            }
        });
    }

    // Use MutationObserver to keep the chat clean of dynamic elements
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
