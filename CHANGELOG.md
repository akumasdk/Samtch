## Samtch v0.0.6

### Changed
- **Script Injection**: Reordered and optimized the script injection pipeline with persistent guards to prevent duplicate execution and ensure faster player hydration.

### Fixed
- **UI Cleanup**: Improved clutter detection logic to handle dynamic label changes, ensuring a cleaner player interface.

---

## Samtch v0.0.5

### What's New
- **New Splash Screen**: Implemented a smoother app entry that remains until the Twitch browser is fully loaded and interactive via JS bridge and a dedicated controller.
- **Enhanced Navigation**: Added double-tap gesture in portrait mode to enter fullscreen directly from the player.
- **Material 3 Icons**: Updated PiP RemoteAction button to use a Material 3 style refresh icon for better consistency.

### Changed
- **UI Cleanup**: Significantly reduced channel page clutter by removing dynamic transition groups and "Open in App" distractions across the site.

---

## Samtch v0.0.4

### What's New
- **Improved Injection Reliability**: Enhanced script injection logic with aggressive polling and immediate execution on page load to ensure ad-blocking and UI cleaning work correctly on fresh app opens.
- **Robust JavaScript Hooks**: Updated player scripts with persistent intervals and root-level observation to maintain stability even with Twitch's dynamic SPA transitions.

### Fixed
- **Stale Chat Toggle**: Resolved an issue where toggling chat would stop working after entering fullscreen due to staled state in the player bridge.
- **Bridge Communication**: Fixed a naming mismatch between native and JavaScript bridges to ensure seamless interaction.

### Changed
- **Increased Minimum SDK**: Bumped `minSdk` to 24 (Android 7.0) to better support modern Compose and WebView features.

---

## Samtch v0.0.3

### What's New
- **Picture-in-Picture (PiP) Support**: Watch your favorite streams while using other apps. The player automatically transitions to PiP mode when you leave the app.
- **Seamless PiP Animation**: Implemented source rect hints and auto-enter support for a smooth, high-quality transition when swiping to the home screen.
- **Player Stability**: Refactored the core player using `movableContentOf` to prevent black screens and WebView recreation when switching between portrait, fullscreen, and PiP modes.

---

## Samtch v0.0.2

### What's New
- **Fullscreen Chat Overlay**: You can now toggle chat while in landscape fullscreen mode.
- **Double-Tap Interaction**: Added a new double-tap gesture in the center of the player to quickly toggle chat visibility.
- **Chat UI Cleaning**: Integrated a specialized script to remove headers, banners, and unnecessary transition elements from the chat embed for a cleaner look.
- **Onboarding Tooltip**: Added a helpful hint that appears when entering fullscreen to inform about the new double-tap gesture.
- **Localization Support**: Tooltip and other UI strings are now properly localized.

### Fixed
- Improved touch event handling in the fullscreen player to intercept gestures before they are consumed by the WebView.
- Refined the ad-blocking script injection timing for better consistency.

---

## Samtch v0.0.1

### What's New
- **Initial Release** of Samtch: A lightweight, ad-free Twitch client for Android.
- **Custom Player**: Highly optimized player with integrated script injection.
- **Ad-Blocking**: Integrated `video-swap-new` script to bypass Twitch video ads.
- **Clean UI**: Custom JavaScript injection to remove "Open in App" prompts and clutter.
- **Discovery Browser**: Full Twitch mobile site browsing for search, following, and discovery.
- **BTTV Support**: Integrated BetterTTV emotes for a better chat experience.
- **Fullscreen Support**: Landscape mode with immersive system bar handling.
- **App Logo**: Custom branding for a native feel.

### Fixed
- Fixed issues with Single Single Page Application (SPA) navigation not triggering standard WebView events.
- Resolved white-screen/blank-page issues during complex navigation transitions.
- Improved the detection of channel root URLs to accurately distinguish between streams and sub-pages.
- Fixed infinite reload loops when returning to the Twitch home page.

### Download
Download the APK below to install on your Android device.
