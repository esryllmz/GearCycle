# GearCycle — Service Boundaries

Status: Draft Last updated: 2026-06-26 (revised after human review)

This document defines the responsibilities, data ownership, and integration surface of each planned service. Keycloak is treated as an external identity and access management component, not a GearCycle business service, and is not described here.

Services are introduced according to the approved development roadmap, not all at once. This document describes target-state boundaries so that early services are built against a coherent long-term shape.

---

## api-gateway

### Responsibilities

- Single entry point for external clients (gearcycle-web and any future clients).
- Routes requests to the appropriate downstream service.
- Validates and forwards authentication (OAuth2 access tokens) to downstream services; does not issue or mint tokens itself.
- Cross-cutting concerns only: routing, rate limiting, request logging correlation IDs.

### Owned data

- None. The gateway is stateless with respect to business data.

### Operations it exposes

- Reverse-proxied versions of downstream services' public APIs. No business endpoints of its own beyond health/status.

### Events it may publish

- None.

### Events it may consume

- None.

### Responsibilities it explicitly does not own

- Business logic of any kind.
- Authorization decisions beyond "is this token valid and well-formed" (fine-grained authorization belongs to each downstream service, since only that service knows its own domain rules).
- Data aggregation across services (no API composition/BFF logic at MVP — if a future screen genuinely needs composed data, that decision will be revisited explicitly rather than added silently here).

### Risks of coupling with other services

- Temptation to add response-shaping or aggregation logic here, which would quietly turn the gateway into a hidden business service. Mitigation: keep it a routing layer; revisit only with an explicit ADR if composition is ever needed.

---

## device-service

### Responsibilities

- Owns device identity and the device lifecycle status state machine (see `docs/architecture/device-lifecycle.md`).
- Records device intake (initial registration).
- Enforces valid lifecycle transitions and their preconditions.
- Publishes facts about lifecycle status changes for other services to react to.

### Owned data

