# KeepStraight

**KeepStraight** helps you catch slouching while you sit. Your **Wear OS watch** tracks posture on your wrist; your **Android phone** is the control center for setup, settings, and history.

Sit up straight — we'll nudge you when you've been slouching too long.

---

## What you need

| Device | Requirement |
|--------|-------------|
| **Watch** | Samsung Galaxy Watch 4 or newer (Wear OS 3+), worn on your wrist |
| **Phone** | Android phone (API 30+), paired with your watch via Bluetooth |
| **Apps** | Install **KeepStraight** on **both** phone and watch |

KeepStraight monitors **sitting posture only**. It pauses while you walk, stand, or take the watch off.

---

## Quick start

### 1. Install both apps

Install KeepStraight on your phone and on your watch. Make sure the watch is already paired with your phone through the Galaxy Wearable app (or your system's Bluetooth settings).

### 2. Open the phone app and complete onboarding

On first launch, the phone app walks you through:

1. **Welcome** — confirms you have a compatible watch.
2. **Pair your watch** — pick your watch from the list. KeepStraight works with **one watch and one phone**.
3. **Notifications** — allow notifications so the phone can alert you too (optional but recommended).
4. **Battery** — follow the prompt to exclude KeepStraight from battery restrictions. This keeps sync reliable in the background.
5. **Calibrate** — capture your **good sitting posture** (see below).
6. **Sensitivity** — choose **Strict**, **Normal** (default), or **Relaxed**.

When onboarding finishes, monitoring starts on the watch automatically. You do **not** need to open the watch app every day.

### 3. Calibrate once, then forget about it

Calibration tells KeepStraight what *your* good posture looks like.

1. Sit the way you want to sit when working (back straight, shoulders relaxed).
2. Wear the watch on your wrist.
3. On the phone, tap **Calibrate** and follow the **3-second countdown**.
4. Hold still while the watch captures your posture.

Done. You can recalibrate anytime from the dashboard or settings if your setup changes (different chair, desk height, etc.).

---

## Daily use

### On your watch

- KeepStraight runs in the background with a small persistent notification (e.g. *Monitoring posture*).
- If you slouch for **5 minutes** while sitting, the watch alerts you with a **double vibration** (and optional flash or sound, depending on your settings).
- If you keep slouching, reminders repeat every **5 seconds** until you sit up again.
- When you stand up, walk, or remove the watch, monitoring pauses — no false alarms.

You can open the watch app to see a simple status line (*Monitoring*, *Not worn*, *Paused*, etc.).

### On your phone

The **Dashboard** is your home screen:

- **Connected** — your watch is online and synced.
- **Recalibrate** — update your baseline posture.
- **Posture monitoring** — turn detection on or off on the watch.
- **Alerts** — pause or resume nudges without stopping monitoring.

From the dashboard you can also open:

| Screen | What it does |
|--------|----------------|
| **History** | See past slump episodes and calibrations, grouped by day |
| **Alert settings** | Haptic, visual, sound (watch), and phone notification toggles |
| **Sensitivity** | Strict / Normal / Relaxed |
| **Settings** | Paired watch info, system shortcuts, unpair watch |

---

## How alerts work

1. You sit with bad posture (slumped) for **5 continuous minutes**.
2. KeepStraight sends the **first alert** on the watch (and optionally on the phone).
3. One entry is saved in **History** for that episode.
4. If you don't correct your posture within 5 seconds, reminders repeat every 5 seconds until you do.
5. When you sit up again, the episode ends.

**Do Not Disturb:** If DND is on for your watch, alerts are silenced — but KeepStraight still tracks posture. Turn DND off if you want haptics during quiet hours.

---

## Tips for best results

- **Calibrate in your real workspace** — same chair, same arm position you use when typing.
- **Keep the watch on your wrist** while sitting. On-desk or off-wrist readings are ignored.
- **Allow battery exemption** on the phone so settings and history stay in sync.
- **Use Reconnect** on the dashboard if the watch shows as disconnected after sleep or travel.
- **Recalibrate** after changing chairs, desk height, or how you wear the watch.

---

## Troubleshooting

| Problem | What to try |
|---------|-------------|
| Watch not listed during pairing | Open Galaxy Wearable / Bluetooth settings; ensure the watch is connected; tap **Refresh** in the app |
| "Connect your watch…" on toggles | Check Bluetooth; tap **Reconnect** on the dashboard |
| No alerts but monitoring is on | Check **Alerts** toggle; review Alert settings; check watch DND |
| Calibration fails | Keep watch on wrist, sit still, stay connected; try again (timeout is ~15 seconds) |
| History empty | Ensure phone and watch stayed connected; slump episodes need 5 minutes of continuous bad posture while sitting |
| Monitoring stopped after long phone disconnect | Tap **Reconnect** — after 2 hours without the phone, the watch pauses until you reconnect |

---

## Privacy

- History lives **only on your phone**. Nothing is uploaded to a cloud account in v1.
- No login required.

For the full product rules and edge cases, see [PRODUCT.md](PRODUCT.md).

---

## For developers

KeepStraight is a Kotlin Multiplatform monorepo:

| Module | Role |
|--------|------|
| `shared` | Posture algorithm, sync models, domain logic |
| `wearApp` | Wear OS monitoring, sensors, alerts, sync queue |
| `androidApp` | Phone UI, Room history, DataStore preferences, Wear sync |

**Build** (JDK 17+, Android SDK 36):

```bash
./gradlew :androidApp:assembleDebug :wearApp:assembleDebug :shared:testDebugUnitTest
```

Debug APKs:

- `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- `wearApp/build/outputs/apk/debug/wearApp-debug.apk`

**Stack:** Kotlin 2.4, KMP, Ghost Serialization 1.3.0, Jetpack Compose, Room 2.7, Wearable Data Layer.
