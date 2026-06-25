# GearCycle — Product Scope

Status: Draft Last updated: 2026-06-26 (revised after human review)

## 1. Problem Statement

Organizations that receive used or damaged electronic devices currently lack a unified, auditable process for deciding and tracking what happens to each device. Decisions about repair, refurbishment, disassembly for parts, resale preparation, or recycling are often made ad hoc, with no consistent record of _why_ a decision was made, _who_ made it, or _what state_ the device and its components are currently in.

GearCycle exists to give these organizations a system of record for the full device lifecycle — from intake through to a final disposition — while keeping repair work, inventory of recovered parts, and quality control as clearly separated, auditable activities.

## 2. Target Organizations

- Device refurbishment and repair businesses that process used or damaged electronics at moderate volume.
- IT asset disposition (ITAD) operations that must demonstrate a defensible, auditable trail for each device's outcome.
- Recommerce operations that need recovered parts and refurbished devices tracked with provenance before resale.

GearCycle assumes a **single organization, single location** for the MVP. Multi-tenancy and multi-warehouse support are explicitly deferred.

## 3. Primary Actors

| Role              | Primary concern                                                                                          |
| ----------------- | -------------------------------------------------------------------------------------------------------- |
| ADMIN             | User and role administration, system configuration                                                       |
| INTAKE_SPECIALIST | Registers devices entering the organization                                                              |
| TECHNICIAN        | Performs inspection and repair work                                                                      |
| INVENTORY_MANAGER | Manages parts, stock levels, and reservations                                                            |
| QUALITY_INSPECTOR | Performs quality checks (repair path) and safety screenings (as-is path) that gate lifecycle transitions |
| SALES_OPERATOR    | Marks devices/stock as ready for resale (no transaction processing in MVP)                               |

See `docs/architecture/device-lifecycle.md` for the permission matrix governing which role may trigger which lifecycle transition.

## 4. MVP Goals

1. Provide a reliable record of every device from intake to final disposition.
2. Make the repair process (inspection → diagnostic findings → repair actions) auditable and separate from device identity.
3. Track parts and stock — including parts recovered from disassembled donor devices — with traceable provenance.
4. Enforce a verification gate — a quality check for repaired devices, or a safety screening for as-is candidates — before a device can be marked refurbished or as-is resale-ready.
5. Notify relevant actors of significant lifecycle events without owning any business decision logic.
6. Demonstrate clean microservice boundaries: independent data ownership, asynchronous domain events for cross-service facts, and synchronous REST only where an immediate answer is required.

## 5. MVP User Journeys

### Journey A — Intake to Repair Decision

1. INTAKE_SPECIALIST registers a new device (`Device intake`).
2. TECHNICIAN performs an inspection and records diagnostic findings.
3. Based on findings, an authorized actor decides: open a repair order, route the device to disassembly, route it to as-is sale candidacy (pending safety screening), or mark it for recycling.

### Journey B — Repair to Refurbished

1. TECHNICIAN opens a repair order against an inspected device.
2. TECHNICIAN reserves and consumes parts, recording repair actions.
3. Repair order is completed.
4. QUALITY_INSPECTOR performs a quality check, creating a new immutable quality check record.
5. On pass, device becomes Refurbished. On fail, the failure creates a rework requirement: a new repair order is opened and, once rework completes, a new quality check record is created. There is no fixed limit on how many times this may repeat; alternatively, an authorized actor may instead route the device to disassembly or recycling.

### Journey C — Disassembly to Recovered Parts

1. TECHNICIAN or ADMIN decides a device is not economically repairable and marks it for disassembly.
2. Disassembly produces one or more recovered parts, each linked to the donor device by reference.
3. Recovered parts enter inventory in a pending-inspection state.
4. A part-level verification step (distinct from the device-level quality check or safety screening; exact name and process are an open product decision) is performed on each recovered part before it becomes available stock.
5. The donor device reaches a terminal decommissioned status.

