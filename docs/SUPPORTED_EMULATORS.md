# 🕹️ Supported Emulators, Recomps & Handheld Ports

**syncMyShit** automatically scans internal storage and MicroSD cards for the following systems and games out of the box. You can also add **any** custom game, recomp, or directory directly from the app!

---

## 📋 Out-of-the-Box Supported Systems

| System / App | Emulator Name | Default Save Locations Tracked | File Extensions |
| :--- | :--- | :--- | :--- |
| **Multi-System** | RetroArch / 64 / 32 | `RetroArch/saves`, `RetroArch/states`, `Android/data/com.retroarch*/files/saves` | `.srm`, `.state`, `.state.auto`, `.rtc`, `.brm` |
| **Sony PSP** | PPSSPP / Gold / Legacy | `PSP/SAVEDATA`, `PSP/PPSSPP_STATE`, `Android/data/org.ppsspp.ppsspp*/files/PSP` | `.bin`, `.sfo`, `.png`, `.ppst` |
| **Sony PS2** | AetherSX2 / NetherSX2 / ARMSX2 | `Android/data/xyz.aethersx2.android/files/memcards`, `xyz.nether.android`, `AetherSX2/memcards` | `.ps2`, `.mcd`, `.p2s` |
| **Sony PS1** | DuckStation / ePSXe / FPse | `Android/data/com.github.stenzek.duckstation/files/memcards`, `duckstation/`, `epsxe/memcards` | `.mcd`, `.mcr`, `.sav`, `.bin` |
| **Sony PS Vita** | Vita3K / Vita3K ZX | `Android/data/org.vita3k.emulator/files/vita3k/ux0/user/00/savedata` | `.bin`, `.dat`, `.png` |
| **Sony PS3** | aPS3e / ARMSX3 | `Android/data/aenu.aps3e/files/dev_hdd0/home/00000001/savedata` | `.bin`, `.dat`, `.png` |
| **Nintendo GC / Wii** | Dolphin / MMJR / Ishiiruka / PrimeHack | `dolphin-emu/GC`, `dolphin-emu/Wii/title`, `Android/data/org.dolphinemu*/files` | `.raw`, `.gci`, `.sav`, `.ssp`, `.bin` |
| **Nintendo Wii U** | CEMU Android | `Android/data/info.cemu.cemu/files/mlc01/usr/save` | `.bin`, `.dat`, `.txt` |
| **Nintendo 3DS** | Citra / Lime3DS / Azahar / Mandarine | `citra-emu/sdmc`, `lime3ds-emu/sdmc`, `Android/data/org.citra.citra_emu*/files/sdmc` | `.sav`, `.dat`, `.bin`, `.db` |
| **Nintendo Switch** | Yuzu / Suyu / Sudachi / Citron / Eden / Skyline | `Android/data/org.yuzu.yuzu_emu*/files/nand/user/save`, `suyu`, `sudachi` | `.bin`, `.dat`, `.sav` |
| **Nintendo DS / DSi** | DraStic / MelonDS / NooDS | `DraStic/backup`, `Android/data/com.dsemu.drastic/files/backup`, `me.magnum.melonds` | `.dsv`, `.dss`, `.sav`, `.mln` |
| **Nintendo 64** | Mupen64Plus FZ / Pro / AE | `Mupen64PlusFZ/GameSaves`, `Android/data/org.mupen64plusae.v3.fzurita*/files` | `.mpk`, `.fla`, `.sra`, `.eep`, `.st*` |
| **Sega Saturn** | YabaSanshiro 2 / Saturn.EMU | `Android/data/org.devmiyax.yabasanshioro2/files`, `yabasanshioro` | `.bin`, `.dat`, `.sav` |
| **Sega Dreamcast** | Flycast / Redream / Reicast | `Flycast/data`, `redream`, `Android/data/io.recompiled.redream/files` | `.bin`, `.state`, `.nvmem` |
| **GBA / GBC / GB** | Pizza Boy / MyBoy / SkyEmu / Linkboy | `Android/data/it.dbtecno.pizzaboy*/files/savegames`, `MyBoy/save` | `.sav`, `.sta`, `.st*` |
| **Classic .EMU** | Snes9x EX+, NES, MD, NeoGeo, MSX, C64, PCE | `Android/data/com.explusalpha.*/files`, `.emu/saves` | `.sav`, `.sta`, `.srm` |
| **Xbox & Xbox 360** | X1 BOX, ax360e, X360 Mobile | `Android/data/com.izzy2lost.x1box/files`, `Android/data/aenu.ax360e/files` | `.bin`, `.dat`, `.sav` |
| **Windows PC** | Winlator / MiceWine / GameHub | `Android/data/com.winlator/files`, `Winlator` | `.sav`, `.dat`, `.ini` |
| **Arcade / MAME** | MAME4droid (2024 / Classic) | `MAME4droid/sta`, `MAME4droid/nvram` | `.sta`, `.nv` |
| **Adventures** | ScummVM | `ScummVM/saves`, `Android/data/org.scummvm.scummvm/files/saves` | `.s??`, `.sav`, `.0??` |
| **Fantasy Console** | PICO-8 (Pixl8 / Infinity P8) | `pico-8/cdata`, `Android/data/be.codedreams.pixl8/files` | `.p8`, `.png`, `.txt` |
| **Virtual Boy** | Virtual Boy (vvb) | `Android/data/com.simongellis.vvb/files/saves` | `.sav`, `.srm` |
| **Nokia N-Gage** | EKA2L1 | `EKA2L1/data/drives/c` | `.dat`, `.bin` |
| **Java Mobile** | J2ME Loader | `j2meloader/rms`, `Android/data/ru.playsoftware.j2meloader/files` | `.db`, `.rms` |
| **Atari Jaguar** | IrataJaguar | `IrataJaguar/saves`, `Android/data/ru.vastness.altmer.iratajaguar/files` | `.eep`, `.sav` |

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
