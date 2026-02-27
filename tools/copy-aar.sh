#!/bin/bash
# Copy Android .aar to Unity Plugins directory
set -e

SRC="src/android/app/build/outputs/aar/app-release.aar"
DST="src/unity/Assets/Plugins/Android/NativeBrowser.aar"

if [ ! -f "$SRC" ]; then
    echo "Error: Source .aar not found: $SRC"
    echo "Run './gradlew assembleRelease' in src/android/ first"
    exit 1
fi

cp "$SRC" "$DST"
echo "✓ Copied $SRC → $DST"
