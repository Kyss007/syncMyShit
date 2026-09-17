# syncMyShit Desktop (Linux & Windows) 🎮

Automagic retro emulator cloud save sync for **Linux** (Steam Deck, Arch, Ubuntu, etc.) and **Windows**.

Pairs seamlessly with the Android app on handhelds like Retroid Pocket 3+, Anbernic RG DS, and Odin.

---

## ⚡ Quick Start

### 1. Standalone App (No Python Required)
Download the prebuilt release from [GitHub Releases](https://github.com/Kyss007/syncMyShit/releases):

#### 🐧 Linux & Steam Deck:
* **Option A (Desktop App):** Download `syncMyShit-linux-x64.tar.gz`, extract it, and run `./install.sh`. It adds `syncMyShit` to your Application Launcher / Start Menu and sets up executable permissions automatically.
* **Option B (Direct binary):**
  ```bash
  chmod +x syncMyShit-linux-x64
  ./syncMyShit-linux-x64
  ```
  *(Launching without arguments automatically opens the Graphical User Interface)*

#### 🪟 Windows:
* Download `syncMyShit-windows-x64.exe` (or `.zip`) and double-click to launch the GUI!

---

## 🖥️ Graphical User Interface (GUI)
Launching the app without CLI arguments automatically opens the dark-themed desktop interface:
* **⚡ Sync Saves Now**: One-click sync across all detected emulators.
* **🔍 Scan Saves**: Lists all found save files, sizes, and timestamps.
* **👁 Auto-Sync Watcher**: Runs in the background, syncing saves when you start or close an emulator.
* **📁 Select Cloud / Sync Folder**: Easily point to your Google Drive Desktop folder, Syncthing, Nextcloud, or SD card.
* **➕ Add Custom Path**: Add custom standalone PC games, ports, or non-standard emulator directories.

---

## 🚀 CLI Commands (Headless / Scripts / Power Users)

```bash
# Open Graphical UI
syncmyshit gui

# Show detected emulators and cloud folder status
syncmyshit status

# Scan save files across all emulators
syncmyshit scan

# Run save sync immediately once
syncmyshit sync

# Run background watcher daemon (detects game start/exit)
syncmyshit watch

# Add custom save folder
syncmyshit add-path "MyPCGame" "/path/to/game/saves"

# Set target sync folder (Google Drive, Syncthing, Nextcloud, etc.)
syncmyshit set-folder "~/GoogleDrive/syncMyShit"

# Interactive terminal menu (for SSH / headless)
syncmyshit --cli
```

---

## 🎮 Supported Desktop Emulators
Automatically detects native installs, Flatpak, and Steam for:
* **Dolphin** (GameCube & Wii)
* **PCSX2** (PlayStation 2)
* **DuckStation** (PlayStation 1)
* **PPSSPP** (PSP)
* **RPCS3** (PlayStation 3)
* **Ryujinx / Yuzu / Suyu / Sudachi** (Nintendo Switch)
* **Cemu** (Wii U)
* **Citra / Lime3DS** (Nintendo 3DS)
* **MelonDS / DraStic** (Nintendo DS)
* **mGBA** (Game Boy Advance)
* **Flycast** (Sega Dreamcast)
* **Vita3K** (Sony PS Vita)
* **RMG / Mupen64Plus** (Nintendo 64)
* **RetroArch** (Universal Multi-System)

---

## 🛡️ Zero Save-Loss Protection
Every time a cloud save is updated or overwritten, the local save is automatically backed up with a timestamp to:
`~/.syncmyshit_backups/<emulator>/<filename>_<timestamp>.<ext>`

Keeps the latest 5 versions so you can rollback at any time and never lose gameplay progress.

---

## 📄 License
Dedicated to the Public Domain under **The Unlicense**.
