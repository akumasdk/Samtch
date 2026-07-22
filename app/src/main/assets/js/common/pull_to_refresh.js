(function() {
    'use strict';
    console.log('[Samtch] Pull-to-refresh script initialized');

    let startY = 0;
    let isPulling = false;
    const threshold = 220; // High threshold to prevent accidents
    const deadZone = 30;    // Ignore small jitters at the start of a scroll

    const indicator = document.createElement('div');
    indicator.id = 'samtch-pull-indicator';
    indicator.style.cssText = `
        position: fixed;
        top: -100px;
        left: 50%;
        transform: translateX(-50%);
        width: 45px;
        height: 45px;
        background: #9146FF;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 10px rgba(0,0,0,0.4);
        z-index: 2147483647;
        transition: top 0.1s ease-out, opacity 0.1s ease-out;
        opacity: 0;
        pointer-events: none;
    `;
    indicator.innerHTML = '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"></path><polyline points="21 3 21 8 16 8"></polyline></svg>';
    document.documentElement.appendChild(indicator);

    function handleTouchStart(e) {
        if (window.scrollY === 0) {
            startY = e.touches[0].pageY;
            isPulling = true;
        } else {
            isPulling = false;
        }
    }

    function handleTouchMove(e) {
        if (!isPulling) return;

        const currentY = e.touches[0].pageY;
        const diff = currentY - startY;

        // Apply dead-zone and only react to downward pulls
        if (diff > deadZone && window.scrollY === 0) {
            // Resist scrolling down the page while pulling
            if (e.cancelable) e.preventDefault();

            const effectiveDiff = diff - deadZone;
            // Lower multiplier (0.3) for a "heavier" resistant feel
            const pullDistance = Math.min(effectiveDiff * 0.3, threshold + 20);

            indicator.style.top = (pullDistance - 30) + 'px';
            indicator.style.opacity = Math.min(pullDistance / 80, 1).toString();

            // Rotation effect based on distance
            const rotation = (effectiveDiff * 1.5) % 360;
            indicator.querySelector('svg').style.transform = `rotate(${rotation}deg)`;
        } else if (diff < -10) {
            // If user scrolls up, immediately cancel the pull
            resetPull();
        }
    }

    function handleTouchEnd(e) {
        if (!isPulling) return;

        const endY = e.changedTouches[0].pageY;
        const diff = (endY - startY) - deadZone;

        if (diff >= threshold && window.scrollY === 0) {
            console.log('[Samtch] PTR: Threshold reached! Triggering refresh...');
            if (window.TwitchBrowserBridge && typeof window.TwitchBrowserBridge.onRefresh === 'function') {
                window.TwitchBrowserBridge.onRefresh();
            }
        }

        resetPull();
    }

    function resetPull() {
        indicator.style.top = '-100px';
        indicator.style.opacity = '0';
        isPulling = false;
    }

    window.addEventListener('touchstart', handleTouchStart, { passive: false });
    window.addEventListener('touchmove', handleTouchMove, { passive: false });
    window.addEventListener('touchend', handleTouchEnd, { passive: true });
})();
