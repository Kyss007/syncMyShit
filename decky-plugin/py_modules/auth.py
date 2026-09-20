"""Google OAuth 2.0 PKCE with phone QR companion page for Steam Deck Game Mode."""

from __future__ import annotations

import base64
import hashlib
import http.server
import json
import logging
import secrets
import socket
import socketserver
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Dict, Optional, Tuple

from store import Store

logger = logging.getLogger("syncMyShit")

# Same client as Android WebOAuthManager — cross-device Drive folder ownership
DEFAULT_CLIENT_ID = "590448604558-6q54r4h31so9md160o2dlrpskna4sh7f.apps.googleusercontent.com"
AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
SCOPE = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def get_lan_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(0.5)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


class AuthManager:
    def __init__(self, store: Store) -> None:
        self.store = store
        self._server: Optional[socketserver.TCPServer] = None
        self._thread: Optional[threading.Thread] = None
        self._verifier: str = ""
        self._state: str = ""
        self._redirect_uri: str = ""
        self._auth_url: str = ""
        self._lan_url: str = ""
        self._lock = threading.Lock()

    @property
    def client_id(self) -> str:
        custom = (self.store.get("custom_oauth_client_id") or "").strip()
        return custom or DEFAULT_CLIENT_ID

    def is_authenticated(self) -> bool:
        tokens = self.store.load_tokens()
        return bool(tokens and tokens.get("access_token") and tokens.get("refresh_token"))

    def get_email(self) -> str:
        tokens = self.store.load_tokens() or {}
        return tokens.get("email") or self.store.get("google_drive_email") or ""

    def is_waiting(self) -> bool:
        return self._server is not None and bool(self._auth_url)

    def get_auth_url(self) -> str:
        return self._auth_url

    def get_lan_url(self) -> str:
        return self._lan_url

    def _pkce(self) -> Tuple[str, str]:
        verifier = _b64url(secrets.token_bytes(48))
        challenge = _b64url(hashlib.sha256(verifier.encode("ascii")).digest())
        return verifier, challenge

    def _stop_server(self) -> None:
        if self._server:
            try:
                self._server.shutdown()
                self._server.server_close()
            except Exception:
                pass
            self._server = None
        self._thread = None

    def cancel(self) -> None:
        with self._lock:
            self._auth_url = ""
            self._lan_url = ""
            self._stop_server()

    def sign_out(self) -> None:
        self.cancel()
        self.store.clear_tokens()

    def start_login(self, port: int = 8765) -> Dict[str, str]:
        """Start LAN companion server and return URLs for QR + OAuth."""
        with self._lock:
            if self.is_waiting() and self._lan_url:
                return {"auth_url": self._auth_url, "lan_url": self._lan_url}

            self._stop_server()
            self._verifier, challenge = self._pkce()
            self._state = secrets.token_urlsafe(16)

            bound = False
            for try_port in (port, 8766, 8767, 8768, 0):
                try:
                    handler = self._make_handler()
                    # Allow address reuse so rapid re-login works
                    socketserver.TCPServer.allow_reuse_address = True
                    self._server = socketserver.TCPServer(("0.0.0.0", try_port), handler)
                    actual = self._server.server_address[1]
                    bound = True
                    break
                except OSError:
                    continue
            if not bound or not self._server:
                raise RuntimeError("Could not bind OAuth companion port")

            self._redirect_uri = f"http://127.0.0.1:{actual}/"
            lan_ip = get_lan_ip()
            self._lan_url = f"http://{lan_ip}:{actual}/mobile"

            params = {
                "client_id": self.client_id,
                "redirect_uri": self._redirect_uri,
                "response_type": "code",
                "scope": SCOPE,
                "code_challenge": challenge,
                "code_challenge_method": "S256",
                "state": self._state,
                "access_type": "offline",
                "prompt": "consent",
            }
            self._auth_url = f"{AUTH_ENDPOINT}?{urllib.parse.urlencode(params)}"

            self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
            self._thread.start()
            logger.info("OAuth companion listening — LAN %s redirect %s", self._lan_url, self._redirect_uri)
            return {"auth_url": self._auth_url, "lan_url": self._lan_url}

    def exchange_code(self, code_or_url: str) -> Dict[str, Any]:
        code = (code_or_url or "").strip()
        if "code=" in code:
            parsed = urllib.parse.urlparse(code)
            qs = urllib.parse.parse_qs(parsed.query)
            code = qs.get("code", [code])[0]

        data = {
            "client_id": self.client_id,
            "code": code,
            "grant_type": "authorization_code",
            "redirect_uri": self._redirect_uri or "http://127.0.0.1:8765/",
            "code_verifier": self._verifier,
        }
        body = urllib.parse.urlencode(data).encode("utf-8")
        req = urllib.request.Request(
            TOKEN_ENDPOINT,
            data=body,
            headers={
                "Content-Type": "application/x-www-form-urlencoded",
                "User-Agent": "syncMyShit-Decky/2.0",
            },
        )
        with urllib.request.urlopen(req, timeout=20) as resp:
            token_data = json.loads(resp.read().decode("utf-8"))

        access = token_data["access_token"]
        refresh = token_data.get("refresh_token")
        expires_in = int(token_data.get("expires_in", 3600))

        email = "Google Drive"
        try:
            ureq = urllib.request.Request(
                USERINFO_ENDPOINT,
                headers={"Authorization": f"Bearer {access}", "User-Agent": "syncMyShit-Decky/2.0"},
            )
            with urllib.request.urlopen(ureq, timeout=10) as uresp:
                info = json.loads(uresp.read().decode("utf-8"))
                email = info.get("email") or email
        except Exception as e:
            logger.warning("userinfo fetch failed: %s", e)

        existing = self.store.load_tokens() or {}
        if not refresh:
            refresh = existing.get("refresh_token")

        saved = {
            "access_token": access,
            "refresh_token": refresh,
            "expires_at": time.time() + expires_in,
            "email": email,
            "connected_at": int(time.time()),
        }
        self.store.save_tokens(saved)
        self.store.set("google_drive_email", email)
        # Stop companion after successful exchange
        threading.Thread(target=self._stop_server, daemon=True).start()
        self._auth_url = ""
        self._lan_url = ""
        logger.info("Signed in as %s", email)
        return saved

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
                    "User-Agent": "syncMyShit-Decky/2.0",
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

    def _make_handler(self):
        mgr = self

        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, format, *args):  # noqa: A003
                return

            def _json(self, code: int, payload: dict) -> None:
                raw = json.dumps(payload).encode("utf-8")
                self.send_response(code)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Content-Length", str(len(raw)))
                self.end_headers()
                self.wfile.write(raw)

            def _html(self, code: int, html: str) -> None:
                raw = html.encode("utf-8")
                self.send_response(code)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(raw)))
                self.end_headers()
                self.wfile.write(raw)

            def do_GET(self):  # noqa: N802
                parsed = urllib.parse.urlparse(self.path)
                qs = urllib.parse.parse_qs(parsed.query)
                path = parsed.path.rstrip("/") or "/"

                if path in ("/mobile", "/phone", "/login"):
                    self._html(200, _mobile_page(mgr._auth_url))
                    return

                if path == "/submit":
                    raw = (qs.get("code") or [""])[0]
                    if not raw:
                        self._json(400, {"success": False, "error": "Missing code"})
                        return
                    try:
                        tokens = mgr.exchange_code(raw)
                        self._json(200, {"success": True, "email": tokens.get("email", "")})
                    except Exception as e:
                        logger.error("submit failed: %s", e)
                        self._json(500, {"success": False, "error": str(e)})
                    return

                if path == "/status":
                    self._json(
                        200,
                        {
                            "authenticated": mgr.is_authenticated(),
                            "email": mgr.get_email(),
                            "waiting": mgr.is_waiting(),
                        },
                    )
                    return

                # Loopback callback (if somehow hit from Deck browser)
                if "code" in qs:
                    try:
                        tokens = mgr.exchange_code(qs["code"][0])
                        self._html(
                            200,
                            _success_page(tokens.get("email", "")),
                        )
                    except Exception as e:
                        self._html(500, f"<h1>Error</h1><p>{e}</p>")
                    return

                if "error" in qs:
                    self._html(400, f"<h1>Auth error</h1><p>{qs.get('error')}</p>")
                    return

                self._html(200, _mobile_page(mgr._auth_url))

            def do_POST(self):  # noqa: N802
                parsed = urllib.parse.urlparse(self.path)
                path = parsed.path.rstrip("/") or "/"
                length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(length).decode("utf-8") if length else ""
                if path == "/submit":
                    qs = urllib.parse.parse_qs(body)
                    raw = (qs.get("code") or [""])[0]
                    if not raw:
                        # also accept JSON
                        try:
                            raw = json.loads(body).get("code", "")
                        except Exception:
                            raw = ""
                    if not raw:
                        self._json(400, {"success": False, "error": "Missing code"})
                        return
                    try:
                        tokens = mgr.exchange_code(raw)
                        self._json(200, {"success": True, "email": tokens.get("email", "")})
                    except Exception as e:
                        self._json(500, {"success": False, "error": str(e)})
                    return
                self._json(404, {"success": False, "error": "Not found"})

        return Handler


