(function() {
    'use strict';

    function log(msg) {
        console.log("[StreamDetector] " + msg);
    }

    function reportUrl(url) {
        if (!url || url.startsWith('blob:') || url.startsWith('data:')) return;

        if (typeof TwitchPlayerBridge !== 'undefined' && TwitchPlayerBridge.onStreamUrlFound) {
            log("Reporting found stream URL: " + url);
            TwitchPlayerBridge.onStreamUrlFound(url);
        }
    }

    function scanForStreams() {
        // 1. Scan <video> tags
        const videos = document.getElementsByTagName('video');
        for (let i = 0; i < videos.length; i++) {
            const v = videos[i];
            if (v.src) reportUrl(v.src);

            const sources = v.getElementsByTagName('source');
            for (let j = 0; j < sources.length; j++) {
                if (sources[j].src) reportUrl(sources[j].src);
            }
        }

        // 2. Scan <iframe> tags (might be nested players)
        const iframes = document.getElementsByTagName('iframe');
        for (let i = 0; i < iframes.length; i++) {
            const src = iframes[i].src;
            if (src && (src.includes('.m3u8') || src.includes('.mp4'))) {
                reportUrl(src);
            }
        }

        // 3. Scan for common JS player variables if they exist
        if (window.player && window.player.source) reportUrl(window.player.source);
        if (window.jwplayer) {
            try {
                const jw = window.jwplayer();
                if (jw && jw.getPlaylist) {
                    const pl = jw.getPlaylist();
                    if (pl && pl[0] && pl[0].file) reportUrl(pl[0].file);
                }
            } catch(e) {}
        }
    }

    // Start periodic scanning
    log("Stream Detector initialized");
    setInterval(scanForStreams, 5000);
    scanForStreams();
})();

// ==UserScript==
// @name         Twitch - Force 160p quality
// @namespace    CommanderRoot
// @copyright    CommanderRoot
// @license      Unlicense
// @version      1.2.9
// @description  Sets Twitch default stream quality to 160p
// @author       https://twitter.com/CommanderRoot
// @match        https://www.twitch.tv/*
// @match        https://m.twitch.tv/*
// @match        https://player.twitch.tv/*
// @grant        none
// @run-at       document-start
// ==/UserScript==
"use strict";

// CONFIG start ------
const doOnlySetting = false; // false = do some trickery with document hidden state / true = only set the localStorage option
// CONFIG end --------

// Code
if (doOnlySetting === false) {
  // Try to trick the site into thinking it's never hidden
  Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: false });
  Object.defineProperty(document, 'webkitVisibilityState', { value: 'visible', writable: false });
  document.hasFocus = function () { return true; };
  const initialHidden = document.hidden;
  let didInitialPlay = false;
  let lastVideoPlaying = false;

  // visibilitychange events are captured and stopped
  document.addEventListener('visibilitychange', function (e) {
    if (document.hidden === false && initialHidden === true && didInitialPlay === false) {
      // Allow propagation to prevent black screen when a stream was opened in a new tab
    } else {
      e.stopImmediatePropagation();
    }
    if (document.hidden) {
      didInitialPlay = true;
    }

    // Try to play the video on Chrome
    if (typeof chrome !== 'undefined') {
      if (document.hidden === true) {
        const videos = document.getElementsByTagName('video');
        if (videos.length > 0) {
          lastVideoPlaying = !videos[0].paused && !videos[0].ended;
        } else {
          lastVideoPlaying = false;
        }
      } else {
        playVideo();
      }
    }
  }, true);

  function playVideo() {
    const videos = document.getElementsByTagName('video');
    if (videos.length > 0) {
      if ((didInitialPlay === false || lastVideoPlaying === true) && !videos[0].ended) {
        videos[0].play();
        didInitialPlay = true;
      }
    }
  }
}

function setQualitySettings() {
  try {
    window.localStorage.setItem('s-qs-ts', Math.floor(Date.now()));
    window.localStorage.setItem('quality-bitrate', '230000');
    window.localStorage.setItem('video-quality', '{"default":"160p30"}');
  } catch (e) {
    console.log(e);
  }
}

setQualitySettings();

// Add event handler for when we switch between pages
window.addEventListener('popstate', () => {
  setQualitySettings();
});
