"""Google OAuth token helpers for Decky — no local HTTP server.

Login happens in Desktop Mode via desktop_login.py / login-desktop.sh.
"""

from __future__ import annotations

import json
import logging
import time
import urllib.parse
import urllib.request
from typing import Any, Dict, Optional

from store import Store

logger = logging.getLogger("syncMyShit")

DEFAULT_CLIENT_ID = "590448604558-6q54r4h31so9md160o2dlrpskna4sh7f.apps.googleusercontent.com"
TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"


class AuthManager:
    def __init__(self, store: Store) -> None:
        self.store = store

    @property
    def client_id(self) -> str:
        custom = (self.store.get("custom_oauth_client_id") or "").strip()
        return custom or DEFAULT_CLIENT_ID

    def is_authenticated(self) -> bool:
        tokens = self.store.load_tokens()
        return bool(tokens and (tokens.get("access_token") or tokens.get("refresh_token")))

    def get_email(self) -> str:
        tokens = self.store.load_tokens() or {}
        return tokens.get("email") or self.store.get("google_drive_email") or ""

    def sign_out(self) -> None:
        self.store.clear_tokens()

    def get_access_token(self) -> Optional[str]:
        tokens = self.store.load_tokens()
        if not tokens:
            return None
        access = tokens.get("access_token")
        expires_at = float(tokens.get("expires_at") or 0)
        if access and time.time() < expires_at - 60:
            return access
        refresh = tokens.get("refresh_token")
        if not refresh:
            return access
        try:
            data = {
                "client_id": self.client_id,
                "refresh_token": refresh,
                "grant_type": "refresh_token",
            }
            body = urllib.parse.urlencode(data).encode("utf-8")
            req = urllib.request.Request(
                TOKEN_ENDPOINT,
                data=body,
                headers={
                    "Content-Type": "application/x-www-form-urlencoded",
                    "User-Agent": "syncMyShit-Decky/2.1",
                },
            )
            with urllib.request.urlopen(req, timeout=15) as resp:
                token_data = json.loads(resp.read().decode("utf-8"))
            access = token_data["access_token"]
            tokens["access_token"] = access
            tokens["expires_at"] = time.time() + int(token_data.get("expires_in", 3600))
            self.store.save_tokens(tokens)
            return access
        except Exception as e:
            logger.error("Token refresh failed: %s", e)
            return access
