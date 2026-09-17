#!/usr/bin/env python3
"""
syncMyShit - Decky Loader Plugin Backend
Automagic retro emulator cloud save sync for Steam Deck.
Dedicated to the Public Domain (The Unlicense)
"""

import asyncio
import logging
import os
import re
import sys
import threading
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

# Setup sys.path so bundled py_modules are available
PLUGIN_DIR = Path(__file__).parent.resolve()
PY_MODULES = PLUGIN_DIR / "py_modules"
if str(PY_MODULES) not in sys.path:
    sys.path.insert(0, str(PY_MODULES))
if str(PLUGIN_DIR) not in sys.path:
    sys.path.insert(0, str(PLUGIN_DIR))

# Try importing decky
try:
    import decky
except ImportError:
    try:
        import decky_plugin as decky
    except ImportError:
        decky = None

logger = logging.getLogger("syncMyShit")
if decky and hasattr(decky, "logger"):
    logger = decky.logger

from config import ConfigManager
from drive_sync import FolderSyncProvider, detect_default_cloud_folder
from emulator_registry import detect_installed_emulators, build_emulator_database
from process_monitor import ProcessMonitor
from sync_engine import SyncEngine


def strip_rich_tags(text: str) -> str:
    """Removes rich/BBCode formatting tags from strings for clean frontend display."""
    return re.sub(r"\[/?[a-zA-Z0-9_=#]+\]", "", text)


