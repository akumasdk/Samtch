(function() {
    'use strict';
    console.log('[Samtch] spa_detector.js active');

    function notifyUrlChange() {
        const currentUrl = window.location.href;
        if (window.TwitchBrowserBridge) {
            window.TwitchBrowserBridge.onUrlChange(currentUrl, false);
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
