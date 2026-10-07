#!/usr/bin/env bash
set -euo pipefail
NEW_APK="${1:?new apk required}"
EXPECTED_PACKAGE="com.otaviobarreto.pokedex"
AAPT="${ANDROID_HOME}/build-tools/35.0.0/aapt"
APKSIGNER="${ANDROID_HOME}/build-tools/35.0.0/apksigner"

PACKAGE="$("$AAPT" dump badging "$NEW_APK" | sed -n "s/package: name='\([^']*\)'.*/\1/p" | head -1)"
[[ "$PACKAGE" == "$EXPECTED_PACKAGE" ]] || { echo "Unexpected package: $PACKAGE"; exit 1; }
"$APKSIGNER" verify --verbose --print-certs "$NEW_APK"
echo "Update contract OK: $PACKAGE"
