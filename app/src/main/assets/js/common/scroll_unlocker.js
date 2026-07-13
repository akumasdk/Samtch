(function() {
    'use strict';
    function fixPageState() {
        [document.body, document.documentElement].forEach(el => {
            if (el) {
                const style = window.getComputedStyle(el);
                if (style.overflow === 'hidden' || el.style.overflow === 'hidden') {
                    el.style.setProperty('overflow', 'auto', 'important');
                    el.style.setProperty('position', 'static', 'important');
                }
            }
        });
    }
    setInterval(fixPageState, 1500);
    fixPageState();
})();
