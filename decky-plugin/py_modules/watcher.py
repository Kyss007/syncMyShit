"""Watch emulator processes and trigger sync on exit."""

from __future__ import annotations

import logging
import os
import threading
import time
from typing import Callable, Dict, Optional, Set

from saves import process_to_emulator_ids

logger = logging.getLogger("syncMyShit")


def _running_process_names() -> Set[str]:
    names: Set[str] = set()
    try:
        for entry in os.listdir("/proc"):
            if not entry.isdigit():
                continue
            try:
                with open(f"/proc/{entry}/comm", "r", encoding="utf-8", errors="ignore") as f:
                    name = f.read().strip().lower()
                    if name:
                        names.add(name)
                # Also check cmdline basename
                with open(f"/proc/{entry}/cmdline", "rb") as f:
                    raw = f.read().split(b"\x00")
                    if raw and raw[0]:
                        base = os.path.basename(raw[0].decode("utf-8", errors="ignore")).lower()
                        if base:
                            names.add(base)
            except Exception:
                continue
    except Exception:
        pass
    return names


class ProcessWatcher:
    def __init__(self, on_exit: Callable[[str], None], poll_seconds: float = 4.0) -> None:
        self.on_exit = on_exit
        self.poll_seconds = poll_seconds
        self._thread: Optional[threading.Thread] = None
        self._stop = threading.Event()
        self._running = False
        self._seen: Dict[str, bool] = {}
        self._map = process_to_emulator_ids()

    @property
    def is_running(self) -> bool:
        return self._running

    def start(self) -> None:
        if self._running:
            return
        self._stop.clear()
        self._running = True
        self._thread = threading.Thread(target=self._loop, daemon=True)
        self._thread.start()
        logger.info("Process watcher started")

    def stop(self) -> None:
        self._stop.set()
        self._running = False
        self._thread = None
        logger.info("Process watcher stopped")

    def _loop(self) -> None:
        # Seed without firing
        alive = _running_process_names()
        for proc, emu_id in self._map.items():
            if proc in alive:
                self._seen[emu_id] = True

        while not self._stop.is_set():
            try:
                alive = _running_process_names()
                currently: Dict[str, bool] = {}
                for proc, emu_id in self._map.items():
                    if proc in alive:
                        currently[emu_id] = True

                for emu_id in list(self._seen.keys()):
                    if self._seen.get(emu_id) and not currently.get(emu_id):
                        logger.info("Emulator exited: %s — triggering sync", emu_id)
                        try:
                            self.on_exit(emu_id)
                        except Exception as e:
                            logger.error("auto-sync callback failed: %s", e)
                        del self._seen[emu_id]

                for emu_id in currently:
                    self._seen[emu_id] = True
            except Exception as e:
                logger.warning("watcher tick error: %s", e)

            self._stop.wait(self.poll_seconds)
