#!/usr/bin/env python3
"""
syncMyShit - Emulator & Recomp Registry for Linux, SteamOS & Windows
Detects standard save file locations for retro emulators, recompilation projects,
and standalone ports across desktop operating systems.
Matches Android Google Drive folder layout 1:1 for cross-device synchronization.
Dedicated to the Public Domain (The Unlicense)
"""

import os
import sys
from pathlib import Path
from typing import Dict, List, Optional


def is_steam_game_path(path: Path) -> bool:
    """
    Returns True if path belongs to Steam Cloud, Proton prefix (compatdata),
    Steam userdata, or official Steam game installations.
    Steam Cloud handles official Steam game saves automatically;
    syncMyShit must never touch or duplicate Steam game saves.
    """
    try:
        checks = [path]
        try:
            resolved = path.resolve()
            if resolved != path:
                checks.append(resolved)
        except Exception:
            pass

        for p in checks:
            p_str = str(p).replace("\\", "/").lower()
            # Steam Cloud userdata directory (account/appid)
            if "/steam/userdata/" in p_str or ("/userdata/" in p_str and "steam" in p_str):
                return True
            # Proton compatibility prefixes (compatdata/<appid>/pfx)
            if "/compatdata/" in p_str or "/pfx/" in p_str or "/steamapps/compatdata/" in p_str:
                return True
            # Steam library common games
            if "/steamapps/common/" in p_str or "/steamapps/" in p_str:
                return True
            # Steam system config / runtime roots
            if "/.steam/steam/" in p_str or "/.local/share/steam/" in p_str:
                return True
            # Flatpak Steam
            if "com.valvesoftware.steam" in p_str:
                return True
    except Exception:
        pass
    return False


class EmulatorDefinition:
    def __init__(
        self,
        id: str,
        name: str,
        category: str,
        process_names: List[str],
        linux_paths: List[Path],
        windows_paths: List[Path],
        extensions: List[str],
        drive_folder: Optional[str] = None,
    ):
        self.id = id
        self.name = name
        self.category = category
        self.process_names = process_names
        self.linux_paths = linux_paths
        self.windows_paths = windows_paths
        self.extensions = extensions
        self.drive_folder = drive_folder or id

    def get_existing_paths(self) -> List[Path]:
        candidates = self.windows_paths if sys.platform == "win32" else self.linux_paths
        existing = []
        for p in candidates:
            try:
                expanded = Path(os.path.expandvars(os.path.expanduser(str(p))))
                if expanded.exists() and expanded.is_dir():
                    # Strictly filter out any Steam game or Proton prefix directories
                    if not is_steam_game_path(expanded):
                        existing.append(expanded)
            except Exception:
                pass
        return existing


def get_emudeck_roots() -> List[Path]:
    """Finds EmuDeck Emulation folders on internal storage and SD card(s)."""
    roots = []
    # Internal home Emulation
    home_emu = Path.home() / "Emulation"
    if home_emu.exists() and home_emu.is_dir():
        roots.append(home_emu)

    # SD Cards on SteamOS (/run/media/mmcblk0p1 or /run/media/deck/*)
    run_media = Path("/run/media")
    if run_media.exists():
        try:
            for card in run_media.glob("*"):
                if card.is_dir():
                    emu_dir = card / "Emulation"
                    if emu_dir.exists() and emu_dir.is_dir():
                        roots.append(emu_dir)
                    for sub in card.glob("*"):
                        if sub.is_dir():
                            emu_sub = sub / "Emulation"
                            if emu_sub.exists() and emu_sub.is_dir():
                                roots.append(emu_sub)
        except Exception:
            pass
    return roots


def get_sdcard_mounts() -> List[Path]:
    """Finds all mounted SD cards or external storage mounts on Linux / SteamOS."""
    mounts = []
    run_media = Path("/run/media")
    if run_media.exists():
        try:
            for card in run_media.glob("*"):
                if card.is_dir():
                    mounts.append(card)
                    for sub in card.glob("*"):
                        if sub.is_dir():
                            mounts.append(sub)
        except Exception:
            pass
    return mounts


