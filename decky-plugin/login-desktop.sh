#!/usr/bin/env bash
# syncMyShit — Google Drive login (Steam Deck Desktop Mode)
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Starting Desktop Mode Google login…"
exec python3 "$SCRIPT_DIR/desktop_login.py" "$@"
