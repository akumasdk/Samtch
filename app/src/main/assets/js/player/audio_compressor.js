(function() {
    'use strict';

    if (window.samtch_compressor_initialized) {
        console.log('[Samtch] Audio compressor already initialized.');
        return;
    }

    console.log("Creating compressor");

    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) {
        console.error("[Samtch] Web Audio API not supported in this browser.");
        return;
    }

    let context = new AudioContext();

    let compressor = context.createDynamicsCompressor();
    compressor.threshold.value = -50;
    compressor.knee.value = 40;
    compressor.ratio.value = 12;
    compressor.attack.value = 0;
    compressor.release.value = 0.25;

    let source = null;
    let active = true; // Enabled by default

    function updateButtonUI(isActive) {
        const btn = document.getElementById('samtch-compressor-btn');
        if (btn) {
            if (isActive) {
                btn.classList.add('active');
                btn.style.color = '#9147ff';
                btn.title = 'Audio Compressor (ON)';
            } else {
                btn.classList.remove('active');
                btn.style.color = 'white';
                btn.title = 'Audio Compressor (OFF)';
            }
        }
    }

    function initAndEnable() {
        let videoElements = document.getElementsByTagName('video');
        if (videoElements.length && !source) {
            if (context.state === 'suspended') {
                context.resume();
            }
            try {
                source = context.createMediaElementSource(videoElements[0]);
                console.log("Enabling compressor (default ON)");
                source.connect(compressor);
                compressor.connect(context.destination);
                active = true;
                updateButtonUI(true);
                return true;
            } catch (e) {
                console.error("[Samtch] Error initializing compressor source:", e);
            }
        }
        return false;
    }

    window.toggleCompressor = () => {
        if (context.state === 'suspended') {
            context.resume();
        }

        if (!source) {
            initAndEnable();
        }

        if (!source) {
            console.error("[Samtch] No target video element found to compress.");
            return false;
        }

        active = !active;

        if (active) {
            console.log("Enabling compressor");
            try {
                source.disconnect(context.destination);
            } catch (_) {}

            source.connect(compressor);
            compressor.connect(context.destination);
        } else {
            console.log("Disabling compressor");
            try {
                source.disconnect(compressor);
                compressor.disconnect(context.destination);
            } catch (_) {}

            source.connect(context.destination);
        }

        updateButtonUI(active);
        return active;
    };

    window.samtch_compressor_initialized = true;
    window.samtch_toggle_compressor = window.toggleCompressor;
    window.samtch_is_compressor_active = () => active;

    // Auto-enable compressor by default when video element is ready
    let initInterval = setInterval(() => {
        if (initAndEnable()) {
            clearInterval(initInterval);
        }
    }, 500);

    initAndEnable();

    console.log("[Samtch] Audio Compressor module ready (Default ON).");
})();
