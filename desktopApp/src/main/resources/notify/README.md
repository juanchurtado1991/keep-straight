# Native desktop notifications

| Platform | Mechanism |
|----------|-----------|
| macOS | Bundled `macos/KeepStraightNotify.app` (UserNotifications). Extracted at runtime to `~/Applications/KeepStraightNotify.app`. |
| Windows | PowerShell toast (no extra binary). |
| Linux | `notify-send` (libnotify). |

## Rebuild macOS helper

On a Mac with Xcode CLT:

```bash
./desktopApp/scripts/build-mac-notify.sh
```

Commit the resulting `.app` so clones get notifications without compiling Swift. Rebuild on both arm64 and Intel if you ship universal binaries (current tree is host-arch from the last build).
