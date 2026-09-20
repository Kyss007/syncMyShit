#!/usr/bin/env bash
# syncMyShit - Decky Plugin Packager
# Packages the Decky plugin into dist/syncMyShit-decky.zip

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$ROOT_DIR/dist"
PLUGIN_DIR="$ROOT_DIR/decky-plugin"

if [ ! -f "$PLUGIN_DIR/dist/index.js" ]; then
    echo "Building Decky frontend bundle..."
    if command -v npm >/dev/null 2>&1; then
        (cd "$PLUGIN_DIR" && npm install --silent && node build.mjs)
    else
        (cd "$PLUGIN_DIR" && node build.mjs)
    fi
fi

echo "Packaging syncMyShit Decky plugin..."
mkdir -p "$DIST_DIR"

STAGE_DIR=$(mktemp -d)
DEST="$STAGE_DIR/syncMyShit"
mkdir -p "$DEST"

# Copy essential files
cp "$PLUGIN_DIR/plugin.json" "$DEST/"
cp "$PLUGIN_DIR/package.json" "$DEST/"
cp "$PLUGIN_DIR/main.py" "$DEST/"
cp "$PLUGIN_DIR/README.md" "$DEST/"
cp "$PLUGIN_DIR/desktop_login.py" "$DEST/"
cp "$PLUGIN_DIR/login-desktop.sh" "$DEST/"
chmod +x "$DEST/login-desktop.sh" "$DEST/desktop_login.py"
cp -r "$PLUGIN_DIR/dist" "$DEST/"

# Copy python modules, skipping __pycache__
mkdir -p "$DEST/py_modules"
rsync -av --exclude='__pycache__' "$PLUGIN_DIR/py_modules/" "$DEST/py_modules/"

# Create zip
ZIP_OUT="$DIST_DIR/syncMyShit-decky.zip"
rm -f "$ZIP_OUT"
(cd "$STAGE_DIR" && zip -r "$ZIP_OUT" syncMyShit)

rm -rf "$STAGE_DIR"
echo "✓ Decky plugin package created at: $ZIP_OUT ($(du -h "$ZIP_OUT" | cut -f1))"
