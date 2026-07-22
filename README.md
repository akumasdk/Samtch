<p align="center">
  <img src="images/logo.png" alt="Samtch Logo" height="192dp" style="border-radius: 50%;">
</p>

<h1 align="center">Samtch</h1>

Samtch is a lightweight Twitch client for Android designed for a clean, ad-free viewing experience. It combines the power of the native Twitch web interface for discovery with a highly optimized custom player for watching, featuring a premium background audio mode.

## 🖼️ Gallery

| Discovery Browser | Following & Search |
| :---: | :---: |
| <img src="images/HomePage.jpg" width="300"> | <img src="images/FollowingPage.jpg" width="300"> |

| Custom Player (Portrait) | Fullscreen Mode |
| :---: | :---: |
| <img src="images/PortraitModeBlockingAds1.jpg" width="300"> | <img src="images/fullScreenView.jpg" width="400"> |

| Ad-Blocking in Action | BTTV Integration |
| :---: | :---: |
| <img src="images/fullScreenViewBlockingAds.jpg" width="400"> | <img src="images/BTTVFunctionality1.jpg" width="300"> |

## ✨ Features

- **Ad-Free Viewing**: Integrated scripts to bypass common video ads and tracking.
- **Premium Audio-Only Mode**: A dedicated "music player" aesthetic for background listening, complete with dark gradients and streamer avatar rings.
- **Rich Stream Metadata**: Real-time titles, game categories, and viewer counts fetched via GraphQL.
- **Clean UI**: Custom JavaScript injection removes "Open in App" prompts and clutters from the player interface.
- **Seamless Discovery**: Use the full Twitch mobile site for browsing, search, and following, while switching automatically to a native-feeling player when a stream is selected.
- **Smart Navigation**:
  - Automatically triggers the custom player when navigating to a live channel.
  - Robust safeguards prevent hijacking your own profile page.
  - Remembers your browsing position when returning from the player.
- **Fullscreen & PiP**: Immersive landscape mode and Picture-in-Picture support for multitasking.
## 📝 Changelog

See the **[CHANGELOG.md](CHANGELOG.md)** for a full history of changes.

## 🚀 Installation

You can download the latest version of Samtch from the **[Releases](https://github.com/akumasdk/Samtch/releases)** page.

We provide two variants of the APK:
- **`Samtch-vX.Y.Z-full.apk`**: Includes the built-in update checker to notify you of new releases directly in the app.
- **`Samtch-vX.Y.Z-foss.apk`**: A "Free and Open Source" version without auto-update permissions, designed for compliance with FOSS standards.

1. Download the latest `.apk` file for your preferred flavor.
2. If prompted, enable "Install from Unknown Sources" in your Android settings.
3. Make sure to click **"Install anyway"** if a Play Protect dialog pops up during installation.
4. Open the file and install.

## 🛠️ Build Instructions

To build Samtch from source:

1. Clone the repository:
   ```bash
   git clone https://github.com/akumasdk/Samtch.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Ensure you have the Android SDK for API 36 installed.
4. Build and run the project using the `app` configuration.

## 🤝 Contributing

Contributions are welcome! If you have ideas for improvements or have found a bug:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

## 🗺️ Roadmap / Future Features

- [x] **Chat in Fullscreen**: Overlay chat during landscape viewing.
- [x] **Picture-in-Picture (PiP)**: Watch streams while using other apps.
- [x] **Background Play**: Listen to stream audio even when the screen is off or the app is minimized.
- [ ] **Navigation Improvements**: Faster transitions and better gesture support.
- [ ] **Android TV Support**: Optimized interface for television and remote control navigation.

## 🙏 Credits & Acknowledgments

- **[pixeltris/TwitchAdSolutions](https://github.com/pixeltris/TwitchAdSolutions)**: Special thanks for the `vaft` and `video-swap` scripts which power the ad-blocking capabilities of this client.

---

*Disclaimer: Samtch is not affiliated with, maintained, authorized, endorsed, or sponsored by Twitch Interactive, Inc.*
