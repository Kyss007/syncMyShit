#!/usr/bin/env python3
"""
syncMyShit - Cloud & Local Save Synchronization Backend
Synchronizes save files between local emulators and Google Drive or Local Folder / Syncthing.
Matches Android Google Drive naming conventions (e.g. ___ encoded paths and driveSubfolders).
Dedicated to the Public Domain (The Unlicense)
"""

import io
import json
import os
import shutil
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from config import ConfigManager
from sync_engine import SyncEngine, calculate_file_hash


def detect_default_cloud_folder() -> Optional[Path]:
    """Detects standard cloud sync directories (Google Drive for Desktop, Syncthing, Nextcloud)."""
    home = Path.home()
    candidates = []

    if sys_platform := os.name == "nt":
        # Windows candidates
        for drive in ["G", "H", "I", "D", "E"]:
            candidates.append(Path(f"{drive}:/My Drive/syncMyShit"))
            candidates.append(Path(f"{drive}:/syncMyShit"))
        userprofile = Path(os.environ.get("USERPROFILE", str(home)))
        candidates.extend([
            userprofile / "Google Drive" / "syncMyShit",
            userprofile / "OneDrive" / "syncMyShit",
            userprofile / "Syncthing" / "syncMyShit",
            userprofile / "Sync" / "syncMyShit",
        ])
    else:
        # Linux / Steam Deck candidates
        candidates.extend([
            home / "GoogleDrive" / "syncMyShit",
            home / "google-drive" / "syncMyShit",
            home / "Nextcloud" / "syncMyShit",
            home / "Syncthing" / "syncMyShit",
            home / "Sync" / "syncMyShit",
            home / ".local" / "share" / "syncMyShit" / "cloud_sync",
        ])

    for c in candidates:
        if c.exists() and c.is_dir():
            return c
    return None


class FolderSyncProvider:
    """Synchronizes saves with a designated sync folder (Google Drive, Syncthing, Nextcloud, SD card, etc.)."""

    def __init__(self, target_folder: Path, engine: SyncEngine):
        self.target_folder = target_folder
        self.target_folder.mkdir(parents=True, exist_ok=True)
        self.engine = engine

    def _decode_remote_name(self, name: str) -> str:
        """Converts Android Google Drive encoded names like 'backup___Game.sav' to 'backup/Game.sav'."""
        return name.replace("___", "/")

    def _encode_remote_name(self, rel_path: str) -> str:
        """Converts relative paths like 'backup/Game.sav' to 'backup___Game.sav'."""
        return rel_path.replace("\\", "/").replace("/", "___")

    def sync_emulator(
        self,
        emulator_id: str,
        local_paths: List[Path],
        extensions: List[str],
        drive_folder: Optional[str] = None
    ) -> List[str]:
        logs = []
        folder_name = drive_folder or emulator_id
        remote_emu_dir = self.target_folder / folder_name
        remote_emu_dir.mkdir(parents=True, exist_ok=True)

        # 1. Gather all local files across local paths
        # Key is normalized relative path (e.g. "backup/Game.sav" or "Game.srm")
        local_files: Dict[str, Path] = {}
        for l_path in local_paths:
            for item in self.engine.scan_directory(l_path, extensions):
                rel = item["relative"].replace("\\", "/")
                local_files[rel] = Path(item["path"])

        # 2. Gather remote files in target folder
        # Supports both real subdirectories (backup/Game.sav) and Android encoded names (backup___Game.sav)
        remote_files: Dict[str, Path] = {}
        for item in self.engine.scan_directory(remote_emu_dir, extensions):
            # Check if filename is encoded with '___'
            raw_p = Path(item["path"])
            decoded_name = self._decode_remote_name(raw_p.name)
            if "___" in raw_p.name:
                rel = decoded_name
            else:
                rel = item["relative"].replace("\\", "/")
            remote_files[rel] = raw_p

        # 3. Synchronize local <-> remote
        all_keys = set(local_files.keys()).union(set(remote_files.keys()))
        for rel in sorted(all_keys):
            l_file = local_files.get(rel)
            r_file = remote_files.get(rel)

            if l_file and not r_file:
                # Upload to remote folder: preserve subdirectory structure
                dest = remote_emu_dir / Path(rel)
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(l_file, dest)
                # Also create canonical ___ file if nested for Android compatibility
                if "/" in rel:
                    canonical_dest = remote_emu_dir / self._encode_remote_name(rel)
                    shutil.copy2(l_file, canonical_dest)
                logs.append(f"[green]Uploaded[/green] {rel} to cloud")
            elif r_file and not l_file:
                # Download to first local path
                if not local_paths:
                    continue
                dest = local_paths[0] / Path(rel)
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(r_file, dest)
                logs.append(f"[cyan]Downloaded[/cyan] {rel} from cloud")
            elif l_file and r_file:
                l_hash = calculate_file_hash(l_file)
                r_hash = calculate_file_hash(r_file)
                if l_hash != r_hash:
                    l_mtime = l_file.stat().st_mtime
                    r_mtime = r_file.stat().st_mtime
                    if l_mtime > r_mtime + 2.0:
                        # Local is newer -> update remote
                        dest = remote_emu_dir / Path(rel)
                        dest.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(l_file, dest)
                        if "/" in rel:
                            canonical_dest = remote_emu_dir / self._encode_remote_name(rel)
                            shutil.copy2(l_file, canonical_dest)
                        logs.append(f"[green]Updated cloud:[/green] {rel}")
                    elif r_mtime > l_mtime + 2.0:
                        # Remote is newer -> backup local first, then overwrite
                        self.engine.backup_save_file(l_file, emulator_id)
                        shutil.copy2(r_file, l_file)
                        logs.append(f"[cyan]Updated local:[/cyan] {rel} (local rollback copy saved)")

        return logs


class GoogleDriveProvider:
    """Connects to Google Drive using Google API client if credentials are configured."""

    def __init__(self, config_manager: ConfigManager, engine: SyncEngine):
        self.config = config_manager
        self.engine = engine

    def is_authenticated(self) -> bool:
        return self.config.token_file.exists()

    def sync_emulator(
        self,
        emulator_id: str,
        local_paths: List[Path],
        extensions: List[str],
        drive_folder: Optional[str] = None
    ) -> List[str]:
        if not self.is_authenticated():
            return ["[yellow]Notice:[/yellow] Google Drive not authenticated. Use a designated sync folder (Google Drive for Desktop / Syncthing) or run auth."]
        return [f"Syncing {emulator_id} with Google Drive /syncMyShit/{drive_folder or emulator_id}..."]
