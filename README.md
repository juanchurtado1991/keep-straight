# KeepStraight

**KeepStraight** helps you catch slouching while you sit. Your **computer webcam** measures posture (offline pose AI); your **Android phone** is the control center for settings and history; your **Wear OS watch** vibrates when alerts are forwarded (phase 2).

Sit up straight — we'll nudge you when you've been slouching too long.

---

## What you need

| Device | Requirement |
|--------|-------------|
| **Computer** | macOS, Windows, or Linux with a webcam |
| **Phone** | Android phone (API 30+) |
| **Watch** | Samsung Galaxy Watch 4 or newer (Wear OS 3+), for haptics |
| **Apps** | KeepStraight **desktop** + **phone** (+ **watch** for wear alerts) |

KeepStraight monitors **sitting posture only**. It pauses while you stand, leave the frame, or when the camera cannot see you clearly.

---

## Quick start

### 1. Desktop companion

```bash
# Download MoveNet Lightning ONNX (once)
./desktopApp/scripts/download-movenet.sh

# Run
./gradlew :desktopApp:run
```

1. Accept the camera consent (“Frames are not saved”).
2. **Calibrate erect**, then **Calibrate slumped**.
3. Tap **Start**. Close the window to keep running in the tray; use **Quit** to release the camera.

**Camera access (all platforms):** the first run must allow the webcam.

| OS | What to check |
|----|----------------|
| **macOS** | System Settings → Privacy & Security → Camera → enable Terminal / IDE / KeepStraight |
| **Windows** | Settings → Privacy & security → Camera → allow desktop apps; close Zoom/Teams if the cam is busy |
| **Linux** | V4L2 device present (`ls /dev/video*`); user in the `video` group if needed |

Fully quit and reopen after changing permissions. KeepStraight uses native drivers (AVFoundation / Media Foundation+DirectShow / V4L2), not the old BridJ stack.

### CI desktop installers

Every **push to `main`**, pull request, or manual Actions run builds fresh **Windows (EXE/MSI)**, **macOS (DMG)**, and **Linux (DEB)** packages. Each installer bundles **MoveNet**, **adb** for that OS, and the **phone + watch APKs** so the desktop wizard can wirelessly sideload companions.

- **Actions → Artifacts** — `KeepStraight-windows` / `macos` / `linux` for that run  
- **Release [`ci-latest`](../../releases/tag/ci-latest)** — always overwritten with the newest same-repo build (easy fixed download links)

### 2. Phone + watch

Install KeepStraight on phone and watch (`applicationId` must be `com.keepstraight` on both). Pair the watch over Bluetooth (Galaxy Wearable).

On the phone: set **Sensitivity** and alert timing. Open **Settings → Desktop companion → Scan desktop QR**.

### 3. Phase 2 bridge (optional)

On the desktop tap **Show QR to pair**. On the phone (same Wi‑Fi), scan that QR. Slump alerts then sync to phone history and watch haptics. The watch does **not** need to be on that Wi‑Fi.

---

## How alerts work

1. While sitting, if your pose looks like the slumped calibration long enough (default **5 minutes**), the **desktop** alerts (beep / notification).
2. With the LAN bridge, the phone records history and can vibrate the watch.
3. Standing / Away / low confidence **pauses** timers (no false slump alerts).

---

## Privacy

- Pose runs **100% offline** on your PC. Frames are not saved.
- History lives on your phone. No cloud account.
- A privacy LED may stay on while the camera is open.

---

## For developers

Kotlin Multiplatform monorepo:

| Module | Role |
|--------|------|
| `shared` | Landmark scorer, presence, desktop session, vision JVM actuals, LAN DTOs |
| `desktopApp` | Compose Desktop companion (webcam + ONNX) |
| `androidApp` | Phone UI (One UI–inspired), Room history, LAN ingest |
| `wearApp` | Wear haptics / sync (wrist slump detection disabled) |

**Build** (JDK 17+, Android SDK 36):

```bash
./gradlew :androidApp:assembleDebug :wearApp:assembleDebug :shared:jvmTest :desktopApp:compileKotlin
```

**Run desktop:**

```bash
./desktopApp/scripts/download-movenet.sh
./gradlew :desktopApp:run
```

**Stack:** Kotlin 2.4, KMP, Compose Multiplatform Desktop, ONNX Runtime, webcam-capture, Ghost Serialization, Room, Wearable Data Layer, Ktor CIO (LAN).
