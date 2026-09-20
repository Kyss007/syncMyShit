#!/usr/bin/env python3
"""
syncMyShit — Google Drive login for Steam Deck Desktop Mode.

Uses a Desktop-type OAuth client (loopback). The Android client ID
shipped with the phone app cannot use http://127.0.0.1 — Google returns
"invalid_request".

First run walks you through creating a Desktop OAuth client (2 minutes).
"""

from __future__ import annotations

import base64
import hashlib
import json
import os
import secrets
import socket
import threading
import time
import urllib.parse
import urllib.request
import webbrowser
from pathlib import Path

AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
SCOPE = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"

# Android client — does NOT work for desktop loopback (invalid_request)
ANDROID_CLIENT_ID = "590448604558-6q54r4h31so9md160o2dlrpskna4sh7f.apps.googleusercontent.com"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def config_dir() -> Path:
    p = Path.home() / ".config" / "syncMyShit"
    p.mkdir(parents=True, exist_ok=True)
    return p


def oauth_config_path() -> Path:
    return config_dir() / "oauth_desktop.json"


def token_paths() -> list[Path]:
    home = Path.home()
    paths = [
        home / ".config" / "syncMyShit" / "drive_token.json",
        home / "homebrew" / "settings" / "syncMyShit" / "drive_token.json",
    ]
    decky = os.environ.get("DECKY_PLUGIN_SETTINGS_DIR")
    if decky:
        paths.insert(0, Path(decky) / "drive_token.json")
    return paths


def load_desktop_client_id() -> str:
    env = (os.environ.get("SYNCMYSHIT_CLIENT_ID") or "").strip()
    if env and env != ANDROID_CLIENT_ID:
        return env
    path = oauth_config_path()
    if path.exists():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            cid = (data.get("client_id") or "").strip()
            if cid and cid != ANDROID_CLIENT_ID:
                return cid
        except Exception:
            pass
    return ""


def save_desktop_client_id(client_id: str) -> None:
    path = oauth_config_path()
    path.write_text(json.dumps({"client_id": client_id.strip()}, indent=2), encoding="utf-8")
    # Also mirror into Decky-readable config.json custom field
    for cfg in (
        config_dir() / "config.json",
        Path.home() / "homebrew" / "settings" / "syncMyShit" / "config.json",
    ):
        try:
            cfg.parent.mkdir(parents=True, exist_ok=True)
            data = {}
            if cfg.exists():
                data = json.loads(cfg.read_text(encoding="utf-8"))
            data["custom_oauth_client_id"] = client_id.strip()
            cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
        except Exception:
            pass


def save_tokens(tokens: dict) -> None:
    raw = json.dumps(tokens, indent=2)
    for path in token_paths():
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(raw, encoding="utf-8")
            os.chmod(path, 0o600)
            print(f"  ✓ Saved tokens → {path}")
        except Exception as e:
            print(f"  ! Could not write {path}: {e}")


def http_response(conn: socket.socket, status: int, body: str, content_type: str = "text/html") -> None:
    data = body.encode("utf-8")
    header = (
        f"HTTP/1.1 {status} OK\r\n"
        f"Content-Type: {content_type}; charset=utf-8\r\n"
        f"Content-Length: {len(data)}\r\n"
        f"Connection: close\r\n\r\n"
    )
    conn.sendall(header.encode("utf-8") + data)


def prompt_for_desktop_client_id() -> str:
    print("")
    print("----------------------------------------------------------------")
    print(" ONE-TIME SETUP — Google Desktop OAuth client")
    print("----------------------------------------------------------------")
    print("")
    print("The Android app Client ID cannot log in from a PC browser.")
    print("You need a \"Desktop app\" client in the same Google Cloud project.")
    print("")
    print("1. Open: https://console.cloud.google.com/apis/credentials")
    print("2. Select your syncMyShit project (or create one)")
    print("3. Enable \"Google Drive API\" if asked")
    print("4. Create Credentials → OAuth client ID")
    print("5. Application type: Desktop app")
    print("6. Name: syncMyShit-Deck")
    print("7. Create → copy the Client ID")
    print("   (looks like: xxxxx.apps.googleusercontent.com)")
    print("")
    print("Also add yourself as a Test user under OAuth consent screen")
    print("if the app is still in Testing mode.")
    print("")
    try:
        webbrowser.open("https://console.cloud.google.com/apis/credentials")
    except Exception:
        pass

    while True:
        cid = input("Paste Desktop Client ID here: ").strip()
        if not cid:
            print("Empty — try again, or Ctrl+C to cancel.")
            continue
        if cid == ANDROID_CLIENT_ID:
            print("That's the Android client ID — it will keep failing.")
            print("Create a Desktop app client and paste that one.")
            continue
        if ".apps.googleusercontent.com" not in cid:
            print("That doesn't look like a Google Client ID. Try again.")
            continue
        save_desktop_client_id(cid)
        print(f"Saved Desktop Client ID → {oauth_config_path()}")
        return cid


