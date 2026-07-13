(function() {
    'use strict';
    function syncVisibility() {
        const controls = document.querySelector('[data-a-target="player-controls"]');
        const isVisible = controls ? controls.getAttribute('data-a-visible') === 'true' : false;
        const signal = isVisible ? 'samtch:ui:show' : 'samtch:ui:hide';
        if (document.title !== signal) {
            document.title = signal;
        }
    }
    setInterval(syncVisibility, 500);
    const observer = new MutationObserver(syncVisibility);
    observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['data-a-visible'] });
})();
