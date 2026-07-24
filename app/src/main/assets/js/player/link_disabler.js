(function() {
    'use strict';

    if (window.samtch_link_obs) window.samtch_link_obs.disconnect();

    function disableTwitchLinks() {
        document.querySelectorAll('.tw-link').forEach(link => {
            if (!link.hasAttribute('data-samtch-disabled')) {
                link.setAttribute('data-samtch-disabled', 'true');
                link.style.pointerEvents = 'none';
                link.onclick = (e) => e.preventDefault();
            }
        });
    }

    window.samtch_link_obs = new MutationObserver(disableTwitchLinks);
    window.samtch_link_obs.observe(document.documentElement, { childList: true, subtree: true });

    disableTwitchLinks();
})();