def main() -> int:
    print("")
    print("==============================================")
    print("  syncMyShit — Desktop Mode Google Login")
    print("==============================================")
    print("")

    client_id = load_desktop_client_id()
    if not client_id:
        client_id = prompt_for_desktop_client_id()
    else:
        print(f"Using Desktop Client ID: {client_id[:20]}…{client_id[-20:]}")
        print(f"(change anytime: delete {oauth_config_path()})")
        print("")

    verifier = b64url(secrets.token_bytes(48))
    challenge = b64url(hashlib.sha256(verifier.encode("ascii")).digest())
    state = secrets.token_urlsafe(16)

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    port = None
    for try_port in (8765, 8766, 8767, 8768, 0):
        try:
            sock.bind(("127.0.0.1", try_port))
            port = sock.getsockname()[1]
            break
        except OSError:
            continue
    if port is None:
        print("ERROR: Could not bind a local port.")
        return 1

    sock.listen(5)
    redirect_uri = f"http://127.0.0.1:{port}/"
    params = {
        "client_id": client_id,
        "redirect_uri": redirect_uri,
        "response_type": "code",
        "scope": SCOPE,
        "code_challenge": challenge,
        "code_challenge_method": "S256",
        "state": state,
        "access_type": "offline",
        "prompt": "consent",
    }
    auth_url = f"{AUTH_ENDPOINT}?{urllib.parse.urlencode(params)}"

    print(f"Listening on {redirect_uri}")
    print("Opening your browser…")
    print("")
    print("If the browser does not open, paste this URL into Firefox:")
    print(auth_url)
    print("")

    try:
        webbrowser.open(auth_url)
    except Exception as e:
        print(f"(webbrowser.open failed: {e})")

    result: dict = {"done": False, "error": None, "tokens": None}

    def serve() -> None:
        while not result["done"]:
            try:
                sock.settimeout(1.0)
                try:
                    conn, _ = sock.accept()
                except socket.timeout:
                    continue
                try:
                    req = b""
                    while b"\r\n\r\n" not in req and len(req) < 65536:
                        chunk = conn.recv(4096)
                        if not chunk:
                            break
                        req += chunk
                    first = req.split(b"\r\n", 1)[0].decode("utf-8", errors="replace")
                    parts = first.split(" ")
                    path = parts[1] if len(parts) >= 2 else "/"
                    parsed = urllib.parse.urlparse(path)
                    qs = urllib.parse.parse_qs(parsed.query)

                    if "code" in qs:
                        code = qs["code"][0]
                        data = {
                            "client_id": client_id,
                            "code": code,
                            "grant_type": "authorization_code",
                            "redirect_uri": redirect_uri,
                            "code_verifier": verifier,
                        }
                        body = urllib.parse.urlencode(data).encode("utf-8")
                        treq = urllib.request.Request(
                            TOKEN_ENDPOINT,
                            data=body,
                            headers={
                                "Content-Type": "application/x-www-form-urlencoded",
                                "User-Agent": "syncMyShit-DesktopLogin/2.1.1",
                            },
                        )
                        with urllib.request.urlopen(treq, timeout=20) as resp:
                            token_data = json.loads(resp.read().decode("utf-8"))

                        access = token_data["access_token"]
                        refresh = token_data.get("refresh_token")
                        expires_in = int(token_data.get("expires_in", 3600))
                        email = "Google Drive"
                        try:
                            ureq = urllib.request.Request(
                                USERINFO_ENDPOINT,
                                headers={
                                    "Authorization": f"Bearer {access}",
                                    "User-Agent": "syncMyShit-DesktopLogin/2.1.1",
                                },
                            )
                            with urllib.request.urlopen(ureq, timeout=10) as uresp:
                                email = json.loads(uresp.read().decode("utf-8")).get("email") or email
                        except Exception:
                            pass

                        tokens = {
                            "access_token": access,
                            "refresh_token": refresh,
                            "expires_at": time.time() + expires_in,
                            "email": email,
                            "connected_at": int(time.time()),
                            "client_id": client_id,
                        }
                        save_tokens(tokens)
                        result["tokens"] = tokens
                        http_response(
                            conn,
                            200,
                            f"""<!DOCTYPE html><html><body style="font-family:sans-serif;background:#061018;color:#eaf6fb;display:flex;align-items:center;justify-content:center;min-height:100vh">
                            <div style="text-align:center"><h1 style="color:#3dff9a">Linked as {email}</h1>
                            <p>Close this tab. Return to Gaming Mode → Decky → syncMyShit.</p></div></body></html>""",
                        )
                        result["done"] = True
                    elif "error" in qs:
                        err = qs.get("error", ["unknown"])[0]
                        desc = (qs.get("error_description") or [""])[0]
                        result["error"] = f"{err}: {desc}".strip(": ")
                        http_response(
                            conn,
                            400,
                            f"<h1>Auth error</h1><p>{err}</p><p>{desc}</p>"
                            "<p>If this is invalid_request, your Client ID is not a Desktop app type.</p>",
                        )
                        result["done"] = True
                    else:
                        http_response(conn, 200, "<h1>syncMyShit</h1><p>Waiting for Google…</p>")
                finally:
                    try:
                        conn.close()
                    except Exception:
                        pass
            except Exception as e:
                result["error"] = str(e)
                result["done"] = True

    t = threading.Thread(target=serve, daemon=True)
    t.start()

    print("Waiting for Google sign-in (up to 5 minutes)…")
    deadline = time.time() + 300
    while not result["done"] and time.time() < deadline:
        time.sleep(0.25)

    try:
        sock.close()
    except Exception:
        pass

    if result.get("tokens"):
        email = result["tokens"].get("email", "")
        print("")
        print(f"SUCCESS — signed in as {email}")
        print("Switch to Gaming Mode → Quick Access → syncMyShit.")
        print("")
        return 0

    err = result.get("error") or "timed out / cancelled"
    print("")
    print(f"FAILED — {err}")
    if "invalid_request" in str(err).lower() or "invalid_client" in str(err).lower():
        print("")
        print("Your Client ID is probably NOT type \"Desktop app\".")
        print(f"Delete {oauth_config_path()} and run this script again.")
        print("Create a new OAuth client with Application type = Desktop app.")
        try:
            oauth_config_path().unlink(missing_ok=True)
        except Exception:
            pass
    print("")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
