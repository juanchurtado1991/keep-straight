#!/usr/bin/env bash
# Rebuild KeepStraightNotify.app into desktopApp resources (macOS only).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/src/main/resources/notify/macos/KeepStraightNotify.app"
SRC="$(mktemp -t KeepStraightNotify).swift"

cleanup() { rm -f "$SRC"; }
trap cleanup EXIT

cat > "$SRC" <<'EOF'
import AppKit
import Foundation
import UserNotifications

/// Posts a native banner then exits.
/// Strategy: schedule with a short delay and quit immediately so macOS presents
/// the notification while no KeepStraightNotify process is in the foreground
/// (foreground deliveries require willPresent and are easy to drop).
final class NotifyApp: NSObject, NSApplicationDelegate, NSUserNotificationCenterDelegate {
    private let title: String
    private let body: String
    private var finished = false

    init(title: String, body: String) {
        self.title = title
        self.body = body
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
        // Legacy path (still delivers banners on many macOS builds).
        let legacy = NSUserNotification()
        legacy.title = title
        legacy.informativeText = body
        legacy.soundName = NSUserNotificationDefaultSoundName
        NSUserNotificationCenter.default.delegate = self
        NSUserNotificationCenter.default.deliver(legacy)

        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { [weak self] settings in
            guard let self else { return }
            DispatchQueue.main.async {
                self.handleUN(settings: settings)
            }
        }
    }

    private func handleUN(settings: UNNotificationSettings) {
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            scheduleUN()
        case .denied:
            // Legacy may still have shown something; don't hard-fail the whole alert.
            succeedAfterHold()
        case .notDetermined:
            NSApp.setActivationPolicy(.regular)
            NSApp.activate(ignoringOtherApps: true)
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { [weak self] _, _ in
                DispatchQueue.main.async {
                    NSApp.setActivationPolicy(.accessory)
                    self?.scheduleUN()
                }
            }
        @unknown default:
            succeedAfterHold()
        }
    }

    private func scheduleUN() {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        // Fire shortly AFTER we quit so the system shows a real banner.
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1.0, repeats: false)
        let request = UNNotificationRequest(
            identifier: "keepstraight-\(UUID().uuidString)",
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request) { [weak self] error in
            if let error {
                fputs("deliver: \(error.localizedDescription)\n", stderr)
            }
            // Quit quickly; notification fires ~1s later in background.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                self?.succeed()
            }
        }
    }

    func userNotificationCenter(_ center: NSUserNotificationCenter, shouldPresent notification: NSUserNotification) -> Bool {
        true
    }

    private func succeedAfterHold() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            self?.succeed()
        }
    }

    private func succeed() {
        guard !finished else { return }
        finished = true
        fputs("ok\n", stdout)
        fflush(stdout)
        NSApp.terminate(nil)
    }
}

let args = CommandLine.arguments
guard args.count >= 3 else {
    fputs("usage: KeepStraightNotify <title> <body>\n", stderr)
    exit(2)
}

let app = NSApplication.shared
let delegate = NotifyApp(title: args[1], body: args[2])
app.delegate = delegate
app.run()
EOF

rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS"

swiftc -O -framework AppKit -framework UserNotifications \
  -o "$APP/Contents/MacOS/KeepStraightNotify" "$SRC"

cat > "$APP/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CFBundleExecutable</key>
	<string>KeepStraightNotify</string>
	<key>CFBundleIdentifier</key>
	<string>com.keepstraight.notify</string>
	<key>CFBundleName</key>
	<string>KeepStraight</string>
	<key>CFBundleDisplayName</key>
	<string>KeepStraight</string>
	<key>CFBundlePackageType</key>
	<string>APPL</string>
	<key>CFBundleShortVersionString</key>
	<string>1.4</string>
	<key>CFBundleVersion</key>
	<string>5</string>
	<key>LSMinimumSystemVersion</key>
	<string>13.0</string>
	<key>LSUIElement</key>
	<true/>
	<key>NSUserNotificationAlertStyle</key>
	<string>alert</string>
</dict>
</plist>
PLIST

chmod +x "$APP/Contents/MacOS/KeepStraightNotify"
codesign --force --deep --sign - "$APP"
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f "$APP" 2>/dev/null || true
rm -rf "$HOME/Library/Application Support/KeepStraight/KeepStraightNotify.app"
rm -rf "$HOME/Applications/KeepStraightNotify.app"

echo "Built $APP"
file "$APP/Contents/MacOS/KeepStraightNotify"
