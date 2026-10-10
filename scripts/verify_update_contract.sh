#!/usr/bin/env bash
set -euo pipefail

NEW_APK="${1:?new apk required}"
BASELINE_APK="${2:-}"
EXPECTED_PACKAGE="com.otaviobarreto.pokedex"
AAPT="${ANDROID_HOME}/build-tools/35.0.0/aapt"
APKSIGNER="${ANDROID_HOME}/build-tools/35.0.0/apksigner"

EXPECTED_VERSION_CODE="$(grep '^versionCode=' ci/release.properties | cut -d= -f2)"
EXPECTED_VERSION_NAME="$(grep '^versionName=' ci/release.properties | cut -d= -f2)"

package_name() {
  "$AAPT" dump badging "$1" | sed -n "s/package: name='\([^']*\)'.*/\1/p" | head -1
}

version_code() {
  "$AAPT" dump badging "$1" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" | head -1
}

version_name() {
  "$AAPT" dump badging "$1" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1
}

cert_sha256() {
  "$APKSIGNER" verify --print-certs "$1" |
    sed -n 's/^Signer #1 certificate SHA-256 digest: //p' |
    head -1 |
    tr '[:upper:]' '[:lower:]'
}

"$APKSIGNER" verify --verbose --print-certs "$NEW_APK" >/dev/null

PACKAGE="$(package_name "$NEW_APK")"
NEW_CODE="$(version_code "$NEW_APK")"
NEW_NAME="$(version_name "$NEW_APK")"
NEW_CERT="$(cert_sha256 "$NEW_APK")"

[[ "$PACKAGE" == "$EXPECTED_PACKAGE" ]] || { echo "Unexpected package: $PACKAGE"; exit 1; }
[[ "$NEW_CODE" == "$EXPECTED_VERSION_CODE" ]] || { echo "Unexpected versionCode: $NEW_CODE"; exit 1; }
[[ "$NEW_NAME" == "$EXPECTED_VERSION_NAME" ]] || { echo "Unexpected versionName: $NEW_NAME"; exit 1; }
[[ -n "$NEW_CERT" ]] || { echo "Unable to read candidate signing certificate"; exit 1; }

if [[ -n "$BASELINE_APK" ]]; then
  "$APKSIGNER" verify --verbose --print-certs "$BASELINE_APK" >/dev/null
  BASE_PACKAGE="$(package_name "$BASELINE_APK")"
  BASE_CODE="$(version_code "$BASELINE_APK")"
  BASE_CERT="$(cert_sha256 "$BASELINE_APK")"

  [[ "$BASE_PACKAGE" == "$EXPECTED_PACKAGE" ]] || { echo "Unexpected baseline package: $BASE_PACKAGE"; exit 1; }
  [[ "$NEW_CODE" -gt "$BASE_CODE" ]] || { echo "Candidate versionCode $NEW_CODE must be greater than baseline $BASE_CODE"; exit 1; }
  [[ "$NEW_CERT" == "$BASE_CERT" ]] || { echo "Signing certificate mismatch between baseline and candidate"; exit 1; }
fi

echo "Update contract OK: $PACKAGE v$NEW_NAME ($NEW_CODE)"
