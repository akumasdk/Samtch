# Changelog

## Samtch v1.1.3

### What's New
- **Enriched Streamer Insights**: Significantly expanded the Stream Info Dialog to include more detailed channel metadata, including the total **Follower Count**, **Channel Description**, and the localized **Account Creation Date**.
- **External Integration**: Added a convenient \"Open in Browser\" button within the stream info dialog for quick access to full Twitch profiles and schedules.
- **Enhanced Visual Accessibility**:
    - **Robust Metadata Visibility**: Overhauled the metadata bar's background and mapping logic to ensure it remains clearly visible across all themes, with improved contrast in Light Mode.
    - **Immersive Dialogs**: Added a sophisticated 150dp blur effect to the background of info dialogs for a more premium, focused feel.
    - **Intelligent Dimming**: Refined the dynamic luminance system to more aggressively dim bright stream previews, preventing UI wash-out.
- **Under-the-Hood Performance**:
    - **Thread-Isolated Analysis**: Offloaded the CPU-intensive background luminance analysis to a dedicated worker thread, keeping the UI perfectly responsive during complex visual transitions.
    - **Resilient Data Fetching**: Migrated to a more robust GQL mapping strategy that gracefully handles missing or optional Twitch API fields.

---

## Samtch v1.1.2

### What's New
- **Fluid Theme Transitions**: Implemented a smooth, synchronized color animation system that elegantly fades the entire UI when switching between Light and Dark themes, including synchronized status bar icon transitions.
- **Advanced Performance Engine**:
    - **Concurrent Chat Processing**: Offloaded message parsing, UI mapping, and history re-rendering to background threads (`Dispatchers.Default`), ensuring the main thread remains free for silky-smooth 60ms scrolling even in high-traffic channels.
    - **Snappy App Startup**: Moved script loading, version checks, and GQL cache warming to background IO, significantly reducing initial hang time.
    - **Optimized Player Metadata**: Backgrounded Media3 metadata construction for faster player response.
- **Refined Immersive Experience**:
    - **Theme-Aware Immersion**: Refactored immersive backgrounds to be Dark Mode exclusive for the main chat and player areas, ensuring the Light Theme remains clean and readable.
    - **Stretched Color Palette**: Updated all immersive backgrounds to use `FillBounds` scaling, "stretching" the preview image to capture and display the full spectrum of the stream's colors.
    - **Polished Emote Menu**: Added subtle, pulsing placeholder dots for emotes during their initial load, providing immediate visual feedback.
- **System Stability**: 
    - **Thread-Safe State**: Migrated internal chat structures to concurrent collections to prevent race conditions during heavy traffic.
    - **PiP Recovery**: Fixed an issue where system bars would lose theme synchronization after returning from Picture-in-Picture mode.

---

## Samtch v1.1.1

### What's New
- **Stability Fix**: Resolved a critical `AnchoredDraggableUninitializedException` crash that occurred when opening Emote, Badge, or User info dialogs.
- **System Bar Reliability**: Overhauled system bar appearance management to ensure status bar icons remain legible when using immersive blurred backgrounds in Light mode.
- **Performance**: Optimized settings screen state to be fully reactive, reducing redundant Flow recreations and improving overall UI snappiness.

---

## Samtch v1.1.0

### What's New
- **Synchronized Immersive Backgrounds**: Centralized background refresh logic using a metadata trigger, ensuring all components (Player, Chat, Info Dialog) update their blurred backgrounds simultaneously when stream metadata changes.
- **Improved Emote Menu UX**:
    - **Elegant Animations**: Replaced bouncy springs with sophisticated Material 3 emphasized easing for a smoother menu transition.
    - **Intelligent Focus**: The emote menu now anticipates usage by opening exactly when the chat box is focused, eliminating redundant animations.
    - **Quick Access Settings**: Added a settings cogwheel button directly within the emote menu for instant configuration access.
    - **Interaction Polishing**: Implemented \"lift-to-dismiss\" logic for the chat area and added haptic vibration feedback when sending messages.
- **Enhanced Chat Customization**: Added a new **Badge Size** setting in the configuration menu with a dedicated \"Gem\" icon for better visual density control.
- **Navigation Refinement**: Improved back-button handling to prioritize closing the emote menu before minimizing the player or exiting fullscreen.
- **Reactive Settings**: Resolved an issue where conditional settings options weren't updating instantly by making the settings screen state fully reactive.

