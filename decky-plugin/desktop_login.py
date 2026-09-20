#!/usr/bin/env python3
"""
syncMyShit — Google Drive login for Steam Deck Desktop Mode.

Opens your normal desktop browser, completes OAuth on localhost,
and writes tokens where the Decky plugin can find them.

Usage (Desktop Mode Konsole):
  python3 ~/homebrew/plugins/syncMyShit/desktop_login.py
  # or:
  ~/homebrew/plugins/syncMyShit/login-desktop.sh
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

CLIENT_ID = "590448604558-6q54r4h31so9md160o2dlrpskna4sh7f.apps.googleusercontent.com"
AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
SCOPE = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def token_paths() -> list[Path]:
    home = Path.home()
    paths = [
        home / ".config" / "syncMyShit" / "drive_token.json",
        home / "homebrew" / "settings" / "syncMyShit" / "drive_token.json",
    ]
    # Also cover common Decky settings location when run as deck
    decky = os.environ.get("DECKY_PLUGIN_SETTINGS_DIR")
    if decky:
        paths.insert(0, Path(decky) / "drive_token.json")
    return paths


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


def main() -> int:
    print("")
    print("==============================================")
    print("  syncMyShit — Desktop Mode Google Login")
    print("==============================================")
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
        "client_id": CLIENT_ID,
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
    print("If the browser does not open, paste this URL into Firefox/Chrome:")
    print(auth_url)
    print("")

    try:
        webbrowser.open(auth_url)
    except Exception as e:
        print(f"(webbrowser.open failed: {e} — use the URL above)")

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
                            "client_id": CLIENT_ID,
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
                                "User-Agent": "syncMyShit-DesktopLogin/2.1",
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
                                    "User-Agent": "syncMyShit-DesktopLogin/2.1",
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
                        }
                        save_tokens(tokens)
                        result["tokens"] = tokens
                        http_response(
                            conn,
                            200,
                            f"""<!DOCTYPE html><html><body style="font-family:sans-serif;background:#061018;color:#eaf6fb;display:flex;align-items:center;justify-content:center;min-height:100vh">
                            <div style="text-align:center"><h1 style="color:#3dff9a">Linked as {email}</h1>
                            <p>You can close this tab and return to Gaming Mode.</p>
                            <p>Open Decky → syncMyShit to sync.</p></div></body></html>""",
                        )
                        result["done"] = True
                    elif "error" in qs:
                        err = qs.get("error", ["unknown"])[0]
                        result["error"] = err
                        http_response(conn, 400, f"<h1>Auth error</h1><p>{err}</p>")
                        result["done"] = True
                    else:
                        http_response(
                            conn,
                            200,
                            "<h1>syncMyShit</h1><p>Waiting for Google redirect…</p>",
                        )
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

    print("Waiting for Google sign-in in your browser (up to 5 minutes)…")
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
        print("Switch back to Gaming Mode → Quick Access → syncMyShit.")
        print("")
        return 0

    print("")
    print(f"FAILED — {result.get('error') or 'timed out / cancelled'}")
    print("")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
