# syncMyShit — Decky Plugin v2

Cloud save sync for Steam Deck Gaming Mode (Quick Access Menu).

## Login (Game Mode)

1. Open Decky → **syncMyShit** → **Link Google Drive**
2. Scan the QR with your phone (same Wi‑Fi as the Deck)
3. Sign in with Google on the phone, then paste the `http://127.0.0.1...` URL back into the phone page
4. Deck connects automatically — sync away

## Install

```bash
curl -sSL https://raw.githubusercontent.com/Kyss007/syncMyShit/main/install-decky.sh | bash
```

## Features

- Phone QR Google Drive login (works in Game Mode)
- Scan EmuDeck / Flatpak / SD card saves
- Sync All + per-emulator sync
- Auto-sync when an emulator process exits
- Activity log + local backups before overwrite