### Journey D — Resale Preparation

1. Once a device is Refurbished, SALES_OPERATOR marks it `READY_FOR_RESALE`. Once a device has passed safety screening as an as-is candidate, it reaches `READY_FOR_AS_IS_SALE` directly from that screening outcome (see Journey A/B′ below) — no separate "mark ready" step is needed for the as-is path in the MVP.
2. No payment, listing, or marketplace integration occurs in MVP — both statuses are status markers only; reaching either does not mean the device has actually been sold.

### Journey B′ — As-Is Candidacy to Ready-for-As-Is-Sale

1. An authorized actor (ADMIN) routes an inspected, unrepairable device to as-is sale candidacy.
2. QUALITY_INSPECTOR performs a safety screening, documenting known defects rather than requiring full functional certification.
3. On pass, the device reaches `READY_FOR_AS_IS_SALE`. On fail, an authorized actor routes it to disassembly or recycling.

### Journey F — Reopening a Resale-Ready Device

1. A new defect is discovered on a device already marked `READY_FOR_RESALE`.
2. ADMIN or QUALITY_INSPECTOR explicitly reopens the device, providing a mandatory reason.
3. The device moves to `AWAITING_INSPECTION` and must go through a fresh inspection before any further repair/disassemble/recycle/as-is decision is made.
4. The reopen action and its reason are permanently recorded in the device's lifecycle history.

### Journey E — Recycling

1. A device that is not repairable, not worth disassembling, or that fails a quality check or safety screening is sent to recycling, at the discretion of an authorized actor. There is no fixed number of failures that automatically triggers recycling in the MVP — each failure is a human decision point.
2. Recycling is terminal; the device cannot return to an active lifecycle, and no correction API exists for this in the MVP.

## 6. In-Scope Capabilities (MVP)

- Device intake registration.
- Device inspection and diagnostic findings.
- Repair order management (open, assign work, record actions, complete, cancel only before parts are touched, or abort via the failed-repair flow once parts are touched).
- Quality checks as a distinct, immutable, append-only gating activity for the repair path.
- Safety screening as a distinct, immutable gating activity for the as-is sale path, separate from quality checks.
- Explicit reopen operation for `READY_FOR_RESALE` devices, with mandatory reason and restricted authority (ADMIN or QUALITY_INSPECTOR).
- Parts catalog, stock items, stock movements, and part reservations.
- Recovered part tracking with donor device traceability.
- Device lifecycle status tracking with enforced valid transitions, including irreversible terminal statuses.
- Internal notifications for significant lifecycle events.
- Role-based authorization aligned to the actors above.

## 7. Explicitly Out-of-Scope Capabilities (MVP)

- Actual sales transactions for either resale path (refurbished resale or as-is resale) — both `READY_FOR_RESALE` and `READY_FOR_AS_IS_SALE` are readiness markers only; no sale record, buyer, or payment exists in the MVP.
- Marketplace listing, pricing, and payment processing.
- External notification delivery (email/SMS providers).
- Multi-tenancy / multi-organization isolation.
- Multi-location or warehouse-to-warehouse transfers.
- Supplier and procurement management for newly purchased parts.
- Warranty management and post-sale returns.
- Analytics, reporting dashboards, and forecasting.
- AI-assisted diagnostics, pricing, or routing.
- Barcode/RFID hardware integration.
- An administrative correction API for mistaken terminal-status transitions (a future audited mechanism is anticipated but not designed or built in the MVP; see `docs/architecture/device-lifecycle.md` Section 10).
- A reopen path for `READY_FOR_AS_IS_SALE` devices (only `READY_FOR_RESALE` has a defined reopen operation in the MVP).

## 8. Success Criteria

