#!/usr/bin/env python3
"""
syncMyShit - Native Google Drive Cloud Save Sync Engine
Authenticates directly with Google Drive API v3 via OAuth 2.0 PKCE.
Synchronizes emulator saves to and from Google Drive (syncMyShit/<Emulator>/<save>).
Matches Android syncMyShit naming, folder structure, and MD5 change detection.
Dedicated to the Public Domain (The Unlicense)
"""

import base64
import hashlib
import http.server
import json
import logging
import os
import secrets
import shutil
import socket
import socketserver
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from config import ConfigManager
from sync_engine import SyncEngine, calculate_file_hash

logger = logging.getLogger("syncMyShit.drive")

# Google OAuth Constants
DEFAULT_CLIENT_ID = "590448604558-6q54r4h31so9md160o2dlrpskna4sh7f.apps.googleusercontent.com"
AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
SCOPE_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email"


class GoogleOAuthManager:
    """Handles Google OAuth 2.0 PKCE authentication flow and token refresh."""

    def __init__(self, config: ConfigManager):
        self.config = config
        self.token_file = config.config_dir / "drive_token.json"
        self._current_verifier: Optional[str] = None
        self._current_state: Optional[str] = None
        self._server: Optional[socketserver.TCPServer] = None
        self._server_thread: Optional[threading.Thread] = None

    @property
    def client_id(self) -> str:
        custom = self.config.get("custom_oauth_client_id")
        if custom and str(custom).strip():
            return str(custom).strip()
        return DEFAULT_CLIENT_ID

    @property
    def client_secret(self) -> Optional[str]:
        custom = self.config.get("custom_oauth_client_secret")
        if custom and str(custom).strip():
            return str(custom).strip()
        return None

    def _generate_pkce(self) -> Tuple[str, str]:
        """Generates (code_verifier, code_challenge) using SHA-256."""
        verifier = base64.urlsafe_b64encode(secrets.token_bytes(32)).rstrip(b"=").decode("ascii")
        digest = hashlib.sha256(verifier.encode("ascii")).digest()
        challenge = base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")
        return verifier, challenge

    def is_authenticated(self) -> bool:
        """Returns True if valid tokens are present."""
        tokens = self.load_tokens()
        return bool(tokens and (tokens.get("access_token") or tokens.get("refresh_token")))

    def load_tokens(self) -> Optional[Dict[str, Any]]:
        """Loads saved OAuth tokens from drive_token.json."""
        if not self.token_file.exists():
            return None
        try:
            with open(self.token_file, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logger.warning(f"Could not load drive tokens: {e}")
            return None

    def save_tokens(self, tokens: Dict[str, Any]):
        """Saves OAuth tokens securely to drive_token.json."""
        try:
            with open(self.token_file, "w", encoding="utf-8") as f:
                json.dump(tokens, f, indent=2)
            os.chmod(self.token_file, 0o600)
        except Exception as e:
            logger.error(f"Failed to save drive tokens: {e}")

    def sign_out(self):
        """Clears local tokens and disconnects Google Drive."""
        if self.token_file.exists():
            try:
                self.token_file.unlink()
            except Exception:
                pass
        self.config.set("google_drive_email", "")

    def get_user_email(self) -> str:
        """Returns signed-in Google account email if known."""
        tokens = self.load_tokens()
        if tokens and tokens.get("email"):
            return tokens["email"]
        return self.config.get("google_drive_email", "")

    def get_valid_access_token(self) -> Optional[str]:
        """Returns a valid access token, auto-refreshing via refresh_token if expired."""
        tokens = self.load_tokens()
        if not tokens:
            return None

        access_token = tokens.get("access_token")
        expires_at = tokens.get("expires_at", 0)
        refresh_token = tokens.get("refresh_token")

        # Buffer: refresh if expiring within 90 seconds
        if access_token and time.time() < (expires_at - 90):
            return access_token

        if not refresh_token:
            return access_token

        # Refresh the token
        logger.info("Access token expired or expiring soon, refreshing with Google OAuth...")
        try:
            data = {
                "client_id": self.client_id,
                "grant_type": "refresh_token",
                "refresh_token": refresh_token,
            }
            if self.client_secret:
                data["client_secret"] = self.client_secret

            body = urllib.parse.urlencode(data).encode("utf-8")
            req = urllib.request.Request(
                TOKEN_ENDPOINT,
                data=body,
                headers={"Content-Type": "application/x-www-form-urlencoded", "User-Agent": "syncMyShit-Decky"}
            )
            with urllib.request.urlopen(req, timeout=15) as resp:
                res_data = json.loads(resp.read().decode("utf-8"))

            new_access_token = res_data.get("access_token")
            expires_in = res_data.get("expires_in", 3600)
            tokens["access_token"] = new_access_token
            tokens["expires_at"] = time.time() + expires_in
            if "refresh_token" in res_data:
                tokens["refresh_token"] = res_data["refresh_token"]

            self.save_tokens(tokens)
            logger.info("Successfully refreshed Google Drive access token")
            return new_access_token
        except Exception as e:
            logger.error(f"Failed to refresh Google Drive access token: {e}")
            return access_token

    def start_auth_flow(self, port: int = 8085) -> str:
        """Starts a local HTTP server and returns authorization URL."""
        self._current_verifier, challenge = self._generate_pkce()
        self._current_state = secrets.token_urlsafe(16)

        # Find available port
        for try_port in [port, 8086, 8087, 8088, 0]:
            try:
                self._stop_server()
                handler = self._create_handler()
                self._server = socketserver.TCPServer(("127.0.0.1", try_port), handler)
                actual_port = self._server.server_address[1]
                break
            except OSError:
                continue

        redirect_uri = f"http://127.0.0.1:{actual_port}/"
        self._redirect_uri = redirect_uri

        params = {
            "client_id": self.client_id,
            "redirect_uri": redirect_uri,
            "response_type": "code",
            "scope": f"{SCOPE_DRIVE_FILE} {SCOPE_USER_EMAIL}",
            "code_challenge": challenge,
            "code_challenge_method": "S256",
            "state": self._current_state,
            "access_type": "offline",
            "prompt": "consent",
        }
        auth_url = f"{AUTH_ENDPOINT}?{urllib.parse.urlencode(params)}"

        self._server_thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._server_thread.start()
        logger.info(f"Started OAuth redirect listener on {redirect_uri}")

        return auth_url

    def exchange_code(self, code: str) -> Dict[str, Any]:
        """Exchanges an authorization code for access and refresh tokens."""
        if not self._current_verifier:
            # Fallback for manual code paste if verifier was lost
            tokens = self.load_tokens()
            self._current_verifier = tokens.get("code_verifier") if tokens else None

        data = {
            "client_id": self.client_id,
            "code": code,
            "grant_type": "authorization_code",
            "redirect_uri": getattr(self, "_redirect_uri", "http://127.0.0.1:8085/"),
        }
        if self._current_verifier:
            data["code_verifier"] = self._current_verifier
        if self.client_secret:
            data["client_secret"] = self.client_secret

        body = urllib.parse.urlencode(data).encode("utf-8")
        req = urllib.request.Request(
            TOKEN_ENDPOINT,
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded", "User-Agent": "syncMyShit-Decky"}
        )
        with urllib.request.urlopen(req, timeout=15) as resp:
            token_data = json.loads(resp.read().decode("utf-8"))

        access_token = token_data.get("access_token")
        refresh_token = token_data.get("refresh_token")
        expires_in = token_data.get("expires_in", 3600)

        # Fetch user's email
        email = "Google Drive User"
        try:
            req_user = urllib.request.Request(
                USERINFO_ENDPOINT,
                headers={"Authorization": f"Bearer {access_token}", "User-Agent": "syncMyShit-Decky"}
            )
            with urllib.request.urlopen(req_user, timeout=10) as resp_user:
                user_info = json.loads(resp_user.read().decode("utf-8"))
                email = user_info.get("email", email)
        except Exception as e:
            logger.warning(f"Could not fetch user email: {e}")

        saved = {
            "access_token": access_token,
            "refresh_token": refresh_token,
            "expires_at": time.time() + expires_in,
            "email": email,
            "connected_at": int(time.time()),
        }
        self.save_tokens(saved)
        self.config.set("google_drive_email", email)
        logger.info(f"Successfully signed in to Google Drive as: {email}")
        return saved

    def _stop_server(self):
        if self._server:
            try:
                self._server.shutdown()
                self._server.server_close()
            except Exception:
                pass
            self._server = None

    def _create_handler(self):
        oauth_mgr = self

        class OAuthCallbackHandler(http.server.BaseHTTPRequestHandler):
            def log_message(self, format, *args):
                pass  # suppress standard http.server stdout logging

            def do_GET(self):
                parsed = urllib.parse.urlparse(self.path)
                qs = urllib.parse.parse_qs(parsed.query)

                code = qs.get("code", [None])[0]
                state = qs.get("state", [None])[0]

                if not code:
                    self.send_response(400)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(b"<h1>Authorization code missing. Please try again.</h1>")
                    return

                try:
                    oauth_mgr.exchange_code(code)
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    html = (
                        "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                        "<title>syncMyShit - Connected</title>"
                        "<style>body{font-family:system-ui,-apple-system,sans-serif;background:#0f172a;"
                        "color:#f8fafc;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}"
                        ".card{background:#1e293b;padding:32px;border-radius:12px;text-align:center;"
                        "box-shadow:0 8px 24px rgba(0,0,0,0.4);max-width:420px;border:1px solid #334155;}"
                        "h1{color:#38bdf8;margin:0 0 12px 0;font-size:24px;}"
                        "p{color:#94a3b8;font-size:14px;line-height:1.5;margin:0;}"
                        "</style></head><body><div class='card'>"
                        "<h1>✅ Sign-in Successful!</h1>"
                        "<p>syncMyShit is now authenticated with Google Drive.<br><br>"
                        "You can close this tab and return to the Steam Deck menu.</p>"
                        "</div></body></html>"
                    )
                    self.wfile.write(html.encode("utf-8"))
                except Exception as e:
                    self.send_response(500)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(f"<h1>Sign-in error: {e}</h1>".encode("utf-8"))
                finally:
                    threading.Thread(target=oauth_mgr._stop_server, daemon=True).start()

        return OAuthCallbackHandler


class GoogleDriveClient:
    """Direct API v3 client for Google Drive."""

    def __init__(self, oauth_mgr: GoogleOAuthManager):
        self.oauth_mgr = oauth_mgr
        self._root_folder_id: Optional[str] = None
        self._folder_cache: Dict[str, str] = {}

    def _request(
        self,
        endpoint: str,
        method: str = "GET",
        params: Optional[Dict[str, Any]] = None,
        json_data: Optional[Dict[str, Any]] = None,
        binary_data: Optional[bytes] = None,
        headers: Optional[Dict[str, str]] = None,
        timeout: int = 30,
    ) -> Any:
        token = self.oauth_mgr.get_valid_access_token()
        if not token:
            raise RuntimeError("Not signed in to Google Drive. Please click 'Sign In to Google Drive'.")

        url = endpoint
        if params:
            url += "?" + urllib.parse.urlencode(params)

        req_headers = {
            "Authorization": f"Bearer {token}",
            "User-Agent": "syncMyShit-Decky",
        }
        if headers:
            req_headers.update(headers)

        body_bytes = None
        if json_data is not None:
            body_bytes = json.dumps(json_data).encode("utf-8")
            req_headers["Content-Type"] = "application/json; charset=UTF-8"
        elif binary_data is not None:
            body_bytes = binary_data

        req = urllib.request.Request(url, data=body_bytes, headers=req_headers, method=method)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            content_type = resp.headers.get("Content-Type", "")
            data = resp.read()
            if "application/json" in content_type:
                return json.loads(data.decode("utf-8"))
            return data

    def get_or_create_root_folder(self, folder_name: str = "syncMyShit") -> str:
        """Finds or creates the root 'syncMyShit' folder in Drive."""
        if self._root_folder_id:
            return self._root_folder_id

        q = f"name = '{folder_name}' and mimeType = 'application/vnd.google-apps.folder' and trashed = false and 'root' in parents"
        res = self._request(
            f"{DRIVE_API_BASE}/files",
            params={"q": q, "spaces": "drive", "fields": "files(id, name)"}
        )
        files = res.get("files", [])
        if files:
            self._root_folder_id = files[0]["id"]
            return self._root_folder_id

        # Create root folder
        create_res = self._request(
            f"{DRIVE_API_BASE}/files",
            method="POST",
            json_data={
                "name": folder_name,
                "mimeType": "application/vnd.google-apps.folder",
                "parents": ["root"],
            },
            params={"fields": "id, name"}
        )
        self._root_folder_id = create_res["id"]
        logger.info(f"Created root Google Drive folder '{folder_name}' (id: {self._root_folder_id})")
        return self._root_folder_id

    def get_or_create_subfolder(self, folder_name: str, parent_id: str) -> str:
        """Finds or creates an emulator subfolder (e.g. GBA, Switch) inside syncMyShit."""
        cache_key = f"{parent_id}:{folder_name}"
        if cache_key in self._folder_cache:
            return self._folder_cache[cache_key]

        q = f"name = '{folder_name}' and mimeType = 'application/vnd.google-apps.folder' and trashed = false and '{parent_id}' in parents"
        res = self._request(
            f"{DRIVE_API_BASE}/files",
            params={"q": q, "spaces": "drive", "fields": "files(id, name)"}
        )
        files = res.get("files", [])
        if files:
            fid = files[0]["id"]
            self._folder_cache[cache_key] = fid
            return fid

        # Create subfolder
        create_res = self._request(
            f"{DRIVE_API_BASE}/files",
            method="POST",
            json_data={
                "name": folder_name,
                "mimeType": "application/vnd.google-apps.folder",
                "parents": [parent_id],
            },
            params={"fields": "id, name"}
        )
        fid = create_res["id"]
        self._folder_cache[cache_key] = fid
        logger.info(f"Created Google Drive subfolder '{folder_name}' (id: {fid})")
        return fid

    def list_files(self, folder_id: str) -> List[Dict[str, Any]]:
        """Lists files inside a Drive folder with MD5, size, and modifiedTime."""
        q = f"'{folder_id}' in parents and trashed = false and mimeType != 'application/vnd.google-apps.folder'"
        fields = "files(id, name, size, modifiedTime, md5Checksum)"
        res = self._request(
            f"{DRIVE_API_BASE}/files",
            params={"q": q, "fields": fields, "pageSize": 1000}
        )
        return res.get("files", [])

    def upload_file(
        self,
        local_path: Path,
        parent_id: str,
        remote_name: Optional[str] = None,
        existing_file_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Uploads a save file to Drive using multipart/related upload."""
        target_name = remote_name or local_path.name
        with open(local_path, "rb") as f:
            file_bytes = f.read()

        boundary = f"===syncMyShit_{int(time.time())}_{secrets.token_hex(4)}==="

        # Prepare metadata
        meta: Dict[str, Any] = {"name": target_name}
        if not existing_file_id:
            meta["parents"] = [parent_id]

        mtime = local_path.stat().st_mtime
        meta["modifiedTime"] = datetime.fromtimestamp(mtime, tz=timezone.utc).isoformat().replace("+00:00", "Z")

        meta_part = json.dumps(meta).encode("utf-8")
        body = (
            f"--{boundary}\r\n"
            f"Content-Type: application/json; charset=UTF-8\r\n\r\n"
        ).encode("utf-8") + meta_part + (
            f"\r\n--{boundary}\r\n"
            f"Content-Type: application/octet-stream\r\n\r\n"
        ).encode("utf-8") + file_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")

        headers = {"Content-Type": f"multipart/related; boundary={boundary}"}

        if existing_file_id:
            url = f"{DRIVE_UPLOAD_BASE}/files/{existing_file_id}"
            method = "PATCH"
        else:
            url = f"{DRIVE_UPLOAD_BASE}/files"
            method = "POST"

        res = self._request(
            url,
            method=method,
            params={"uploadType": "multipart", "fields": "id, name, modifiedTime, md5Checksum, size"},
            binary_data=body,
            headers=headers,
        )
        return res

    def download_file(self, file_id: str, dest_path: Path, remote_mtime_iso: Optional[str] = None):
        """Downloads a file from Drive and sets file mtime to match remote timestamp."""
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        tmp_dest = dest_path.with_name(f"{dest_path.name}.tmp_{secrets.token_hex(3)}")

        data = self._request(f"{DRIVE_API_BASE}/files/{file_id}", params={"alt": "media"})
        with open(tmp_dest, "wb") as f:
            f.write(data)

        # Atomic rename
        shutil.move(str(tmp_dest), str(dest_path))

        # Preserve remote modified time if provided
        if remote_mtime_iso:
            try:
                dt = datetime.fromisoformat(remote_mtime_iso.replace("Z", "+00:00"))
                mtime = dt.timestamp()
                os.utime(dest_path, (mtime, mtime))
            except Exception:
                pass


class GoogleDriveSyncProvider:
    """True Google Drive Cloud Save Sync Provider."""

    def __init__(self, oauth_mgr: GoogleOAuthManager, engine: SyncEngine):
        self.oauth_mgr = oauth_mgr
        self.engine = engine
        self.client = GoogleDriveClient(oauth_mgr)

    def sync_emulator(
        self,
        emulator_id: str,
        local_paths: List[Path],
        extensions: List[str],
        drive_folder: Optional[str] = None,
    ) -> List[str]:
        logs = []
        if not self.oauth_mgr.is_authenticated():
            raise RuntimeError("Google Drive is not connected. Please tap 'Sign In to Google Drive'.")

        folder_name = drive_folder or emulator_id
        root_id = self.client.get_or_create_root_folder("syncMyShit")
        emu_folder_id = self.client.get_or_create_subfolder(folder_name, root_id)

        # 1. Gather local save files
        local_files: Dict[str, Dict[str, Any]] = {}
        for l_path in local_paths:
            for item in self.engine.scan_directory(l_path, extensions):
                rel = item["relative"].replace("\\", "/")
                # Android compatibility: replace slash with triple underscore if nested
                remote_key = rel.replace("/", "___")
                local_files[remote_key] = {
                    "path": Path(item["path"]),
                    "size": item["size"],
                    "mtime": item["mtime"],
                    "relative": rel,
                    "target_dir": l_path,
                }

        # 2. Gather remote files from Google Drive
        remote_files = self.client.list_files(emu_folder_id)
        remote_by_name: Dict[str, Dict[str, Any]] = {}
        for r in remote_files:
            remote_by_name[r["name"]] = r

        # 3. Synchronize local files to Google Drive
        for remote_name, local_item in local_files.items():
            l_path: Path = local_item["path"]
            r_file = remote_by_name.get(remote_name)

            if r_file:
                # Compare MD5 checksums first
                r_md5 = r_file.get("md5Checksum", "")
                l_md5 = calculate_file_hash(l_path)

                if r_md5 and l_md5 and r_md5.lower() == l_md5.lower():
                    # Identical file, skip transfer
                    continue

                # Parse remote modified time
                r_mtime = 0.0
                if r_file.get("modifiedTime"):
                    try:
                        dt = datetime.fromisoformat(r_file["modifiedTime"].replace("Z", "+00:00"))
                        r_mtime = dt.timestamp()
                    except Exception:
                        pass

                l_mtime = local_item["mtime"]

                # Resolve direction
                if l_mtime > (r_mtime + 1.0):
                    # Local is newer -> Upload to Drive
                    self.client.upload_file(l_path, emu_folder_id, remote_name, existing_file_id=r_file["id"])
                    logs.append(f"Uploaded updated save: {local_item['relative']}")
                elif r_mtime > (l_mtime + 1.0):
                    # Remote is newer -> Download from Drive
                    self.engine.create_backup(l_path)
                    self.client.download_file(r_file["id"], l_path, r_file.get("modifiedTime"))
                    logs.append(f"Downloaded newer save: {local_item['relative']}")
            else:
                # File does not exist on Drive -> Upload
                self.client.upload_file(l_path, emu_folder_id, remote_name)
                logs.append(f"Uploaded new save to Google Drive: {local_item['relative']}")

        # 4. Download remote saves that do not exist locally
        if local_paths:
            primary_local_dir = local_paths[0]
            for r_name, r_file in remote_by_name.items():
                if r_name not in local_files:
                    # Convert '___' back to '/'
                    local_rel = r_name.replace("___", "/")
                    dest_file = primary_local_dir / local_rel
                    self.client.download_file(r_file["id"], dest_file, r_file.get("modifiedTime"))
                    logs.append(f"Downloaded save from Google Drive: {local_rel}")

        return logs
