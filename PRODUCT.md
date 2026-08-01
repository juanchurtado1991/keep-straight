# KeepStraight — Product Specification

This document defines **exactly** how KeepStraight must behave in v1. Implementation details may change; **user-visible behavior and product rules must not**.

---

## 1. Product overview

**KeepStraight** monitors **sitting posture** using a **Wear OS watch** on the wrist and a **required Android phone companion app**.

| Role | Device | Responsibility |
|------|--------|----------------|
| Sensor & alerts | Wear OS watch (Galaxy Watch 4+ baseline) | IMU sampling, slump detection, haptics/visual/sound alerts, offline sync queue |
| Control & history | Android phone | Pairing, calibration UI, settings, unlimited history, phone notifications |

**Language:** English only (UI copy, history labels, notifications).

**Pairing model:** Exactly **one watch ↔ one phone**. The phone app stores a single `pairedWatchId` and ignores all other Wear nodes.

**Cloud:** None in v1. All history stays on the phone locally.

**History export:** Not in v1.

---

## 2. Problem statement

Users who sit for long periods often **slump** without noticing. KeepStraight detects sustained bad posture while **seated**, alerts the user on the watch (and optionally on the phone), and records episodes on the phone for review.

KeepStraight **only** targets **sitting** posture. It does **not** monitor standing desk posture, walking, or sleep.

---

## 3. Hardware & platform requirements

### 3.1 Watch (minimum baseline)

**Samsung Galaxy Watch 4** (40 mm / 44 mm), Wear OS 3+, API 30+.

Required capabilities:

- Accelerometer (sampled ~2 Hz effective via 500 ms tick + sensor events)
- Step counter (`TYPE_STEP_COUNTER`)
- Off-body detection (Samsung `TYPE_LOW_LATENCY_OFFBODY_DETECT` preferred; software fallback if missing)
- Foreground service with `FOREGROUND_SERVICE_HEALTH`
- Haptic actuator
- Wearable Data Layer sync to paired phone
- Round display (minimal optional UI)

### 3.2 Phone

- Android API 30+ (`minSdk 30`)
- Bluetooth pairing with watch via system/Galaxy Wearable
- Google Play services (Wearable API)
- Recommended: battery optimization **exempt** for reliable background sync

### 3.3 Future platforms (out of v1 scope)

Architecture in `shared` prepares for iOS / watchOS, but v1 ships **Android phone + Wear OS watch only**.

---

## 4. Core concepts

### 4.1 Calibration (baseline posture)

Calibration captures the IMU **pitch** and **roll** of the user's **good sitting posture** while wearing the watch.

- **First calibration:** mandatory during phone onboarding before monitoring is useful.
- **Recalibration:** always available from phone (dashboard hero card + settings). Same flow as initial calibration.
- **Capture location:** IMU samples are read on the **watch** (2–3 s average after countdown).
- **Capture UI:** on the **phone** (3-second countdown, then “Capturing…”).
- **On success:** new baseline syncs to watch; slump state resets; phone logs `CALIBRATED` in history.
- **On failure:** timeout (~15 s) or disconnect → error state; **previous calibration retained**.
- Recalibration does **not** change sensitivity, alert toggles, or monitoring enabled flags—only `basePitch` / `baseRoll`.

### 4.2 Sensitivity presets

Phone setting maps to angular tolerances on watch:

| Preset | Slump tolerance (°) | Standing pitch delta (°) | Standing roll delta (°) |
|--------|---------------------|--------------------------|-------------------------|
| Strict | 7 | 15 | 10 |
| Normal | 10 | 18 | 12 |
| Relaxed | 15 | 22 | 15 |

Changing sensitivity on phone updates DataStore and re-syncs `PostureCalibrationConfig` to watch (same baseline angles).

### 4.3 Activity classification (watch)

Before evaluating slump, the watch classifies activity:

| State | Meaning | Posture monitoring |
|-------|---------|-------------------|
| `SITTING` | Default when not walking/standing/off-wrist | **Active** — slump timer runs |
| `WALKING` | ≥3 steps in 10 s window | Paused — analyzer reset |
| `STANDING` | Pitch/roll deviates beyond standing thresholds **sustained 30 s** | Paused — analyzer reset |
| `NOT_WORN` | Off-body sensor or heuristic | Paused — sensors may stop |
| `AMBIGUOUS` | Transitional (e.g. standing candidate < 30 s) | Treated as **not sitting** for slump |

