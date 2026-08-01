# KeepStraight — Product Specification

**This document is the single source of truth** for KeepStraight v1. Implementation (`shared`, `androidApp`, `wearApp`) must converge to match it. Where the codebase diverges today, see [§15 Code parity gaps](#15-code-parity-gaps).

User-visible behavior and product rules defined here **must not** ship differently without updating this document first.

---

## 1. Product overview

**KeepStraight** monitors **sitting posture** using a **Wear OS watch** on the wrist and a **required Android phone companion app**.

| Role | Device | Responsibility |
|------|--------|----------------|
| Sensor & alerts | Wear OS watch (Galaxy Watch 4+ baseline) | IMU sampling, slump detection, haptics/visual/sound alerts, offline sync queue |
| Control & history | Android phone | Pairing, calibration UI, settings, unlimited history, phone notifications |

**Language:** English only (UI copy, history labels, notifications).

**Pairing model:** Exactly **one watch ↔ one phone**. The phone app stores a single `pairedWatchId` (and optional `PairedDeviceInfo`) and **ignores all messages** from any other Wear node.

**Cloud:** None in v1. All history stays on the phone locally.

**History export:** Not in v1.

**Design intent:** Minimalist, clean UI on phone (Material 3) and watch (single status line). Reliable posture detection with **no false positives from typing** or brief movements — only sustained slump while classified as sitting.

---

## 2. Problem statement

Users who sit for long periods often **slump** without noticing. KeepStraight detects sustained bad posture while **seated**, alerts the user on the watch (and optionally on the phone), and records episodes on the phone for review.

KeepStraight **only** targets **sitting** posture. It does **not** monitor standing desk posture, walking, or sleep.

---

## 3. Hardware & platform requirements

### 3.1 Watch (minimum baseline)

**Samsung Galaxy Watch 4** (40 mm / 44 mm), Wear OS 3+, API 30+ (`minSdk 30`).

Required capabilities:

| Capability | Requirement |
|------------|-------------|
| Accelerometer | ~2 Hz effective (`SENSOR_DELAY_NORMAL`, 500 ms service tick + events) |
| Step counter | `TYPE_STEP_COUNTER` |
| Off-body detect | Samsung `TYPE_LOW_LATENCY_OFFBODY_DETECT` primary; software fallback if missing |
| Foreground service | `FOREGROUND_SERVICE_HEALTH` |
| Haptic | `VibrationEffect` double pulse |
| Sync | Wearable Data Layer to paired phone |
| Display | Round, small; optional 1-line status UI |

**Watch UI baseline (GW4):** centered content, **max 14 sp** status text, safe-area margins for round screen. No complications or tiles in v1.

**Speaker:** Limited on GW4. Sound alerts are **best-effort**; haptic + visual are primary.

### 3.2 Phone

- Android API 30+ (`minSdk 30`), `compileSdk 36`, `targetSdk 35`
- Bluetooth pairing with watch via system / Galaxy Wearable
- Google Play services (Wearable API)
- Manifest permission: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for direct exemption intent
- Battery optimization **exempt** recommended for reliable background sync

### 3.3 QA gate (mandatory before v1 release)

Full manual verification on **Galaxy Watch 4 physical device** or **Wear OS Round API 30 emulator** profiled for GW4:

- Boot auto-start, off-wrist pause/resume, slump + 5 s alert loop, sync to phone, sensitivity change, 2 h disconnect + Reconnect, battery deep link, DND suppression, round UI readable.

### 3.4 Future platforms (out of v1 scope)

Architecture in `shared` prepares for iOS / watchOS, but v1 ships **Android phone + Wear OS watch only**.

---

## 4. Core concepts

### 4.1 Calibration (baseline posture)

Calibration captures the IMU **pitch** and **roll** of the user's **good sitting posture** while wearing the watch.

| Rule | Behavior |
|------|----------|
| First calibration | Mandatory during phone onboarding before monitoring starts |
| Recalibration | Always available from **Dashboard hero card** and **Settings → Recalibrate Posture** |
| Capture location | **Watch** averages IMU 2–3 s after phone countdown |
| Capture UI | **Phone**: 3 s countdown (`Hold still… N`), then “Capturing…” |
| During capture | Posture evaluation pauses briefly (~3 s); monitoring service stays alive |
| On success | Baseline syncs to watch; slump state + alert loop cleared; history logs `CALIBRATED`; toast *“Posture recalibrated”* |
| On failure | Timeout (~15 s) or disconnect → error; **previous calibration retained** |
| Scope | Recalibration changes only `basePitch` / `baseRoll` — not sensitivity, alert prefs, or monitoring toggles |
| Connection | Requires paired watch **connected**; otherwise show *“Connect your watch to recalibrate”* |

**Recalibrate subtitle (dashboard):** *“Use after changing chair, desk, or sitting position.”*

### 4.2 Sensitivity presets

Phone setting maps to angular tolerances synced in `PostureCalibrationConfig`:

| Preset | Slump tolerance (°) | Standing pitch delta (°) | Standing roll delta (°) |
|--------|---------------------|--------------------------|-------------------------|
| Strict | 7 | 15 | 10 |
| Normal | 10 | 18 | 12 |
| Relaxed | 15 | 22 | 15 |

Changing sensitivity updates DataStore and re-syncs config to watch (**same** baseline angles; no re-calibration required).

UI: segmented control **Strict | Normal | Relaxed** with short English explanation per preset. Reachable from Dashboard navigation and **Settings → Sensitivity**.

### 4.3 Activity classification (watch)

Multi-signal fusion with **hysteresis** — no single-sample transitions. Pitch/roll smoothed with a **fixed 5-sample circular buffer** before comparison.

#### Input signals

| Signal | Source | Sitting | Walking | Standing |
|--------|--------|---------|---------|----------|
| Step delta | Step counter, 10 s window | ~0 | ≥ 3 steps | ~0 |
| Pitch vs baseline | Accelerometer vs `basePitch` | within slump + standing margin | N/A | deviates > standing pitch delta sustained |
| Roll vs baseline | vs `baseRoll` | within range | variable | arm-at-side: roll shift > standing roll delta |
| Gravity magnitude | Accelerometer vector length | stable ~1 g band | unstable | stable but angle wrong |
| Vertical wrist pose | Dominant `az` axis | forearm ~horizontal (desk) | oscillating | arm ~vertical (hanging) |

#### State machine

```
SITTING   → WALKING     if stepDelta >= 3 in 10 s window
SITTING   → STANDING    if !walking AND (pitchDelta > standingPitch OR rollDelta > standingRoll) for >= 30 s
STANDING  → SITTING     if angles return within sitting band for >= 15 s
WALKING   → SITTING     if stepDelta == 0 for >= 20 s AND sitting angles restored
AMBIGUOUS → (not sitting for slump)  transitional / standing candidate < 30 s
NOT_WORN  → forced when off-wrist detected
```

| State | Posture monitoring |
|-------|-------------------|
| `SITTING` | **Active** — slump timer runs |
| `WALKING` | Paused — analyzer reset |
| `STANDING` | Paused — analyzer reset |
| `NOT_WORN` | Paused — sensors may stop |
| `AMBIGUOUS` | Treated as **not sitting** — no slump alerts |

**Slump vs standing thresholds:** Slump uses **tighter** angular tolerance than standing detection so a user can accumulate 5 min of slump while still classified as `SITTING`.

### 4.4 Slump detection (watch)

While `activityState == SITTING` and monitoring is enabled:

1. Compare smoothed pitch/roll to baseline using **slump tolerance** for active sensitivity preset.
2. **Bad posture** (pitch or roll beyond tolerance): start or continue slump timer.
3. **Good posture:** reset slump timer and active slump episode.
4. Bad posture continuous for **`slumpDurationThresholdMs` (default 5 min / 300_000 ms)**:
   - Fire **initial alert** (channels per `AlertPreferences`, respecting DND).
   - Emit **one** `SLUMP_DETECTED` to phone with `durationSeconds`.
5. If not corrected within **5 s** after initial alert:
   - Repeat alert every **`repeatAlertIntervalMs` (default 5_000 ms)** until corrected.
   - Repeat alerts **do not** create additional history rows.

Episode ends when posture corrected or user walks, stands, or removes watch.

### 4.5 Anti false-positive design

KeepStraight must **not** alert on typing, brief fidgets, or momentary slouch:

- **5-minute sustained** bad posture required before first alert.
- Activity classifier pauses slump evaluation when not `SITTING`.
- **Sustained transition windows** (15–30 s) for standing/walking — not single samples.
- **5-sample smoothing** on pitch/roll before angle comparison.
- **AMBIGUOUS** states never accumulate slump time.
- Off-wrist forces `NOT_WORN` — no desk/charger false slumps.

### 4.6 Do Not Disturb

If watch system DND is active (interruption filter ≠ ALL):

- Alerts **suppressed** (no haptic, flash, sound).
- Monitoring and slump tracking **continue**.
- Watch UI may show `DND_ACTIVE`.

---

## 5. Watch application behavior

### 5.1 Auto-start & lifecycle

- **Boot:** if `monitoring_enabled` in watch prefs → start `PostureMonitoringService` automatically.
- **Wear again:** when user puts watch back on wrist after off-body → resume prior state (`ACTIVE`, `PHONE_RETRY`, etc.) automatically.
- User **never** needs to open the watch app for monitoring.
- Optional watch app: **one status line** (*Monitoring*, *Not worn*, *Standing*, *Paused*, *Phone disconnected*) + white flash overlay on visual alerts when foreground.

### 5.2 Foreground service

- Persistent notification (*Monitoring posture* or state-specific text, e.g. *Watch not worn — monitoring paused*).
- Samples accelerometer + step counter on **~500 ms** tick.
- Processes through `PostureMonitoringEngine` in `shared`.
- On service start: **pre-create** haptic waveform object (zero allocation on alert hot path).

### 5.3 Off-wrist behavior

**Primary (hardware):** `TYPE_LOW_LATENCY_OFFBODY_DETECT` — `0` = off body, `1` = on wrist.

**Software fallback** when hardware sensor unavailable:

- Acceleration variance near-zero for **> 60 s** with no step motion → likely on desk/charger.
- Sudden stable face-up flat orientation + lack of wrist micro-movements.
- Magnitude variance threshold combined with gravity vector checks.

**When off-wrist:**

- State → `NOT_WORN`.
- **Stop** accelerometer/step listeners (save battery).
- No posture evaluation, no alerts.
- Notification: *Watch not worn — monitoring paused*.

**When on-wrist again:** resume sensors and prior monitoring state automatically.

### 5.4 Watch monitoring states (user-visible)

| State | When | Alerts | Sensors |
|-------|------|--------|---------|
| `ACTIVE` | On wrist, sitting | On | On |
| `NOT_SITTING` | Walking or standing | Off | On |
| `NOT_WORN` | Off body | Off | **Off** |
| `ALERTS_PAUSED` | Phone sent `PAUSE_ALERTS` | Off | On |
| `ALGORITHM_OFF` | Phone sent `STOP_ALGORITHM` | Off | Off |
| `PHONE_RETRY` | Phone unreachable, within 2 h window | On | On |
| `PHONE_DISCONNECTED_PAUSED` | Phone unreachable ≥ 2 h | Off | Off |
| `DND_ACTIVE` | System DND | Suppressed | On |

### 5.5 Alerts (watch)

Synced from phone via `AlertPreferences`:

| Channel | Default | Behavior |
|---------|---------|----------|
| Haptic | On | **Double pulse:** 120 ms on → 80 ms off → 120 ms on; **low–medium amplitude**; waveform pre-created at service start |
| Visual | On | Broadcast flash intent; white full-screen overlay if `MainActivity` visible |
| Sound | Off | Default notification ringtone; **stop previous** ringtone before playing new; graceful no-op if no speaker |

Master **Alerts** toggle on phone → `PAUSE_ALERTS` / `RESUME_ALERTS` on watch (algorithm keeps running).

### 5.6 Phone connection policy (watch side)

Phone is **required** for history and settings sync.

If watch cannot deliver events to phone:

1. Enqueue in **`PendingSyncQueue`** file `pending_sync.bin` (survives watch reboot).
2. Store **all** undelivered events during the retry window (no history on watch — queue only).
3. Start **ConnectionRetryManager**: alarm every **15 minutes**.
4. Retry for up to **2 hours** from first failure → state `PHONE_RETRY`; monitoring/alerts continue.
5. After 2 h exhausted → `PHONE_DISCONNECTED_PAUSED`; monitoring stops until phone sends `RESUME_CONNECTION` via **Reconnect**.

On successful sync: flush queue to phone (batch), cancel retry alarms, clear retry state. Phone deduplicates on insert (`timestamp` + `eventType` unique index).

---

## 6. Phone application behavior

### 6.1 System deep links

Central `SystemIntentsHelper` opens system settings:

| UI action | Intent | When |
|-----------|--------|------|
| Allow unrestricted battery | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → app package | Onboarding step 4; Settings; battery banner **Fix** |
| Battery settings fallback | `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` | If direct REQUEST fails or OEM blocks |
| Notification settings | `ACTION_APP_NOTIFICATION_SETTINGS` + `EXTRA_APP_PACKAGE` | Onboarding step 3; Alert settings; Settings |
| App permissions | `ACTION_APPLICATION_DETAILS_SETTINGS` | Settings |
| Bluetooth / pairing | `ACTION_BLUETOOTH_SETTINGS` | Onboarding step 2 if no watch; Settings change-watch |
| Wear companion (optional) | Launch `com.google.android.apps.wear.companion` if installed | Onboarding *Pair your watch* helper |

Battery restriction detection: `PowerManager.isIgnoringBatteryOptimizations(packageName)` — reactive banner via Flow.

### 6.2 Onboarding (first launch)

Sequential steps; cannot finish without required items:

1. **Welcome** — *“KeepStraight monitors your posture while sitting. Requires your phone and one paired watch.”*
2. **Pair your watch** — list Wear nodes; user selects **exactly one** → save `pairedWatchId` + `PairedDeviceInfo.pairedAt`.
   - **0 nodes:** **Open Bluetooth settings** + optional **Open Wear companion** links; Refresh button.
   - **>1 node:** block Continue until user unpairs extras (copy explains one watch only).
3. **Phone notifications** — request `POST_NOTIFICATIONS` (Android 13+); if denied → **Open notification settings**.
4. **Battery optimization** — explain background sync need; primary **Allow unrestricted battery**; secondary **Open battery settings** fallback; checkbox *“I've allowed unrestricted battery”* (auto-checked if already exempt).
5. **Watch permissions** — guide user to grant on watch: `BODY_SENSORS`, `ACTIVITY_RECOGNITION`, FGS notification (remote/on-watch prompts).
6. **Calibrate** — mandatory first calibration flow (§6.4).
7. **Sensitivity** — Strict / Normal / Relaxed (default Normal).

On completion: `onboardingComplete = true`; sync all prefs + calibration to paired watch only; send `START_ALGORITHM`; request sync flush.

### 6.3 Dashboard

- **Battery banner** if phone battery restricted — copy: *“Background restrictions may delay posture history sync”*; **Fix** → battery intent; dismissible per session; re-check on resume.
- **Connection banner** if paired watch unreachable — **Reconnect** button.
- **Connected card** when watch online.
- **Recalibrate** hero card (disabled if disconnected) — title *Recalibrate Posture* with subtitle (§4.1).
- Toggles (disabled if disconnected, subtitle *“Connect your watch…”*):
  - **Posture monitoring** → `START_ALGORITHM` / `STOP_ALGORITHM`
  - **Alerts** → `RESUME_ALERTS` / `PAUSE_ALERTS`
- Navigation: History, Alert settings, Sensitivity, Settings.

### 6.4 Calibration / recalibration flow

1. User sits in good posture; watch on wrist; phone connected.
2. Tap **Calibrate Posture** (first time) or **Recalibrate Posture**.
3. Phone: 3 s countdown *Hold still… 3, 2, 1*.
4. Phone → watch: `CALIBRATE_CAPTURE`.
5. Watch averages IMU ~3 s (evaluation paused); returns `CalibrationCaptureResult`.
6. Phone saves pitch/roll; builds `PostureCalibrationConfig` with current sensitivity; syncs via `/keepstraight/calibration`.
7. Watch resets slump state; success toast; auto-navigate back ~1.5 s.
8. History: `CALIBRATED`.

**Timeout:** 15 s without watch response → error, retry available, previous baseline kept.

Safe during active monitoring — only brief capture pause.

### 6.5 Alert settings

Per-channel toggles (synced to watch unless noted):

- Haptic (watch)
- Visual (watch)
- Sound (watch)
- Phone notification (phone only; requires master **Alerts** enabled on dashboard)

Deep link to system notification settings available.

### 6.6 Phone notifications

When `SLUMP_DETECTED` arrives **and** `alertsEnabled` **and** `phoneNotificationEnabled`:

→ show slump notification with duration context.

### 6.7 History

- **Room** on phone; **unlimited** retention; **Paging 3** list UI.
- Grouped by day: *Today*, *Yesterday*, or `EEEE, MMM d` (Locale US).
- Empty state: friendly English copy when no events.

| Type | Meaning | UI |
|------|---------|-----|
| `SLUMP_DETECTED` | Slump reached 5 min threshold (one row per episode) | Distinct icon/label; show duration seconds |
| `CALIBRATED` | Successful calibration/recalibration | Distinct icon/label from slump |
| `MONITORING_PAUSED` | User paused **Alerts** toggle (not posture monitoring) | Audit trail |
| `MONITORING_RESUMED` | User resumed **Alerts** toggle | Audit trail |

**No export** in v1. **No delete-all** in v1 (see §12.2).

### 6.8 Settings

| Setting | Action |
|---------|--------|
| Paired watch | Show `pairedWatchId` or *No watch paired* |
| Notifications | Deep link → notification settings |
| Battery / background | Deep link → battery exemption (+ fallback) |
| Bluetooth | Deep link → Bluetooth settings |
| App details | Deep link → application details |
| **Recalibrate Posture** | In-app calibration flow (§6.4) |
| **Sensitivity** | In-app preset picker (§4.2) |
| **Change paired watch** | Clear `pairedWatchId`; navigate to onboarding pair step; old history **retained** (mixed) |
| Unpair watch | Clear pairing without guided re-pair (shortcut to change flow) |

---

## 7. Sync protocol (phone ↔ watch)

Transport: Google Wearable **Message API**. Serialization: **Ghost Serialization 1.3.0**.

**Single-watch enforcement:** phone stores `pairedWatchId`; ignores inbound from other nodes; all outbound targets paired node only.

| Path | Direction | Payload |
|------|-----------|---------|
| `/keepstraight/calibration` | Phone → watch | `PostureCalibrationConfig` |
| `/keepstraight/control` | Phone → watch | `WatchControlMessage` |
| `/keepstraight/preferences` | Phone → watch | `AlertPreferences` |
| `/keepstraight/calibrate-request` | Reserved | — |
| `/keepstraight/calibrate-result` | Watch → phone | `CalibrationCaptureResult` |
| `/keepstraight/events` | Watch → phone | `PostureEvent` |
| `/keepstraight/events/batch` | Watch → phone | `PostureEventBatch` |
| `/keepstraight/sync-request` | Phone → watch | empty (flush queue) |
| `/keepstraight/sync-ack` | Reserved | — |

### 7.1 Control commands

| Command | Effect |
|---------|--------|
| `START_ALGORITHM` | Enable monitoring service |
| `STOP_ALGORITHM` | Disable monitoring service |
| `PAUSE_ALERTS` | Suppress alerts; emit `MONITORING_PAUSED` event |
| `RESUME_ALERTS` | Re-enable alerts; emit `MONITORING_RESUMED` event |
| `CALIBRATE_CAPTURE` | Start IMU capture window |
| `RESUME_CONNECTION` | End phone-disconnected pause; restart monitoring; flush sync |
| `SYNC_PREFERENCES` | Trigger preference sync handling |

---

## 8. Data models (Ghost)

```kotlin
@GhostSerialization
enum class SensitivityLevel { STRICT, NORMAL, RELAXED }

@GhostSerialization
data class PostureCalibrationConfig(
    val basePitch: Float,
    val baseRoll: Float,
    val sensitivity: SensitivityLevel = NORMAL,
    val slumpDurationThresholdMs: Long = 300_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
)

@GhostSerialization
data class AlertPreferences(
    val hapticEnabled: Boolean = true,
    val visualEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val phoneNotificationEnabled: Boolean = false,
)

@GhostSerialization
data class PairedDeviceInfo(
    val watchNodeId: String,
    val pairedAt: Long,
)
```

Phone persists `pairedWatchId` + `pairedAt` in DataStore. Sensitivity changes rebuild `PostureCalibrationConfig` with same `basePitch`/`baseRoll`.

---

## 9. Data & privacy (v1)

- All posture history on **phone** local storage (Room).
- Watch: **`pending_sync.bin` queue only** — temporary, not user-facing history.
- No analytics cloud, no accounts.
- See §12.2 for intentional v1 omissions (delete-all, export).

---

## 10. UI & design standards

### 10.1 Phone

- **Material 3** Compose; minimalist, spacious layout; clear hierarchy.
- Cards for dashboard sections; consistent top bar; English strings only.
- Toggles and primary actions visually distinct; connection/disconnected states obvious (banner + disabled controls).

### 10.2 Watch

- Single centered status line, max **14 sp**, round safe-area padding.
- No required buttons; service runs without UI.
- White flash overlay for visual alerts when app open.

---

## 11. Non-functional requirements

| Requirement | Scope |
|-------------|--------|
| **Zero-allocation hot path** | `PostureAnalyzer`, `ActivityClassifier`, alert dispatch — no heap alloc per sensor sample or alert fire |
| **Low power** | 500 ms sample tick; sensors off when off-wrist or algorithm stopped |
| **Reliability** | Sustained-state algorithms; conservative ambiguous handling |
| **Sync latency** | Phone toggles/sensitivity apply on watch within normal Wearable message latency |

Verified via unit tests (`shared`) + manual GW4 QA gate (§3.3).

---

## 12. Edge cases & explicit non-goals

### 12.1 Handled edge cases

- Stand up → pause, slump reset.
- Walk → pause.
- Watch removed / on desk / charger → off-wrist pause, no false slumps.
- Phone offline < 2 h → queue all events, retry 15 min.
- Phone offline ≥ 2 h → full pause until Reconnect.
- DND → no alerts; tracking continues.
- Recalibrate during slump → new baseline, slump cleared.
- Repeat alerts every 5 s → one history row per episode.
- Second watch node → ignored; onboarding blocks multi-watch.
- Change watch → old history kept on phone.

### 12.2 v1 non-goals

- iOS / Apple Watch shipping.
- Standalone watch (no phone).
- Multiple watches or phones per account.
- History export / CSV / share / cloud backup.
- Automatic recalibration reminders.
- Watch complications / tiles.
- Standing-desk posture mode.
- Delete-all history action.

---

## 13. Success criteria (acceptance)

On **Galaxy Watch 4+** with paired Android phone, all must pass:

1. Onboarding: pair one watch, grant permissions, calibrate, set sensitivity → monitoring starts without opening watch app.
2. Slump ≥ 5 min sitting → alert; if not fixed in 5 s → repeat every 5 s until corrected.
3. Walk / stand / off-wrist → no slump alerts.
4. One `SLUMP_DETECTED` history row per episode; repeat alerts do not duplicate.
5. Toggles (monitoring, alerts) and sensitivity apply on watch promptly.
6. Disconnect → retry; Reconnect → flush queue, restore monitoring.
7. DND suppresses alerts only.
8. Recalibrate from dashboard **and** Settings → new baseline, `CALIBRATED` row, slump loop cleared.
9. Battery banner when restricted; Fix opens exemption flow.
10. History: Paging 3, day groups, distinct CALIBRATED vs SLUMP presentation.
11. `MONITORING_PAUSED` / `MONITORING_RESUMED` logged when Alerts toggle used.
12. English UI throughout.
13. Manual GW4 QA gate (§3.3) signed off.

---

## 14. Module map (implementation reference)

| Module | Responsibility |
|--------|----------------|
| `shared` | Domain rules, models, sync paths, use cases, `PostureMonitoringEngine`, zero-alloc hot path |
| `wearApp` | Sensors, off-body, FGS, alerts, sync queue, retry alarms, minimal UI |
| `androidApp` | Onboarding, deep links, dashboard, history (Room + Paging 3), settings, Wear sync, notifications |

---

## 15. Code parity gaps

**Current codebase may not fully match this spec.** Track implementation work here; remove rows as code converges.

### 15.1 Phone app

| Spec reference | Required | Code today |
|----------------|----------|------------|
| §6.2 step 5 | Watch permissions onboarding step | Missing — jumps Battery → Calibrate |
| §6.2 step 2 | Block Continue when >1 Wear node | Missing — lists all nodes |
| §6.2 | Wear companion deep link | Missing |
| §6.1 | Battery fallback intent | Missing — only direct REQUEST intent |
| §6.8 | Settings → Recalibrate Posture | Missing — dashboard only |
| §6.8 | Settings → Sensitivity | Missing — dashboard nav only |
| §6.8 | Change paired watch guided flow | Partial — Unpair only |
| §6.7 | Distinct history icons CALIBRATED vs SLUMP | Missing — text only |
| §8 | Persist `pairedAt` in DataStore | Partial — `pairedWatchId` only |

### 15.2 Watch app

| Spec reference | Required | Code today |
|----------------|----------|------------|
| §5.3 | Full off-body software fallback (60 s variance, face-up) | Partial — magnitude delta heuristic only |
| §5.5 | Haptic low–medium amplitude | Uses max amplitude (255) |
| §5.2 | Pre-created haptic waveform at service start | Created on each alert |

### 15.3 Shared / domain

| Spec reference | Required | Code today |
|----------------|----------|------------|
| §4.3 | STANDING → SITTING after 15 s in sitting band | Missing — immediate return to SITTING |
| §4.3 | WALKING → SITTING after 20 s + angles restored | Missing |
| §4.3 | Gravity magnitude + vertical wrist (`az`) signals | Missing |
| §4.3 | 5-sample smoothing | **Implemented** |
| §4.3 | Standing hold 30 s | **Implemented** |

### 15.4 Accepted v1 product omissions (not code bugs)

These are **intentionally out of scope** — do not implement without spec update:

- Delete-all history (§12.2)
- History export (§12.2)
- Recalibration reminders (§12.2)
- Explicit “sound unavailable” UI on watch (§3.1)
- Cloud backup / accounts (§12.2)

---

See `README.md` for build instructions.
