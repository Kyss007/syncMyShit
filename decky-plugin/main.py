"""
syncMyShit Decky Plugin v2 — thin RPC facade.
Dedicated to the Public Domain (The Unlicense)
"""

from __future__ import annotations

import logging
import os
import sys
import time
from typing import Any, Dict, Optional

# Ensure py_modules is importable when loaded by plugin_loader
_PLUGIN_DIR = os.path.dirname(os.path.realpath(__file__))
_PY = os.path.join(_PLUGIN_DIR, "py_modules")
if _PY not in sys.path:
    sys.path.insert(0, _PY)

from auth import AuthManager  # noqa: E402
from drive import DriveClient  # noqa: E402
from saves import scan_emulators  # noqa: E402
from store import Store  # noqa: E402
from sync import SyncService  # noqa: E402
from watcher import ProcessWatcher  # noqa: E402

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("syncMyShit")


class Plugin:
    async def _main(self) -> None:
        self.store = Store()
        self.auth = AuthManager(self.store)
        self.drive = DriveClient(self.auth)
        self.sync = SyncService(self.store, self.drive)
        self.watcher: Optional[ProcessWatcher] = None

        if self.store.get("auto_sync_on_exit", True) and self.auth.is_authenticated():
            self._start_watcher()
        logger.info("syncMyShit v2 ready")

    async def _unload(self) -> None:
        self._stop_watcher()
        self.auth.cancel()

    def _start_watcher(self) -> None:
        if self.watcher and self.watcher.is_running:
            return

        def on_exit(emu_id: str) -> None:
            if not self.auth.is_authenticated():
                return
            logger.info("Auto-sync after exit: %s", emu_id)
            self.sync.sync_emulator(emu_id)

        self.watcher = ProcessWatcher(on_exit)
        self.watcher.start()

    def _stop_watcher(self) -> None:
        if self.watcher:
            self.watcher.stop()
            self.watcher = None

    # ── RPC ──────────────────────────────────────────────────────────

    async def get_status(self) -> Dict[str, Any]:
        try:
            return {
                "success": True,
                "is_authenticated": self.auth.is_authenticated(),
                "is_waiting": self.auth.is_waiting(),
                "email": self.auth.get_email(),
                "lan_url": self.auth.get_lan_url() if self.auth.is_waiting() else "",
                "auth_url": self.auth.get_auth_url() if self.auth.is_waiting() else "",
                "auto_sync": bool(self.store.get("auto_sync_on_exit", True)),
                "is_monitoring": bool(self.watcher and self.watcher.is_running),
                "last_sync_timestamp": int(self.store.get("last_sync_timestamp") or 0),
                "version": "2.0.0",
            }
        except Exception as e:
            logger.error("get_status: %s", e)
            return {"success": False, "error": str(e), "is_authenticated": False, "email": ""}

    async def start_login(self) -> Dict[str, Any]:
        try:
            urls = self.auth.start_login()
            return {
                "success": True,
                "lan_url": urls["lan_url"],
                "auth_url": urls["auth_url"],
            }
        except Exception as e:
            logger.error("start_login: %s", e, exc_info=True)
            return {"success": False, "error": str(e)}

    async def cancel_login(self) -> Dict[str, Any]:
        self.auth.cancel()
        return {"success": True}

    async def submit_code(self, code_or_url: str = "") -> Dict[str, Any]:
        try:
            tokens = self.auth.exchange_code(code_or_url)
            if self.store.get("auto_sync_on_exit", True):
                self._start_watcher()
            return {"success": True, "email": tokens.get("email", "")}
        except Exception as e:
            logger.error("submit_code: %s", e, exc_info=True)
            return {"success": False, "error": str(e)}

    async def sign_out(self) -> Dict[str, Any]:
        self._stop_watcher()
        self.auth.sign_out()
        return {"success": True}

    async def scan(self) -> Dict[str, Any]:
        try:
            items = scan_emulators()
            return {
                "success": True,
                "emulators": [
                    {
                        "id": i.id,
                        "name": i.name,
                        "category": i.category,
                        "save_path": i.save_path,
                        "exists": i.exists,
                        "save_count": i.save_count,
                        "drive_folder": i.drive_folder,
                    }
                    for i in items
                ],
                "total_saves": sum(i.save_count for i in items),
            }
        except Exception as e:
            logger.error("scan: %s", e, exc_info=True)
            return {"success": False, "emulators": [], "total_saves": 0, "error": str(e)}

    async def run_sync(self, emulator_id: str = "") -> Dict[str, Any]:
        if not self.auth.is_authenticated():
            return {"success": False, "error": "Not signed in", "uploaded": 0, "downloaded": 0}
        try:
            if emulator_id:
                return self.sync.sync_emulator(emulator_id)
            return self.sync.sync_all()
        except Exception as e:
            logger.error("run_sync: %s", e, exc_info=True)
            return {"success": False, "error": str(e), "uploaded": 0, "downloaded": 0}

    async def toggle_auto_sync(self, enabled: bool = True) -> Dict[str, Any]:
        self.store.set("auto_sync_on_exit", bool(enabled))
        if enabled and self.auth.is_authenticated():
            self._start_watcher()
        else:
            self._stop_watcher()
        return {
            "success": True,
            "auto_sync": bool(enabled),
            "is_monitoring": bool(self.watcher and self.watcher.is_running),
        }

    async def get_activity(self) -> Dict[str, Any]:
        logs = self.store.load_activity()
        logs = list(reversed(logs[-20:]))
        return {"success": True, "logs": logs}

    async def clear_activity(self) -> Dict[str, Any]:
        self.store.save_activity([])
        return {"success": True}
