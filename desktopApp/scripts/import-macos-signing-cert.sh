#!/usr/bin/env bash
# Import a Developer ID .p12 into an ephemeral keychain for Compose Desktop signing.
set -euo pipefail

P12_PATH="${1:?Usage: import-macos-signing-cert.sh /path/to/cert.p12 [password]}"
P12_PASSWORD="${2:-}"

KEYCHAIN="${RUNNER_TEMP:-/tmp}/build.keychain"
KEYCHAIN_PASSWORD="${KEYCHAIN_PASSWORD:-actions}"

echo "$P12_PATH" > /dev/null
security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security default-keychain -s "$KEYCHAIN"
security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security import "$P12_PATH" -k "$KEYCHAIN" -P "$P12_PASSWORD" \
  -T /usr/bin/codesign -T /usr/bin/security -T /usr/bin/productsign
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security list-keychains -s "$KEYCHAIN"

if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "MACOS_SIGNING_KEYCHAIN=$KEYCHAIN" >> "$GITHUB_ENV"
fi
echo "Imported signing certificate into $KEYCHAIN"
