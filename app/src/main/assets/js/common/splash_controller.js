(function() {
    function notifyLoaded() {
        if (window.TwitchBrowserBridge && typeof window.TwitchBrowserBridge.onDomLoaded === 'function') {
            console.log("Notifying Android: DOM Loaded");
            window.TwitchBrowserBridge.onDomLoaded();
        }
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        notifyLoaded();
    } else {
        window.addEventListener('DOMContentLoaded', notifyLoaded);
        // Fallback for extreme cases
        window.addEventListener('load', notifyLoaded);
    }

    // Secondary fallback: if nothing happens after 3 seconds of script execution
    setTimeout(notifyLoaded, 3000);
})();
