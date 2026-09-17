#!/usr/bin/env bash
# syncMyShit - Decky Plugin Packager
# Packages the Decky plugin into dist/syncMyShit-decky.zip

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$ROOT_DIR/dist"
PLUGIN_DIR="$ROOT_DIR/decky-plugin"

echo "Building Decky frontend bundle..."
(cd "$PLUGIN_DIR" && node build.mjs)

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
