#!/usr/bin/env bash
# syncMyShit - Decky Loader Plugin Uninstaller for Steam Deck
# Dedicated to the Public Domain (The Unlicense)

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}================================================${NC}"
echo -e "${CYAN}🗑️  syncMyShit - Decky Loader Plugin Uninstaller${NC}"
echo -e "${CYAN}================================================${NC}"

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

# Determine if sudo is required
SUDO_CMD=""
if [ "$EUID" -ne 0 ]; then
    TEST_DIR="$HOMEBREW_DIR"
    if [ -d "$HOMEBREW_DIR/plugins" ]; then
        TEST_DIR="$HOMEBREW_DIR/plugins"
    fi
    if [ ! -w "$TEST_DIR" ] && [ -e "$TEST_DIR" ]; then
        if command -v sudo >/dev/null 2>&1; then
            SUDO_CMD="sudo"
        fi
    fi
fi

# 2. Remove the plugin folder
if [ -d "$PLUGIN_DEST" ]; then
    echo -e "${YELLOW}Removing syncMyShit from:${NC} $PLUGIN_DEST"
    $SUDO_CMD rm -rf "$PLUGIN_DEST"
    echo -e "${GREEN}✓ Plugin files removed successfully!${NC}"
else
    echo -e "${YELLOW}syncMyShit plugin was not found in $PLUGIN_DEST.${NC}"
fi

# Optional --purge flag to remove settings and logs
CONFIG_DIR="$TARGET_HOME/.config/syncMyShit"
if [[ "$*" == *"--purge"* ]]; then
    if [ -d "$CONFIG_DIR" ]; then
        echo -e "${YELLOW}Purging configuration directory...${NC}"
        rm -rf "$CONFIG_DIR"
        echo -e "${GREEN}✓ Removed $CONFIG_DIR${NC}"
    fi
else
    if [ -d "$CONFIG_DIR" ]; then
        echo -e "\n${CYAN}Notice:${NC} Your settings and cloud config in ${CYAN}$CONFIG_DIR${NC} were preserved."
        echo -e "To delete configuration data as well, run: ${YELLOW}uninstall-decky.sh --purge${NC}"
    fi
fi

# 3. Reload Decky Loader service if running
if systemctl is-active --quiet plugin_loader.service 2>/dev/null; then
    echo -e "\n${YELLOW}Restarting Decky Loader service to refresh Quick Access Menu...${NC}"
    sudo systemctl restart plugin_loader.service 2>/dev/null || true
    echo -e "${GREEN}✓ Decky Loader reloaded!${NC}"
else
    echo -e "\n${YELLOW}Tip: If Decky is active, restart your Deck or Decky service:${NC}"
    echo -e "  sudo systemctl restart plugin_loader.service"
fi

echo -e "\n${GREEN}✓ syncMyShit has been completely uninstalled from Decky Loader.${NC}\n"
