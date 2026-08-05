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

At package time, Gradle picks the adb folder for the **packaging task** (`packageMsi` → `windows/`, `packageDmg` → `macos/`, `packageDeb` → `linux/`). When you run `:desktopApp:run` or compile on the host, the host OS folder is used.

Override manually with `-PadbPlatform=windows|macos|linux` when building several installers in one invocation.

## Companion APKs

Phone and wear release APKs are staged into every desktop build for wireless sideload (intentional). Trim size with:

- `-Pkeepstraight.companionApks=phone` — phone APK only
- `-Pkeepstraight.companionApks=wear` — wear APK only
- `-Pkeepstraight.skipApkSync=true` — skip Android builds during desktop-only iteration

See `NOTICE.txt` for the Android SDK Platform-Tools license.
