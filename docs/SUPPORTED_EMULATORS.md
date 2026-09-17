# 🕹️ Supported Emulators, Recomps & Handheld Ports

**syncMyShit** automatically scans internal storage and MicroSD cards for the following systems and games out of the box. You can also add **any** custom game, recomp, or directory directly from the app!

---

## 📋 Out-of-the-Box Supported Systems

| System / App | Emulator Name | Default Save Locations Tracked | File Extensions |
| :--- | :--- | :--- | :--- |
| **Multi-System** | RetroArch / 64 / 32 | `RetroArch/saves`, `RetroArch/states`, `Android/data/com.retroarch*/files/saves` | `.srm`, `.state`, `.state.auto`, `.rtc`, `.brm` |
| **Sony PSP** | PPSSPP / Gold | `PSP/SAVEDATA`, `PSP/PPSSPP_STATE`, `Android/data/org.ppsspp.ppsspp*/files/PSP` | `.bin`, `.sfo`, `.png`, `.ppst` |
| **Sony PS2** | AetherSX2 / NetherSX2 | `Android/data/xyz.aethersx2.android/files/memcards`, `xyz.nether.android`, `AetherSX2/memcards` | `.ps2`, `.mcd`, `.p2s` |
| **Nintendo GC / Wii** | Dolphin / MMJR / MMJR2 | `dolphin-emu/GC`, `dolphin-emu/Wii/title`, `Android/data/org.dolphinemu*/files` | `.raw`, `.gci`, `.sav`, `.ssp`, `.bin` |
| **Nintendo 3DS** | Citra / Lime3DS / Azahar | `citra-emu/sdmc`, `lime3ds-emu/sdmc`, `Android/data/org.citra.citra_emu*/files/sdmc` | `.sav`, `.dat`, `.bin`, `.db` |
| **Nintendo Switch** | Yuzu / Suyu / Sudachi / Uzuy | `Android/data/org.yuzu.yuzu_emu*/files/nand/user/save`, `suyu`, `sudachi` | `.bin`, `.dat`, `.sav` |
| **Sony PS1** | DuckStation | `Android/data/com.github.stenzek.duckstation/files/memcards`, `duckstation/` | `.mcd`, `.mcr`, `.sav` |
| **Sony PS Vita** | Vita3K | `Android/data/org.vita3k.emulator/files/vita3k/ux0/user/00/savedata` | `.bin`, `.dat`, `.png` |
| **Nintendo 64** | Mupen64Plus FZ / Pro | `Mupen64PlusFZ/GameSaves`, `Android/data/org.mupen64plusae.v3.fzurita*/files` | `.mpk`, `.fla`, `.sra`, `.eep`, `.st*` |
| **Sega Dreamcast** | Flycast | `Flycast/data`, `Android/data/com.flyinghead.Flycast/files/data` | `.bin`, `.state`, `.nvmem` |
| **Sega Dreamcast** | Redream | `redream`, `Android/data/io.recompiled.redream/files` | `.bin`, `.state` |
| **Nintendo DS** | DraStic | `DraStic/backup`, `DraStic/savestates`, `Android/data/com.dsemu.drastic/files/backup` | `.dsv`, `.dss` |
| **Nintendo DS** | MelonDS | `Android/data/me.magnum.melonds/files`, `melonDS` | `.sav`, `.mln` |
| **GBA / GBC** | Pizza Boy GBA / GBC | `Android/data/it.dbtecno.pizzaboy*/files/savegames`, `PizzaBoyGBA/savegames` | `.sav`, `.sta` |
| **GBA / GBC** | MyBoy! / MyOldBoy! | `MyBoy/save`, `MyOldBoy/save` | `.sav`, `.st*` |

---

## ⚡ Native PC Decompilations & Recompiled Projects

| Game / Project | Description | Default Android Paths Tracked | Supported Extensions |
| :--- | :--- | :--- | :--- |
| **Zelda 64: Recompiled** | Majora's Mask & OoT Recomp | `Android/data/com.zelda64recomp.*/files/saves`, `Zelda64Recomp/saves` | `.sav`, `.bin`, `.json` |
| **Ship of Harkinian (SoH)** | Native Ocarina of Time port | `ShipOfHarkinian/saves`, `Android/data/com.soh.android/files/saves` | `.sav`, `.json` |
| **2 Ship 2 Harkinian (2S2H)**| Native Majora's Mask port | `2Ship2Harkinian/saves`, `Android/data/com.soh.twoship/files/saves` | `.sav`, `.json` |
| **Super Mario 64 (sm64ex)** | Native SM64 Android port | `Android/data/com.retro.sm64ex/files/save`, `sm64ex/save` | `.eep`, `.sav` |
| **Perfect Dark Recomp** | Native Perfect Dark port | `Android/data/com.perfectdark.recomp/files/saves`, `PerfectDarkRecomp/saves` | `.sav`, `.eep` |
| **AM2R** | Another Metroid 2 Remake | `Android/data/com.am2r.android/files/save`, `AM2R/save` | `.sav`, `.dat` |
| **PortMaster Saves** | Handheld Linux/Android ports | `PortMaster/saves`, `roms/ports/savedata`, `ports/savedata` | `.sav`, `.dat`, `.json`, `*` |
| **Balatro Mobile** | Love2D / Android mobile port | `Android/data/org.love2d.android/files/save`, `Balatro/saves` | `.jkr`, `.dat` |

---

## ➕ Adding Custom Games and Paths

Don't see your favorite standalone game, custom emulator fork, or indie port?

1. Go to **Emulators** tab in **syncMyShit**.
2. Tap **+ Add Custom**.
3. Enter:
   - **Name**: e.g., `Hollow Knight Android` or `Sonic Mania Decomp`
   - **Platform / Tag**: e.g., `Native Port`
   - **Absolute Storage Directory Path**: e.g., `/sdcard/HollowKnight/saves`
   - **File Extensions**: e.g., `.dat, .save` (or `*` for everything in the directory)
   - **Google Drive Subfolder**: e.g., `HollowKnight`
4. Tap **Add & Track**!
syncMyShit will now automatically include this folder in all pre-play pulls, post-play pushes, and periodic cloud syncs!
