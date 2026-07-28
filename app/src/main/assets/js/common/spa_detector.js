(function() {
    'use strict';
    console.log('[Samtch] spa_detector.js active');

    let lastUrl = window.location.href;

    function notifyUrlChange() {
        const currentUrl = window.location.href;
        // Even if URL is same, we might want to notify for initial injection
        // but to avoid loops, let's be careful.
        if (window.TwitchBrowserBridge) {
            window.TwitchBrowserBridge.onUrlChange(currentUrl);
        }
    }

    // Hook into pushState
    const originalPushState = history.pushState;
    history.pushState = function() {
        originalPushState.apply(this, arguments);
        notifyUrlChange();
    };

    // Hook into replaceState
    const originalReplaceState = history.replaceState;
    history.replaceState = function() {
        originalReplaceState.apply(this, arguments);
        notifyUrlChange();
    };

    // Listen for popstate (back/forward navigation)
    window.addEventListener('popstate', notifyUrlChange);

    // Initial check on script load
    notifyUrlChange();
})();
