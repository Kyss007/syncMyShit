#!/usr/bin/env bash
# syncMyShit - Decky Loader Plugin Installer for Steam Deck
# Dedicated to the Public Domain (The Unlicense)

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}==============================================${NC}"
echo -e "${CYAN}🎮 syncMyShit - Decky Loader Plugin Installer${NC}"
echo -e "${CYAN}==============================================${NC}"

# 1. Determine Decky plugins directory
HOMEBREW_DIR="$HOME/homebrew"
if [ ! -d "$HOMEBREW_DIR" ] && [ -d "/homebrew" ]; then
    HOMEBREW_DIR="/homebrew"
fi

PLUGIN_DEST="$HOMEBREW_DIR/plugins/syncMyShit"
mkdir -p "$HOMEBREW_DIR/plugins"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_PLUGIN_DIR="$SCRIPT_DIR/decky-plugin"

# 2. Install from local repository or download release
if [ -d "$LOCAL_PLUGIN_DIR" ] && [ -f "$LOCAL_PLUGIN_DIR/plugin.json" ]; then
    echo -e "${GREEN}Installing from local repository...${NC}"
    
    # Ensure frontend is built
    if [ ! -f "$LOCAL_PLUGIN_DIR/dist/index.js" ]; then
        echo -e "${YELLOW}Building Decky frontend bundle...${NC}"
        (cd "$LOCAL_PLUGIN_DIR" && npm install --silent && node build.mjs)
    fi

    rm -rf "$PLUGIN_DEST"
    mkdir -p "$PLUGIN_DEST"
    cp "$LOCAL_PLUGIN_DIR/plugin.json" "$PLUGIN_DEST/"
    cp "$LOCAL_PLUGIN_DIR/package.json" "$PLUGIN_DEST/"
    cp "$LOCAL_PLUGIN_DIR/main.py" "$PLUGIN_DEST/"
    cp "$LOCAL_PLUGIN_DIR/README.md" "$PLUGIN_DEST/"
    cp -r "$LOCAL_PLUGIN_DIR/dist" "$PLUGIN_DEST/"
    mkdir -p "$PLUGIN_DEST/py_modules"
    rsync -av --exclude='__pycache__' "$LOCAL_PLUGIN_DIR/py_modules/" "$PLUGIN_DEST/py_modules/"
else
    echo -e "${CYAN}Downloading latest syncMyShit Decky release from GitHub...${NC}"
    TMP_DIR=$(mktemp -d)
    ZIP_URL="https://github.com/Kyss007/syncMyShit/releases/latest/download/syncMyShit-decky.zip"
    
    curl -sSL "$ZIP_URL" -o "$TMP_DIR/syncMyShit-decky.zip" || {
        echo -e "${RED}Error: Failed to download release zip. Check internet connection.${NC}"
        rm -rf "$TMP_DIR"
        exit 1
    }

    rm -rf "$PLUGIN_DEST"
    mkdir -p "$PLUGIN_DEST"
    unzip -q "$TMP_DIR/syncMyShit-decky.zip" -d "$HOMEBREW_DIR/plugins/"
    rm -rf "$TMP_DIR"
fi

# Ensure correct permissions
chmod -R u+rwX "$PLUGIN_DEST"

echo -e "\n${GREEN}✓ syncMyShit successfully installed to:${NC}"
echo -e "  ${CYAN}$PLUGIN_DEST${NC}"

# 3. Reload Decky Loader service if possible
if systemctl is-active --quiet plugin_loader.service 2>/dev/null; then
    echo -e "\n${YELLOW}Restarting Decky Loader service...${NC}"
    sudo systemctl restart plugin_loader.service 2>/dev/null || true
    echo -e "${GREEN}✓ Decky Loader reloaded!${NC}"
else
    echo -e "\n${YELLOW}Tip: If Decky is currently running, restart your Deck or Decky service:${NC}"
    echo -e "  sudo systemctl restart plugin_loader.service"
fi

echo -e "\n${GREEN}All done! Open your Quick Access Menu (...) in Gaming Mode to use syncMyShit!${NC}\n"
