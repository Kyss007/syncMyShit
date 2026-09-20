"""Steam Deck / EmuDeck / Flatpak save path discovery.

Drive subfolder names match Android EmulatorRegistry.driveSubfolder for cross-device sync.
"""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional


def _is_steam_path(path: Path) -> bool:
    try:
        s = str(path.resolve()).replace("\\", "/").lower()
    except Exception:
        s = str(path).replace("\\", "/").lower()
    needles = (
        "/steam/userdata/",
        "/compatdata/",
        "/steamapps/common/",
        "/steamapps/compatdata/",
        "/.steam/steam/",
        "com.valvesoftware.steam",
    )
    return any(n in s for n in needles)


def _emudeck_roots() -> List[Path]:
    roots: List[Path] = []
    home = Path.home() / "Emulation"
    if home.is_dir():
        roots.append(home)
    media = Path("/run/media")
    if media.exists():
        try:
            for card in media.iterdir():
                if not card.is_dir():
                    continue
                emu = card / "Emulation"
                if emu.is_dir():
                    roots.append(emu)
                for sub in card.iterdir():
                    if sub.is_dir():
                        nested = sub / "Emulation"
                        if nested.is_dir():
                            roots.append(nested)
        except Exception:
            pass
    return roots


@dataclass
class Emulator:
    id: str
    name: str
    category: str
    process_names: List[str]
    paths: List[Path]
    extensions: List[str]
    drive_folder: str

    def existing_paths(self) -> List[Path]:
        found: List[Path] = []
        for p in self.paths:
            try:
                expanded = Path(os.path.expanduser(str(p)))
                if expanded.is_dir() and not _is_steam_path(expanded):
                    found.append(expanded)
            except Exception:
                pass
        return found


def build_emulators() -> List[Emulator]:
    home = Path.home()
    flat = home / ".var" / "app"
    emu = _emudeck_roots()

    def ed(*parts: str) -> List[Path]:
        return [r.joinpath(*parts) for r in emu]

    return [
        Emulator(
            id="retroarch",
            name="RetroArch",
            category="Multi-system",
            process_names=["retroarch"],
            paths=[
                home / ".config" / "retroarch" / "saves",
                home / ".config" / "retroarch" / "states",
                flat / "org.libretro.RetroArch" / "config" / "retroarch" / "saves",
                flat / "org.libretro.RetroArch" / "config" / "retroarch" / "states",
                *ed("saves", "retroarch", "saves"),
                *ed("saves", "retroarch", "states"),
            ],
            extensions=[".srm", ".state", ".sav", ".rtc", ".nv", ".brm", ".cht"],
            drive_folder="RetroArch",
        ),
        Emulator(
            id="dolphin",
            name="Dolphin",
            category="GameCube / Wii",
            process_names=["dolphin-emu", "dolphin"],
            paths=[
                home / ".local" / "share" / "dolphin-emu" / "GC",
                home / ".local" / "share" / "dolphin-emu" / "Wii" / "title",
                flat / "org.DolphinEmu.dolphin-emu" / "data" / "dolphin-emu" / "GC",
                flat / "org.DolphinEmu.dolphin-emu" / "data" / "dolphin-emu" / "Wii" / "title",
                *ed("saves", "dolphin", "GC"),
                *ed("saves", "dolphin", "Wii", "title"),
            ],
            extensions=[".raw", ".gcp", ".gci", ".bin", ".sav", ".dat", ".ssp"],
            drive_folder="Dolphin",
        ),
        Emulator(
            id="pcsx2",
            name="PCSX2",
            category="PlayStation 2",
            process_names=["pcsx2-qt", "pcsx2"],
            paths=[
                home / ".config" / "PCSX2" / "memcards",
                home / ".config" / "PCSX2" / "sstates",
                flat / "net.pcsx2.PCSX2" / "config" / "PCSX2" / "memcards",
                flat / "net.pcsx2.PCSX2" / "config" / "PCSX2" / "sstates",
                *ed("saves", "pcsx2", "memcards"),
                *ed("saves", "pcsx2", "sstates"),
                *ed("saves", "pcsx2", "saves"),
            ],
            extensions=[".ps2", ".p2s", ".mcd", ".bin"],
            drive_folder="AetherSX2",  # Android AetherSX2 / NetherSX2
        ),
        Emulator(
            id="duckstation",
            name="DuckStation",
            category="PlayStation",
            process_names=["duckstation-qt", "duckstation"],
            paths=[
                home / ".local" / "share" / "duckstation" / "memcards",
                home / ".local" / "share" / "duckstation" / "savestates",
                flat / "org.duckstation.DuckStation" / "data" / "duckstation" / "memcards",
                *ed("saves", "duckstation", "memcards"),
                *ed("saves", "duckstation", "savestates"),
            ],
            extensions=[".mcd", ".mcr", ".sav"],
            drive_folder="DuckStation",
        ),
        Emulator(
            id="ppsspp",
            name="PPSSPP",
            category="PSP",
            process_names=["PPSSPPSDL", "ppsspp"],
            paths=[
                home / ".config" / "ppsspp" / "PSP" / "SAVEDATA",
                home / ".config" / "ppsspp" / "PSP" / "PPSSPP_STATE",
                flat / "org.ppsspp.PPSSPP" / "config" / "ppsspp" / "PSP" / "SAVEDATA",
                *ed("saves", "ppsspp", "PSP", "SAVEDATA"),
                *ed("saves", "ppsspp", "PSP", "PPSSPP_STATE"),
            ],
            extensions=[".bin", ".sfo", ".ppst", ".dat"],
            drive_folder="PPSSPP",
        ),
        Emulator(
            id="ryujinx",
            name="Ryujinx",
            category="Switch",
            process_names=["Ryujinx", "ryujinx"],
            paths=[
                home / ".config" / "Ryujinx" / "bis" / "user" / "save",
                flat / "org.ryujinx.Ryujinx" / "config" / "Ryujinx" / "bis" / "user" / "save",
                *ed("saves", "ryujinx"),
            ],
            extensions=[".bin", ".dat", ".save"],
            drive_folder="Yuzu",  # shared Switch cloud folder with Android
        ),
        Emulator(
            id="yuzu",
            name="Yuzu / Suyu / Sudachi",
            category="Switch",
            process_names=["yuzu", "suyu", "sudachi"],
            paths=[
                home / ".local" / "share" / "yuzu" / "nand" / "user" / "save",
                home / ".local" / "share" / "suyu" / "nand" / "user" / "save",
                *ed("saves", "yuzu"),
                *ed("saves", "suyu"),
            ],
            extensions=[".bin", ".dat", ".save"],
            drive_folder="Yuzu",
        ),
        Emulator(
            id="citra",
            name="Citra / Lime3DS / Azahar",
            category="3DS",
            process_names=["citra-qt", "citra", "lime3ds", "azahar"],
            paths=[
                home / ".local" / "share" / "citra-emu" / "sdmc",
                home / ".local" / "share" / "lime3ds-emu" / "sdmc",
                *ed("saves", "citra"),
                *ed("saves", "lime3ds"),
            ],
            extensions=[".sav", ".bin", ".dat"],
            drive_folder="Citra",
        ),
        Emulator(
            id="melonds",
            name="melonDS",
            category="NDS",
            process_names=["melonDS", "melonds"],
            paths=[
                home / ".config" / "melonDS",
                home / ".local" / "share" / "melonDS",
                *ed("saves", "melonds"),
            ],
            extensions=[".sav", ".mln", ".dsv"],
            drive_folder="MelonDS",
        ),
        Emulator(
            id="mgba",
            name="mGBA",
            category="GBA",
            process_names=["mgba", "mgba-qt"],
            paths=[
                home / ".config" / "mgba",
                *ed("saves", "mgba"),
            ],
            extensions=[".sav", ".ss0", ".ss1", ".ss2"],
            drive_folder="mGBA",
        ),
        Emulator(
            id="flycast",
            name="Flycast",
            category="Dreamcast",
            process_names=["flycast"],
            paths=[
                home / ".local" / "share" / "flycast",
                flat / "org.flycast.Flycast" / "data" / "flycast",
                *ed("saves", "flycast"),
            ],
            extensions=[".bin", ".srm", ".state"],
            drive_folder="Flycast",
        ),
        Emulator(
            id="cemu",
            name="Cemu",
            category="Wii U",
            process_names=["Cemu", "cemu"],
            paths=[
                home / ".local" / "share" / "Cemu" / "mlc01",
                *ed("saves", "cemu"),
            ],
            extensions=[".bin", ".sav", ".dat"],
            drive_folder="CEMU",
        ),
        Emulator(
            id="vita3k",
            name="Vita3K",
            category="PS Vita",
            process_names=["Vita3K", "vita3k"],
            paths=[
                home / ".local" / "share" / "Vita3K" / "ux0" / "user" / "00" / "savedata",
                *ed("saves", "vita3k"),
            ],
            extensions=[".bin", ".dat"],
            drive_folder="Vita3K",
        ),
        Emulator(
            id="rpcs3",
            name="RPCS3",
            category="PS3",
            process_names=["rpcs3"],
            paths=[
                home / ".config" / "rpcs3" / "dev_hdd0" / "home" / "00000001" / "savedata",
                *ed("saves", "rpcs3"),
            ],
            extensions=[".bin", ".dat", ".sav"],
            drive_folder="PS3_aPS3e",
        ),
    ]