**Important:** Bad slump angles that also look “standing” use **separate thresholds**: slump uses **tighter** tolerance than standing detection so users can accumulate 5 min of slump while technically slightly slouched but still classified as sitting.

### 4.4 Slump detection (watch)

While `SITTING` and monitoring is enabled:

1. Compare current pitch/roll to calibrated baseline using **slump tolerance** for active sensitivity preset.
2. If **bad posture** (either pitch or roll beyond tolerance):
   - Start or continue slump timer.
3. If **good posture**:
   - Reset slump timer and any active slump episode.
4. When bad posture is continuous for **5 minutes** (`300_000 ms`):
   - Fire **initial alert** (haptic/visual/sound per preferences, respecting DND).
   - Emit **one** `SLUMP_DETECTED` event to phone (with duration seconds).
5. If user **does not correct** within **5 seconds** after initial alert:
   - Repeat alert every **5 seconds** until posture corrected.
   - Repeat alerts **do not** emit additional history events (only the initial event is logged).

When user corrects posture or leaves sitting (walk/stand/off-wrist), slump episode ends and timers reset.

### 4.5 Do Not Disturb

If watch system DND is active (interruption filter ≠ ALL):

- Alerts are **suppressed** (no haptic, flash, sound).
- Monitoring and slump tracking **continue**.
- Watch UI state may show `DND_ACTIVE`.

---

## 5. Watch application behavior

### 5.1 Auto-start & lifecycle

- On boot: if `monitoring_enabled` flag is true in watch prefs, start `PostureMonitoringService` automatically.
- User **does not** need to open the watch app for monitoring to run.
- Optional watch app shows **one status line** (Monitoring / Not worn / Paused / etc.) and white flash overlay on visual alerts when app is foreground.

### 5.2 Foreground service

- Runs as foreground service with persistent notification (“Monitoring posture” or state-specific text).
- Samples accelerometer + step counter on ~500 ms tick.
- Processes samples through `PostureMonitoringEngine` in `shared`.

### 5.3 Off-wrist behavior

When watch is removed:

- Transition to `NOT_WORN`.
- Stop posture evaluation and alerts.
- Notification updates to reflect not worn.
- When worn again: resume prior monitoring logic automatically.

### 5.4 Watch monitoring states (user-visible)

| State | When | Alerts | Sensors |
|-------|------|--------|---------|
| `ACTIVE` | On wrist, sitting | On | On |
| `NOT_SITTING` | Walking or standing | Off | On |
| `NOT_WORN` | Off body | Off | Reduced/off |
| `ALERTS_PAUSED` | Phone sent pause alerts | Off | On |
| `ALGORITHM_OFF` | Phone disabled monitoring | Off | Off |
| `PHONE_RETRY` | Phone unreachable, retry window | On | On |
| `PHONE_DISCONNECTED_PAUSED` | Phone unreachable ≥ 2 h | Off | Off |
| `DND_ACTIVE` | System DND | Suppressed | On |

### 5.5 Alerts (watch)

Configurable from phone (`AlertPreferences`), synced to watch:

| Channel | Default | Behavior |
|---------|---------|----------|
| Haptic | On | Double pulse pattern |
| Visual | On | Broadcast flash; white overlay if MainActivity visible |
| Sound | Off | Default notification ringtone; stop previous before new |

Master `alertsEnabled` toggle on phone sends `PAUSE_ALERTS` / `RESUME_ALERTS` to watch.

### 5.6 Phone connection policy (watch side)

The phone is **required** for full product operation (history, settings sync). If watch cannot deliver events to phone:

1. Enqueue events in local **PendingSyncQueue** (not unlimited history—queue only).
2. Start **ConnectionRetryManager**: alarm every **15 minutes**.
3. Continue retrying for up to **2 hours** from first failure.
4. During retry window: state `PHONE_RETRY`; monitoring/alerts continue.
5. After **2 hours** exhausted: state `PHONE_DISCONNECTED_PAUSED`; monitoring stops until user taps **Reconnect** on phone (`RESUME_CONNECTION` control message).

