"""Persistent settings and OAuth token storage for the Decky plugin."""

from __future__ import annotations

import json
import logging
import os
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger("syncMyShit")

DEFAULTS: Dict[str, Any] = {
    "auto_sync_on_exit": True,
    "google_drive_email": "",
}


def _plugin_data_dir() -> Path:
    env = os.environ.get("DECKY_PLUGIN_SETTINGS_DIR")
    if env:
        p = Path(env)
        p.mkdir(parents=True, exist_ok=True)
        return p
    home = Path.home()
    for c in (
        home / "homebrew" / "settings" / "syncMyShit",
        home / ".config" / "syncMyShit",
    ):
        try:
            c.mkdir(parents=True, exist_ok=True)
            return c
        except Exception:
            continue
    p = home / ".syncmyshit"
    p.mkdir(parents=True, exist_ok=True)
    return p


def _token_candidates(primary: Path) -> List[Path]:
    """Desktop login writes ~/.config; Decky may use homebrew/settings — check both."""
    home = Path.home()
    seen = set()
    out: List[Path] = []
    for p in (
        primary,
        home / ".config" / "syncMyShit" / "drive_token.json",
        home / "homebrew" / "settings" / "syncMyShit" / "drive_token.json",
    ):
        key = str(p)
        if key not in seen:
            seen.add(key)
            out.append(p)
    return out


class Store:
    def __init__(self) -> None:
        self.root = _plugin_data_dir()
        self.config_path = self.root / "config.json"
        self.token_path = self.root / "drive_token.json"
        self.activity_path = self.root / "activity.json"
        self._config: Dict[str, Any] = dict(DEFAULTS)
        self._load_config()

    def _load_config(self) -> None:
        if self.config_path.exists():
            try:
                data = json.loads(self.config_path.read_text(encoding="utf-8"))
                if isinstance(data, dict):
                    self._config = {**DEFAULTS, **data}
            except Exception as e:
                logger.warning("Failed to load config: %s", e)

    def save_config(self) -> None:
        try:
            self.config_path.write_text(json.dumps(self._config, indent=2), encoding="utf-8")
        except Exception as e:
            logger.warning("Failed to save config: %s", e)

    def get(self, key: str, default: Any = None) -> Any:
        return self._config.get(key, default if default is not None else DEFAULTS.get(key))

    def set(self, key: str, value: Any) -> None:
        self._config[key] = value
        self.save_config()

    def load_tokens(self) -> Optional[Dict[str, Any]]:
        for path in _token_candidates(self.token_path):
            if not path.exists():
                continue
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
                if isinstance(data, dict) and (data.get("access_token") or data.get("refresh_token")):
                    return data
            except Exception:
                continue
        return None

    def save_tokens(self, tokens: Dict[str, Any]) -> None:
        raw = json.dumps(tokens, indent=2)
        for path in _token_candidates(self.token_path):
            try:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(raw, encoding="utf-8")
                os.chmod(path, 0o600)
            except Exception as e:
                logger.warning("Could not save tokens to %s: %s", path, e)

    def clear_tokens(self) -> None:
        for path in _token_candidates(self.token_path):
            try:
                if path.exists():
                    path.unlink()
            except Exception:
                pass
        self.set("google_drive_email", "")

    def load_activity(self) -> list:
        if not self.activity_path.exists():
            return []
        try:
            data = json.loads(self.activity_path.read_text(encoding="utf-8"))
            return data if isinstance(data, list) else []
        except Exception:
            return []

    def save_activity(self, entries: list) -> None:
        self.activity_path.write_text(json.dumps(entries[-40:], indent=2), encoding="utf-8")

    def append_activity(self, entry: Dict[str, Any]) -> None:
        entries = self.load_activity()
        entries.append(entry)
        self.save_activity(entries)
