#!/usr/bin/env python3
"""
syncMyShit - Desktop Process Monitor
Detects emulator process launches (Pre-Play sync) and exits (Post-Play sync).
Dedicated to the Public Domain (The Unlicense)
"""

import time
from typing import Callable, Dict, List, Set

try:
    import psutil
    HAS_PSUTIL = True
except ImportError:
    HAS_PSUTIL = False


class ProcessMonitor:
    def __init__(
        self,
        emulator_targets: Dict[str, List[str]],  # emulator_id -> list of process binary names
        on_emulator_launched: Callable[[str, str], None],
        on_emulator_exited: Callable[[str, str], None],
        poll_interval: float = 3.0,
    ):
        self.emulator_targets = emulator_targets
        self.on_emulator_launched = on_emulator_launched
        self.on_emulator_exited = on_emulator_exited
        self.poll_interval = poll_interval
        self._running = False
        self._active_processes: Dict[int, str] = {}  # pid -> emulator_id

    def get_current_running(self) -> Dict[int, str]:
        """Returns map of active PIDs matching known emulators."""
        if not HAS_PSUTIL:
            return {}

        active = {}
        target_lookup = {}
        for emu_id, names in self.emulator_targets.items():
            for name in names:
                target_lookup[name.lower()] = emu_id

        for proc in psutil.process_iter(["pid", "name"]):
            try:
                name = (proc.info["name"] or "").lower()
                if name in target_lookup:
                    active[proc.info["pid"]] = target_lookup[name]
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass
        return active

    def tick(self):
        """Single polling tick to detect state changes."""
        current = self.get_current_running()

        # Newly launched
        for pid, emu_id in current.items():
            if pid not in self._active_processes:
                self._active_processes[pid] = emu_id
                try:
                    self.on_emulator_launched(emu_id, str(pid))
                except Exception:
                    pass

        # Exited
        exited_pids = [pid for pid in self._active_processes if pid not in current]
        for pid in exited_pids:
            emu_id = self._active_processes.pop(pid)
            try:
                self.on_emulator_exited(emu_id, str(pid))
            except Exception:
                pass

    def run_forever(self):
        """Continuously monitors processes."""
        self._running = True
        self._active_processes = self.get_current_running()
        while self._running:
            self.tick()
            time.sleep(self.poll_interval)

    def stop(self):
        self._running = False