---

## Samtch v1.0.0 (First Stable Release)

### What's New
- **Dark Immersive Backgrounds on Light Theme**: Optimized system bar appearance to ensure icons remain legible when using blurred backgrounds in Light Mode.
- **Enhanced UI Contrast & Immersive Effects**: Refined glass styling, metadata bar animations, and overall interface contrast for a more premium feel.
- **Twitch Badges & User Info**: Implemented native dialogs for Twitch badges and user profiles directly within the chat.
- **Channel Emotes Integration**: Expanded 3rd-party emote support to include channel-specific emotes from BTTV and 7TV.
- **Improved Authentication**: Refined OAuth flow and Helix API integration for more reliable login and session management.
- **Stability Fixes**: Addressed various edge cases in chat rendering, emote mapping, and system bar synchronization.

---

## Samtch v0.4.2

### What's New
- **PiP Overlay Visibility Fix**: Updated player overlay visibility logic so controls behave correctly when Picture-in-Picture mode is active.

---

## Samtch v0.4.1

### What's New
- **Immersive Background**: Introduced a blurred stream preview background for the portrait player and chat input area. Includes smooth crossfade transitions when the stream image updates.
- **Redesigned Portrait Chat Input**: Optimized the chat interface by separating the emote menu and player mode toggle buttons for better accessibility.
- **Advanced Gesture Rockers**: Overhauled fullscreen volume and brightness controls with significantly increased sensitivity and high-precision accumulation for a smoother feel.
- **Intelligent Gesture Passthrough**: Refined touch handling to allow interacting with the Twitch player iframe (like quality settings) even while the gesture rockers are active.
- **Per-Channel Recent Emotes**: The "Recent" emote tab now stores and displays usage history separately for every channel, ensuring a tailored experience.
- **System Brightness Synchronization**: App-level brightness adjustments now sync perfectly with Android system settings and automatically restore to original levels when exiting fullscreen or entering PiP.
- **Immersive Toggle Setting**: Added a new "Immersive Background" option in Settings to allow users to choose between the blurred effect or standard backgrounds.
- **Refined Navigation Flow**: 
    - Back pressing in fullscreen now correctly returns the app to portrait mode.
    - Toggling Audio-Only mode from fullscreen now performs a clean transition back to the portrait layout.

---

## Samtch v0.4.0

### What's New
- **Tabbed Emote Browser**: Added a full-featured emote menu with dedicated tabs for **Recent**, **Channel**, and **Global** emotes for a superior chat interaction experience.
- **Intelligent Emote Suggestions**: Implemented a smart completion system that scores emotes based on match quality and case sensitivity, triggered by `:` or general text entry.
- **Modern Pill-Shaped Input**: Redesigned the chat input area with a sleek, modular pill layout featuring smooth **morphing icon animations** for a premium feel.
- **Emote Info Bottom Sheet**: Added a detailed information dialog for every emote in the menu and chat stream, featuring quick "Use" and "Copy" actions.
- **Versatile Status Banner**: Repurposed the ad-blocking indicator into a unified **StatusBanner** that announces specialized viewing modes like "Chat Only" and "Audio Only".
- **Enhanced Chat Customization**: Added granular user preferences for **Chat Font Size** and **Emote Size** with "Reset to Default" support across all application settings.
- **IRC System Notifications**: Introduced a sticky, dismissible banner above the chat input to catch and display critical Twitch server status updates (NOTICE).
- **Interactive Guest Prompts**: Integrated a "one-tap" login prompt directly into the chat input bar to help new users quickly authenticate.
- **SevenTV v3 Compatibility**: Completely overhauled SevenTV parsing to support the latest v3 API, including zero-width overlay emotes and improved CDN reliability.
- **Resource Optimization**: Improved performance by automatically culling (unmounting) the emote menu when it is hidden behind the software keyboard.
- **Refined Stability**: 
    - Fixed a bug where system bar themes wouldn't synchronize correctly when exiting fullscreen mode.
    - Optimized periodic metadata updates to prevent stream avatars from flickering or reloading.

---

## Samtch v0.3.2

