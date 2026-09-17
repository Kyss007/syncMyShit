# syncMyShit - Decky Loader Plugin

🎮 **Automagic Retro Emulator Cloud Save Sync for Steam Deck**

This plugin brings full `syncMyShit` cloud save synchronization directly into the Steam Deck's **Quick Access Menu (QAM / `...` button)** in Gaming Mode.

![Steam Deck QAM](https://raw.githubusercontent.com/Kyss007/syncMyShit/main/docs/banner.png)

---

## Features

- ⚡ **One-Click Handheld Sync**: Sync all retro emulator saves directly from SteamOS Gaming Mode without opening Desktop Mode.
- 🔄 **Auto-Sync on Game Exit**: Built-in background daemon detects when an emulator process (e.g. `retroarch`, `dolphin-emu`, `pcsx2`, `duckstation`, `ppsspp`) closes and automatically uploads updated saves to the cloud!
- 🗂️ **Zero Save-Loss Protection**: Timestamped local backups are kept before overwriting saves.
- 👾 **Full EmuDeck & Flatpak Support**: Detects RetroArch, Dolphin, PCSX2, DuckStation, PPSSPP, Ryujinx, Yuzu, Cemu, Citra, MelonDS, DraStic, mGBA, Flycast, Vita3K, and Mupen64Plus across internal storage and MicroSD cards (`/run/media/`).
- ☁️ **Universal Cloud Compatibility**: Connects seamlessly with Google Drive, Syncthing, Nextcloud, or any local/network directory matching the Android and PC clients.
- 📜 **Live Activity Log**: Displays recent sync events (uploads, downloads, timestamps) directly inside the Decky menu.

---

## Installation on Steam Deck

### Method 1: Automatic Installer (Recommended)
In Desktop Mode on your Steam Deck, open a terminal (Konsole) and run:
```bash
curl -sSL https://raw.githubusercontent.com/Kyss007/syncMyShit/main/install-decky.sh | bash
```

### Method 2: Manual Installation
1. Ensure [Decky Loader](https://deckbrew.xyz/) is installed on your Steam Deck.
2. Download `syncMyShit-decky.zip` from the [Releases](https://github.com/Kyss007/syncMyShit/releases) page.
3. Extract the `syncMyShit` folder into `~/homebrew/plugins/`:
   ```bash
   mkdir -p ~/homebrew/plugins
   cp -r syncMyShit ~/homebrew/plugins/
   ```
4. Restart Decky Loader or your Steam Deck:
   ```bash
   sudo systemctl restart plugin_loader
   ```

---

## Usage

1. Press the **`...` (Quick Access)** button on your Steam Deck.
2. Scroll down to the **Decky Loader (Plug)** tab.
3. Select **syncMyShit**.
4. Tap **Sync All Saves Now** or enable **Auto-Sync on Game Exit** for automated hands-off syncing!

---

## License

Dedicated to the Public Domain (The Unlicense). See `LICENSE` for details.
