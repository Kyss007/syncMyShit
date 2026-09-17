<div align="center">

# ☁️ syncMyShit
### Automagic Cloud Save Sync for Android Retro Emulation Handhelds

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?logo=android)](https://www.android.com/)
[![Download APK](https://img.shields.io/badge/Download-Latest%20Release%20APK-00E5FF?style=for-the-badge&logo=android)](https://github.com/Kyss007/syncMyShit/releases/latest)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%201.9-purple?logo=kotlin)](https://kotlinlang.org/)
[![Google Drive](https://img.shields.io/badge/Cloud-Google%20Drive%20v3-yellow?logo=googledrive)](https://developers.google.com/drive)
[![License: Unlicense](https://img.shields.io/badge/License-The%20Unlicense%20(Public%20Domain)-green.svg)](LICENSE)
[![Cost](https://img.shields.io/badge/Cost-100%25%20Free%20Forever-success.svg)](LICENSE)

> [!CAUTION]
> **EARLY EXPERIMENTAL / UNTESTED SOFTWARE**
> This application is in early development and has **NOT** been tested on physical handheld devices yet.
> Background services, save detection, and Scoped Storage integrations may have bugs.
> **Always make manual backups of your save files** before running or testing this application.

*Cloud save synchronization for Android retro emulation devices using Google Drive.*

---

### 📥 [Download Release APKs](https://github.com/Kyss007/syncMyShit/releases)
- **[GitHub Releases](https://github.com/Kyss007/syncMyShit/releases)** (Automated builds from CI)
- **Installation**: Download the `.apk` directly to your Android device, tap to install (allow *"Install unknown apps"* if prompted), and proceed through the setup wizard.

---

</div>

## 💡 What is syncMyShit?
Managing save files across multiple Android devices or emulators is tedious:
- Every emulator stores saves in different directories (`/RetroArch/saves`, `Android/data/...`, `/PSP/SAVEDATA`).
- Native decompilations and recomp projects (Zelda 64 Recompiled, Ship of Harkinian, SM64) use their own separate directory structures.
- **syncMyShit** is designed to automate this workflow:
  1. Connect your Google Drive in the setup wizard.
  2. Scans for known emulator save directories and recomp games.
  3. Pre-play: Checks Google Drive for newer saves before game launch.
  4. Post-play: Syncs save files to Google Drive in the background after exiting.

---

## ✨ Features (Intended Design)

- 🪄 **Background Sync**: Intended to run in the background using `UsageStatsManager` and foreground services.
- 🔓 **Scoped Storage Handling**:
  - Implements **Storage Access Framework (SAF)** DocumentTree and optional **Shizuku** rootless ADB bindings.
  - Aims to access emulator folders inside `Android/data/` on Android 11+ (such as DraStic, Dolphin, AetherSX2).
- 🆓 **100% Free & Unlicensed (Public Domain)**: Zero fees, zero subscriptions, no proprietary lock-in ([The Unlicense](LICENSE)).
- 🔍 **Pre-configured Profiles for 20+ Emulators & Recomps**:
  - Profiles defined for RetroArch, PPSSPP, Dolphin, DuckStation, Vita3K, Yuzu/Suyu, Mupen64Plus, DraStic, MelonDS, Pizza Boy, MyBoy, Zelda 64 Recompiled, Ship of Harkinian, SM64, Balatro, and PortMaster.
- ➕ **Custom Paths**: Add arbitrary directories or file extension filters (`.sav`, `.dat`, `.json`, `*`).
- 🎮 **Controller / D-Pad Focus**: Navigation states with focus borders for D-Pad / gamepad navigation.
- 🛡️ **Snapshot Backups**:
  - Creates timestamped copies in `.syncmyshit_backups/` and Google Drive `_backups/` before files are replaced.
- ☁️ **Drive Authentication**:
  - Google Sign-In with Drive API v3, plus custom OAuth 2.0 Client ID input option.

---

## 🎯 Target Platform & Status

- **Target OS**: Android 8.0+ (API 26 to API 34)
- **Hardware Status**: **UNTESTED ON PHYSICAL HARDWARE**.
  - No physical device testing has been conducted yet.
  - Early testers, feedback, and issue reports on GitHub are welcome.

---

## 🚀 Setup Steps

1. **Install APK**: Download from [Releases](https://github.com/Kyss007/syncMyShit/releases).
2. **Permissions**: Grant All Files Access and Usage Stats Access in the wizard.
3. **Google Drive**: Sign in to your Google account.
4. **Select Emulators**: Review detected save directories and configure custom paths.

---

## ⚙️ Google Drive Setup Options

For details on connecting Google Drive, using your own Google Cloud Console OAuth Client ID, or running on de-Googled handhelds, check out the [Google Drive Setup Guide](docs/GOOGLE_DRIVE_SETUP.md).

For a complete list of supported emulators, paths, and extensions, see the [Supported Emulators Guide](docs/SUPPORTED_EMULATORS.md).

For permissions details and troubleshooting Scoped Storage on Android 11–14+, see the [Permissions Guide](docs/PERMISSIONS_GUIDE.md).

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 (Java 17)
- Android SDK (API 34, Build Tools 34.0.0)
- Git

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Kyss007/syncMyShit.git
cd syncMyShit

# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Output APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Contributing

Contributions are warmly welcomed! If you'd like to add support for another emulator, source port, or handheld project:
1. Fork this repository.
2. Add your emulator configuration to [`EmulatorRegistry.kt`](app/src/main/java/com/syncmyshit/app/data/local/EmulatorRegistry.kt).
3. Submit a Pull Request!

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
