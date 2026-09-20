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

# 1. Determine Decky plugins directory and target user
if [ -n "$SUDO_USER" ]; then
    TARGET_USER="$SUDO_USER"
    TARGET_HOME="$(getent passwd "$SUDO_USER" | cut -d: -f6)"
else
    TARGET_USER="$(id -un)"
    TARGET_HOME="$HOME"
fi

# Detect Decky location
if [ -d "$TARGET_HOME/homebrew" ]; then
    HOMEBREW_DIR="$TARGET_HOME/homebrew"
elif [ -d "/home/deck/homebrew" ]; then
    HOMEBREW_DIR="/home/deck/homebrew"
elif [ -d "/homebrew" ]; then
    HOMEBREW_DIR="/homebrew"
else
    HOMEBREW_DIR="$TARGET_HOME/homebrew"
fi

PLUGIN_DEST="$HOMEBREW_DIR/plugins/syncMyShit"

# Determine if sudo is required to write to HOMEBREW_DIR
SUDO_CMD=""
if [ "$EUID" -ne 0 ]; then
    # Test if target plugins directory or parent is writable
    TEST_DIR="$HOMEBREW_DIR"
    if [ -d "$HOMEBREW_DIR/plugins" ]; then
        TEST_DIR="$HOMEBREW_DIR/plugins"
    fi
    if [ ! -w "$TEST_DIR" ] && [ -e "$TEST_DIR" ]; then
        if command -v sudo >/dev/null 2>&1; then
            SUDO_CMD="sudo"
            echo -e "${YELLOW}Notice: elevated permissions required to write to $HOMEBREW_DIR. Using sudo...${NC}"
        fi
    fi
fi

$SUDO_CMD mkdir -p "$HOMEBREW_DIR/plugins"

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

    $SUDO_CMD rm -rf "$PLUGIN_DEST"
    $SUDO_CMD mkdir -p "$PLUGIN_DEST"
    $SUDO_CMD cp "$LOCAL_PLUGIN_DIR/plugin.json" "$PLUGIN_DEST/"
    $SUDO_CMD cp "$LOCAL_PLUGIN_DIR/package.json" "$PLUGIN_DEST/"
    $SUDO_CMD cp "$LOCAL_PLUGIN_DIR/main.py" "$PLUGIN_DEST/"
    $SUDO_CMD cp "$LOCAL_PLUGIN_DIR/README.md" "$PLUGIN_DEST/"
    $SUDO_CMD cp -r "$LOCAL_PLUGIN_DIR/dist" "$PLUGIN_DEST/"
    $SUDO_CMD mkdir -p "$PLUGIN_DEST/py_modules"
    $SUDO_CMD rsync -av --exclude='__pycache__' "$LOCAL_PLUGIN_DIR/py_modules/" "$PLUGIN_DEST/py_modules/"
else
    echo -e "${CYAN}Downloading latest syncMyShit Decky release from GitHub...${NC}"
    TMP_DIR=$(mktemp -d)
    ZIP_URL="https://github.com/Kyss007/syncMyShit/releases/latest/download/syncMyShit-decky.zip"
    
    curl -sSL "$ZIP_URL" -o "$TMP_DIR/syncMyShit-decky.zip" || {
        echo -e "${RED}Error: Failed to download release zip. Check internet connection.${NC}"
        rm -rf "$TMP_DIR"
        exit 1
    }

    $SUDO_CMD rm -rf "$PLUGIN_DEST"
    $SUDO_CMD mkdir -p "$PLUGIN_DEST"
    $SUDO_CMD unzip -q "$TMP_DIR/syncMyShit-decky.zip" -d "$HOMEBREW_DIR/plugins/"
    rm -rf "$TMP_DIR"
fi

# Ensure correct permissions and ownership
$SUDO_CMD chmod -R a+rX "$PLUGIN_DEST"
if [ -n "$SUDO_CMD" ] || [ "$EUID" -eq 0 ]; then
    # Keep ownership consistent with parent directory
    PARENT_OWNER=$(stat -c '%u:%g' "$HOMEBREW_DIR/plugins" 2>/dev/null || echo "root:root")
    $SUDO_CMD chown -R "$PARENT_OWNER" "$PLUGIN_DEST" 2>/dev/null || true
fi

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