On successful sync: flush queue, cancel retry alarms, clear retry state.

---

## 6. Phone application behavior

### 6.1 Onboarding (first launch)

Sequential steps; user cannot finish without required items:

1. **Welcome** — explains phone + watch requirement.
2. **Pair watch** — list Wear nodes; user selects **one**; stored as `pairedWatchId`. Refresh + Bluetooth settings deep link if none found.
3. **Notifications** — request `POST_NOTIFICATIONS` (Android 13+); link to notification settings.
4. **Battery** — explain unrestricted battery; deep link to exemption; user acknowledges checkbox (or auto-skip if already exempt).
5. **Calibrate** — navigates to calibration flow (mandatory before finish).
6. **Sensitivity** — Strict / Normal / Relaxed (default Normal).

On completion: `onboardingComplete = true`, sync all preferences + monitoring/alerts state to watch, request sync.

### 6.2 Dashboard

- **Battery banner** if phone battery optimization still restricts app (dismissible per session; re-check on resume).
- **Connection banner** if paired watch not reachable — **Reconnect** button.
- **Connected card** when watch online.
- **Recalibrate** hero card (disabled if not connected).
- Toggles (disabled if not connected, with subtitle “Connect your watch…”):
  - **Posture monitoring** → `START_ALGORITHM` / `STOP_ALGORITHM`
  - **Alerts** → `RESUME_ALERTS` / `PAUSE_ALERTS`
- Navigation: History, Alert settings, Sensitivity, Settings.

### 6.3 Calibration / recalibration flow (phone UI)

1. User sits in good posture, watch worn, phone connected.
2. Tap **Calibrate / Recalibrate**.
3. Phone shows 3-second countdown (`Hold still… N`).
4. Phone sends `CALIBRATE_CAPTURE` to watch.
5. Watch averages IMU ~3 s, returns `CalibrationCaptureResult`.
6. Phone saves pitch/roll, builds config with current sensitivity, syncs to watch.
7. Success message; auto-navigate back after ~1.5 s.
8. History records `CALIBRATED`.

**Timeout:** 15 s waiting for watch response → error, retry available.

### 6.4 Alert settings

Per-channel toggles synced to watch + phone behavior:

- Haptic (watch)
- Visual (watch)
- Sound (watch)
- Phone notification (phone only; requires `alertsEnabled` master toggle on phone)

### 6.5 Phone notifications

When `SLUMP_DETECTED` event arrives and:

- `alertsEnabled` is true, and
- `phoneNotificationEnabled` in alert preferences is true,

→ show slump notification with duration context.

### 6.6 History

- Stored in **Room** on phone only; **unlimited** retention (no auto-delete in v1).
- Paging UI grouped by day: Today, Yesterday, or `EEEE, MMM d` (Locale US).
- Event types:

| Type | Meaning |
|------|---------|
| `SLUMP_DETECTED` | Slump episode reached 5 min threshold (one per episode) |
| `CALIBRATED` | Successful calibration/recalibration |
| `MONITORING_PAUSED` | Alerts paused via toggle |
| `MONITORING_RESUMED` | Alerts resumed |

Slump rows show duration (time spent slumped at alert moment).

**No export** button in v1.

### 6.7 Settings

- Show paired watch id or “No watch paired”.
- Deep links: Notifications, Battery, Bluetooth, App details.
- **Unpair watch** (clears `pairedWatchId`).

### 6.8 Reconnect

Dashboard **Reconnect** (or connection banner):

1. Send `RESUME_CONNECTION` to watch.
2. Sync all preferences (calibration, sensitivity, alert prefs, monitoring, alerts).
3. Request sync flush from watch.

---

## 7. Sync protocol (phone ↔ watch)

Transport: Google Wearable **Message API** (v1). Paths under `/keepstraight/…`.

