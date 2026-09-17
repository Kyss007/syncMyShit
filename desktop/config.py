#!/usr/bin/env python3
"""
syncMyShit - Configuration Manager for Linux & Windows
Dedicated to the Public Domain (The Unlicense)
"""

import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List


def get_config_dir() -> Path:
    """Returns platform-standard configuration directory."""
    if sys.platform == "win32":
        base = os.environ.get("APPDATA")
        if base:
            path = Path(base) / "syncMyShit"
        else:
            path = Path.home() / ".syncMyShit"
    elif sys.platform == "darwin":
        path = Path.home() / "Library" / "Application Support" / "syncMyShit"
    else:
        # Linux / BSD / Steam Deck
        base = os.environ.get("XDG_CONFIG_HOME")
        if base:
            path = Path(base) / "syncMyShit"
        else:
            path = Path.home() / ".config" / "syncMyShit"

    path.mkdir(parents=True, exist_ok=True)
    return path


class ConfigManager:
    """Manages persistent settings for syncMyShit desktop."""

    DEFAULT_CONFIG = {
        "google_drive_folder": "syncMyShit",
        "sync_mode": "google_drive",  # "google_drive" or "local_folder"
        "local_sync_folder": "",
        "auto_sync_on_process": True,
        "poll_interval_seconds": 3,
        "keep_backups_count": 5,
        "custom_paths": [],
        "enabled_emulators": [],
        "last_sync_timestamp": 0,
    }

    def __init__(self):
        self.config_dir = get_config_dir()
        self.config_file = self.config_dir / "config.json"
        self.token_file = self.config_dir / "drive_token.json"
        self._data = self._load()

    def _load(self) -> Dict[str, Any]:
        if self.config_file.exists():
            try:
                with open(self.config_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    cfg = dict(self.DEFAULT_CONFIG)
                    cfg.update(data)
                    return cfg
            except Exception:
                pass
        return dict(self.DEFAULT_CONFIG)

    def save(self):
        with open(self.config_file, "w", encoding="utf-8") as f:
            json.dump(self._data, f, indent=2)

    def get(self, key: str, default: Any = None) -> Any:
        return self._data.get(key, default)

    def set(self, key: str, value: Any):
        self._data[key] = value
        self.save()

    def add_custom_path(self, name: str, path: str):
        paths: List[Dict[str, str]] = self._data.get("custom_paths", [])
        for p in paths:
            if p.get("path") == path:
                p["name"] = name
                self.save()
                return
        paths.append({"name": name, "path": path})
        self._data["custom_paths"] = paths
        self.save()

    def remove_custom_path(self, path: str):
        paths = [p for p in self._data.get("custom_paths", []) if p.get("path") != path]
        self._data["custom_paths"] = paths
        self.save()
