(function() {
    'use strict';
    function disableTwitchLinks() {
        document.querySelectorAll('.tw-link').forEach(link => {
            if (!link.hasAttribute('data-samtch-disabled')) {
                link.setAttribute('data-samtch-disabled', 'true');
                link.style.pointerEvents = 'none';
                link.onclick = (e) => e.preventDefault();
            }
        });
    }
    const observer = new MutationObserver(disableTwitchLinks);
    observer.observe(document.body, { childList: true, subtree: true });
    disableTwitchLinks();
})();