### What's New
- **Advanced Project Modularization**: Performed a deep reorganization of the UI package, grouping components, models, and viewmodels into dedicated feature-based subfolders for better maintainability.
- **Metadata Logic Decomposition**: Broke down the monolithic metadata components into focused, reusable files for the info bar styles, dialogs, and individual info cards.
- **Enhanced Sizing Accuracy**: Integrated `BoxWithConstraints` into the player logic to ensure perfect edge-to-edge layout across all device form factors, including foldables, tablets, and large-cutout phones.
- **Settings UI Redesign**: Modernized selection and logout dialogs with `RadioButton` controls, improved action hierarchies, and consistent Material 3 styling.
- **Contextual UI Controls**: The "Log Out" option now intelligently only appears when a user is actively authenticated with Twitch.
- **JavaScript Bridge Extraction**: Isolated all `@JavascriptInterface` definitions into standalone bridge classes to decouple web-logic from native UI components.
- **Localization Stability**: Enforced a consistent locale for all numeric and duration formatting to prevent regional-dependent string bugs.
- **Refined Insets Handling**: Improved system bar padding logic to provide a seamless "behind-the-bars" immersive background while ensuring all interactive content remains in the safe area.

---

## Samtch v0.3.1

### What's New
- **Comprehensive Player Refactoring**: Modularized the player architecture by extracting core logic into dedicated handlers for effects, input, layout, and WebView management.
- **Enhanced Stream Metadata**:
    - **Tag Support**: Stream tags are now parsed and displayed within the stream information dialog.
    - **Visual Sync**: Improved avatar and preview image loading with synchronized, non-cached refreshes and dedicated loading indicators.
- **Optimized Mini-Player**:
    - **Smart Overlays**: Created a dedicated mini-player overlay that dynamically adapts its visual density based on the current player mode.
    - **Improved Transitions**: Refined the maximization animations for a glitch-free experience, especially in Chat Only mode.
- **Polished UI & Behavior**:
    - **Consistent Immersive Mode**: System bars are now strictly managed, remaining visible in portrait/audio/chat modes and only hiding for true landscape fullscreen.
    - **Slimmer Adblock Banner**: Refined the adblock status indicator for a more non-intrusive look while ensuring it correctly consumes touch input.
- **Stability Fixes**: 
    - Resolved state synchronization issues between the audio player screen and system media notifications.
    - Fixed a bug causing redundant browser reloads during certain UI transitions.

---

## Samtch v0.3.0

### What's New
- **Full Native Twitch Chat**: Implemented a comprehensive native chat experience with support for **BetterTTV** and **7TV** emotes. Includes high-performance auto-scrolling, compact mode, and badge support.
- **Aggressive Navigation Sentinel**: Introduced a high-performance SPA navigation interception system that proactively blocks transitions to channel pages, ensuring the browser remains perfectly stable while the native player opens.
- **Intelligent Resource Management**: The browser now completely unloads (navigating to `about:blank`) when the player is active to save system resources, and automatically pre-loads your dashboard context in the background for an instant return.
- **Dynamic Stream Metadata Bar**: Redesigned the metadata bar with an expandable/collapsible layout. It features an intelligent auto-shrink "Slim" mode that triggers after 15 seconds to maximize chat visibility.
- **Safe History Management**: Implemented a custom Kotlin-side history stack to replace standard WebView history, ensuring back-navigation always lands on safe exploration pages.
- **Interactive Escape Mechanisms**: Added detection for web UI "back" and "home" buttons, allowing users to reliably escape from accidental channel loads back to the safe dashboard.
- **Refined Viewer Experience**:
    - **Marquee Preservation**: Title scroll positions are now synchronized across metadata bar styles for a seamless transition.
    - **Visual Indicators**: Moved the red "Live" indicator to the stream time pill and added a person icon for viewer counts.
    - **Mini-Player Enhancements**: Added a nudge animation and hint system for first-time mini-player users.
- **Stability & Security**:
    - **Mobile Domain Enforcement**: Strictly forces `m.twitch.tv` to prevent accidental drifting to the desktop site, especially after login.
    - **Backup Exclusion**: Updated configuration to prevent Android from restoring potentially "poisoned" settings after an uninstall.
    - **Loading State Refinements**: Synchronized the loading overlay with custom JavaScript cleanup signals to eliminate visual UI flashes.

---

