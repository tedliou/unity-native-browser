#!/usr/bin/env bash
# Deploy APK to Android device via adb
set -euo pipefail

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Script directory for relative paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Package name
PACKAGE_NAME="com.tedliou.android.browser"
ACTIVITY_NAME="com.unity3d.player.UnityPlayerActivity"

# Help function
show_help() {
    cat << 'EOF'
Usage: deploy.sh [OPTIONS]

Deploy and launch Android browser plugin APK on connected device/emulator.

Prerequisites:
  - Android device or emulator connected via USB (adb devices)
  - APK built from Unity: build/NativeBrowser.apk

Options:
  -h, --help       Show this help message
  -l, --logcat     Show logcat output after launch (Ctrl+C to stop)
  
Output:
  Installs APK and launches main activity on device.
  
Example:
  ./tools/deploy.sh           # Deploy and launch
  ./tools/deploy.sh --logcat  # Deploy, launch, and show logs
EOF
    exit 0
}

# Parse arguments
SHOW_LOGCAT=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            ;;
        -l|--logcat)
            SHOW_LOGCAT=true
            shift
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}" >&2
            show_help
            ;;
    esac
done

# Check prerequisites
check_adb() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}✗ adb not found. Please install Android SDK Platform Tools${NC}" >&2
        return 1
    fi
    
    if ! adb devices | grep -q "device$\|emulator"; then
        echo -e "${RED}✗ No Android device/emulator connected${NC}" >&2
        echo "Connect device via USB or start an emulator"
        adb devices
        return 1
    fi
    
    echo -e "${YELLOW}  Connected device: $(adb devices | grep -m 1 'device\|emulator' | head -c 40)${NC}"
}

# Main deploy flow
main() {
    echo -e "${GREEN}=== Deploy Android Browser Plugin ===${NC}"
    
    echo "Checking prerequisites..."
    check_adb || return 1
    
    APK_PATH="$PROJECT_ROOT/build/NativeBrowser.apk"
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${YELLOW}⚠ APK not found: $APK_PATH${NC}"
        echo ""
        echo -e "${YELLOW}Build APK from Unity:${NC}"
        echo "  1. Open Unity project: src/unity/"
        echo "  2. File → Build Settings"
        echo "  3. Set Platform to Android"
        echo "  4. Configure Build Settings (Minimum API: 28)"
        echo "  5. Build → build/NativeBrowser.apk"
        echo ""
        return 1
    fi
    
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}✓ Found APK: $APK_PATH ($APK_SIZE)${NC}"
    
    echo "Installing APK on device..."
    if adb install -r "$APK_PATH" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ APK installed successfully${NC}"
    else
        echo -e "${RED}✗ APK installation failed${NC}" >&2
        return 1
    fi
    
    echo "Launching activity..."
    if adb shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Activity launched: $PACKAGE_NAME${NC}"
    else
        echo -e "${RED}✗ Failed to launch activity${NC}" >&2
        adb shell pm list packages | grep -q "$PACKAGE_NAME" && {
            echo "Package exists but activity launch failed"
        } || {
            echo "Package not installed"
        }
        return 1
    fi
    
    echo -e "${GREEN}✓ Deploy successful${NC}"
    echo ""
    echo -e "${YELLOW}Tip: View device logs with:${NC}"
    echo "  adb logcat | grep 'NativeBrowser\|UnityEngine\|FATAL'"
    
    if [ "$SHOW_LOGCAT" = true ]; then
        echo ""
        echo -e "${YELLOW}Showing logcat (Ctrl+C to stop)...${NC}"
        adb logcat
    fi
}

main "$@"