def _success_page(email: str) -> str:
    return f"""<!DOCTYPE html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Connected</title>
<style>
body{{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
background:#071018;color:#e8fff0;font-family:ui-rounded,system-ui,sans-serif}}
.box{{text-align:center;padding:2rem}}
h1{{color:#3dff9a;font-size:1.6rem}}
</style></head>
<body><div class="box"><h1>Linked</h1><p>{email}</p>
<p>You can close this and pick up your Steam Deck.</p></div></body></html>"""


def _mobile_page(auth_url: str) -> str:
    safe_url = auth_url.replace('"', "&quot;")
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<title>syncMyShit — Link Deck</title>
<style>
  :root {{
    --bg: #061018;
    --panel: #0d1f2a;
    --line: #1e3a48;
    --cyan: #2ee6ff;
    --lime: #3dff9a;
    --muted: #8aa4b0;
    --text: #eaf6fb;
  }}
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    min-height: 100vh;
    background:
      radial-gradient(ellipse at 20% 0%, #0a3a44 0%, transparent 50%),
      radial-gradient(ellipse at 100% 100%, #0a2a1a 0%, transparent 45%),
      var(--bg);
    color: var(--text);
    font-family: "Trebuchet MS", "Segoe UI", sans-serif;
    padding: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
  }}
  .card {{
    width: 100%;
    max-width: 420px;
    background: var(--panel);
    border: 2px solid var(--line);
    border-radius: 4px;
    padding: 22px;
    box-shadow: 6px 6px 0 #021018;
  }}
  .brand {{
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.02em;
    color: var(--cyan);
    margin-bottom: 4px;
  }}
  .tag {{
    font-size: 13px;
    color: var(--muted);
    margin-bottom: 18px;
    line-height: 1.4;
  }}
  .step {{
    border-left: 3px solid var(--lime);
    padding: 10px 12px;
    margin-bottom: 12px;
    background: rgba(61,255,154,0.06);
    font-size: 13px;
    line-height: 1.45;
    color: #c5dde6;
  }}
  .step strong {{ color: var(--lime); }}
  a.btn, button.btn {{
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    padding: 14px;
    margin: 8px 0 14px;
    border: none;
    border-radius: 3px;
    font-size: 15px;
    font-weight: 700;
    text-decoration: none;
    cursor: pointer;
  }}
  .btn-google {{ background: #fff; color: #111; }}
  .btn-go {{ background: var(--lime); color: #042014; }}
  input {{
    width: 100%;
    padding: 12px;
    border: 1px solid var(--line);
    border-radius: 3px;
    background: #07141c;
    color: var(--text);
    font-size: 14px;
    margin-bottom: 8px;
  }}
  input:focus {{ outline: 2px solid var(--cyan); border-color: transparent; }}
  #ok {{
    display: none;
    text-align: center;
    padding: 28px 12px;
  }}
  #ok h2 {{ color: var(--lime); font-size: 22px; margin-bottom: 8px; }}
  #err {{ color: #ff6b6b; font-size: 13px; min-height: 1.2em; margin-top: 6px; }}
</style>
</head>
<body>
  <div class="card" id="main">
    <div class="brand">syncMyShit</div>
    <p class="tag">Scan worked. Now bribe Google, then paste the weird localhost URL back here.</p>

    <div class="step"><strong>1.</strong> Open Google sign-in in a new tab (keep this page open).</div>
    <a class="btn btn-google" href="{safe_url}" target="_blank" rel="noopener">Open Google Sign-In</a>

    <div class="step"><strong>2.</strong> After you approve, your phone shows a page that won't load
    (<code>http://127.0.0.1…</code>). Copy that full address from the address bar and paste it below.</div>
    <input id="code" type="text" placeholder="Paste http://127.0.0.1… URL or code" autocomplete="off" />
    <button class="btn btn-go" type="button" onclick="go()">Connect Steam Deck</button>
    <div id="err"></div>
  </div>
  <div class="card" id="ok">
    <h2>You're linked</h2>
    <p id="email" style="color:#8aa4b0;font-size:14px"></p>
    <p style="margin-top:12px;font-size:13px;color:#8aa4b0">Close this tab and grab your Deck.</p>
  </div>
<script>
async function go() {{
  const err = document.getElementById('err');
  err.textContent = '';
  const val = document.getElementById('code').value.trim();
  if (!val) {{ err.textContent = 'Paste the URL or code first.'; return; }}
  try {{
    const res = await fetch('/submit?code=' + encodeURIComponent(val));
    const data = await res.json();
    if (data.success) {{
      document.getElementById('main').style.display = 'none';
      document.getElementById('ok').style.display = 'block';
      document.getElementById('email').textContent = data.email || '';
    }} else {{
      err.textContent = data.error || 'Could not connect.';
    }}
  }} catch (e) {{
    err.textContent = 'Cannot reach Steam Deck — same Wi‑Fi?';
  }}
}}
</script>
</body>
</html>"""
