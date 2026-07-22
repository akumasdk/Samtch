## Samtch v0.1.4

### What's New
- **Premium Audio-Only Experience**: Redesigned the audio-only player with a modern "music player" aesthetic, featuring a dark gradient background, Twitch-purple avatar rings, and a balanced horizontal layout for portrait mode.
- **Rich Stream Metadata**: Integrated GraphQL queries to fetch and display detailed stream information, including titles, game categories, and real-time viewer counts, in both the audio-only player and the miniplayer.
- **Polished Loading Sequence**: Implemented a themed loading overlay with a spinning wheel for both the player and the browser, ensuring a smooth transition while the UI is being cleaned of distractions.
- **Smart Background Audio Lifecycle**: Optimized background playback to reuse existing playback sessions when minimizing, eliminating the "refresh" gap for a perfectly seamless listening experience.
- **System Media Integration**: Enhanced background playback notifications with rich metadata (streamer name, title, category) and a custom notification icon for a more professional feel.
- **Resilient Infrastructure**: Implemented dynamic scraping of the Twitch GraphQL Client-ID from the web frontend to ensure long-term stability of the HLS stream extraction logic.
- **Improved Accessibility**: Enlarged the miniplayer's close button and hit target for better touch responsiveness.

---

## Samtch v0.1.3

### What's New
- **Instant Browser Return**: Optimized the browser to stay persistent in the background, allowing for an immediate return from the player without reloads.
- **Smooth Navigation Transitions**: Added fluid slide and fade animations when switching between the browser and the native player.
- **Exit Confirmation**: Added a confirmation dialog when pressing back on the browser home screen to prevent accidental app exits.
- **High-Contrast UI**: Fixed status bar icon colors in the browser to ensure legibility against dark backgrounds.
- **Refined Pull-to-Refresh**: Increased gesture resistance and added a dead-zone to prevent accidental page refreshes while scrolling.
- **Robust Player Cleaning**: Improved the script injection system to be self-cleaning and more reliable. Added targeted removal of \"Clip\" buttons.
- **Improved Settings Navigation**: Updated the Settings screen with a smooth horizontal transition.

---

## Samtch v0.1.2

### What's New
- **Picture-in-Picture Control**: Added a toggle in settings to enable/disable automatic PiP entry.
- **Background Playback (Beta)**: Marked background play as a beta feature (disabled by default) and improved its persistence with a sticky, ongoing notification and `START_STICKY` service mode.
- **UI Enhancements**: Updated settings icons with new solid-style assets and improved the overall settings layout.
- **Improved Service Lifecycle**: Resolved issues where background audio would stop prematurely when navigating away from the player.

---

## Samtch v0.1.1

### What's New
- **F-Droid Compliance**: Refactored the project to support F-Droid's strictly open-source requirements.
- **Build Flavors**: Introduced `full` and `foss` distribution flavors. 
    - `full`: Standard version with GitHub auto-updates.
    - `foss`: Clean version for F-Droid without auto-update permissions or non-free trackers.
- **Improved CI/CD**: GitHub Actions now automatically builds and attaches both `full` and `foss` APKs to every release.
- **Default Variant**: The `full` flavor is now the default for development builds in Android Studio.

---

## Samtch v0.1.0

### What's New
- **Robust Update Mechanism**: Refactored the in-app update system to use a more reliable file sharing method, resolving "Cannot parse package" errors on modern Android versions.
- **Improved Update UI**: Added a progress indicator during downloads and better error handling via SnackBar notifications.
- **Localization**: Added Spanish translation for the BetterTTV settings success message.
- **Stability Improvements**: Resolved critical infinite recursion issues in the bridge and optimized JavaScript communication.

---

## Samtch v0.0.9

### What's New
- **Native Pull-to-Refresh**: Added support for refreshing the Twitch browser using standard pull gestures with full nested scrolling integration.
- **Enhanced UI Cleaning**: Improved player detection logic to ensure interface cleaning only occurs when a stream is active.
- **Automation Resilience**: BetterTTV settings automation is now more robust with improved element polling and visual confirmation.
- **Bridge Stability**: Fixed critical infinite recursion issues in the bridge and optimized JavaScript communication.

---

## Samtch v0.0.8

### What's New
- **Settings Screen**: Introduced a new Settings menu, accessible via a custom Gear icon in the Twitch browser.
- **BetterTTV Configuration**: Dedicated screen to manage BetterTTV settings and emotes with automated menu opening.
- **In-App Update Checker**: Stay up to date with automatic notifications and integrated APK installation from GitHub.
- **Localization**: Full support for Spanish (Español) and externalized strings for easier future translations.
- **Material 3 Integration**: The app now fully supports Material 3, including system-wide Dark/Light mode and dynamic colors.
- **Preserved Browser State**: Opening settings now overlays the browser without reloading, ensuring a seamless experience.

### Improved
- **Visual Branding**: Integrated official-styled BetterTTV and Samtch icons as high-quality vector drawables.
- **Smooth Navigation**: Added native-like slide and fade transitions for the settings interface.

---

## Samtch v0.0.7

### Added
- **Script Pre-loading**: Introduced a centralized memory cache for scripts to eliminate redundant asset I/O and improve performance.

### Changed
- **Changed Adblocking Script Strategy**: Moved to "vaft" script instead of "video-swap"
- **Injection Resilience**: Significantly improved the script injection pipeline to handle poor network conditions and slow page loads with extended polling and re-triggering logic.

---

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
