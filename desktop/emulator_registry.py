#!/usr/bin/env python3
"""
syncMyShit - Emulator Registry for Linux & Windows
Detects standard save file locations for retro emulators across desktop operating systems.
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
    ):
        self.id = id
        self.name = name
        self.category = category
        self.process_names = process_names
        self.linux_paths = linux_paths
        self.windows_paths = windows_paths
        self.extensions = extensions

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
        ),
        # Citra (Nintendo 3DS)
        EmulatorDefinition(
            id="citra",
            name="Citra",
            category="Nintendo 3DS",
            process_names=["citra-qt", "citra-qt.exe", "citra.exe"],
            linux_paths=[
                home / ".local" / "share" / "citra-emu" / "sdmc",
                home / ".var" / "app" / "org.citra_emu.citra" / "data" / "citra-emu" / "sdmc",
            ],
            windows_paths=[
                appdata / "Citra" / "sdmc",
            ],
            extensions=[".sav", ".bin", ".dat"],
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
            })

    return detected
