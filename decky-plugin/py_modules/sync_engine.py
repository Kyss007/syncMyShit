#!/usr/bin/env python3
"""
syncMyShit - Desktop Save Sync Engine
Handles file hashing, zero save-loss backups, and conflict-safe save synchronization.
Dedicated to the Public Domain (The Unlicense)
"""

import hashlib
import os
import shutil
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple


def calculate_file_hash(path: Path) -> str:
    """Computes SHA-256 hash of a file."""
    sha = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()


class SyncEngine:
    def __init__(self, keep_backups: int = 5):
        self.keep_backups = keep_backups

    def backup_save_file(self, file_path: Path, emulator_id: str) -> Optional[Path]:
        """Creates a timestamped backup before any overwrite."""
        if not file_path.exists():
            return None

        backup_dir = file_path.parent / ".syncmyshit_backups" / emulator_id
        backup_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_name = f"{file_path.stem}_{timestamp}{file_path.suffix}"
        backup_path = backup_dir / backup_name

        try:
            shutil.copy2(file_path, backup_path)
            self._prune_backups(backup_dir, file_path.stem)
            return backup_path
        except Exception:
            return None

    def _prune_backups(self, backup_dir: Path, stem: str):
        """Keeps only the latest N backups for a save file."""
        try:
            backups = sorted(
                [f for f in backup_dir.glob(f"{stem}_*") if f.is_file()],
                key=lambda f: f.stat().st_mtime,
                reverse=True
            )
            for old in backups[self.keep_backups:]:
                try:
                    old.unlink()
                except Exception:
                    pass
        except Exception:
            pass

    def scan_directory(self, folder: Path, extensions: List[str]) -> List[Dict[str, object]]:
        """Scans a directory recursively for matching save files."""
        results = []
        if not folder.exists() or not folder.is_dir():
            return results

        ext_set = set(e.lower() for e in extensions)

        for root, dirs, files in os.walk(folder):
            # Ignore backup and cache directories
            dirs[:] = [d for d in dirs if not d.startswith(".") and d != ".syncmyshit_backups"]
            for file in files:
                p = Path(root) / file
                if p.suffix.lower() in ext_set or not extensions:
                    try:
                        stat = p.stat()
                        results.append({
                            "path": str(p),
                            "name": p.name,
                            "relative": str(p.relative_to(folder)),
                            "size": stat.st_size,
                            "mtime": stat.st_mtime,
                            "hash": calculate_file_hash(p),
                        })
                    except Exception:
                        pass
        return results

    def sync_file_pair(
        self,
        local_path: Path,
        remote_data: bytes,
        remote_mtime: float,
        remote_hash: str,
        emulator_id: str
    ) -> Tuple[str, str]:
        """
        Determines conflict-safe action:
        - 'upload': local is newer
        - 'download': remote is newer
        - 'up_to_date': identical content
        """
        if not local_path.exists():
            # Local missing, download
            local_path.parent.mkdir(parents=True, exist_ok=True)
            with open(local_path, "wb") as f:
                f.write(remote_data)
            os.utime(local_path, (remote_mtime, remote_mtime))
            return ("downloaded", f"Restored {local_path.name} from cloud")

        local_hash = calculate_file_hash(local_path)
        if local_hash == remote_hash:
            return ("up_to_date", "Identical hash, no sync needed")

        local_mtime = local_path.stat().st_mtime
        if remote_mtime > local_mtime + 2.0:
            # Cloud has newer version: safety backup local first
            self.backup_save_file(local_path, emulator_id)
            with open(local_path, "wb") as f:
                f.write(remote_data)
            os.utime(local_path, (remote_mtime, remote_mtime))
            return ("downloaded", f"Updated {local_path.name} from cloud (local backed up)")
        elif local_mtime > remote_mtime + 2.0:
            return ("needs_upload", f"Local {local_path.name} is newer, upload needed")
        else:
            # Timestamp tie but hash differs: safety backup and upload local
            self.backup_save_file(local_path, emulator_id)
            return ("needs_upload", f"Content changed, local backup created")
