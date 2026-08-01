# KeepStraight

Personal posture monitoring for **Samsung Galaxy Watch 4+** (Wear OS 3) and Android phone.

## Modules

| Module | Role |
|---|---|
| `shared` | Posture algorithm, activity heuristics, Ghost-serialized sync models |
| `wearApp` | Background monitoring, haptics, offline sync queue |
| `androidApp` | Control center, history (Room), onboarding, deep links |

## Build

Requires JDK 17+, Android SDK 36.

```bash
./gradlew :androidApp:assembleDebug :wearApp:assembleDebug :shared:testDebugUnitTest
```

APKs:

- `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- `wearApp/build/outputs/apk/debug/wearApp-debug.apk`

## Stack

- Kotlin 2.4.0, KMP `shared`, Ghost Serialization 1.3.0
- Jetpack Compose (phone + wear), Room 2.7, Wearable Data Layer
- English UI

## Usage

1. Install both apps on paired phone and watch.
2. Complete phone onboarding (pair watch, battery exemption, calibrate).
3. Watch monitors sitting posture automatically; phone stores history and controls alerts.
