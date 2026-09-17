#!/usr/bin/env bash
set -e

INSTALL_DIR="$HOME/.local/bin"
APP_DIR="$HOME/.local/share/applications"

mkdir -p "$INSTALL_DIR"
mkdir -p "$APP_DIR"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN_PATH=""

if [ -f "$SCRIPT_DIR/syncMyShit" ]; then
    BIN_PATH="$SCRIPT_DIR/syncMyShit"
elif [ -f "$SCRIPT_DIR/syncMyShit-linux-x64" ]; then
    BIN_PATH="$SCRIPT_DIR/syncMyShit-linux-x64"
elif [ -f "$SCRIPT_DIR/dist/syncMyShit" ]; then
    BIN_PATH="$SCRIPT_DIR/dist/syncMyShit"
fi

if [ -n "$BIN_PATH" ]; then
    cp "$BIN_PATH" "$INSTALL_DIR/syncMyShit"
    chmod +x "$INSTALL_DIR/syncMyShit"
    echo "✔ Installed syncMyShit binary to $INSTALL_DIR/syncMyShit"
else
    echo "Error: syncMyShit binary not found in $SCRIPT_DIR"
    exit 1
fi

cat <<EOF > "$APP_DIR/syncMyShit.desktop"
[Desktop Entry]
Name=syncMyShit
Comment=Automagic Retro Game Cloud Save Sync
Exec=$INSTALL_DIR/syncMyShit
Icon=preferences-desktop-gaming
Terminal=false
Type=Application
Categories=Game;Utility;Archiving;
StartupNotify=true
EOF

chmod +x "$APP_DIR/syncMyShit.desktop"
echo "✔ Installed desktop application launcher to $APP_DIR/syncMyShit.desktop"
echo ""
echo "🎉 Installation complete! You can now launch syncMyShit from your application menu or run 'syncMyShit' in terminal."
