#!/usr/bin/env bash
# syncMyShit - Decky Loader Plugin Updater for Steam Deck
# Dedicated to the Public Domain (The Unlicense)

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}==============================================${NC}"
echo -e "${CYAN}🔄 syncMyShit - Decky Loader Plugin Updater  ${NC}"
echo -e "${CYAN}==============================================${NC}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# If running from local repo, invoke install-decky.sh with --update
if [ -f "$SCRIPT_DIR/install-decky.sh" ]; then
    bash "$SCRIPT_DIR/install-decky.sh" --update "$@"
else
    # Remote execution via curl
    curl -sSL https://raw.githubusercontent.com/Kyss007/syncMyShit/main/install-decky.sh | bash -s -- --update "$@"
fi