- Device aggregate: identifier, intake details, current lifecycle status, status history (including reopen events with their mandatory reason and the reopening actor's reference).
- Does **not** own diagnostic findings, repair orders, repair actions, parts, stock, quality check details, or safety screening details — those belong to other contexts and are referenced only by device identifier.

### Operations it exposes

- Register device intake.
- Query device by identifier.
- Query device lifecycle status and history.
- Request a lifecycle status transition (validated against current state and preconditions; the actual decision to repair/disassemble/recycle/ as-is is made by an authorized actor, not computed by this service).
- Reopen a `READY_FOR_RESALE` device (requires a mandatory reason; restricted to ADMIN or QUALITY_INSPECTOR; transitions the device to `AWAITING_INSPECTION`).

### Events it may publish

- `DeviceIntakeRegistered` (v1)
- `DeviceLifecycleStatusChanged` (v1) — includes previous status, new status, and reason/trigger reference.
- `DeviceDecommissioned` (v1) — published when a device becomes a terminal donor device, so inventory-service knows a disassembly may produce recovered parts referencing this device.
- `DeviceResaleReadyReopened` (v1) — published specifically on the `READY_FOR_RESALE` → `AWAITING_INSPECTION` transition, carrying the mandatory reopen reason and reopening actor's reference, since these fields are not relevant to any other transition.

### Events it may consume

- `RepairOrderCompleted` (v1) — to allow the device's status to advance toward a quality-check-pending state.
- `RepairOrderAborted` (v1) — to allow an authorized actor's subsequent decision (rework, disassembly, recycling) to be reflected once made; the event itself does not change device status, since the abort alone does not determine the next step.
- `QualityCheckCompleted` (v1) — to transition a device to Refurbished, back to repair (rework), or to recycling, depending on outcome. There is no fixed retry limit; this consumption may recur indefinitely.
- `SafetyScreeningCompleted` (v1) — to transition a device to `READY_FOR_AS_IS_SALE`, disassembly, or recycling, depending on outcome.

### Responsibilities it explicitly does not own

- Deciding _why_ a device should be repaired vs. disassembled vs. routed to as-is candidacy (that decision is made by an authorized human actor and recorded via the transition operation; device-service only validates legality of the requested transition).
- Tracking parts or stock.
- Performing quality checks or safety screenings.

### Risks of coupling with other services

- If device-service starts storing diagnostic or repair detail "for convenience," it duplicates repair-service's ownership and the two will drift out of sync. Mitigation: device-service only ever stores a _status_, never repair _content_.
- If other services query device-service synchronously on every operation rather than reacting to published events, device-service becomes a single point of contention for unrelated work.

---

## repair-service

### Responsibilities

- Owns inspections and the diagnostic findings they produce.
- Owns repair orders and the repair actions performed under them.
- Coordinates part reservation/consumption requests against inventory-service (via REST, since a repair action needs an immediate answer about part availability).

### Owned data

- Inspection aggregate: findings, performed-by, performed-at.
- Repair order aggregate: status, linked device identifier (reference only), repair actions, linked finding references, linked part/stock-item references (by identifier only).

### Operations it exposes

- Record an inspection and its diagnostic findings.
- Open a repair order against an inspected device.
- Record a repair action under a repair order (may trigger part reservation/consumption via inventory-service).
- Complete a repair order.
- Cancel a repair order (only valid if no part has been installed or consumed under it; releases any reservations).
- Abort a repair order (the failed-repair flow; valid once at least one part has been installed or consumed; never reverses or erases the resulting stock movements; marks the repair order with a distinct status from cancellation).

### Events it may publish

- `InspectionRecorded` (v1)
- `RepairOrderOpened` (v1)
- `RepairOrderCompleted` (v1)
- `RepairOrderCancelled` (v1) — published only when no part was installed or consumed.
- `RepairOrderAborted` (v1) — published when the repair order is ended after at least one part was installed or consumed; distinct from `RepairOrderCancelled` so consumers (e.g., for cost accounting or notifications) can tell the two cases apart.

### Events it may consume

- `QualityCheckCompleted` (v1) — a failed quality check creates a rework requirement: repair-service opens a new repair order referencing the same device. There is no fixed retry limit in the MVP; this may recur indefinitely until an authorized actor instead routes the device to recycling.

### Responsibilities it explicitly does not own

- Device lifecycle status itself (repair-service raises facts; only device-service decides what they mean for the device's status).
- Part catalog, stock levels, or stock movements (repair-service requests reservation/consumption from inventory-service; it does not maintain its own copy of stock data, and it never reverses a stock movement itself — abort leaves consumed-part records as they are).
- Quality checks or safety screenings (a separate bounded context with its own gating authority).

### Risks of coupling with other services

- If repair-service caches stock quantities locally "to avoid a network call," it will drift from the authoritative data in inventory-service. Mitigation: always call inventory-service synchronously for availability-sensitive decisions.
- If repair-service is given authority to directly set device lifecycle status, the boundary between "repair facts" and "lifecycle decisions" collapses into a single service, undermining the separation that protects auditability.
- If "cancel" and "abort" are conflated into a single status or operation, the system loses the ability to distinguish "no parts were ever touched" from "parts were consumed and the repair failed," which breaks both the stock-history immutability guarantee and any future cost-accounting needs. Mitigation: keep them as distinct repair-order statuses and distinct published events.

---

## inventory-service

### Responsibilities

- Owns the parts catalog (part definitions).
- Owns stock items (serialized or quantity-based), stock movements, and part reservations.
- Owns recovered parts as stock items with provenance reference to a donor device (by identifier only).

### Owned data

- Part aggregate: catalog definition.
- Stock item aggregate: physical instance/quantity, status, provenance (nullable donor device reference for recovered parts).
- Stock movement records (append-only ledger).
- Part reservation aggregate: linked repair order reference (by identifier only), quantity, status.

### Operations it exposes

- Manage part catalog (create/update part definitions).
- Receive stock (manual receipt of purchased parts).
- Reserve/release/consume stock on behalf of a repair order.
- Register a recovered part from a donor device.
- Query stock availability.

### Events it may publish

- `StockItemReceived` (v1)
- `StockReserved` (v1)
- `StockConsumed` (v1)
- `StockReservationReleased` (v1) — published when a repair order is cancelled before any part was consumed; the reservation record itself is never deleted, only marked released.
- `RecoveredPartRegistered` (v1) — includes donor device reference; pending inspection status.
- `RecoveredPartAvailable` (v1) — published after the recovered part passes its part-level verification step and becomes usable stock. (This verification is distinct from the device-level quality check and safety screening owned by the quality control context — see `docs/domain-glossary.md`; its exact name and process is an open product decision.)

### Events it may consume

- `DeviceDecommissioned` (v1) — signals that a donor device exists and disassembly may follow; does not itself create recovered parts (that is a deliberate operation by an authorized actor).
- A future part-level verification outcome event (name and shape not yet defined; see open product decisions) — for recovered-part stock items, this transitions the item from pending-inspection to available (or to rejected/recycled). This is intentionally **not** modeled as consuming `QualityCheckCompleted` or `SafetyScreeningCompleted`, since those are device-level outcomes and a recovered part is not a device.

### Responsibilities it explicitly does not own

- Device identity or lifecycle status.
- Repair order content or diagnostic findings.
- Deciding whether a recovered part passes its part-level verification step (that authority belongs to the quality control context, even though the exact process is not yet designed — see open product decisions).

### Risks of coupling with other services

- If inventory-service stores device condition or repair status "to make queries easier," it becomes implicitly coupled to device-service and repair-service internals, breaking independent deployability.
- If reservation logic is duplicated inside repair-service instead of being delegated to inventory-service, two services could believe different things about available quantity.

---

## notification-service

### Responsibilities

- Consumes domain events from other services and creates internal notification records for relevant actors.
- Provides a query API so the frontend can display notifications.

### Owned data

- Notification aggregate: recipient role/actor reference, message content, related event reference, read/unread status, created-at.

### Operations it exposes

- Query notifications for the current actor.
- Mark a notification as read.

### Events it may publish

- `NotificationCreated` (v1) — published only for any future service that might want to react to a notification being created (not required for MVP; may be omitted entirely if no consumer exists yet — do not add speculatively).

### Events it may consume

- `DeviceLifecycleStatusChanged` (v1)
- `DeviceResaleReadyReopened` (v1)
- `RepairOrderCompleted` (v1)
- `RepairOrderAborted` (v1)
- `QualityCheckCompleted` (v1)
- `SafetyScreeningCompleted` (v1)
- `RecoveredPartAvailable` (v1)

(Exact event-to-notification mapping is a configuration/business-rule decision to be made when this service is implemented, not assumed here.)

### Responsibilities it explicitly does not own

- Any business decision logic (it reacts to facts; it does not decide whether a device should change status).
- External delivery (email/SMS) — out of scope for MVP per product-scope.md.

### Risks of coupling with other services

- If other services start pushing business decisions into notification-service ("decide whether this is urgent"), logic that belongs in the originating domain leaks out of its rightful owner.
- Must be idempotent when consuming events, since at-least-once delivery means the same event may be processed more than once; duplicate notifications must be avoided.

---

## Cross-Service Integration Summary

| Interaction                                                                                   | Mechanism           | Reason                                                                                                                                                                                                                                                                                   |
| --------------------------------------------------------------------------------------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| repair-service → inventory-service (reserve/consume/release parts)                            | REST (synchronous)  | Repair action needs an immediate, consistent answer about availability before proceeding.                                                                                                                                                                                                |
| device-service → repair-service / inventory-service / notification-service (status facts)     | Kafka domain events | Other services react to facts asynchronously; no immediate response is required by device-service.                                                                                                                                                                                       |
| quality control context → device-service (`QualityCheckCompleted`,`SafetyScreeningCompleted`) | Kafka domain events | Device-service reacts to verification outcomes asynchronously to advance lifecycle status.**Note:**the quality control context is not yet assigned to a specific service (see ADR-001); these events are documented here by context, not by service name, until that assignment is made. |
| inventory-service ← device-service (`DeviceDecommissioned`)                                   | Kafka domain event  | Inventory only needs to know a donor device now exists; no synchronous coordination required.                                                                                                                                                                                            |
| notification-service ← all other services/contexts                                            | Kafka domain events | Purely reactive, asynchronous by nature; never blocks the originating action.                                                                                                                                                                                                            |

All services owning persistent state use the Transactional Outbox pattern for publishing domain events, and all event consumers are idempotent. No service queries another service's database, and no JPA entity is shared across service boundaries.
