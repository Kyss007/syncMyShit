"""Google Drive API v3 helpers — folder layout matches Android syncMyShit."""

from __future__ import annotations

import json
import logging
import mimetypes
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional

from auth import AuthManager

logger = logging.getLogger("syncMyShit")

ROOT_FOLDER = "syncMyShit"
DRIVE_API = "https://www.googleapis.com/drive/v3"
UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"


class DriveClient:
    def __init__(self, auth: AuthManager) -> None:
        self.auth = auth
        self._root_id: Optional[str] = None
        self._folder_cache: Dict[str, str] = {}

    def _headers(self, json_body: bool = False) -> Dict[str, str]:
        token = self.auth.get_access_token()
        if not token:
            raise RuntimeError("Not authenticated")
        h = {
            "Authorization": f"Bearer {token}",
            "User-Agent": "syncMyShit-Decky/2.0",
        }
        if json_body:
            h["Content-Type"] = "application/json"
        return h

    def _request(
        self,
        url: str,
        method: str = "GET",
        data: Optional[bytes] = None,
        headers: Optional[Dict[str, str]] = None,
        timeout: int = 60,
    ) -> Any:
        req = urllib.request.Request(url, data=data, method=method, headers=headers or self._headers())
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                raw = resp.read()
                if not raw:
                    return {}
                ctype = resp.headers.get("Content-Type", "")
                if "json" in ctype or raw[:1] in (b"{", b"["):
                    return json.loads(raw.decode("utf-8"))
                return raw
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Drive API {e.code}: {body[:400]}") from e

    def get_or_create_root(self) -> str:
        if self._root_id:
            return self._root_id
        q = (
            "mimeType = 'application/vnd.google-apps.folder' "
            f"and name = '{ROOT_FOLDER}' and trashed = false and 'root' in parents"
        )
        params = urllib.parse.urlencode(
            {"q": q, "spaces": "drive", "fields": "files(id,name)", "pageSize": "1"}
        )
        result = self._request(f"{DRIVE_API}/files?{params}")
        files = result.get("files") or []
        if files:
            self._root_id = files[0]["id"]
            return self._root_id

        meta = json.dumps(
            {"name": ROOT_FOLDER, "mimeType": "application/vnd.google-apps.folder"}
        ).encode("utf-8")
        created = self._request(
            f"{DRIVE_API}/files?fields=id,name",
            method="POST",
            data=meta,
            headers=self._headers(json_body=True),
        )
        self._root_id = created["id"]
        return self._root_id

    def get_or_create_subfolder(self, name: str, parent_id: Optional[str] = None) -> str:
        parent = parent_id or self.get_or_create_root()
        cache_key = f"{parent}:{name}"
        if cache_key in self._folder_cache:
            return self._folder_cache[cache_key]

        q = (
            "mimeType = 'application/vnd.google-apps.folder' "
            f"and name = '{name}' and trashed = false and '{parent}' in parents"
        )
        params = urllib.parse.urlencode(
            {"q": q, "spaces": "drive", "fields": "files(id,name)", "pageSize": "1"}
        )
        result = self._request(f"{DRIVE_API}/files?{params}")
        files = result.get("files") or []
        if files:
            fid = files[0]["id"]
            self._folder_cache[cache_key] = fid
            return fid

        meta = json.dumps(
            {
                "name": name,
                "mimeType": "application/vnd.google-apps.folder",
                "parents": [parent],
            }
        ).encode("utf-8")
        created = self._request(
            f"{DRIVE_API}/files?fields=id,name",
            method="POST",
            data=meta,
            headers=self._headers(json_body=True),
        )
        fid = created["id"]
        self._folder_cache[cache_key] = fid
        return fid

    def list_files(self, folder_id: str) -> List[Dict[str, Any]]:
        out: List[Dict[str, Any]] = []
        page_token = None
        while True:
            params: Dict[str, str] = {
                "q": f"'{folder_id}' in parents and trashed = false and mimeType != 'application/vnd.google-apps.folder'",
                "spaces": "drive",
                "fields": "nextPageToken,files(id,name,md5Checksum,modifiedTime,size)",
                "pageSize": "1000",
            }
            if page_token:
                params["pageToken"] = page_token
            result = self._request(f"{DRIVE_API}/files?{urllib.parse.urlencode(params)}")
            out.extend(result.get("files") or [])
            page_token = result.get("nextPageToken")
            if not page_token:
                break
        return out

    def find_by_name(self, folder_id: str, name: str) -> Optional[Dict[str, Any]]:
        safe = name.replace("\\", "\\\\").replace("'", "\\'")
        q = f"name = '{safe}' and '{folder_id}' in parents and trashed = false"
        params = urllib.parse.urlencode(
            {
                "q": q,
                "spaces": "drive",
                "fields": "files(id,name,md5Checksum,modifiedTime,size)",
                "pageSize": "1",
            }
        )
        result = self._request(f"{DRIVE_API}/files?{params}")
        files = result.get("files") or []
        return files[0] if files else None

    def upload_file(self, local_path: Path, folder_id: str, remote_name: Optional[str] = None) -> Dict[str, Any]:
        name = remote_name or local_path.name
        existing = self.find_by_name(folder_id, name)
        mime = mimetypes.guess_type(str(local_path))[0] or "application/octet-stream"
        file_bytes = local_path.read_bytes()

        boundary = "syncmyshit_boundary"
        if existing:
            # Simple media upload update
            url = f"{UPLOAD_API}/files/{existing['id']}?uploadType=media&fields=id,name,md5Checksum,modifiedTime,size"
            headers = self._headers()
            headers["Content-Type"] = mime
            return self._request(url, method="PATCH", data=file_bytes, headers=headers)

        metadata = json.dumps({"name": name, "parents": [folder_id]})
        body = (
            f"--{boundary}\r\n"
            f"Content-Type: application/json; charset=UTF-8\r\n\r\n"
            f"{metadata}\r\n"
            f"--{boundary}\r\n"
            f"Content-Type: {mime}\r\n\r\n"
        ).encode("utf-8") + file_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")

        headers = self._headers()
        headers["Content-Type"] = f"multipart/related; boundary={boundary}"
        url = f"{UPLOAD_API}/files?uploadType=multipart&fields=id,name,md5Checksum,modifiedTime,size"
        return self._request(url, method="POST", data=body, headers=headers)

    def download_file(self, file_id: str, dest: Path) -> None:
        url = f"{DRIVE_API}/files/{file_id}?alt=media"
        req = urllib.request.Request(url, headers=self._headers())
        dest.parent.mkdir(parents=True, exist_ok=True)
        tmp = dest.with_suffix(dest.suffix + ".part")
        with urllib.request.urlopen(req, timeout=120) as resp:
            with open(tmp, "wb") as f:
                while True:
                    chunk = resp.read(65536)
                    if not chunk:
                        break
                    f.write(chunk)
        tmp.replace(dest)
