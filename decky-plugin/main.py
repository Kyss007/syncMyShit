"""
syncMyShit Decky Plugin — sync + status only.
Google login is Desktop Mode via login-desktop.sh / desktop_login.py.
"""

from __future__ import annotations

import logging
import os
import sys
from typing import Any, Dict

_PLUGIN_DIR = os.path.dirname(os.path.realpath(__file__))
_PY = os.path.join(_PLUGIN_DIR, "py_modules")
if _PY not in sys.path:
    sys.path.insert(0, _PY)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("syncMyShit")

PLUGIN_VERSION = "2.1.1"


class Plugin:
    def __init__(self) -> None:
        self._ready = False
        self.store = None  # type: ignore
        self.auth = None  # type: ignore
        self.drive = None  # type: ignore
        self.sync = None  # type: ignore
        self.watcher = None  # type: ignore

    def _ensure(self) -> None:
        if self._ready:
            return
        from auth import AuthManager
        from drive import DriveClient
        from store import Store
        from sync import SyncService

        self.store = Store()
        self.auth = AuthManager(self.store)
        self.drive = DriveClient(self.auth)
        self.sync = SyncService(self.store, self.drive)
        self.watcher = None
        self._ready = True
        logger.info("syncMyShit %s backend ready", PLUGIN_VERSION)

    async def _main(self) -> None:
        try:
            self._ensure()
            if self.store.get("auto_sync_on_exit", True) and self.auth.is_authenticated():
                self._start_watcher()
            logger.info("syncMyShit %s loaded", PLUGIN_VERSION)
        except Exception as e:
            logger.error("Plugin _main failed: %s", e, exc_info=True)

    async def _unload(self) -> None:
        self._stop_watcher()

    def _start_watcher(self) -> None:
        self._ensure()
        if self.watcher and self.watcher.is_running:
            return
        from watcher import ProcessWatcher

        def on_exit(emu_id: str) -> None:
            if not self.auth.is_authenticated():
                return
            self.sync.sync_emulator(emu_id)

        self.watcher = ProcessWatcher(on_exit)
        self.watcher.start()

    def _stop_watcher(self) -> None:
        if self.watcher:
            try:
                self.watcher.stop()
            except Exception:
                pass
            self.watcher = None

    async def get_status(self) -> Dict[str, Any]:
        try:
            self._ensure()
            return {
                "success": True,
                "is_authenticated": self.auth.is_authenticated(),
                "email": self.auth.get_email(),
                "auto_sync": bool(self.store.get("auto_sync_on_exit", True)),
                "is_monitoring": bool(self.watcher and self.watcher.is_running),
                "last_sync_timestamp": int(self.store.get("last_sync_timestamp") or 0),
                "version": PLUGIN_VERSION,
                "login_hint": "~/homebrew/plugins/syncMyShit/login-desktop.sh",
            }
        except Exception as e:
            logger.error("get_status: %s", e, exc_info=True)
            return {
                "success": False,
                "error": str(e),
                "is_authenticated": False,
                "email": "",
                "version": PLUGIN_VERSION,
            }

    async def sign_out(self) -> Dict[str, Any]:
        try:
            self._ensure()
            self._stop_watcher()
            self.auth.sign_out()
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    async def scan(self) -> Dict[str, Any]:
        try:
            self._ensure()
            from saves import scan_emulators

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
        try:
            self._ensure()
            if not self.auth.is_authenticated():
                return {
                    "success": False,
                    "error": "Not signed in — run login-desktop.sh in Desktop Mode",
                    "uploaded": 0,
                    "downloaded": 0,
                }
            if emulator_id:
                return self.sync.sync_emulator(emulator_id)
            return self.sync.sync_all()
        except Exception as e:
            logger.error("run_sync: %s", e, exc_info=True)
            return {"success": False, "error": str(e), "uploaded": 0, "downloaded": 0}

    async def toggle_auto_sync(self, enabled: bool = True) -> Dict[str, Any]:
        try:
            self._ensure()
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
        except Exception as e:
            return {"success": False, "error": str(e), "auto_sync": False, "is_monitoring": False}

    async def get_activity(self) -> Dict[str, Any]:
        try:
            self._ensure()
            logs = list(reversed(self.store.load_activity()[-20:]))
            return {"success": True, "logs": logs}
        except Exception as e:
            return {"success": False, "logs": [], "error": str(e)}

    async def clear_activity(self) -> Dict[str, Any]:
        try:
            self._ensure()
            self.store.save_activity([])
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}
