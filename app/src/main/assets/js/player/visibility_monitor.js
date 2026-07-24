(function() {
    'use strict';

    if (window.samtch_visibility_int) clearInterval(window.samtch_visibility_int);
    if (window.samtch_visibility_obs) window.samtch_visibility_obs.disconnect();

    function syncVisibility() {
        const controls = document.querySelector('[data-a-target="player-controls"]');
        const isVisible = controls ? controls.getAttribute('data-a-visible') === 'true' : false;
        const signal = isVisible ? 'samtch:ui:show' : 'samtch:ui:hide';
        if (document.title !== signal) {
            document.title = signal;
        }
    }

    window.samtch_visibility_int = setInterval(syncVisibility, 500);

    window.samtch_visibility_obs = new MutationObserver(syncVisibility);
    window.samtch_visibility_obs.observe(document.documentElement, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['data-a-visible']
    });

    syncVisibility();
})();
