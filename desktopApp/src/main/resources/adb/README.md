# Bundled `adb` (Android SDK Platform-Tools)

KeepStraight ships a minimal `adb` per OS so the desktop wizard can wirelessly
install the phone and watch apps without requiring Android Studio.

| Path | Contents |
|------|----------|
| `macos/adb` | Darwin universal binary |
| `linux/adb` | Linux x86_64 |
| `windows/adb.exe` + `AdbWinApi.dll` + `AdbWinUsbApi.dll` | Windows |

Refresh from Google’s official zips:

```bash
./desktopApp/scripts/download-adb.sh
```

See `NOTICE.txt` for the Android SDK Platform-Tools license.