class Plugin:
    def __init__(self):
        self.config = ConfigManager()
        self.engine = SyncEngine(keep_backups=self.config.get("keep_backups_count", 5))
        self.recent_logs: List[Dict[str, Any]] = []
        self._monitor: Optional[ProcessMonitor] = None
        self._monitor_thread: Optional[threading.Thread] = None
        self.loop: Optional[asyncio.AbstractEventLoop] = None

    def _get_active_sync_folder(self) -> Path:
        f = self.config.get("local_sync_folder")
        if f:
            expanded = Path(os.path.expanduser(f))
            if expanded.exists():
                return expanded

        detected = detect_default_cloud_folder()
        if detected:
            self.config.set("local_sync_folder", str(detected))
            self.config.set("sync_mode", "local_folder")
            return detected

        default_dir = Path.home() / ".config" / "syncMyShit" / "cloud_sync"
        default_dir.mkdir(parents=True, exist_ok=True)
        self.config.set("local_sync_folder", str(default_dir))
        self.config.set("sync_mode", "local_folder")
        return default_dir

    def _start_monitor(self):
        if self._monitor and self._monitor._running:
            return

        db = build_emulator_database()
        targets: Dict[str, List[str]] = {}
        for emu in db:
            targets[emu.id] = emu.process_names

        def on_exit(emu_id: str, pid: str):
            logger.info(f"[syncMyShit] Emulator {emu_id} (pid {pid}) exited - starting post-game sync")
            self._execute_sync(emulator_id=emu_id, trigger="auto")

        def on_launch(emu_id: str, pid: str):
            logger.info(f"[syncMyShit] Emulator {emu_id} (pid {pid}) launched - pre-game check")

        self._monitor = ProcessMonitor(
            emulator_targets=targets,
            on_emulator_launched=on_launch,
            on_emulator_exited=on_exit,
            poll_interval=float(self.config.get("poll_interval_seconds", 3.0)),
        )

        def run_loop():
            if self._monitor:
                self._monitor.run_forever()

        self._monitor_thread = threading.Thread(target=run_loop, daemon=True)
        self._monitor_thread.start()
        logger.info("[syncMyShit] Process monitor thread started")

    def _stop_monitor(self):
        if self._monitor:
            self._monitor.stop()
            self._monitor = None
        if self._monitor_thread and self._monitor_thread.is_alive():
            self._monitor_thread = None
        logger.info("[syncMyShit] Process monitor stopped")

    def _execute_sync(self, emulator_id: Optional[str] = None, trigger: str = "manual") -> Dict[str, Any]:
        sync_folder = self._get_active_sync_folder()
        provider = FolderSyncProvider(sync_folder, self.engine)
        detected = detect_installed_emulators()
        ops_count = 0
        sync_logs: List[str] = []

        now_str = time.strftime("%H:%M:%S")

        for emu in detected:
            if emulator_id and emu["id"] != emulator_id:
                continue

            paths = [Path(p) for p in emu["paths"]]
            logs = provider.sync_emulator(
                emu["id"],
                paths,
                emu["extensions"],
                drive_folder=emu.get("drive_folder")
            )
            for raw_log in logs:
                clean = strip_rich_tags(raw_log)
                sync_logs.append(f"{emu['name']}: {clean}")
                self.recent_logs.append({
                    "time": now_str,
                    "emulator": emu["name"],
                    "message": clean,
                    "type": "upload" if "Uploaded" in raw_log or "Updated cloud" in raw_log else "download"
                })
                ops_count += 1

        # Also sync custom paths if doing full sync
        if not emulator_id:
            for cp in self.config.get("custom_paths", []):
                p = Path(cp["path"])
                if p.exists():
                    logs = provider.sync_emulator(
                        cp["name"].lower().replace(" ", "_"),
                        [p],
                        [],
                        drive_folder=cp["name"]
                    )
                    for raw_log in logs:
                        clean = strip_rich_tags(raw_log)
                        sync_logs.append(f"{cp['name']}: {clean}")
                        self.recent_logs.append({
                            "time": now_str,
                            "emulator": cp["name"],
                            "message": clean,
                            "type": "upload" if "Uploaded" in raw_log or "Updated cloud" in raw_log else "download"
                        })
                        ops_count += 1

        self.config.set("last_sync_timestamp", int(time.time()))

        # Keep max 100 recent logs
        if len(self.recent_logs) > 100:
            self.recent_logs = self.recent_logs[-100:]

        summary = f"Synced successfully ({ops_count} change{'s' if ops_count != 1 else ''})" if ops_count > 0 else "All saves are up to date"
        return {
            "success": True,
            "operations_count": ops_count,
            "logs": sync_logs,
            "message": summary,
            "timestamp": int(time.time()),
        }

    # Decky lifecycle hooks
    async def _main(self):
        self.loop = asyncio.get_event_loop()
        logger.info("[syncMyShit] Initializing syncMyShit Decky plugin...")

        # Initialize active sync folder
        folder = self._get_active_sync_folder()
        logger.info(f"[syncMyShit] Active sync folder: {folder}")

        # Start process monitor if auto-sync is enabled
        if self.config.get("auto_sync_on_process", True):
            self._start_monitor()

    async def _unload(self):
        logger.info("[syncMyShit] Unloading syncMyShit Decky plugin...")
        self._stop_monitor()

    # Decky callable RPC endpoints
    async def get_status(self) -> Dict[str, Any]:
        """Returns overall status of syncMyShit for the UI."""
        sync_folder = str(self._get_active_sync_folder())
        detected = detect_installed_emulators()
        custom_paths = self.config.get("custom_paths", [])
        is_monitoring = self._monitor is not None and self._monitor._running

        return {
            "success": True,
            "sync_folder": sync_folder,
            "sync_mode": self.config.get("sync_mode", "local_folder"),
            "auto_sync": self.config.get("auto_sync_on_process", True),
            "is_monitoring": is_monitoring,
            "last_sync_timestamp": self.config.get("last_sync_timestamp", 0),
            "detected_emulators_count": len(detected),
            "custom_paths_count": len(custom_paths),
            "keep_backups": self.config.get("keep_backups_count", 5),
        }

    async def scan_saves(self) -> Dict[str, Any]:
        """Scans all detected emulators and returns list of save files."""
        detected = detect_installed_emulators()
        results: List[Dict[str, Any]] = []
        total_files = 0

        for emu in detected:
            paths = [Path(p) for p in emu["paths"]]
            emu_saves = []
            for p in paths:
                found = self.engine.scan_directory(p, emu["extensions"])
                for f in found:
                    emu_saves.append({
                        "name": f["name"],
                        "relative": f["relative"],
                        "size": f["size"],
                        "mtime": f["mtime"],
                    })
            total_files += len(emu_saves)
            results.append({
                "id": emu["id"],
                "name": emu["name"],
                "category": emu["category"],
                "paths": emu["paths"],
                "drive_folder": emu.get("drive_folder", emu["id"]),
                "save_count": len(emu_saves),
                "saves": emu_saves[:20],  # sample preview
            })

        for cp in self.config.get("custom_paths", []):
            p = Path(cp["path"])
            cp_saves = []
            if p.exists():
                found = self.engine.scan_directory(p, [])
                for f in found:
                    cp_saves.append({
                        "name": f["name"],
                        "relative": f["relative"],
                        "size": f["size"],
                        "mtime": f["mtime"],
                    })
            total_files += len(cp_saves)
            results.append({
                "id": f"custom_{cp['name'].lower()}",
                "name": cp["name"],
                "category": "Custom Path",
                "paths": [cp["path"]],
                "drive_folder": cp["name"],
                "save_count": len(cp_saves),
                "saves": cp_saves[:20],
            })

        return {
            "success": True,
            "total_saves": total_files,
            "emulators": results,
        }

    async def run_sync(self, emulator_id: Optional[str] = None) -> Dict[str, Any]:
        """Triggers a cloud save sync."""
        logger.info(f"[syncMyShit] Running save sync (target={emulator_id or 'all'})")
        # Run blocking sync in executor to avoid blocking event loop
        loop = asyncio.get_event_loop()
        res = await loop.run_in_executor(None, self._execute_sync, emulator_id, "manual")
        return res

    async def toggle_watcher(self, enabled: bool) -> Dict[str, Any]:
        """Enables or disables background process auto-sync watcher."""
        self.config.set("auto_sync_on_process", enabled)
        if enabled:
            self._start_monitor()
        else:
            self._stop_monitor()
        return {
            "success": True,
            "auto_sync": enabled,
            "is_monitoring": self._monitor is not None and self._monitor._running,
        }

    async def set_sync_folder(self, path: str) -> Dict[str, Any]:
        """Sets the cloud sync destination directory."""
        if not path:
            return {"success": False, "error": "Path cannot be empty"}

        expanded = Path(os.path.expanduser(path)).resolve()
        try:
            expanded.mkdir(parents=True, exist_ok=True)
            self.config.set("local_sync_folder", str(expanded))
            self.config.set("sync_mode", "local_folder")
            logger.info(f"[syncMyShit] Updated sync directory to: {expanded}")
            return {"success": True, "sync_folder": str(expanded)}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def get_recent_logs(self) -> Dict[str, Any]:
        """Returns recent sync log entries."""
        return {
            "success": True,
            "logs": list(reversed(self.recent_logs[-30:])),
        }

    async def clear_logs(self) -> Dict[str, Any]:
        """Clears stored sync logs."""
        self.recent_logs.clear()
        return {"success": True}

    async def add_custom_path(self, name: str, path: str) -> Dict[str, Any]:
        """Adds a custom emulator save directory."""
        if not name or not path:
            return {"success": False, "error": "Name and path are required"}
        self.config.add_custom_path(name, path)
        return {"success": True, "custom_paths": self.config.get("custom_paths", [])}

    async def remove_custom_path(self, path: str) -> Dict[str, Any]:
        """Removes a custom emulator save directory."""
        self.config.remove_custom_path(path)
        return {"success": True, "custom_paths": self.config.get("custom_paths", [])}
