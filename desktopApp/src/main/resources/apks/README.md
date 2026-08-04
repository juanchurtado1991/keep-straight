# Bundled APKs (sideload installer)

You don't need to copy APKs here by hand. `:desktopApp:processResources` (which `run` and every
`package*` task depend on) runs `:desktopApp:syncCompanionApks`, which builds
`:androidApp:assembleRelease` + `:wearApp:assembleRelease` and stages them as
`build/companionApks/apks/keepstraight-phone.apk` and `.../keepstraight-wear.apk`.
That directory is registered as an extra resource root, so the wireless installer finds them on
the classpath at runtime.

To skip the Android builds while iterating on desktop-only code:

```bash
./gradlew :desktopApp:run -Pkeepstraight.skipApkSync=true
```

Both modules are signed with the local debug keystore (`sideload` signing config) so the release
APKs are installable over adb and share one certificate — the Wear Data Layer only delivers
between apps with the same package name and signature.

Place platform-tools under `adb/windows`, `adb/macos`, `adb/linux`
(see project NOTICE for Android SDK platform-tools license).
