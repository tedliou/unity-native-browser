#!/usr/bin/env bash
# Build Android .aar release and copy to Unity
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
Usage: build.sh [OPTIONS]

Build Android native browser plugin (.aar) for Unity.

Options:
  -h, --help      Show this help message
  -d, --debug     Build debug .aar instead of release
  
Output:
  .aar file → src/unity/Assets/Plugins/Android/NativeBrowser.aar
  
Example:
  ./tools/build.sh           # Release build
  ./tools/build.sh --debug   # Debug build
EOF
    exit 0
}

# Parse arguments
BUILD_TYPE="release"
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            ;;
        -d|--debug)
            BUILD_TYPE="debug"
            shift
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}" >&2
            show_help
            ;;
    esac
done

# Check prerequisites
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ Java not found. Please install JDK 11+${NC}" >&2
        return 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo -e "${YELLOW}  Java: $JAVA_VERSION${NC}"
}

# Attempt to detect or verify JAVA_HOME
check_java_home() {
    if [ -z "${JAVA_HOME:-}" ]; then
        echo -e "${YELLOW}⚠ JAVA_HOME not set, attempting to detect...${NC}"
        if command -v java &> /dev/null; then
            JAVA_BIN=$(command -v java)
            JAVA_HOME=$(cd "$(dirname "$JAVA_BIN")" && pwd)
            JAVA_HOME="${JAVA_HOME%/bin}"
            export JAVA_HOME
            echo -e "${GREEN}  Detected JAVA_HOME: $JAVA_HOME${NC}"
        else
            echo -e "${RED}✗ Cannot detect JAVA_HOME and java not in PATH${NC}" >&2
            return 1
        fi
    else
        echo -e "${YELLOW}  JAVA_HOME: $JAVA_HOME${NC}"
    fi
}

# Main build flow
main() {
    echo -e "${GREEN}=== Android Browser Plugin Build ===${NC}"
    
    echo "Checking prerequisites..."
    check_java || return 1
    check_java_home || return 1
    
    cd "$PROJECT_ROOT/src/android"
    
    echo "Building ${BUILD_TYPE} .aar..."
    if [ "$BUILD_TYPE" = "debug" ]; then
        ./gradlew assembleDebug > /dev/null 2>&1 || {
            echo -e "${RED}✗ Build failed${NC}" >&2
            return 1
        }
        AAR_FILE="app/build/outputs/aar/app-debug.aar"
    else
        ./gradlew assembleRelease > /dev/null 2>&1 || {
            echo -e "${RED}✗ Build failed${NC}" >&2
            return 1
        }
        AAR_FILE="app/build/outputs/aar/app-release.aar"
    fi
    
    if [ ! -f "$AAR_FILE" ]; then
        echo -e "${RED}✗ .aar file not found: $AAR_FILE${NC}" >&2
        return 1
    fi
    
    AAR_SIZE=$(du -h "$AAR_FILE" | cut -f1)
    echo -e "${GREEN}✓ Built: $AAR_FILE ($AAR_SIZE)${NC}"
    
    echo "Copying .aar to Unity..."
    cd "$PROJECT_ROOT"
    bash "$SCRIPT_DIR/copy-aar.sh" || {
        echo -e "${RED}✗ Copy failed${NC}" >&2
        return 1
    }
    
    echo -e "${GREEN}✓ Build successful${NC}"
    echo -e "${GREEN}  Output: src/unity/Assets/Plugins/Android/NativeBrowser.aar${NC}"
}

main "$@"
