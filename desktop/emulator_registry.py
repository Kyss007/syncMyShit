#!/usr/bin/env python3
"""
syncMyShit - Emulator Registry for Linux & Windows
Detects standard save file locations for retro emulators across desktop operating systems.
Matches Android Google Drive folder layout for cross-device synchronization.
Dedicated to the Public Domain (The Unlicense)
"""

import os
import sys
from pathlib import Path
from typing import Dict, List, Optional


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
                    existing.append(expanded)
            except Exception:
                pass
        return existing


def build_emulator_database() -> List[EmulatorDefinition]:
    home = Path.home()
    appdata = Path(os.environ.get("APPDATA", str(home / "AppData" / "Roaming")))
    localappdata = Path(os.environ.get("LOCALAPPDATA", str(home / "AppData" / "Local")))
    docs = home / "Documents"

    return [
        # RetroArch (Universal)
        EmulatorDefinition(
            id="retroarch",
            name="RetroArch",
            category="Multi-system",
            process_names=["retroarch", "retroarch.exe"],
            linux_paths=[
                home / ".config" / "retroarch" / "saves",
                home / ".var" / "app" / "org.libretro.RetroArch" / "config" / "retroarch" / "saves",
                home / ".local" / "share" / "Steam" / "steamapps" / "common" / "RetroArch" / "saves",
            ],
            windows_paths=[
                appdata / "RetroArch" / "saves",
                Path("C:/RetroArch-Win64/saves"),
                Path("C:/Program Files (x86)/Steam/steamapps/common/RetroArch/saves"),
            ],
            extensions=[".srm", ".state", ".sav", ".rtc", ".nv"],
            drive_folder="RetroArch_Saves",
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
            ],
            windows_paths=[
                docs / "Dolphin Emulator" / "GC",
                docs / "Dolphin Emulator" / "Wii" / "title",
                appdata / "Dolphin Emulator" / "GC",
                appdata / "Dolphin Emulator" / "Wii" / "title",
            ],
            extensions=[".raw", ".gcp", ".gci", ".bin", ".sav"],
            drive_folder="Dolphin",
        ),
        # PCSX2 (PlayStation 2)
        EmulatorDefinition(
            id="pcsx2",
            name="PCSX2",
            category="PlayStation 2",
            process_names=["pcsx2-qt", "pcsx2", "pcsx2.exe", "pcsx2-qt.exe"],
            linux_paths=[
                home / ".config" / "PCSX2" / "memcards",
                home / ".var" / "app" / "net.pcsx2.PCSX2" / "config" / "PCSX2" / "memcards",
            ],
            windows_paths=[
                docs / "PCSX2" / "memcards",
                appdata / "PCSX2" / "memcards",
            ],
            extensions=[".ps2", ".p2s", ".sav", ".bin"],
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
                home / ".var" / "app" / "org.duckstation.DuckStation" / "data" / "duckstation" / "memcards",
            ],
            windows_paths=[
                docs / "DuckStation" / "memcards",
                appdata / "DuckStation" / "memcards",
            ],
            extensions=[".mcd", ".mcr", ".sav"],
            drive_folder="DuckStation",
        ),
        # PPSSPP (PSP)
        EmulatorDefinition(
            id="ppsspp",
            name="PPSSPP",
            category="PlayStation Portable",
            process_names=["PPSSPPSDL", "PPSSPPQt", "PPSSPPWindows64.exe", "PPSSPPWindows.exe"],
            linux_paths=[
                home / ".config" / "ppsspp" / "PSP" / "SAVEDATA",
                home / ".var" / "app" / "org.ppsspp.PPSSPP" / "config" / "ppsspp" / "PSP" / "SAVEDATA",
            ],
            windows_paths=[
                docs / "PPSSPP" / "PSP" / "SAVEDATA",
                appdata / "PPSSPP" / "PSP" / "SAVEDATA",
            ],
            extensions=[".bin", ".sfo", ".png", ".dat", ".sav"],
            drive_folder="PPSSPP",
        ),
        # RPCS3 (PlayStation 3)
        EmulatorDefinition(
            id="rpcs3",
            name="RPCS3",
            category="PlayStation 3",
            process_names=["rpcs3", "rpcs3.exe"],
            linux_paths=[
                home / ".config" / "rpcs3" / "dev_hdd0" / "home",
                home / ".var" / "app" / "net.rpcs3.RPCS3" / "config" / "rpcs3" / "dev_hdd0" / "home",
            ],
            windows_paths=[
                appdata / "rpcs3" / "dev_hdd0" / "home",
            ],
            extensions=[".bin", ".dat", ".sfo", ".sav"],
            drive_folder="PS3_RPCS3",
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
            ],
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
            ],
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
            ],
            windows_paths=[
                localappdata / "Cemu" / "mlc01" / "usr" / "save",
            ],
            extensions=[".dat", ".bin", ".sav"],
            drive_folder="WiiU_Cemu",
        ),
        # Citra / Lime3DS / Mandarine (Nintendo 3DS)
        EmulatorDefinition(
            id="citra",
            name="Citra / Lime3DS",
            category="Nintendo 3DS",
            process_names=["citra-qt", "citra-qt.exe", "citra.exe", "lime3ds", "lime3ds.exe"],
            linux_paths=[
                home / ".local" / "share" / "citra-emu" / "sdmc",
                home / ".var" / "app" / "org.citra_emu.citra" / "data" / "citra-emu" / "sdmc",
                home / ".local" / "share" / "lime3ds" / "sdmc",
                home / ".var" / "app" / "io.github.lime3ds.Lime3DS" / "data" / "lime3ds" / "sdmc",
            ],
            windows_paths=[
                appdata / "Citra" / "sdmc",
                appdata / "Lime3DS" / "sdmc",
            ],
            extensions=[".sav", ".bin", ".dat", ".bmssv"],
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
            ],
            windows_paths=[
                appdata / "melonDS",
            ],
            extensions=[".sav", ".dsv"],
            drive_folder="MelonDS",
        ),
        # DraStic (Nintendo DS - Wine / Desktop)
        EmulatorDefinition(
            id="drastic",
            name="DraStic DS",
            category="Nintendo DS",
            process_names=["drastic", "drastic.exe"],
            linux_paths=[
                home / "DraStic",
                home / ".drastic",
                home / "RetroPie" / "roms" / "nds",
            ],
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
            ],
            windows_paths=[
                appdata / "mGBA",
            ],
            extensions=[".sav", ".ss1", ".ss2", ".state"],
            drive_folder="mGBA",
        ),
        # Flycast (Dreamcast / Naomi)
        EmulatorDefinition(
            id="flycast",
            name="Flycast",
            category="Sega Dreamcast",
            process_names=["flycast", "flycast.exe"],
            linux_paths=[
                home / ".local" / "share" / "flycast",
                home / ".var" / "app" / "com.flycast.Flycast" / "data" / "flycast",
            ],
            windows_paths=[
                appdata / "Flycast",
            ],
            extensions=[".bin", ".dat", ".sav"],
            drive_folder="Flycast",
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
            ],
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
            ],
            windows_paths=[
                appdata / "RMG" / "Save",
            ],
            extensions=[".mpk", ".fla", ".sra", ".eep", ".sav"],
            drive_folder="Mupen64Plus",
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
