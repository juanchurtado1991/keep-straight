#!/usr/bin/env bash
# Clears KeepStraight desktop Java preferences (wizard, camera consent, bridge token,
# calibration, alert settings) so the next launch behaves like a fresh install.
set -euo pipefail

NODE="com.keepstraight.desktop"

case "$(uname -s)" in
  Darwin)
    PLIST="${HOME}/Library/Preferences/com.apple.java.util.prefs.plist"
    if [[ ! -f "$PLIST" ]]; then
      echo "No prefs plist at $PLIST (already clean)"
      exit 0
    fi
    # The plist is shared with other JVM apps (JetBrains, Google), so drop only our node.
    python3 - "$PLIST" "$NODE" <<'PY'
import plistlib
import sys

path, node = sys.argv[1], sys.argv[2]
with open(path, "rb") as handle:
    data = plistlib.load(handle)

root = data.get("/", {})
removed = [key for key in list(root) if key.rstrip("/") == node]
for key in removed:
    del root[key]

if removed:
    with open(path, "wb") as handle:
        plistlib.dump(data, handle)
    print(f"Cleared {node} from {path}")
else:
    print(f"No {node} node in {path} (already clean)")
PY
    # cfprefsd caches the domain; without this the JVM can read back the old values.
    killall -u "$(id -un)" cfprefsd 2>/dev/null || true
    ;;
  *)
    PREFS="${HOME}/.java/.userPrefs/com/keepstraight"
    if [[ -d "$PREFS" ]]; then
      rm -rf "$PREFS"
      echo "Cleared $PREFS"
    else
      echo "No prefs at $PREFS (already clean)"
    fi
    ;;
esac
