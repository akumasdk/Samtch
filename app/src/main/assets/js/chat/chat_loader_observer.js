(function() {
    'use strict';

    console.log('Chat Loader Observer script started');

    // Function to check if chat input is loaded
    function checkChatInputLoaded() {
        const chatInput = document.querySelector('.chat-input');
        if (chatInput) {
            console.log('Chat input element found!');
            // Notify Kotlin through the JavaScript interface
            if (window.TwitchChatBridge) {
                window.TwitchChatBridge.onChatLoaded();
            }
            return true;
        }
        return false;
    }

    // Try immediately
    if (checkChatInputLoaded()) {
        return;
    }

    // Use MutationObserver to watch for DOM changes
    const observer = new MutationObserver(function(mutations) {
        if (checkChatInputLoaded()) {
            observer.disconnect();
        }
    });

    // Start observing the document body for changes
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    // Fallback timeout after 10 seconds
    setTimeout(function() {
        if (!document.querySelector('.chat-input')) {
            console.log('Chat input not found after 10 seconds, notifying anyway');
            if (window.TwitchChatBridge) {
                window.TwitchChatBridge.onChatLoaded();
            }
        }
        observer.disconnect();
    }, 10000);
})();

