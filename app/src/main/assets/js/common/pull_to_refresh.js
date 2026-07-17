(function() {
    'use strict';
    console.log('[Samtch] Pull-to-refresh script initialized');

    let startY = 0;
    let isPulling = false;
    const threshold = 150; // increased threshold for mobile reliability

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
        transition: top 0.15s ease-out, opacity 0.15s ease-out;
        opacity: 0;
        pointer-events: none;
    `;
    indicator.innerHTML = '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"></path><polyline points="21 3 21 8 16 8"></polyline></svg>';
    document.documentElement.appendChild(indicator);

    function handleTouchStart(e) {
        if (window.scrollY === 0) {
            startY = e.touches[0].pageY;
            isPulling = true;
            console.log('[Samtch] PTR: Start detected at top');
        } else {
            isPulling = false;
        }
    }

    function handleTouchMove(e) {
        if (!isPulling) return;

        const currentY = e.touches[0].pageY;
        const diff = currentY - startY;

        if (diff > 0 && window.scrollY === 0) {
            // Resist scrolling down the page while pulling
            if (e.cancelable) e.preventDefault();

            const pullDistance = Math.min(diff * 0.4, threshold + 20);
            indicator.style.top = (pullDistance - 20) + 'px';
            indicator.style.opacity = Math.min(pullDistance / threshold, 1).toString();

            // Rotation effect
            const rotation = (diff * 2) % 360;
            indicator.querySelector('svg').style.transform = `rotate(${rotation}deg)`;
        } else if (diff < 0) {
            isPulling = false;
            indicator.style.top = '-100px';
            indicator.style.opacity = '0';
        }
    }

    function handleTouchEnd(e) {
        if (!isPulling) return;

        const endY = e.changedTouches[0].pageY;
        const diff = endY - startY;
        console.log('[Samtch] PTR: End detected. Distance:', diff);

        if (diff >= threshold && window.scrollY === 0) {
            console.log('[Samtch] PTR: Threshold reached! Triggering refresh...');
            if (window.TwitchBrowserBridge && typeof window.TwitchBrowserBridge.onRefresh === 'function') {
                window.TwitchBrowserBridge.onRefresh();
            } else {
                console.error('[Samtch] PTR Error: TwitchBrowserBridge.onRefresh is missing');
            }
        }

        indicator.style.top = '-100px';
        indicator.style.opacity = '0';
        isPulling = false;
    }

    window.addEventListener('touchstart', handleTouchStart, { passive: false });
    window.addEventListener('touchmove', handleTouchMove, { passive: false });
    window.addEventListener('touchend', handleTouchEnd, { passive: true });
})();
