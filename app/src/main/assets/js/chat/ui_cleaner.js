(function() {
    'use strict';

    function removeTransitionGroup() {
        const element = document.querySelector('.stream-chat-header');
        if (element) {
            console.log('[Samtch] Removing the first transition group element');
            element.remove();
        }
    }

    function clean() {
        removeTransitionGroup();
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