@dataclass
class ScanItem:
    id: str
    name: str
    category: str
    drive_folder: str
    save_path: str
    exists: bool
    save_count: int
    process_names: List[str] = field(default_factory=list)


def count_saves(folder: Path, extensions: List[str]) -> int:
    if not folder.is_dir():
        return 0
    ext = {e.lower() for e in extensions}
    n = 0
    try:
        for root, dirs, files in os.walk(folder):
            dirs[:] = [d for d in dirs if not d.startswith(".") and d != ".syncmyshit_backups"]
            for f in files:
                p = Path(root) / f
                if not ext or p.suffix.lower() in ext:
                    n += 1
    except Exception:
        pass
    return n


def scan_emulators() -> List[ScanItem]:
    items: List[ScanItem] = []
    for emu in build_emulators():
        paths = emu.existing_paths()
        if not paths:
            continue
        # Prefer path with most saves
        best = paths[0]
        best_count = count_saves(best, emu.extensions)
        for p in paths[1:]:
            c = count_saves(p, emu.extensions)
            if c > best_count:
                best, best_count = p, c
        items.append(
            ScanItem(
                id=emu.id,
                name=emu.name,
                category=emu.category,
                drive_folder=emu.drive_folder,
                save_path=str(best),
                exists=True,
                save_count=best_count,
                process_names=list(emu.process_names),
            )
        )
    items.sort(key=lambda x: (-x.save_count, x.name.lower()))
    return items


def get_emulator(emu_id: str) -> Optional[Emulator]:
    for e in build_emulators():
        if e.id == emu_id:
            return e
    return None


def process_to_emulator_ids() -> Dict[str, str]:
    mapping: Dict[str, str] = {}
    for e in build_emulators():
        for proc in e.process_names:
            mapping[proc.lower()] = e.id
    return mapping
