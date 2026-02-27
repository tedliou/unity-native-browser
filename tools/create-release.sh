#!/usr/bin/env bash
# Create a local release: build .aar, pack UPM .tgz, and optionally create
# a GitHub Release with all artifacts attached.
#
# Usage:
#   ./tools/create-release.sh              # Build artifacts only
#   ./tools/create-release.sh --publish    # Build + create GitHub Release
#   ./tools/create-release.sh --help
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PKG_ROOT="$PROJECT_ROOT/src/unity/Assets/Plugins/NativeBrowser"
AAR_SRC="$PROJECT_ROOT/src/android/app/build/outputs/aar/app-release.aar"
BUILD_DIR="$PROJECT_ROOT/build/release"

show_help() {
    cat << 'EOF'
Usage: create-release.sh [OPTIONS]

Build all release artifacts for NativeBrowser.

Artifacts:
  build/release/NativeBrowser.aar                    Android library
  build/release/com.tedliou.nativebrowser-X.Y.Z.tgz  UPM tarball
  (optional) GitHub Release with all artifacts attached

Options:
  -h, --help       Show this help message
  -p, --publish    Create GitHub Release after building (requires gh CLI)
  -d, --draft      Create as draft release (implies --publish)

Prerequisites:
  - JDK 11+ (for Gradle)
  - npm (for UPM .tgz packing)
  - gh CLI (for --publish, authenticated)
EOF
    exit 0
}

PUBLISH=false
DRAFT=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)   show_help ;;
        -p|--publish) PUBLISH=true; shift ;;
        -d|--draft)  DRAFT=true; PUBLISH=true; shift ;;
        *) echo -e "${RED}Error: Unknown option $1${NC}" >&2; show_help ;;
    esac
done

# Read version from package.json (single source of truth)
read_version() {
    local pkg="$PKG_ROOT/package.json"
    if [ ! -f "$pkg" ]; then
        echo -e "${RED}✗ package.json not found at $pkg${NC}" >&2
        exit 1
    fi
    # Extract version using simple grep + cut (no jq dependency)
    grep '"version"' "$pkg" | head -1 | cut -d'"' -f4
}

# --- Build .aar ---
build_aar() {
    echo -e "${YELLOW}Building Android .aar...${NC}"
    cd "$PROJECT_ROOT/src/android"
    ./gradlew assembleRelease --quiet || {
        echo -e "${RED}✗ Gradle build failed${NC}" >&2
        exit 1
    }
    cd "$PROJECT_ROOT"

    if [ ! -f "$AAR_SRC" ]; then
        echo -e "${RED}✗ .aar not found: $AAR_SRC${NC}" >&2
        exit 1
    fi

    # Copy .aar into package AND build output
    cp "$AAR_SRC" "$PKG_ROOT/Runtime/Plugins/Android/NativeBrowser.aar"
    cp "$AAR_SRC" "$BUILD_DIR/NativeBrowser.aar"
    echo -e "${GREEN}✓ .aar built and copied${NC}"
}

# --- Pack UPM .tgz ---
pack_tgz() {
    echo -e "${YELLOW}Packing UPM .tgz...${NC}"

    if ! command -v npm &> /dev/null; then
        echo -e "${RED}✗ npm not found. Install Node.js to pack .tgz${NC}" >&2
        exit 1
    fi

    cd "$PKG_ROOT"
    npm pack --quiet 2>/dev/null
    cd "$PROJECT_ROOT"

    # npm pack creates the file in the working directory
    local tgz
    tgz=$(ls "$PKG_ROOT"/com.tedliou.nativebrowser-*.tgz 2>/dev/null | head -1)
    if [ -z "$tgz" ]; then
        echo -e "${RED}✗ .tgz not found after npm pack${NC}" >&2
        exit 1
    fi

    mv "$tgz" "$BUILD_DIR/"
    echo -e "${GREEN}✓ $(basename "$tgz") packed${NC}"
}

# --- Publish GitHub Release ---
publish_release() {
    local version="$1"
    local tag="v$version"

    if ! command -v gh &> /dev/null; then
        echo -e "${RED}✗ gh CLI not found. Install: https://cli.github.com/${NC}" >&2
        exit 1
    fi

    echo -e "${YELLOW}Creating GitHub Release $tag...${NC}"

    local aar="$BUILD_DIR/NativeBrowser.aar"
    local tgz
    tgz=$(ls "$BUILD_DIR"/com.tedliou.nativebrowser-*.tgz 2>/dev/null | head -1)

    local draft_flag=""
    if [ "$DRAFT" = true ]; then
        draft_flag="--draft"
    fi

    local assets=()
    [ -f "$aar" ] && assets+=("$aar#NativeBrowser.aar (Android library)")
    [ -n "$tgz" ] && [ -f "$tgz" ] && assets+=("$tgz#UPM Package (.tgz)")

    cd "$PROJECT_ROOT"
    gh release create "$tag" \
        --title "NativeBrowser $tag" \
        --generate-notes \
        $draft_flag \
        "${assets[@]}"

    echo -e "${GREEN}✓ GitHub Release created: $tag${NC}"
}

# --- Main ---
main() {
    local version
    version=$(read_version)
    echo -e "${GREEN}=== NativeBrowser Release v${version} ===${NC}"

    mkdir -p "$BUILD_DIR"

    build_aar
    pack_tgz

    echo ""
    echo -e "${GREEN}Artifacts:${NC}"
    ls -lh "$BUILD_DIR"/ 2>/dev/null
    echo ""

    if [ "$PUBLISH" = true ]; then
        publish_release "$version"
    else
        echo -e "${YELLOW}Tip: Run with --publish to create a GitHub Release${NC}"
    fi
}

main "$@"
