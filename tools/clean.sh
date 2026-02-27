#!/usr/bin/env bash
# Clean build artifacts and cached files
set -euo pipefail

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Script directory for relative paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Help function
show_help() {
    cat << 'EOF'
Usage: clean.sh [OPTIONS]

Clean build artifacts and intermediate files.

Cleans by default:
  - Android build/ directories
  - Copied .aar from Unity Plugins/

Options:
  -h, --help       Show this help message
  --unity          Also clean Unity Library/ cache (slow reimport after)
  
Example:
  ./tools/clean.sh           # Clean Android builds + .aar copy
  ./tools/clean.sh --unity   # Also clean Unity cache (warns first)
  
WARNING:
  --unity flag deletes Unity's 10GB+ Library cache.
  Reimporting can take 10+ minutes on first run.
EOF
    exit 0
}

# Parse arguments
CLEAN_UNITY=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            ;;
        --unity)
            CLEAN_UNITY=true
            shift
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}" >&2
            show_help
            ;;
    esac
done

# Confirm before deleting Unity Library
confirm_unity_clean() {
    echo -e "${RED}WARNING: --unity flag will delete entire Unity Library cache${NC}"
    echo -e "${YELLOW}  This will cause Unity to reimport all assets on next load${NC}"
    echo -e "${YELLOW}  Reimporting can take 10+ minutes${NC}"
    echo ""
    read -p "Press Enter to continue or Ctrl+C to cancel: "
    echo "Proceeding with full clean..."
}

# Main clean flow
main() {
    echo -e "${GREEN}=== Clean Build Artifacts ===${NC}"
    
    CLEANED_ITEMS=0
    
    # Clean Android builds
    echo "Cleaning Android builds..."
    
    ANDROID_BUILD_DIR="$PROJECT_ROOT/src/android/app/build"
    if [ -d "$ANDROID_BUILD_DIR" ]; then
        echo -e "${YELLOW}  Removing: $ANDROID_BUILD_DIR${NC}"
        rm -rf "$ANDROID_BUILD_DIR"
        ((CLEANED_ITEMS++))
    fi
    
    ANDROID_GRADLE_BUILD="$PROJECT_ROOT/src/android/build"
    if [ -d "$ANDROID_GRADLE_BUILD" ]; then
        echo -e "${YELLOW}  Removing: $ANDROID_GRADLE_BUILD${NC}"
        rm -rf "$ANDROID_GRADLE_BUILD"
        ((CLEANED_ITEMS++))
    fi
    
    # Clean copied .aar
    echo "Cleaning Unity plugins..."
    
    AAR_COPY="$PROJECT_ROOT/src/unity/Assets/Plugins/Android/NativeBrowser.aar"
    if [ -f "$AAR_COPY" ]; then
        echo -e "${YELLOW}  Removing: $AAR_COPY${NC}"
        rm -f "$AAR_COPY"
        ((CLEANED_ITEMS++))
    fi
    
    # Optional: Clean Unity Library
    if [ "$CLEAN_UNITY" = true ]; then
        confirm_unity_clean
        
        UNITY_LIBRARY="$PROJECT_ROOT/src/unity/Library"
        if [ -d "$UNITY_LIBRARY" ]; then
            LIBRARY_SIZE=$(du -sh "$UNITY_LIBRARY" 2>/dev/null | cut -f1 || echo "unknown")
            echo -e "${YELLOW}  Removing Unity Library ($LIBRARY_SIZE)...${NC}"
            rm -rf "$UNITY_LIBRARY"
            ((CLEANED_ITEMS++))
            echo -e "${YELLOW}  ⚠ Next Unity load will reimport all assets (10+ min)${NC}"
        fi
    fi
    
    if [ $CLEANED_ITEMS -eq 0 ]; then
        echo -e "${YELLOW}⚠ Nothing to clean${NC}"
        return 0
    fi
    
    echo -e "${GREEN}✓ Cleaned $CLEANED_ITEMS item(s)${NC}"
}

main "$@"
