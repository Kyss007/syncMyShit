"""Newest-wins save sync with local backups and activity logging."""

from __future__ import annotations

import hashlib
import logging
import os
import shutil
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

from drive import DriveClient
from saves import Emulator, get_emulator, scan_emulators
from store import Store

logger = logging.getLogger("syncMyShit")


def _file_md5(path: Path) -> str:
    h = hashlib.md5()
    with open(path, "rb") as f:
        while True:
            chunk = f.read(65536)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()


def _parse_drive_time(s: Optional[str]) -> float:
    if not s:
        return 0.0
    try:
        # 2024-01-01T12:00:00.000Z
        s = s.replace("Z", "+00:00")
        return datetime.fromisoformat(s).timestamp()
    except Exception:
        return 0.0


class SyncService:
    def __init__(self, store: Store, drive: DriveClient) -> None:
        self.store = store
        self.drive = drive
        self.keep_backups = 5

    def _backup(self, path: Path, emu_id: str) -> None:
        if not path.exists():
            return
        backup_dir = path.parent / ".syncmyshit_backups" / emu_id
        backup_dir.mkdir(parents=True, exist_ok=True)
        stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        dest = backup_dir / f"{path.stem}_{stamp}{path.suffix}"
        try:
            shutil.copy2(path, dest)
            backups = sorted(
                [f for f in backup_dir.glob(f"{path.stem}_*") if f.is_file()],
                key=lambda f: f.stat().st_mtime,
                reverse=True,
            )
            for old in backups[self.keep_backups :]:
                try:
                    old.unlink()
                except Exception:
                    pass
        except Exception as e:
            logger.warning("backup failed: %s", e)

    def _log(self, status: str, message: str, file_count: int = 0, kind: str = "sync") -> None:
        self.store.append_activity(
            {
                "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M"),
                "status": status,
                "message": message,
                "file_count": file_count,
                "type": kind,
            }
        )

    def _iter_local_saves(self, emu: Emulator) -> List[Path]:
        files: List[Path] = []
        ext = {e.lower() for e in emu.extensions}
        for folder in emu.existing_paths():
            try:
                for root, dirs, names in os.walk(folder):
                    dirs[:] = [d for d in dirs if not d.startswith(".") and d != ".syncmyshit_backups"]
                    for name in names:
                        p = Path(root) / name
                        if not ext or p.suffix.lower() in ext:
                            files.append(p)
            except Exception:
                pass
        return files

    def sync_emulator(self, emu_id: str) -> Dict[str, Any]:
        emu = get_emulator(emu_id)
        if not emu:
            return {"success": False, "error": f"Unknown emulator: {emu_id}", "uploaded": 0, "downloaded": 0}

        paths = emu.existing_paths()
        if not paths:
            return {"success": False, "error": f"No local save folder for {emu.name}", "uploaded": 0, "downloaded": 0}

        primary = paths[0]
        uploaded = 0
        downloaded = 0
        conflicts = 0

        try:
            folder_id = self.drive.get_or_create_subfolder(emu.drive_folder)
            remote_files = {f["name"]: f for f in self.drive.list_files(folder_id)}
            local_files = self._iter_local_saves(emu)
            local_by_name = {p.name: p for p in local_files}

            # Upload / download by name (flat layout matching Android)
            all_names = set(local_by_name) | set(remote_files)

            for name in all_names:
                local = local_by_name.get(name)
                remote = remote_files.get(name)

                if local and not remote:
                    self.drive.upload_file(local, folder_id, name)
                    uploaded += 1
                    continue

                if remote and not local:
                    dest = primary / name
                    self.drive.download_file(remote["id"], dest)
                    downloaded += 1
                    continue

                if local and remote:
                    local_mtime = local.stat().st_mtime
                    remote_mtime = _parse_drive_time(remote.get("modifiedTime"))
                    local_md5 = _file_md5(local)
                    remote_md5 = (remote.get("md5Checksum") or "").lower()

                    if remote_md5 and local_md5 == remote_md5:
                        continue

                    if local_mtime > remote_mtime + 2:
                        self.drive.upload_file(local, folder_id, name)
                        uploaded += 1
                    elif remote_mtime > local_mtime + 2:
                        self._backup(local, emu.id)
                        self.drive.download_file(remote["id"], local)
                        downloaded += 1
                        conflicts += 1
                    else:
                        # Timestamps close — prefer remote if md5 differs
                        if remote_md5 and local_md5 != remote_md5:
                            self._backup(local, emu.id)
                            self.drive.download_file(remote["id"], local)
                            downloaded += 1
                            conflicts += 1

            msg = f"{emu.name}: ↑{uploaded} ↓{downloaded}"
            self._log("success", msg, uploaded + downloaded, "sync")
            self.store.set("last_sync_timestamp", int(time.time()))
            return {
                "success": True,
                "uploaded": uploaded,
                "downloaded": downloaded,
                "conflicts_resolved": conflicts,
                "emulator": emu.name,
            }
        except Exception as e:
            logger.error("sync %s failed: %s", emu_id, e, exc_info=True)
            self._log("error", f"{emu.name}: {e}", 0, "sync")
            return {"success": False, "error": str(e), "uploaded": uploaded, "downloaded": downloaded}

    def sync_all(self) -> Dict[str, Any]:
        items = scan_emulators()
        if not items:
            self._log("error", "No emulator saves found on this Deck", 0, "sync")
            return {"success": False, "error": "No emulator saves found", "uploaded": 0, "downloaded": 0}

        total_up = 0
        total_down = 0
        errors: List[str] = []
        for item in items:
            res = self.sync_emulator(item.id)
            total_up += int(res.get("uploaded") or 0)
            total_down += int(res.get("downloaded") or 0)
            if not res.get("success"):
                errors.append(res.get("error") or item.id)

        ok = len(errors) == 0
        if ok:
            self._log("success", f"All emulators: ↑{total_up} ↓{total_down}", total_up + total_down, "sync")
        else:
            self._log("error", f"Partial sync — {len(errors)} failed", total_up + total_down, "sync")

        return {
            "success": ok or (total_up + total_down) > 0,
            "uploaded": total_up,
            "downloaded": total_down,
            "error": "; ".join(errors) if errors and not ok else None,
        }