- Every device has a single, queryable lifecycle status at all times, with a complete, ordered history of how it got there, including reopen events and their recorded reasons.
- No service can be shown to directly query another service's database or share a JPA entity.
- A recovered part can be traced back to its donor device.
- A device cannot move from a terminal status (Recycled, Decommissioned, or any future Sold status) back into an active status through any normal lifecycle API.
- Quality control (for repaired devices) and safety screening (for as-is candidates) are demonstrably separate gates from each other, from inspection, and from the technician's own judgment.
- Stock movement history is never erased or silently reversed, even when a repair order is aborted after parts were consumed.
- Cancellation and abort of a repair order are distinguishable in the data and produce different, correct effects on stock (release vs. no reversal).

## 9. Known Assumptions

These assumptions are used to unblock the draft and must be confirmed or corrected by domain stakeholders where not already resolved by an approved decision. See `docs/architecture/device-lifecycle.md` for the lifecycle-specific subset, and `docs/adr/` for ADR-level decisions.

1. A device follows one primary lifecycle path at a time; disassembly is the only transition that produces multiple downstream aggregates (recovered parts).
2. A repair order is created as a deliberate decision after inspection, not automatically on intake.
3. A quality check is mandatory before a device becomes Refurbished. Recovered parts require a separate, part-level verification step before becoming available stock — this is explicitly distinct from the device-level quality check and from safety screening (see Section 10, item 2 below; the exact name and process for part-level verification remains an open decision).
4. Disassembly is terminal for the donor device (status `DECOMMISSIONED`).
5. **Resolved by approved decision:** the as-is path uses status `READY_FOR_AS_IS_SALE` (not `SOLD_AS_IS`), is not terminal, requires safety screening rather than full quality check, and does not imply an actual sale has occurred.
6. Resale preparation in MVP is a status marker with no transactional capability, for both the refurbished-resale and as-is-resale paths.
7. Notifications are internal/in-app only for MVP.
8. **Resolved by approved decision:** repair order cancellation is only valid before any part is installed or consumed; once a part is touched, ending the repair order early must go through the abort (failed-repair) flow, which never reverses recorded stock movements.
9. **Resolved by approved decision:** there is no fixed retry limit on quality check failures in the MVP; each failure creates a rework requirement and a new immutable quality check record, with the decision to keep reworking vs. recycle made by a human actor each time.
10. **Resolved by approved decision:** a `READY_FOR_RESALE` device may be reopened to `AWAITING_INSPECTION` via an explicit operation requiring a mandatory reason and ADMIN or QUALITY_INSPECTOR authority; this does not extend to `READY_FOR_AS_IS_SALE` in the MVP.
11. **Resolved by approved decision:**`DECOMMISSIONED`, `RECYCLED`, and any future `SOLD`-type status are irreversible through normal lifecycle APIs in the MVP; no correction API is built now, and direct database modification is not an acceptable workaround if a mistake occurs.

## 10. Open Product Decisions

These require explicit human sign-off and are intentionally left unresolved in this draft rather than silently decided. Items resolved by the most recent round of human review have been removed from this list (see Section 9 for what was resolved and how).

1. Should INVENTORY_MANAGER have authority to reject a recovered part outright (skip part-level verification, send straight to recycling) if it is visibly unusable?
2. What is the exact name, record shape, and authority (which role) for the part-level verification step that gates a recovered part becoming available stock? This was deliberately left distinct from both the device-level quality check and safety screening, but not yet designed.
3. Is there a reopen path for `READY_FOR_AS_IS_SALE` devices, analogous to the one defined for `READY_FOR_RESALE`, or is the as-is path considered final once reached (short of disassembly/recycling, which are not modeled as reachable from it in this draft)?
4. What does the future audited administrative correction mechanism for mistaken terminal-status transitions (`DECOMMISSIONED`, `RECYCLED`, future `SOLD`-type statuses) need to capture, and who is authorized to invoke it? Deferred by approved decision but not yet designed.
5. When real sales processing is eventually built, what is the terminal "sold" status (or statuses) for each resale path, and does reaching it retire `READY_FOR_RESALE` / `READY_FOR_AS_IS_SALE` or coexist with them as a further transition?
