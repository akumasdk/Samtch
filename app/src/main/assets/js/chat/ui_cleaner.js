(function() {
    'use strict';

    function removeTransitionGroups() {
        const elements = document.querySelectorAll('.tw-transition-group');
        elements.forEach(element => {
            if (!element.closest('.chat-input')) {
                element.remove();
            }
        });
    }

    function clean() {
        removeTransitionGroups();
    }

    // Use MutationObserver to keep the chat clean of dynamic elements
    const observer = new MutationObserver(clean);
    observer.observe(document.body, { childList: true, subtree: true });

    // Initial execution
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', clean);
    } else {
        clean();
    }
})();
