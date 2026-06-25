# ADR-001: Use Microservices for Lifecycle Bounded Contexts

Status: Proposed

> **Revision note (2026-06-26):** Reviewed against the human decisions applied to `docs/architecture/device-lifecycle.md` and `docs/architecture/service-boundaries.md` (repair cancellation vs. abort, quality check rework, resale-ready reopening, the `READY_FOR_AS_IS_SALE` rename, terminal-status irreversibility). No change to this ADR's decision, alternatives, or consequences was required; those decisions affect the _shape_ of events and operations within the agreed service boundaries, not the boundaries themselves.

## Context

GearCycle manages a device's journey across several distinct concerns: device identity and lifecycle status, inspection and repair work, parts and inventory (including recovered parts), and quality control. These concerns have different rates of change, different data ownership needs, and different actors, and the project's explicit goal is to demonstrate realistic, defensible microservice boundaries rather than a single monolith — while also avoiding complexity that is not justified by an actual domain or operational requirement.

The project instructions explicitly require: each service owns its own database, no service queries another service's database, no domain entities are shared between services, and distributed transactions are not allowed.

## Decision

Decompose the system into independently deployable services aligned to bounded contexts identified in the domain analysis:

- `api-gateway` — routing and authentication forwarding only.
- `device-service` — device identity and lifecycle status.
- `repair-service` — inspections, diagnostic findings, repair orders, repair actions.
- `inventory-service` — parts catalog, stock items, stock movements, reservations, recovered parts.
- `notification-service` — internal notifications derived from domain events.

Each service owns its own database and exposes its capabilities through REST (for operations needing an immediate response) and Kafka domain events (for asynchronous facts other services may react to). Reliable event publishing uses the Transactional Outbox pattern; all event consumers are idempotent. Quality control is treated as a distinct context but is not yet assigned its own service in this ADR — that assignment will be made explicitly when the context is implemented, to avoid prematurely over-splitting the system.

Services are introduced incrementally according to the approved development roadmap, not all at once.

## Alternatives Considered

1. **Single modular monolith.** Lower operational overhead, simpler transactions, easier to refactor boundaries early. Rejected as the primary direction because a core project goal is to demonstrate production-oriented microservice boundaries and operational practices (independent deployability, service-owned data, event-driven integration) that a monolith would not exercise. However, a monolith would have been the _more defensible_ choice on pure simplicity grounds at this scale, and this trade-off is acknowledged rather than dismissed.
2. **One service per bounded context, including a separate quality-control-service from day one.** Rejected for now as premature: the quality control context's data ownership and event surface are not yet exercised by a concrete implementation task, so creating the service immediately would risk an interface without a real architectural boundary behind it yet. This will be revisited explicitly once quality control is implemented.
3. **Coarser-grained services (e.g., merging repair and inventory into one "operations" service).** Rejected because it would blur the explicit requirement to distinguish a physical part/stock concern from repair work, and would make the parts-reservation interaction implicit rather than an explicit, observable REST boundary.

## Consequences

- Cross-service data consistency must be achieved via domain events and the Transactional Outbox pattern, not distributed transactions, which increases design effort for any workflow spanning services (e.g., device status changes following repair or quality outcomes).
- Each service must define and version its own domain events, since other services will depend on their shape over time.
- Local development requires running multiple services together (addressed by Docker Compose, introduced separately).
- Testing must cover both intra-service behavior and inter-service event contracts (Kafka integration tests), increasing the testing surface compared to a monolith.

## Risks

- Splitting too early, before a context's real shape is known, risks designing the wrong boundary and having to merge or re-split services later. Mitigated by deferring quality-control-service creation until that context is actually implemented.
- Asynchronous, eventually-consistent device status updates (e.g., after a repair order completes) may surface UX questions about transient inconsistency that a monolith would not have. This is accepted as a deliberate trade-off for the architectural goals of the project.
- Operational overhead (multiple databases, multiple deployable units) is higher than a monolith at this project's actual scale; this is accepted because demonstrating that overhead correctly is itself a goal of the project.

## Revisit Conditions

- If a bounded context's data ownership or event surface turns out to be trivial in practice (e.g., quality control never needs to evolve independently of repair-service), reconsider merging it rather than giving it a dedicated service.
- If the Transactional Outbox / event-driven consistency model proves to introduce more operational complexity than the project can sustain at its actual scale, reconsider consolidating the most tightly-coupled services.
- Revisit when quality control is implemented, to decide explicitly whether it becomes its own service or remains part of repair-service or device-service.
