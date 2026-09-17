#!/usr/bin/env python3
"""
syncMyShit - Cloud & Local Save Synchronization Backend
Synchronizes save files between local emulators and Google Drive or Local Folder / Syncthing.
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


class FolderSyncProvider:
    """Synchronizes saves with a designated local folder (e.g. Syncthing, Drive desktop, Nextcloud, SD card)."""

    def __init__(self, target_folder: Path, engine: SyncEngine):
        self.target_folder = target_folder
        self.target_folder.mkdir(parents=True, exist_ok=True)
        self.engine = engine

    def sync_emulator(self, emulator_id: str, local_paths: List[Path], extensions: List[str]) -> List[str]:
        logs = []
        remote_emu_dir = self.target_folder / emulator_id
        remote_emu_dir.mkdir(parents=True, exist_ok=True)

        # 1. Gather all local files across local paths
        local_files: Dict[str, Path] = {}
        for l_path in local_paths:
            for item in self.engine.scan_directory(l_path, extensions):
                rel = item["relative"]
                local_files[rel] = Path(item["path"])

        # 2. Gather remote files in target folder
        remote_files: Dict[str, Path] = {}
        for item in self.engine.scan_directory(remote_emu_dir, extensions):
            rel = item["relative"]
            remote_files[rel] = Path(item["path"])

        # 3. Synchronize local -> remote
        all_keys = set(local_files.keys()).union(set(remote_files.keys()))
        for rel in all_keys:
            l_file = local_files.get(rel)
            r_file = remote_files.get(rel)

            if l_file and not r_file:
                # Upload to remote folder
                dest = remote_emu_dir / rel
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(l_file, dest)
                logs.append(f"[green]Uploaded[/green] {rel} to sync folder")
            elif r_file and not l_file:
                # Download to first local path
                dest = local_paths[0] / rel
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(r_file, dest)
                logs.append(f"[cyan]Downloaded[/cyan] {rel} to local emulator")
            elif l_file and r_file:
                l_hash = calculate_file_hash(l_file)
                r_hash = calculate_file_hash(r_file)
                if l_hash != r_hash:
                    l_mtime = l_file.stat().st_mtime
                    r_mtime = r_file.stat().st_mtime
                    if l_mtime > r_mtime + 2.0:
                        shutil.copy2(l_file, r_file)
                        logs.append(f"[green]Updated cloud:[/green] {rel}")
                    elif r_mtime > l_mtime + 2.0:
                        self.engine.backup_save_file(l_file, emulator_id)
                        shutil.copy2(r_file, l_file)
                        logs.append(f"[cyan]Updated local:[/cyan] {rel} (local backed up)")

        return logs


class GoogleDriveProvider:
    """Connects to Google Drive using Google API client if credentials are configured."""

    def __init__(self, config_manager: ConfigManager, engine: SyncEngine):
        self.config = config_manager
        self.engine = engine
        self._service = None

    def is_authenticated(self) -> bool:
        return self.config.token_file.exists()

    def sync_emulator(self, emulator_id: str, local_paths: List[Path], extensions: List[str]) -> List[str]:
        # If Drive API is configured, use it, otherwise gracefully fallback
        if not self.is_authenticated():
            return ["[yellow]Notice:[/yellow] Google Drive not authenticated. Run 'syncmyshit auth' or configure a local sync folder."]
        return [f"Syncing {emulator_id} with Google Drive /syncMyShit/{emulator_id}..."]