## Samtch v0.2.4

### What's New
- **Native Ad-Block Status Banner**: Replaced the legacy JavaScript-injected overlay with a polished native Compose banner for real-time ad-blocking feedback.
- **Stream Duration Display**: The player metadata bar now displays how long a stream has been live, calculated directly from Twitch's stream start time.
- **Enhanced Playback Monitoring**: Improved detection of video elements within Shadow DOMs and iframes, ensuring the loading screen disappears promptly when audio starts.
- **Improved Responsiveness**: The loading overlay now dismisses immediately when ad-blocking is detected, providing a snappier startup experience.
- **Unified Script Bridge**: Refactored the communication between JavaScript and the native player for better reliability and faster script "ready" signals.
- **Default Ad-Blocking**: Set **VAFT** as the default ad-blocking method for improved coverage.

---

## Samtch v0.2.3

### What's New
- **Sound-Synchronized Loading**: The loading overlay is now accurately dismissed only when video playback (with audio) is detected, ensuring a smoother transition.
- **Reliable Playback Detection**: Implemented a dedicated JavaScript monitor for the video element with audio-level sensing and a 5-second fallback.
- **Optimized Script Injection**: Refined the injection pipeline and removed redundant bridge signals for a cleaner, more stable implementation.
- **Cleaned Up Script Monitors**: Removed legacy and redundant script monitors to improve performance and stability.

---

## Samtch v0.2.2

### What's New
- **Configurable Ad-Blocking**: Added a setting to choose between VAFT and Video Swap ad-blocking methods.
- **Improved Ad-Blocking Stability**: Set **Video Swap** as the default ad-blocking method for better reliability.
- **Refined Script Injection**: Enhanced the injection pipeline with high-priority early loading and JavaScript bridge signals for better coordination.
- **Persistent Ad-Blocking Notice**: Added a notice in settings to clarify that ad-blocking changes take effect on the next stream.
- **Enhanced Audio Player UI**: Fixed layout issues in the audio-only player and added a marquee effect for long game names.
- **Improved Loading Feedback**: Refined the loading box dismissal logic with multiple safety fallbacks and better bridge communication.
- **Localized Settings**: Full English and Spanish localization for the new ad-blocking selection settings.

---

## Samtch v0.2.1

### What's New
- **Optimized Video Playback**: Switched to the `video_swap` script for improved stream stability and addressed issues with the `vaft` implementation.
- **Faster Player Startup**: Implemented GQL cache pre-warming on app startup and added a loading preview fallback for near-instant visual feedback.
- **Enhanced Web UI Cleaning**: Refined the cleanup logic for single-page application transitions and improved pull-to-refresh reliability.
- **Fluid UI Transitions**: Integrated polished animated transitions throughout the player interface.

---

## Samtch v0.2.0

### What's New
- **High-Fidelity Metadata**: Switched to a robust GraphQL-based metadata acquisition system for faster and more reliable stream information.
- **Enhanced Media Artwork**: Implemented 1080p stream preview thumbnails and high-resolution streamer avatars.
- **Rich Background Experience**: ExoPlayer notifications and audio-only mode now feature crisp stream thumbnails as artwork, replacing the previous low-res avatars.
- **UI Layout Stability**: Refined player controls to prevent layout displacement caused by dynamic Twitch quality badges.
- **Optimized Scripting**: Removed legacy DOM scraping logic in favor of direct API data, reducing resource usage and improving reliability.
- **Polished Control Interface**: Adjusted the injection order and styling of player buttons (Chat, Audio-Only, Fullscreen) for a more stable and professional interface.
- **Premium Audio-Only Experience**: Redesigned the audio-only player with a modern "music player" aesthetic, featuring a dark gradient background, Twitch-purple avatar rings, and a balanced horizontal layout for portrait mode.
- **Polished Loading Sequence**: Implemented a themed loading overlay with a spinning wheel for both the player and the browser, ensuring a smooth transition while the UI is being cleaned of distractions.
- **System Media Integration**: Enhanced background playback notifications with rich metadata (streamer name, title, category) and a custom notification icon for a more professional feel.
- **Resilient Infrastructure**: Implemented dynamic scraping of the Twitch GraphQL Client-ID from the web frontend to ensure long-term stability of the HLS stream extraction logic.

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
