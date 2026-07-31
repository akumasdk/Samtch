(function() {
    'use strict';

    console.log("[Twitch TV Script] Initializing merged UI Cleaner and Quality Selector...");

    let controlsHidden = false;

    function injectHidingStyles() {
        if (controlsHidden) return;
        const styleId = 'samtch-tv-cleaner';
        if (document.getElementById(styleId)) return;

        console.log("[Twitch TV Script] Injecting hiding styles...");
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            /* Hide player controls but keep them in DOM for scripts */
            .video-player__controls,
            [data-a-target="player-controls"],
            .video-player__overlay,
            .tw-tower {
                opacity: 0 !important;
                pointer-events: none !important;
            }

            /* Ensure video takes full screen */
            .video-player__container, .video-player__default-player {
                background: black !important;
            }

            /* Hide unnecessary banners completely */
            .ad-banner,
            .tw-upsell-banner {
                display: none !important;
            }
        `;
        document.head.appendChild(style);
        controlsHidden = true;
    }

    function waitForElement(selector, callback) {
        const element = document.querySelector(selector);
        if (element) {
            callback(element);
            return;
        }

        const observer = new MutationObserver((mutations, obs) => {
            const el = document.querySelector(selector);
            if (el) {
                obs.disconnect();
                callback(el);
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    function runAutomation() {
        waitForElement('[data-a-target="player-settings-button"]', (settingsButton) => {
            console.log("[Twitch TV Script] Found settings button. Attempting to set quality...");

            let attempts = 0;
            const maxAttempts = 5;

            const tryClickSettings = () => {
                attempts++;
                settingsButton.click();

                // Verify if settings menu appeared
                setTimeout(() => {
                    const qualityMenuItem = document.querySelector('[data-a-target="player-settings-menu-item-quality"]');
                    const settingsButtonCheck = document.querySelector('[data-a-target="player-settings-button"]');

                    if (qualityMenuItem && settingsButtonCheck) {
                        console.log("[Twitch TV Script] Settings menu confirmed. Proceeding with quality automation...");

                        // Proceed to quality selection
                        handleQualitySelection(qualityMenuItem);
                    } else if (attempts < maxAttempts) {
                        console.warn(`[Twitch TV Script] Verification failed (Menu: ${!!qualityMenuItem}, Button: ${!!settingsButtonCheck}). Retrying click...`);
                        tryClickSettings();
                    } else {
                        console.error("[Twitch TV Script] Failed to verify settings menu. Forcing UI hide anyway.");
                        injectHidingStyles();
                    }
                }, 500);
            };

            tryClickSettings();
        });
    }

    function handleQualitySelection(qualityButton) {
        console.log("[Twitch TV Script] Clicking quality menu...");

        let qAttempts = 0;
        const tryQualityClick = () => {
            qAttempts++;
            qualityButton.click();

            setTimeout(() => {
                const subMenu = document.querySelector('[data-a-target="player-settings-submenu-quality-option"]');
                if (subMenu) {
                    console.log("[Twitch TV Script] Quality submenu opened.");
                    selectSourceQuality();
                } else if (qAttempts < 5) {
                    tryQualityClick();
                }
            }, 500);
        };

        tryQualityClick();
    }

    function selectSourceQuality() {
        const options = document.querySelectorAll('[data-a-target="player-settings-submenu-quality-option"]');
        const sourceOption = [...options].find(el => el.innerText.includes("1080p") || el.innerText.includes("Source"));

        if (sourceOption) {
            console.log("[Twitch TV Script] Selecting Source quality...");
            sourceOption.click();

            // Wait for selection to apply, then close menu with verification
            let closeAttempts = 0;
            const tryCloseMenu = () => {
                closeAttempts++;
                const settingsButton = document.querySelector('[data-a-target="player-settings-button"]');
                if (settingsButton) {
                    console.log(`[Twitch TV Script] Closing menu (Attempt ${closeAttempts})...`);
                    settingsButton.click();

                    // Verification check
                    setTimeout(() => {
                        const isMenuStillOpen = !!document.querySelector('[data-a-target="player-settings-menu"]') ||
                                              !!document.querySelector('[data-a-target="player-settings-submenu-quality-option"]');

                        if (!isMenuStillOpen) {
                            console.log("[Twitch TV Script] Menu closed successfully. Finalizing UI...");
                            setTimeout(injectHidingStyles, 200);
                        } else if (closeAttempts < 3) {
                            console.warn("[Twitch TV Script] Menu still detected. Retrying close...");
                            tryCloseMenu();
                        } else {
                            console.error("[Twitch TV Script] Menu failed to close. Forcing hide.");
                            injectHidingStyles();
                        }
                    }, 600);
                } else {
                    injectHidingStyles();
                }
            };

            setTimeout(tryCloseMenu, 800);
        } else {
            console.warn("[Twitch TV Script] Source quality option not found. Hiding UI as fallback.");
            injectHidingStyles();
        }
    }

    // Start logic
    if (document.readyState === "complete" || document.readyState === "interactive") {
        runAutomation();
    } else {
        window.addEventListener("load", runAutomation);
    }
})();
