# KeepStraight — Implementation Plan

**Product spec (source of truth):** [PRODUCT.md](PRODUCT.md)

**Status:** Core v1 built (KMP monorepo, shared domain, wear service, phone UI, sync). **Next:** close [PRODUCT.md §15](PRODUCT.md#15-code-parity-gaps) code parity gaps + GW4 QA gate.

---

## Completed (phase 1)

| Area | Delivered |
|------|-----------|
| Gradle/KMP | `shared`, `androidApp`, `wearApp`, Ghost 1.3.0, wrapper |
| shared | `PostureAnalyzer`, `ActivityClassifier` (base), `PostureMonitoringEngine`, models, unit tests |
| wearApp | FGS, boot receiver, sensors, alerts, sync queue, retry 15 min × 2 h |
| androidApp | Onboarding (6 steps), dashboard, Room + Paging 3, calibration, toggles, history, sync |
| Sync | 1:1 `pairedWatchId`, calibration, batch events, control commands, Reconnect |

---

## Pending (phase 2 — implement in code)

Each row maps to [PRODUCT.md §15](PRODUCT.md#15-code-parity-gaps). Remove from §15 when done.

### androidApp

- [ ] **Onboarding step 5 — Watch permissions** (§6.2): guide `BODY_SENSORS`, `ACTIVITY_RECOGNITION`, FGS on watch
- [ ] **Multi-watch block** (§6.2): disable Continue when >1 Wear node; explain unpair extras
- [ ] **Wear companion deep link** (§6.2): optional launch `com.google.android.apps.wear.companion`
- [ ] **Battery fallback intent** (§6.1): `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` in `SystemIntentsHelper`
- [ ] **Settings → Recalibrate Posture** (§6.8): in-app navigation to calibration flow
- [ ] **Settings → Sensitivity** (§6.8): in-app preset picker (not dashboard-only)
- [ ] **Change paired watch flow** (§6.8): clear pairing + re-run onboarding pair step
- [ ] **Persist `pairedAt`** (§8): store `PairedDeviceInfo` in DataStore
- [ ] **History distinct icons** (§6.7): visual differentiation `CALIBRATED` vs `SLUMP_DETECTED`

### wearApp

- [ ] **Off-body software fallback** (§5.3): 60 s variance, face-up flat, micro-movement thresholds
- [ ] **Haptic pre-create waveform** (§5.2): allocate once in service `onCreate`
- [ ] **Haptic low–medium amplitude** (§5.5): ~128, not max 255
- [ ] **GW4 UI polish** (§10.2): 14 sp max, round safe-area padding on status screen

### shared

- [ ] **STANDING → SITTING hysteresis** (§4.3): 15 s in sitting angle band
- [ ] **WALKING → SITTING hysteresis** (§4.3): 20 s zero steps + angles restored
- [ ] **Extra classifier signals** (§4.3): gravity magnitude stability + vertical wrist (`az`)
- [ ] **Unit tests** for new classifier transitions

### QA

- [ ] **GW4 manual gate** (§3.3, §13): run all 13 acceptance criteria on physical GW4 or Round API 30 emulator

---

## Recommended implementation order

1. `shared` — ActivityClassifier parity + tests
2. `wearApp` — off-body fallback + haptics
3. `androidApp` — onboarding/settings/history UI gaps
4. GW4 UI polish
5. Manual QA; update PRODUCT.md §15

---

## Out of scope v1 (do not implement)

See [PRODUCT.md §12.2](PRODUCT.md#122-v1-non-goals) and [§15.4](PRODUCT.md#154-accepted-v1-product-omissions-not-code-bugs): no delete-all history, no export, no cloud, no recalibration reminders, no complications.
