#!/usr/bin/env bash
# Vendors official Android platform-tools adb binaries for desktop sideload.
# macOS / Windows / Linux → desktopApp/src/main/resources/adb/{macos,windows,linux}/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADB_ROOT="$ROOT/src/main/resources/adb"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

download_and_extract() {
  local url="$1"
  local zip="$TMP/pt.zip"
  curl -fsSL -o "$zip" "$url"
  unzip -q -o "$zip" -d "$TMP/extract"
}

echo "Fetching platform-tools (Windows)…"
rm -rf "$TMP/extract"
download_and_extract "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
mkdir -p "$ADB_ROOT/windows"
cp "$TMP/extract/platform-tools/adb.exe" \
  "$TMP/extract/platform-tools/AdbWinApi.dll" \
  "$TMP/extract/platform-tools/AdbWinUsbApi.dll" \
  "$ADB_ROOT/windows/"

echo "Fetching platform-tools (Linux)…"
rm -rf "$TMP/extract"
download_and_extract "https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
mkdir -p "$ADB_ROOT/linux"
cp "$TMP/extract/platform-tools/adb" "$ADB_ROOT/linux/"
chmod +x "$ADB_ROOT/linux/adb"
cp "$TMP/extract/platform-tools/NOTICE.txt" "$ADB_ROOT/NOTICE.txt"

echo "Fetching platform-tools (macOS)…"
rm -rf "$TMP/extract"
download_and_extract "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
mkdir -p "$ADB_ROOT/macos"
cp "$TMP/extract/platform-tools/adb" "$ADB_ROOT/macos/"
chmod +x "$ADB_ROOT/macos/adb"

echo "Done:"
find "$ADB_ROOT" -type f | sort
