# syncMyShit Desktop (Linux & Windows) 🎮

Automagic retro emulator cloud save sync for **Linux** (Steam Deck, Arch, Ubuntu, etc.) and **Windows**.

Pairs seamlessly with the Android app on handhelds like Retroid Pocket 3+.

---

## ⚡ Quick Start

### Standalone Executable (No Python Required)
Download the prebuilt binary from [GitHub Releases](https://github.com/Kyss007/syncMyShit/releases):
* **Linux:** `chmod +x syncMyShit-linux-x64 && ./syncMyShit-linux-x64 status`
* **Windows:** Double-click or run in PowerShell `syncMyShit-windows-x64.exe status`

---

## 🚀 CLI Commands

### 1. Check Status & Detected Emulators
```bash
syncmyshit status
```
Automatically finds your emulators (RetroArch, Dolphin, PCSX2, DuckStation, PPSSPP, RPCS3, Ryujinx, Cemu, Citra, MelonDS, mGBA) across native installs, Steam, and Flatpak.

### 2. Scan Saves
```bash
syncmyshit scan
```
Scans and calculates hashes for all save files across detected emulators.

### 3. Run One-Time Sync
```bash
syncmyshit sync
```
Immediately compares local saves with your cloud/sync folder and downloads/uploads newer saves with zero save-loss local backups.

### 4. Run Automagic Background Daemon (Watcher)
```bash
syncmyshit watch
```
Runs in the background. Detects when you open an emulator (runs Pre-Play Sync) and when you close it (runs Post-Play Save Sync).

### 5. Add Custom Game / Emulator Path
```bash
syncmyshit add-path "MyPCGame" "/path/to/game/saves"
```

### 6. Set Custom Sync Folder (Syncthing / Drive / Nextcloud)
```bash
syncmyshit set-folder "~/GoogleDrive/syncMyShit"
```

---

## 🛡️ Zero Save-Loss Protection
Every time a cloud save is downloaded, the local file is automatically backed up with a timestamp to:
`~/.syncmyshit_backups/<emulator>/<filename>_<timestamp>.<ext>`
Keeps the latest 5 backups by default so you can never lose gameplay progress.

---

## 📄 License
Dedicated to the Public Domain under **The Unlicense**.