| Path | Direction | Payload |
|------|-----------|---------|
| `/keepstraight/calibration` | Phone → watch | `PostureCalibrationConfig` |
| `/keepstraight/control` | Phone → watch | `WatchControlMessage` |
| `/keepstraight/preferences` | Phone → watch | `AlertPreferences` |
| `/keepstraight/calibrate-request` | (reserved) | — |
| `/keepstraight/calibrate-result` | Watch → phone | `CalibrationCaptureResult` |
| `/keepstraight/events` | Watch → phone | `PostureEvent` |
| `/keepstraight/events/batch` | Watch → phone | `PostureEventBatch` |
| `/keepstraight/sync-request` | Phone → watch | empty (flush queue) |
| `/keepstraight/sync-ack` | (reserved) | — |

Serialization: **Ghost Serialization 1.3.0** (`Ghost.encodeToBytes` / `Ghost.deserialize`).

### 7.1 Control commands

| Command | Effect on watch |
|---------|-----------------|
| `START_ALGORITHM` | Enable monitoring service |
| `STOP_ALGORITHM` | Disable monitoring service |
| `PAUSE_ALERTS` | Suppress alert dispatch |
| `RESUME_ALERTS` | Re-enable alerts |
| `CALIBRATE_CAPTURE` | Start IMU capture window |
| `RESUME_CONNECTION` | End phone-disconnected pause; restart monitoring; request sync |
| `SYNC_PREFERENCES` | Trigger preference sync handling |

---

## 8. Data & privacy (v1)