def build_emulator_database() -> List[EmulatorDefinition]:
    home = Path.home()
    appdata = Path(os.environ.get("APPDATA", str(home / "AppData" / "Roaming")))
    localappdata = Path(os.environ.get("LOCALAPPDATA", str(home / "AppData" / "Local")))
    docs = home / "Documents"
    emudeck = get_emudeck_roots()
    sdcards = get_sdcard_mounts()

    return [
        # RetroArch (Universal - Multi-system)
        EmulatorDefinition(
            id="retroarch",
            name="RetroArch",
            category="Multi-system",
            process_names=["retroarch", "retroarch.exe"],
            linux_paths=[
                home / ".config" / "retroarch" / "saves",
                home / ".config" / "retroarch" / "states",
                home / ".var" / "app" / "org.libretro.RetroArch" / "config" / "retroarch" / "saves",
                home / ".var" / "app" / "org.libretro.RetroArch" / "config" / "retroarch" / "states",
            ] + [r / "saves" / "retroarch" / "saves" for r in emudeck] + [r / "saves" / "retroarch" / "states" for r in emudeck],
            windows_paths=[
                appdata / "RetroArch" / "saves",
                Path("C:/RetroArch-Win64/saves"),
            ],
            extensions=[".srm", ".state", ".sav", ".rtc", ".nv", ".state.auto", ".brm", ".cht"],
            drive_folder="RetroArch",
        ),
        # Dolphin (GameCube / Wii)
        EmulatorDefinition(
            id="dolphin",
            name="Dolphin",
            category="GameCube / Wii",
            process_names=["dolphin-emu", "Dolphin.exe", "dolphin.exe"],
            linux_paths=[
                home / ".local" / "share" / "dolphin-emu" / "GC",
                home / ".local" / "share" / "dolphin-emu" / "Wii" / "title",
                home / ".var" / "app" / "org.DolphinEmu.dolphin-emu" / "data" / "dolphin-emu" / "GC",
                home / ".var" / "app" / "org.DolphinEmu.dolphin-emu" / "data" / "dolphin-emu" / "Wii" / "title",
            ] + [r / "saves" / "dolphin" / "GC" for r in emudeck] + [r / "saves" / "dolphin" / "Wii" / "title" for r in emudeck],
            windows_paths=[
                docs / "Dolphin Emulator" / "GC",
                docs / "Dolphin Emulator" / "Wii" / "title",
                appdata / "Dolphin Emulator" / "GC",
                appdata / "Dolphin Emulator" / "Wii" / "title",
            ],
            extensions=[".raw", ".gcp", ".gci", ".bin", ".sav", ".dat", ".ssp"],
            drive_folder="Dolphin",
        ),
        # PCSX2 (PlayStation 2) - Syncs with Android AetherSX2/NetherSX2
        EmulatorDefinition(
            id="pcsx2",
            name="PCSX2",
            category="PlayStation 2",
            process_names=["pcsx2-qt", "pcsx2", "pcsx2.exe", "pcsx2-qt.exe"],
            linux_paths=[
                home / ".config" / "PCSX2" / "memcards",
                home / ".config" / "PCSX2" / "sstates",
                home / ".var" / "app" / "net.pcsx2.PCSX2" / "config" / "PCSX2" / "memcards",
                home / ".var" / "app" / "net.pcsx2.PCSX2" / "config" / "PCSX2" / "sstates",
            ] + [r / "saves" / "pcsx2" / "saves" for r in emudeck] + [r / "saves" / "pcsx2" / "memcards" for r in emudeck] + [r / "saves" / "pcsx2" / "sstates" for r in emudeck],
            windows_paths=[
                docs / "PCSX2" / "memcards",
                appdata / "PCSX2" / "memcards",
            ],
            extensions=[".ps2", ".p2s", ".sav", ".bin", ".mcd"],
            drive_folder="AetherSX2",
        ),
        # DuckStation (PlayStation 1)
        EmulatorDefinition(
            id="duckstation",
            name="DuckStation",
            category="PlayStation 1",
            process_names=["duckstation-qt", "duckstation.exe", "duckstation-qt-x64-ReleaseLTCG.exe"],
            linux_paths=[
                home / ".local" / "share" / "duckstation" / "memcards",
                home / ".local" / "share" / "duckstation" / "savestates",
                home / ".var" / "app" / "org.duckstation.DuckStation" / "data" / "duckstation" / "memcards",
                home / ".var" / "app" / "org.duckstation.DuckStation" / "data" / "duckstation" / "savestates",
            ] + [r / "saves" / "duckstation" / "saves" for r in emudeck] + [r / "saves" / "duckstation" / "memcards" for r in emudeck],
            windows_paths=[
                docs / "DuckStation" / "memcards",
                appdata / "DuckStation" / "memcards",
            ],
            extensions=[".mcd", ".mcr", ".sav", ".bin"],
            drive_folder="DuckStation",
        ),
        # PPSSPP (PlayStation Portable)
        EmulatorDefinition(
            id="ppsspp",
            name="PPSSPP",
            category="PlayStation Portable",
            process_names=["PPSSPPSDL", "PPSSPPQt", "PPSSPPWindows64.exe", "PPSSPPWindows.exe"],
            linux_paths=[
                home / ".config" / "ppsspp" / "PSP" / "SAVEDATA",
                home / ".config" / "ppsspp" / "PSP" / "PPSSPP_STATE",
                home / ".var" / "app" / "org.ppsspp.PPSSPP" / "config" / "ppsspp" / "PSP" / "SAVEDATA",
                home / ".var" / "app" / "org.ppsspp.PPSSPP" / "config" / "ppsspp" / "PSP" / "PPSSPP_STATE",
            ] + [r / "saves" / "ppsspp" / "PSP" / "SAVEDATA" for r in emudeck] + [r / "saves" / "ppsspp" / "saves" for r in emudeck],
            windows_paths=[
                docs / "PPSSPP" / "PSP" / "SAVEDATA",
                appdata / "PPSSPP" / "PSP" / "SAVEDATA",
            ],
            extensions=[".bin", ".sfo", ".png", ".dat", ".sav", ".ppst"],
            drive_folder="PPSSPP",
        ),
        # RPCS3 (PlayStation 3) - Syncs with Android aPS3e/ARMSX3
        EmulatorDefinition(
            id="rpcs3",
            name="RPCS3",
            category="PlayStation 3",
            process_names=["rpcs3", "rpcs3.exe"],
            linux_paths=[
                home / ".config" / "rpcs3" / "dev_hdd0" / "home",
                home / ".var" / "app" / "net.rpcs3.RPCS3" / "config" / "rpcs3" / "dev_hdd0" / "home",
            ] + [r / "saves" / "rpcs3" / "dev_hdd0" / "home" for r in emudeck],
            windows_paths=[
                appdata / "rpcs3" / "dev_hdd0" / "home",
            ],
            extensions=[".bin", ".dat", ".sfo", ".sav"],
            drive_folder="PS3_aPS3e",
        ),
        # Ryujinx (Nintendo Switch)
        EmulatorDefinition(
            id="ryujinx",
            name="Ryujinx",
            category="Nintendo Switch",
            process_names=["Ryujinx", "Ryujinx.exe", "ryujinx"],
            linux_paths=[
                home / ".config" / "Ryujinx" / "bis" / "user" / "save",
                home / ".var" / "app" / "org.ryujinx.Ryujinx" / "config" / "Ryujinx" / "bis" / "user" / "save",
            ] + [r / "saves" / "ryujinx" / "bis" / "user" / "save" for r in emudeck],
            windows_paths=[
                appdata / "Ryujinx" / "bis" / "user" / "save",
            ],
            extensions=[".dat", ".bin", ".sav"],
            drive_folder="Switch",
        ),
        # Yuzu / Suyu / Sudachi (Nintendo Switch)
        EmulatorDefinition(
            id="yuzu",
            name="Yuzu / Suyu / Sudachi",
            category="Nintendo Switch",
            process_names=["yuzu", "yuzu.exe", "suyu", "suyu.exe", "sudachi", "sudachi.exe"],
            linux_paths=[
                home / ".local" / "share" / "yuzu" / "nand" / "user" / "save",
                home / ".var" / "app" / "org.yuzu_emu.yuzu" / "data" / "yuzu" / "nand" / "user" / "save",
                home / ".local" / "share" / "suyu" / "nand" / "user" / "save",
                home / ".var" / "app" / "org.suyu_emu.suyu" / "data" / "suyu" / "nand" / "user" / "save",
                home / ".local" / "share" / "sudachi" / "nand" / "user" / "save",
            ] + [r / "saves" / "yuzu" / "nand" / "user" / "save" for r in emudeck],
            windows_paths=[
                appdata / "yuzu" / "nand" / "user" / "save",
                appdata / "suyu" / "nand" / "user" / "save",
                appdata / "sudachi" / "nand" / "user" / "save",
            ],
            extensions=[".dat", ".bin", ".sav"],
            drive_folder="Switch",
        ),
        # Cemu (Wii U)
        EmulatorDefinition(
            id="cemu",
            name="Cemu",
            category="Wii U",
            process_names=["Cemu", "Cemu.exe", "cemu"],
            linux_paths=[
                home / ".local" / "share" / "cemu" / "mlc01" / "usr" / "save",
                home / ".var" / "app" / "info.cemu.Cemu" / "data" / "cemu" / "mlc01" / "usr" / "save",
            ] + [r / "saves" / "cemu" / "mlc01" / "usr" / "save" for r in emudeck],
            windows_paths=[
                localappdata / "Cemu" / "mlc01" / "usr" / "save",
            ],
            extensions=[".dat", ".bin", ".sav"],
            drive_folder="WiiU_Cemu",
        ),
        # Citra / Lime3DS / Azahar (Nintendo 3DS)
        EmulatorDefinition(
            id="citra",
            name="Citra / Lime3DS / Azahar",
            category="Nintendo 3DS",
            process_names=["citra-qt", "citra-qt.exe", "citra.exe", "lime3ds", "lime3ds.exe", "azahar"],
            linux_paths=[
                home / ".local" / "share" / "citra-emu" / "sdmc",
                home / ".var" / "app" / "org.citra_emu.citra" / "data" / "citra-emu" / "sdmc",
                home / ".local" / "share" / "lime3ds" / "sdmc",
                home / ".var" / "app" / "io.github.lime3ds.Lime3DS" / "data" / "lime3ds" / "sdmc",
                home / ".local" / "share" / "azahar" / "sdmc",
            ] + [r / "saves" / "citra" / "sdmc" for r in emudeck],
            windows_paths=[
                appdata / "Citra" / "sdmc",
                appdata / "Lime3DS" / "sdmc",
            ],
            extensions=[".sav", ".bin", ".dat", ".bmssv", ".db"],
            drive_folder="Citra_3DS",
        ),
        # MelonDS (Nintendo DS)
        EmulatorDefinition(
            id="melonds",
            name="MelonDS",
            category="Nintendo DS",
            process_names=["melonDS", "melonDS.exe"],
            linux_paths=[
                home / ".config" / "melonDS",
                home / ".var" / "app" / "net.kuribo64.melonDS" / "config" / "melonDS",
            ] + [r / "saves" / "melonds" / "saves" for r in emudeck],
            windows_paths=[
                appdata / "melonDS",
            ],
            extensions=[".sav", ".dsv", ".mln"],
            drive_folder="MelonDS",
        ),
        # DraStic (Nintendo DS)
        EmulatorDefinition(
            id="drastic",
            name="DraStic DS",
            category="Nintendo DS",
            process_names=["drastic", "drastic.exe"],
            linux_paths=[
                home / "DraStic" / "backup",
                home / "DraStic",
                home / ".drastic",
                home / "RetroPie" / "roms" / "nds",
            ] + [r / "saves" / "drastic" for r in emudeck],
            windows_paths=[
                docs / "DraStic",
                Path("C:/DraStic"),
            ],
            extensions=[".dsv", ".dss", ".sav", ".state", ".dst"],
            drive_folder="DraStic",
        ),
        # mGBA (Game Boy Advance)
        EmulatorDefinition(
            id="mgba",
            name="mGBA",
            category="Game Boy Advance",
            process_names=["mgba-qt", "mGBA.exe", "mgba.exe"],
            linux_paths=[
                home / ".local" / "share" / "mgba",
                home / ".var" / "app" / "io.mgba.mGBA" / "data" / "mgba",
            ] + [r / "saves" / "mgba" / "saves" for r in emudeck],
            windows_paths=[
                appdata / "mGBA",
            ],
            extensions=[".sav", ".ss1", ".ss2", ".state"],
            drive_folder="mGBA",
        ),
        # Flycast (Sega Dreamcast / Naomi)
        EmulatorDefinition(
            id="flycast",
            name="Flycast",
            category="Sega Dreamcast",
            process_names=["flycast", "flycast.exe"],
            linux_paths=[
                home / ".local" / "share" / "flycast",
                home / ".var" / "app" / "com.flycast.Flycast" / "data" / "flycast",
            ] + [r / "saves" / "flycast" for r in emudeck],
            windows_paths=[
                appdata / "Flycast",
            ],
            extensions=[".bin", ".dat", ".sav", ".state", ".nvmem"],
            drive_folder="Flycast",
        ),
        # Redream (Sega Dreamcast)
        EmulatorDefinition(
            id="redream",
            name="Redream",
            category="Sega Dreamcast",
            process_names=["redream", "redream.exe"],
            linux_paths=[
                home / ".config" / "redream",
                home / ".local" / "share" / "redream",
                home / "redream",
            ] + [r / "saves" / "redream" for r in emudeck],
            windows_paths=[
                appdata / "redream",
            ],
            extensions=[".bin", ".state"],
            drive_folder="Redream",
        ),
        # Vita3K (Sony PS Vita)
        EmulatorDefinition(
            id="vita3k",
            name="Vita3K",
            category="Sony PS Vita",
            process_names=["Vita3K", "Vita3K.exe", "vita3k"],
            linux_paths=[
                home / ".local" / "share" / "Vita3K" / "Vita3K" / "ux0" / "user" / "00" / "savedata",
                home / ".var" / "app" / "org.vita3k.Vita3K" / "data" / "Vita3K" / "ux0" / "user" / "00" / "savedata",
            ] + [r / "saves" / "vita3k" / "ux0" / "user" / "00" / "savedata" for r in emudeck],
            windows_paths=[
                appdata / "Vita3K" / "ux0" / "user" / "00" / "savedata",
            ],
            extensions=[".bin", ".dat"],
            drive_folder="Vita3K",
        ),
        # RMG / Mupen64Plus (Nintendo 64)
        EmulatorDefinition(
            id="mupen64plus",
            name="RMG / Mupen64Plus",
            category="Nintendo 64",
            process_names=["RMG", "RMG.exe", "mupen64plus"],
            linux_paths=[
                home / ".local" / "share" / "RMG" / "Save",
                home / ".var" / "app" / "com.github.Rosalie241.RMG" / "data" / "RMG" / "Save",
                home / ".local" / "share" / "mupen64plus" / "save",
            ] + [r / "saves" / "mupen64plus" / "save" for r in emudeck],
            windows_paths=[
                appdata / "RMG" / "Save",
            ],
            extensions=[".mpk", ".fla", ".sra", ".eep", ".sav", ".st0", ".st1", ".st2"],
            drive_folder="Mupen64",
        ),
        # YabaSanshiro / Kronos (Sega Saturn)
        EmulatorDefinition(
            id="yabasanshiro",
            name="YabaSanshiro / Saturn",
            category="Sega Saturn",
            process_names=["yabause", "kronos"],
            linux_paths=[
                home / ".local" / "share" / "yabause",
                home / ".config" / "yabause",
                home / ".config" / "kronos",
            ] + [r / "saves" / "saturn" for r in emudeck],
            windows_paths=[
                appdata / "yabause",
                appdata / "kronos",
            ],
            extensions=[".bin", ".dat", ".sav"],
            drive_folder="SegaSaturn",
        ),
        # Xemu / Xenia (Xbox & Xbox 360)
        EmulatorDefinition(
            id="xbox_emulators",
            name="Xemu / Xbox",
            category="Xbox & Xbox 360",
            process_names=["xemu", "xemu.exe", "xenia.exe"],
            linux_paths=[
                home / ".local" / "share" / "xemu" / "xemu",
                home / ".var" / "app" / "app.xemu.xemu" / "data" / "xemu" / "xemu",
            ] + [r / "saves" / "xbox" for r in emudeck],
            windows_paths=[
                appdata / "xemu" / "xemu",
            ],
            extensions=[".bin", ".dat", ".sav"],
            drive_folder="Xbox",
        ),
        # Snes9x / .EMU Suite
        EmulatorDefinition(
            id="broglia_emu_suite",
            name="Snes9x / Classic",
            category="Classic Consoles",
            process_names=["snes9x-gtk", "snes9x", "snes9x.exe", "snes9x-x64.exe"],
            linux_paths=[
                home / ".config" / "snes9x",
                home / ".var" / "app" / "com.snes9x.Snes9x" / "config" / "snes9x",
                home / ".snes9x",
            ] + [r / "saves" / "snes" for r in emudeck],
            windows_paths=[
                appdata / "snes9x",
                docs / "snes9x",
            ],
            extensions=[".srm", ".sav", ".000", ".001"],
            drive_folder="Broglia_Emu",
        ),
        # ScummVM (Point & Click Adventures)
        EmulatorDefinition(
            id="scummvm",
            name="ScummVM",
            category="ScummVM",
            process_names=["scummvm", "scummvm.exe"],
            linux_paths=[
                home / ".local" / "share" / "scummvm" / "saves",
                home / ".var" / "app" / "org.scummvm.ScummVM" / "data" / "scummvm" / "saves",
            ] + [r / "saves" / "scummvm" for r in emudeck],
            windows_paths=[
                appdata / "ScummVM" / "Saves",
            ],
            extensions=[".s??", ".sav", ".0??"],
            drive_folder="ScummVM",
        ),
        # PICO-8
        EmulatorDefinition(
            id="pico8",
            name="PICO-8",
            category="PICO-8",
            process_names=["pico8", "pico8.64", "pico-8"],
            linux_paths=[
                home / ".lexaloffle" / "pico-8" / "cdata",
                home / ".local" / "share" / "pico-8" / "cdata",
            ],
            windows_paths=[
                appdata / "pico-8" / "cdata",
            ],
            extensions=[".p8", ".png"],
            drive_folder="PICO8",
        ),
        # MAME (Arcade)
        EmulatorDefinition(
            id="mame4droid",
            name="MAME (Arcade)",
            category="Arcade (MAME)",
            process_names=["mame", "mame64", "mame.exe"],
            linux_paths=[
                home / ".mame" / "nvram",
                home / ".mame" / "sta",
            ] + [r / "saves" / "mame" / "nvram" for r in emudeck],
            windows_paths=[
                docs / "mame" / "nvram",
            ],
            extensions=[".sta", ".nv"],
            drive_folder="MAME",
        ),

        # ==========================================
        # RECOMPILATION PROJECTS
        # ==========================================
        # Zelda 64: Recompiled (Majora's Mask & Ocarina of Time)
        EmulatorDefinition(
            id="zelda64_recomp",
            name="Zelda 64: Recompiled (MM & OoT)",
            category="Recomp",
            process_names=[
                "Zelda64Recomp",
                "Zelda64Recompiled",
                "zelda64recomp",
                "MajoraRecomp",
                "OoTRecomp",
                "Zelda64Recomp.exe",
            ],
            linux_paths=[
                home / ".local" / "share" / "Zelda64Recomp" / "saves",
                home / ".local" / "share" / "Zelda64Recomp",
                home / ".local" / "share" / "ZeldaRecomp" / "saves",
                home / ".local" / "share" / "zelda64recomp" / "saves",
                home / ".config" / "Zelda64Recomp" / "saves",
                home / ".config" / "zelda64recomp" / "saves",
                home / "Zelda64Recomp" / "saves",
                home / "ZeldaRecomp" / "saves",
                home / ".var" / "app" / "com.zelda64recomp.zelda64recomp" / "data" / "saves",
                home / "roms" / "ports" / "Zelda64Recomp" / "saves",
                home / "roms" / "ports" / "zelda64recomp" / "saves",
            ] + [r / "saves" / "ports" / "zelda64recomp" for r in emudeck]
              + [r / "saves" / "ports" / "zelda64" for r in emudeck]
              + [r / "roms" / "ports" / "Zelda64Recomp" / "saves" for r in emudeck]
              + [r / "roms" / "ports" / "zelda64recomp" / "saves" for r in emudeck]
              + [m / "roms" / "ports" / "Zelda64Recomp" / "saves" for m in sdcards]
              + [m / "roms" / "ports" / "zelda64recomp" / "saves" for m in sdcards],
            windows_paths=[
                appdata / "Zelda64Recomp" / "saves",
                localappdata / "Zelda64Recomp" / "saves",
                Path("C:/Zelda64Recomp/saves"),
            ],
            extensions=[".sav", ".bin", ".json"],
            drive_folder="Recomp_Zelda64",
        ),
        # Ship of Harkinian (SoH & 2 Ship 2 Harkinian)
        EmulatorDefinition(
            id="ship_of_harkinian",
            name="Ship of Harkinian (SoH & 2S2H)",
            category="Recomp",
            process_names=[
                "soh.elf",
                "soh",
                "2s2h.elf",
                "2s2h",
                "Ship of Harkinian",
                "2 Ship 2 Harkinian",
                "soh.exe",
                "2s2h.exe",
            ],
            linux_paths=[
                home / ".local" / "share" / "soh" / "saves",
                home / ".local" / "share" / "Ship of Harkinian" / "saves",
                home / ".local" / "share" / "2s2h" / "saves",
                home / ".local" / "share" / "2 Ship 2 Harkinian" / "saves",
                home / ".var" / "app" / "com.harbourmasters.ShipOfHarkinian" / "data" / "soh" / "saves",
                home / ".var" / "app" / "com.harbourmasters.ShipOfHarkinian" / "data" / "saves",
                home / "ShipOfHarkinian" / "saves",
                home / "2Ship2Harkinian" / "saves",
                home / "roms" / "ports" / "soh" / "saves",
                home / "roms" / "ports" / "2s2h" / "saves",
            ] + [r / "saves" / "ports" / "soh" for r in emudeck]
              + [r / "saves" / "ports" / "2s2h" for r in emudeck]
              + [r / "roms" / "ports" / "soh" / "saves" for r in emudeck]
              + [r / "roms" / "ports" / "2s2h" / "saves" for r in emudeck]
              + [m / "roms" / "ports" / "soh" / "saves" for m in sdcards]
              + [m / "roms" / "ports" / "2s2h" / "saves" for m in sdcards],
            windows_paths=[
                appdata / "Ship of Harkinian" / "saves",
                appdata / "2 Ship 2 Harkinian" / "saves",
                localappdata / "soh" / "saves",
                Path("C:/ShipOfHarkinian/saves"),
            ],
            extensions=[".sav", ".json"],
            drive_folder="Recomp_SoH",
        ),
        # Super Mario 64 (sm64ex / sm64pc)
        EmulatorDefinition(
            id="sm64_android",
            name="Super Mario 64 (sm64ex / sm64pc)",
            category="Recomp",
            process_names=[
                "sm64.us",
                "sm64.eu",
                "sm64.jp",
                "sm64ex",
                "sm64pc",
                "sm64.us.f3dex2e.exe",
                "sm64.exe",
            ],
            linux_paths=[
                home / ".local" / "share" / "sm64pc" / "save",
                home / ".local" / "share" / "sm64pc",
                home / ".local" / "share" / "sm64ex" / "save",
                home / ".local" / "share" / "sm64ex",
                home / ".config" / "sm64pc",
                home / ".config" / "sm64ex",
                home / "sm64ex" / "save",
                home / "sm64pc" / "save",
                home / "sm64ex",
                home / "sm64pc",
            ] + [r / "saves" / "ports" / "sm64" for r in emudeck]
              + [r / "roms" / "ports" / "sm64" for r in emudeck]
              + [m / "roms" / "ports" / "sm64" for m in sdcards],
            windows_paths=[
                appdata / "sm64pc",
                localappdata / "sm64ex",
            ],
            extensions=[".eep", ".sav"],
            drive_folder="Recomp_SM64",
        ),
        # Perfect Dark Recompiled
        EmulatorDefinition(
            id="perfect_dark_recomp",
            name="Perfect Dark Recompiled",
            category="Recomp",
            process_names=[
                "PerfectDarkRecomp",
                "pdrecomp",
                "PerfectDarkRecomp.exe",
            ],
            linux_paths=[
                home / ".local" / "share" / "PerfectDarkRecomp" / "saves",
                home / ".local" / "share" / "PerfectDarkRecomp",
                home / ".config" / "PerfectDarkRecomp" / "saves",
                home / "PerfectDarkRecomp" / "saves",
                home / "PerfectDarkRecomp",
            ] + [r / "saves" / "ports" / "perfectdark" for r in emudeck]
              + [r / "roms" / "ports" / "perfectdark" / "saves" for r in emudeck]
              + [m / "roms" / "ports" / "perfectdark" / "saves" for m in sdcards],
            windows_paths=[
                appdata / "PerfectDarkRecomp" / "saves",
                localappdata / "PerfectDarkRecomp",
            ],
            extensions=[".sav", ".eep"],
            drive_folder="Recomp_PerfectDark",
        ),

        # ==========================================
        # STANDALONE GAMES & SOURCE PORTS
        # ==========================================
        # AM2R (Another Metroid 2 Remake)
        EmulatorDefinition(
            id="am2r_android",
            name="AM2R (Another Metroid 2 Remake)",
            category="Standalone / Port",
            process_names=["AM2R", "am2r", "AM2R.exe"],
            linux_paths=[
                home / ".config" / "AM2R",
                home / ".local" / "share" / "AM2R",
                home / "AM2R" / "save",
                home / "AM2R",
            ] + [r / "roms" / "ports" / "am2r" for r in emudeck]
              + [m / "roms" / "ports" / "am2r" for m in sdcards],
            windows_paths=[
                localappdata / "AM2R",
            ],
            extensions=[".sav", ".dat"],
            drive_folder="Game_AM2R",
        ),
        # Source Engine (Portal / HL2 Standalone & Android ports - excludes official Steam installations)
        EmulatorDefinition(
            id="source_engine",
            name="Source Engine (Standalone / Port)",
            category="Standalone / Port",
            process_names=["srceng", "hl2_linux"],
            linux_paths=[
                home / ".local" / "share" / "srceng" / "portal" / "SAVE",
                home / ".local" / "share" / "srceng" / "hl2" / "SAVE",
                home / "srceng" / "portal" / "SAVE",
                home / "srceng" / "hl2" / "SAVE",
                home / "roms" / "portal" / "SAVE",
            ],
            windows_paths=[
                Path("C:/srceng/portal/SAVE"),
            ],
            extensions=[".sav", ".tga"],
            drive_folder="Game_SourceEngine",
        ),
        # PojavLauncher / Minecraft Java
        EmulatorDefinition(
            id="pojav_launcher",
            name="PojavLauncher / Minecraft Java",
            category="Standalone / Port",
            process_names=["java", "pojavlauncher", "minecraft"],
            linux_paths=[
                home / ".minecraft" / "saves",
                home / ".var" / "app" / "net.kdt.pojavlaunch" / "data" / ".minecraft" / "saves",
                home / ".var" / "app" / "com.mojang.Minecraft" / "data" / ".minecraft" / "saves",
                home / ".local" / "share" / "PrismLauncher" / "instances",
            ],
            windows_paths=[
                appdata / ".minecraft" / "saves",
            ],
            extensions=[".dat", ".dat_old", ".mca"],
            drive_folder="Game_PojavMinecraft",
        ),
        # Minecraft (Bedrock - Linux Launcher)
        EmulatorDefinition(
            id="minecraft_bedrock",
            name="Minecraft (Bedrock)",
            category="Standalone / Port",
            process_names=["mcpelauncher-client", "mcpelauncher-ui", "Minecraft.Windows.exe"],
            linux_paths=[
                home / ".local" / "share" / "mcpelauncher" / "games" / "com.mojang" / "minecraftWorlds",
                home / ".var" / "app" / "io.mrarm.mcpelauncher" / "data" / "mcpelauncher" / "games" / "com.mojang" / "minecraftWorlds",
            ],
            windows_paths=[
                localappdata / "Packages" / "Microsoft.MinecraftUWP_8wekyb3d8bbwe" / "LocalState" / "games" / "com.mojang" / "minecraftWorlds",
            ],
            extensions=[".dat", ".dat_old", ".ldb"],
            drive_folder="Game_MinecraftBedrock",
        ),
        # PortMaster Saves
        EmulatorDefinition(
            id="portmaster",
            name="PortMaster Saves",
            category="Standalone / Port",
            process_names=["portmaster", "PortMaster"],
            linux_paths=[
                home / "PortMaster" / "saves",
                home / "roms" / "ports" / "savedata",
                home / "ports" / "savedata",
                home / "Emulation" / "tools" / "PortMaster" / "saves",
                home / "Emulation" / "roms" / "ports" / "savedata",
            ] + [r / "tools" / "PortMaster" / "saves" for r in emudeck]
              + [r / "roms" / "ports" / "savedata" for r in emudeck]
              + [m / "PortMaster" / "saves" for m in sdcards]
              + [m / "roms" / "ports" / "savedata" for m in sdcards],
            windows_paths=[
                docs / "PortMaster" / "saves",
            ],
            extensions=[".sav", ".dat", ".json", ".bin", ".state"],
            drive_folder="PortMaster",
        ),
        # Balatro (Love2D / Standalone Mobile Port - excludes official Steam installation)
        EmulatorDefinition(
            id="balatro_mobile",
            name="Balatro (Love2D / Standalone Port)",
            category="Standalone / Port",
            process_names=["love", "balatro"],
            linux_paths=[
                home / ".local" / "share" / "love" / "Balatro",
                home / ".var" / "app" / "org.love2d.Love" / "data" / "love" / "Balatro",
                home / "Balatro" / "saves",
            ] + [r / "roms" / "ports" / "balatro" / "saves" for r in emudeck]
              + [m / "roms" / "ports" / "balatro" / "saves" for m in sdcards],
            windows_paths=[
                appdata / "Balatro",
            ],
            extensions=[".jkr", ".dat"],
            drive_folder="Game_Balatro",
        ),
    ]


def detect_installed_emulators() -> List[Dict[str, object]]:
    """Scans system and returns list of installed/detected emulators with their save paths."""
    database = build_emulator_database()
    detected = []

    for emu in database:
        existing = emu.get_existing_paths()
        if existing:
            detected.append({
                "id": emu.id,
                "name": emu.name,
                "category": emu.category,
                "process_names": emu.process_names,
                "paths": [str(p) for p in existing],
                "extensions": emu.extensions,
                "drive_folder": emu.drive_folder,
            })

    return detected
