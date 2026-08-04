# KeepStraight — Implementation Plan

**Status:** Core v1 + code parity **complete**. **Next:** GW4 manual QA gate.

---

## Completed (phase 1 + parity)

| Area | Delivered |
|------|-----------|
| Gradle/KMP | `shared`, `androidApp`, `wearApp`, Ghost 1.3.0, wrapper |
| shared | `PostureAnalyzer`, `ActivityClassifier` (hysteresis + gravity/wrist signals), `PostureMonitoringEngine`, models, unit tests |
| wearApp | FGS, boot receiver, sensors, off-body fallback, pre-created low–medium haptics, sync queue, retry 15 min × 2 h, GW4 status UI |
| androidApp | Onboarding (7 steps incl. watch permissions), multi-watch block, Wear companion link, battery fallback, dashboard, Room + Paging 3, calibration, toggles, history icons, Settings recalibrate/sensitivity/change-watch, `pairedAt` |
| Sync | 1:1 `pairedWatchId`, calibration, batch events, control commands, Reconnect |

---

## Pending

### QA

- [ ] **GW4 manual gate**: run acceptance criteria on physical GW4 or Round API 30 emulator

---

## Out of scope v1 (do not implement)

No delete-all history, no export, no cloud, no recalibration reminders, no complications.