- All posture history stays **on phone** local storage.
- Watch retains only **sync queue** (temporary, for offline phone outages ≤ 2 h policy).
- No analytics cloud, no account system in v1.
- No “delete all history” in v1 — see [§12 Known gaps](#12-known-gaps-v1).

---

## 9. Edge cases & explicit non-goals

### 9.1 Handled edge cases

- User stands up → monitoring pauses, slump resets.
- User walks → monitoring pauses.
- Watch removed → pause, no false slump alerts on desk.
- Phone offline < 2 h → queue events, retry every 15 min.
- Phone offline ≥ 2 h → full pause until Reconnect.
- DND on watch → no alerts; tracking continues.
- Recalibrate during active slump → baseline updates; slump state cleared.
- Repeat slump alerts every 5 s → no duplicate history spam.

### 9.2 v1 non-goals

- iOS / Apple Watch shipping.
- Standalone watch app (no phone).
- Multiple watches or phones per account.
- History export / CSV / share.
- Automatic “please recalibrate” reminders.
- Complications / tiles on watch.
- Standing desk posture mode.
- Cloud backup.

---

## 10. Success criteria (acceptance)

A v1 release is acceptable when, on **Galaxy Watch 4+** with paired Android phone:

1. Onboarding pairs one watch, calibrates, and starts monitoring without opening watch app manually.
2. Simulated slump ≥ 5 min while sitting triggers alert loop (5 s repeat until fixed).
3. Walking/standing/off-wrist suppresses slump detection.
4. Slump events appear in phone history; repeat alerts do not duplicate events.
5. Phone toggles for monitoring/alerts/sensitivity apply on watch within sync latency.
6. Phone disconnect triggers retry; Reconnect restores sync and monitoring.
7. DND suppresses alerts but not tracking.
8. Recalibrate updates baseline and logs `CALIBRATED`.
9. Battery optimization banner appears when phone restricted; deep link works.
10. English UI throughout.

---

## 11. Module map (implementation reference)

| Module | Product responsibility |
|--------|------------------------|
| `shared` | Domain rules, models, sync paths, use cases, `PostureMonitoringEngine` |
| `wearApp` | Sensors, FGS, alerts, sync queue, retry alarms, minimal UI |
| `androidApp` | Onboarding, dashboard, history, settings, Wear sync client, notifications |

---

## 12. Known gaps (v1)

These are **accepted limitations** or **known mismatches** between this spec, the original implementation plan, and the current codebase. They do **not** block a v1 release unless marked **blocking**.

### 12.1 Accepted product gaps (ship v1 as-is)

| Gap | Expected behavior in v1 | Future consideration |
|-----|-------------------------|----------------------|
| **No delete-all history** | History grows without a bulk-delete action | Add “Clear history” in a later release |
| **No history export** | No CSV, share sheet, or backup file | Cloud backup or export in a later release |
| **Mixed history after watch change** | Unpairing or pairing a new watch keeps old events in Room | Optional “start fresh” when changing watch |
| **No recalibration reminders** | Recalibrate is always available on dashboard; no proactive nudge | Optional reminder after N days or posture drift |
| **Sound on speakerless watches** | Sound alert is best-effort; no explicit UI if hardware cannot play | Silent fallback only; optional “sound unavailable” hint |
| **Watch on charger / dock** | Off-body sensor + software fallback treat desk/charger as not worn; may pause monitoring | Tune heuristics if users report false pauses |
| **Typing / micro-movements** | Slump requires **5 min sustained** bad posture while classified as sitting; no typing-specific mode | Tighter anti-FP heuristics if field reports false alerts |

### 12.2 Onboarding & pairing gaps

| Gap | Spec / plan intent | Current state |
|-----|-------------------|---------------|
| **Watch runtime permissions step** | Dedicated onboarding step for watch `BODY_SENSORS`, `ACTIVITY_RECOGNITION`, and FGS notification consent | Phone onboarding goes Battery → Calibrate; watch permissions declared in manifest but not guided from phone flow |
| **Multiple Wear nodes** | If >1 watch node is visible, user must resolve (unpair extras) before continuing | Phone lists nodes and stores one selection; no explicit block or copy for multiple watches |
| **Change paired watch flow** | Settings → change watch → clear `pairedWatchId` → re-run pair step | Settings exposes **Unpair watch** only; no guided re-pair flow after unpair |
| **Recalibrate from Settings** | Same recalibration flow reachable from Settings | Recalibrate hero card on dashboard only; Settings has no recalibrate entry |

### 12.3 Sync & offline queue gaps

| Gap | Spec / plan intent | Current state |
|-----|-------------------|---------------|
| **Queue size cap** | Early plan: FIFO cap (~50 events), drop oldest when full | Watch queue file (`pending_sync.bin`) has **no max size**; bounded only by 2 h retry window + device storage |
| **Deduplication on ingest** | Room unique index on `(timestamp, eventType)` when flushing queue | Implemented in phone DB; not previously documented in sync sections |
| **Reserved sync paths** | `/keepstraight/calibrate-request`, `/keepstraight/sync-ack` reserved | Defined in `SyncPaths`; unused in v1 flows |

### 12.4 Activity classification gaps (plan vs implementation)

The plan described richer **return-to-sitting** hysteresis and extra IMU signals. v1 implements a **simpler** classifier:

| Planned enhancement | In v1 code |
|--------------------|------------|
| STANDING → SITTING after angles in sitting band for **≥ 15 s** | Not implemented — returns to `SITTING` immediately when standing angles drop |
| WALKING → SITTING after **≥ 20 s** with zero steps and sitting angles restored | Not implemented — `WALKING` ends when step window resets |
| Extra signals: gravity magnitude stability, vertical wrist (`az`) axis | Not implemented |
| 5-sample pitch/roll smoothing before comparison | **Implemented** |
| Standing candidate held **≥ 30 s** before `STANDING` | **Implemented** |
| `AMBIGUOUS` treated as not sitting (no slump alerts) | **Implemented** |

Conservative standing detection reduces false slump alerts but may keep monitoring paused slightly longer after returning to the desk.

### 12.5 UI & documentation gaps

| Gap | Notes |
|-----|-------|
| **History row visuals** | Plan: distinct presentation for `CALIBRATED` vs `SLUMP_DETECTED`; v1 uses text/type only |
| **Watch UI typography** | Plan: GW4 round safe-area, ~14 sp single status line; implemented minimally but not spec’d pixel-perfect here |
| **Haptic timing in spec body** | §5.5 says “double pulse”; exact pattern is **120 ms on → 80 ms off → 120 ms on** (pre-built waveform at alert time) |
| **Non-functional: zero-allocation** | Engineering requirement from plan (hot path in `shared`); not a user-visible product rule — tracked in code review, not acceptance tests |

### 12.6 Platform version note

| Item | Original plan | v1 as built |
|------|---------------|-------------|
| Phone `minSdk` | 26 in early scaffold | **30** (aligned with watch baseline) |
| `compileSdk` | 35 in plan | **36** in Gradle |

---

Behavior elsewhere in this document remains authoritative for **user-visible v1 rules**. Known gaps above describe what is **out of scope**, **deferred**, or **not yet aligned** with the full original plan.

See `README.md` for build instructions.
