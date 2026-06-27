# Day 04 - Device Domain Model

## Goal

Implement the first pure domain model for `device-service`: the `Device` aggregate,
lifecycle status rules encoded in `DeviceLifecycleStatus`, value objects (`DeviceId`,
`DeviceCategory`), domain exceptions, and plain JUnit 5 unit tests that prove valid
and invalid lifecycle transitions. No persistence, no Spring annotations inside the
domain, no REST layer.

## Completed

- Added `DeviceId` value object (Java record wrapping `UUID`; factory methods
  `generate()` and `of(UUID)`).
- Added `DeviceCategory` enum (`SMARTPHONE`, `LAPTOP`, `TABLET`, `DESKTOP`, `MONITOR`,
  `AUDIO`, `OTHER`).
- Added `DeviceLifecycleStatus` enum with all 11 statuses from `docs/device-lifecycle.md`
  and the full set of valid transitions encoded in a static initializer block.
  Exposes `canTransitionTo(DeviceLifecycleStatus)` and `isTerminal()`.
- Added `Device` aggregate:
  - Fields: `DeviceId id`, `DeviceCategory category`, `DeviceLifecycleStatus status`,
    `Instant receivedAt`, `String notes`.
  - Factory: `Device.intake(DeviceCategory)` â€” produces a device in `INTAKEN` status.
  - `transitionTo(DeviceLifecycleStatus)` â€” enforces the transition table; guards the
    `READY_FOR_RESALE â†’ AWAITING_INSPECTION` path so it can only be reached via
    `reopenForInspection()`.
  - `reopenForInspection(String reason)` â€” enforces mandatory non-blank reason and
    stores it in `notes`; the only permitted path from `READY_FOR_RESALE` to
    `AWAITING_INSPECTION`.
- Added `InvalidLifecycleTransitionException` (unchecked) for illegal status transitions.
- Added `InvalidDeviceStateException` (unchecked) for invalid domain input (null
  category, null/blank reopen reason, null target status).
- Added `DeviceTest` with 29 plain JUnit 5 unit tests (no `@SpringBootTest`):
  valid intake, all representative valid transitions, the full repair path, the
  as-is path, terminal status enforcement, the reopen path including reason
  validation, and explicitly rejected transitions.

## Package Structure

```
com.gearcycle.device.domain.model
    Device.java
    DeviceCategory.java
    DeviceId.java
    DeviceLifecycleStatus.java

com.gearcycle.device.domain.exception
    InvalidDeviceStateException.java
    InvalidLifecycleTransitionException.java
```

## Validation

```
mvn validate
```

Result: `BUILD SUCCESS` â€” both reactor modules pass enforcer rules.

```
mvn -pl services/device-service test
```

Result: `Tests run: 30, Failures: 0, Errors: 0, Skipped: 0` (`DeviceTest`: 29,
`DeviceServiceApplicationTests`: 1).

```
git diff --check HEAD
```

Result: no trailing-whitespace or mixed-indentation issues.

## Decisions

- **Lifecycle statuses follow `docs/device-lifecycle.md`**, not the initial task
  description, which used an older naming scheme. The doc's statuses (`INTAKEN`,
  `IN_REPAIR`, `AWAITING_QUALITY_CHECK`, `AWAITING_SAFETY_SCREENING`, `REFURBISHED`,
  `READY_FOR_RESALE`, `AWAITING_INSPECTION`, `READY_FOR_AS_IS_SALE`, `DECOMMISSIONED`,
  `RECYCLED`) are the authoritative model.
- **Transition table lives in the enum** (static initializer block), not in the
  aggregate or a separate policy class. The enum is the natural home for "what states
  are reachable from this state." `EnumSet`-backed sets give O(1) lookup.
- **`READY_FOR_AS_IS_SALE` is not terminal** per Section 6 of the lifecycle doc:
  the absence of defined outgoing transitions in the MVP is not the same as being
  terminal. It is modelled with an empty allowed-transitions set, not with `isTerminal()
  == true`.
- **`reopenForInspection(String reason)` is the sole path from `READY_FOR_RESALE` to
  `AWAITING_INSPECTION`**. Calling `transitionTo(AWAITING_INSPECTION)` from
  `READY_FOR_RESALE` throws with a message directing the caller to use `reopenForInspection()`.
- **No Lombok, no JPA, no Spring** inside the domain package. Plain Java 21 records,
  enums, and classes only.
- **`Device.intake(DeviceCategory)`** named after the physical intake flow, consistent
  with the `INTAKEN` initial status and the domain glossary definition of "Device intake."

## Out of Scope (Day 04)

- Application service layer (command handlers, use cases).
- Persistence and JPA mapping.
- REST controllers and DTOs.
- Lifecycle history / audit log (event recording for `DeviceResaleReadyReopened`,
  `DeviceLifecycleStatusChanged`, etc.).
- Role-based authorization checks on transitions (actor permissions from Section 4
  of the lifecycle doc).
- Kafka event publishing.
- Docker, Keycloak, frontend.
- Administrative terminal-status correction mechanism (deferred per Section 10 of the
  lifecycle doc).

## Next Day

Introduce the application service layer and the first REST endpoint for device intake,
or add persistence (Flyway + JPA mapping) and integration tests â€” to be scoped in the
Day 05 proposal.

