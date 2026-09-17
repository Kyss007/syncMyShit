# 🛡️ Android Permissions Guide

To provide seamless, "automagic" background save synchronization without requiring user intervention every time you play, **syncMyShit** requests two standard Android system permissions.

---

## 1. All Files Access (`MANAGE_EXTERNAL_STORAGE`)

### Why is this needed?
Starting in Android 11 (API 30), Google introduced Scoped Storage. Emulators, decompilations, and retro games store their save files in different locations:
- Direct root folders: `/storage/emulated/0/RetroArch/saves`, `PSP/SAVEDATA`, `dolphin-emu/GC/`
- Removable MicroSD cards: `/storage/XXXX-XXXX/...`
- App directories: `Android/data/<package_name>/files/` (e.g. **DraStic**, **Dolphin**, **AetherSX2**, **Citra**, **Yuzu**)

### 🔓 Two Ways syncMyShit Solves Scoped Storage for DraStic & Dolphin:

1. **Option A: Shizuku Rootless Access (Recommended - 1 Click for All Emulators)**:
   - If you have [Shizuku](https://shizuku.rikka.app/) installed on your handheld, **syncMyShit** detects it automagically.
   - Simply tap **Grant Shizuku Permission** in the app.
   - All `Android/data` directories for DraStic, Dolphin, AetherSX2, and every emulator sync instantly with zero root required and no folder picking needed!

2. **Option B: Storage Access Framework (SAF) DocumentTree**:
   - If you don't use Shizuku, **syncMyShit** allows granting access directly to the emulator's save folder via Android's native folder picker.
   - It saves persistent permissions so you only have to grant it once.

### How to Grant:
- The setup wizard will automatically open the **All files access** screen.
- Toggle the switch **Allow access to manage all files** for **syncMyShit**.

---

## 2. Usage Stats Access (`PACKAGE_USAGE_STATS`)

### Why is this needed?
This is what makes syncMyShit **automagic**:
- When you launch an emulator (e.g., AetherSX2 or RetroArch), the background watcher detects the game opening and pulls down newer save files from Google Drive before you start.
- When you return to your handheld launcher (e.g. Daijishou, Beacon, EmulationStation-DE, or Android Home), it detects that you finished playing and immediately uploads your newly saved progress to Google Drive.

### How to Grant:
- In Step 1 of the Setup Wizard, tap **Grant** next to **Usage Stats Access**.
- Find **syncMyShit** in the list of apps and toggle **Permit usage access** to ON.

---

## 3. Battery Optimization (Recommended for Handhelds)

Android aggressive battery savers can occasionally kill background services. For 100% reliable background syncing on handhelds (Retroid Pocket, AYN Odin, Anbernic):

1. Long press the **syncMyShit** app icon and tap **App info** (or go to Android Settings > Apps > syncMyShit).
2. Tap **Battery** or **App battery usage**.
3. Select **Unrestricted** (or "Don't optimize").
