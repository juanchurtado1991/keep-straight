# ADR 001: UDF Layered KMP Architecture

## Status

Accepted — 2026-08-05

## Context

KeepStraight spans Android phone, Wear OS, and Compose Desktop with shared KMP domain logic.
Presentation was inconsistent: `MainViewModel` (phone), `DesktopSessionController` (desktop),
and `MonitoringSession` (wear) mixed UI state, side effects, and infrastructure.

## Decision

Adopt **layered KMP + UDF ligero** (State / Event / Effect):

```text
UI → Presentation (ViewModel/Store) → Application (UseCase) → Domain → Infrastructure
```

- **State**: immutable UI model (`StateFlow`)
- **Event**: user/system input (`onEvent`)
- **Effect**: one-shot actions (`SharedFlow`) — navigation, snackbars, permissions
- **Not** strict MVI reducers globally; reducers only for finite flows (calibration, wizard)

## Rules

1. UI never imports `sync.*`, `bridge.*`, Room DAOs, or Wearable APIs.
2. Side effects go through use cases or gateway interfaces in `shared`.
3. Domain engines (`PostureMonitoringEngine`, `DesktopPostureSession`) stay imperative.
4. FGS/services are infrastructure; they do not own business rules.

## Package layout

```text
shared/
  domain/           engines, scorers
  application/      use cases
  presentation/     UiState, Event, Effect, FeatureStore
  repository/       gateway interfaces

{androidApp,desktopApp,wearApp}/
  ui/
  presentation/     ViewModels / Stores
  infrastructure/   platform adapters
```

## Consequences

- More files, clearer boundaries, easier JVM tests.
- Migration is incremental; legacy facades delegate until removed.
