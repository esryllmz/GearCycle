# GearCycle — Domain Glossary

Status: Draft Last updated: 2026-06-26 (revised after human review)

Each term lists its owning bounded context. A term may be _referenced_ by other contexts (by identifier only), but it is _owned_ — defined, validated, and mutated — by exactly one.

---

### Device

**Definition:** A single electronic unit received by the organization, identified by a UUID, tracked through a lifecycle status from intake to an eventual disposition: refurbished-and-ready-for-resale, ready-for-as-is-sale, decommissioned (disassembled for parts), or recycled. Only decommissioned and recycled are terminal in the MVP; the two resale-readiness statuses are markers describing eligibility, not records of an actual completed sale, since the MVP does not implement sales transactions.

**Owning context:** Device lifecycle

**Must not be confused with:**

- _Stock item_ — a device is not inventory; it does not have a quantity.
- _Donor device_ — a donor device is a device, but the term specifically denotes a device whose lifecycle outcome is disassembly for parts.
- _Part_ — a device is a complete unit, not a catalog component.

---

### Device intake

**Definition:** The act of registering a device into the system when it is first received by the organization, capturing its initial known condition and origin. Produces the device's initial lifecycle status.

**Owning context:** Device lifecycle

**Must not be confused with:**

- _Inspection_ — intake records what is known at receipt; inspection is a separate, later diagnostic activity performed by a technician.

---

### Inspection

**Definition:** A diagnostic assessment performed by a technician on a device to determine its condition and produce one or more diagnostic findings, used to decide the device's next lifecycle step (repair, disassembly, as-is sale candidacy, or recycling).

**Owning context:** Inspection and repair

**Must not be confused with:**

- _Quality check_ — inspection happens _before_ repair work to diagnose a problem; a quality check happens _after_ work to verify it was done correctly. They are performed by different roles (TECHNICIAN vs. QUALITY_INSPECTOR) and serve opposite points in the timeline.

---

### Diagnostic finding

**Definition:** A discrete observation recorded during an inspection (e.g., "battery does not hold charge," "screen cracked"), used as input to the repair-or-not decision and to the eventual repair order's scope.

**Owning context:** Inspection and repair

**Must not be confused with:**

- _Repair action_ — a finding describes a _problem_; a repair action describes _work performed_ in response to one or more findings.

---

### Repair order

**Definition:** A unit of work opened against an inspected device, authorizing one or more repair actions and the reservation/consumption of parts, with its own status (open, in progress, completed, cancelled, or aborted). A repair order may only be _cancelled_ if no parts have been installed or consumed under it; once a part has been installed or consumed, ending the repair order early must go through the distinct _abort_ (failed-repair) flow instead, which never erases or reverses the stock history already recorded.

**Owning context:** Inspection and repair

**Must not be confused with:**

- _Device_ — a repair order references a device by identifier; it does not own device identity or device lifecycle status.
- _Aborted repair order_ (informal) — cancellation and abort are not interchangeable terms for the same outcome; they apply to different situations (no parts touched vs. parts already touched) and have different consequences for stock history. See `docs/architecture/device-lifecycle.md` Section 9.

---

### Repair action

**Definition:** A discrete unit of work performed under a repair order (e.g., "replaced battery," "resoldered connector"), optionally linked to the diagnostic finding(s) it addresses and to any parts consumed.

**Owning context:** Inspection and repair

**Must not be confused with:**

- _Diagnostic finding_ — the action is the response; the finding is the problem being responded to.

---

### Part

**Definition:** A catalog definition of a type of component (e.g., "Battery Model X-200"), independent of how many physical units exist or where they came from. A part definition does not represent a physical object.

**Owning context:** Parts and inventory

**Must not be confused with:**

- _Stock item_ — the part is the _type_; the stock item is the _physical instance or quantity on hand_ of that type.
- _Recovered part_ — a recovered part is a stock item with a specific provenance (harvested from a donor device), not a separate catalog concept.

---

### Stock item

**Definition:** A physical instance or quantity of a part held by the organization. May be tracked as a serialized individual unit (for high-value or traceability-sensitive components) or as a fungible quantity (for low-value, interchangeable components), with its own status (e.g., pending inspection, available, reserved, consumed).

**Owning context:** Parts and inventory

**Must not be confused with:**

- _Part_ — the stock item is a physical instance/quantity; the part is the catalog type it instantiates.
- _Stock movement_ — the stock item is the thing being moved; the movement is the record of a change.

---

### Stock movement

**Definition:** An immutable record of a change in stock item quantity or status (e.g., received, reserved, consumed, adjusted), forming an auditable ledger of how stock levels changed over time.

**Owning context:** Parts and inventory

**Must not be confused with:**

- _Part reservation_ — a reservation is a _hold_ against future consumption; a movement is a record of a change that has _already_ occurred.

---

### Part reservation

**Definition:** A hold placed on a quantity of a stock item on behalf of a specific repair order, preventing that quantity from being allocated elsewhere until it is consumed or the reservation is released.

**Owning context:** Parts and inventory

**Must not be confused with:**

- _Stock movement_ — a reservation does not by itself change on-hand quantity; it constrains what is _available_. Consumption (a movement) occurs when reserved stock is actually used.

---

### Recovered part

**Definition:** A stock item that originated from disassembling a donor device, rather than from external procurement. Enters inventory in a pending-inspection state and requires a verification step before it can become available stock, since its condition is not guaranteed safe or usable. **Note:** this verification is performed on a _part_, not a _device_, and is therefore a distinct concept from the device-level Quality check and Safety screening entries above — the exact name and record shape for recovered-part verification is an open product decision (see `docs/product-scope.md`) and is deliberately left unnamed here rather than conflated with either device-level gate.

**Owning context:** Parts and inventory (provenance reference to Device lifecycle context, by donor device identifier only)

**Must not be confused with:**

- _Part_ — a recovered part is a stock item instance, not a catalog definition.
- _Donor device_ — the donor device is the _source_; the recovered part is the _output_ of disassembling it.

---

### Donor device

**Definition:** A device whose lifecycle outcome is disassembly: it is deliberately broken down so that some of its components can be recovered as stock items elsewhere. Once a device becomes a donor device, it reaches a terminal decommissioned status and cannot return to an active lifecycle.

**Owning context:** Device lifecycle

**Must not be confused with:**

- _Recycled device_ — a donor device's harvestable components are recovered first; recycling refers to the responsible disposal of what remains (or of devices with nothing worth recovering).

---

### Quality check

**Definition:** An immutable, append-only verification record created by a QUALITY_INSPECTOR that gates a device becoming Refurbished. Each repair attempt that completes is followed by a new quality check record (pass or fail); a failed quality check is never edited or reused — it creates a rework requirement, after which a new quality check record must be created once rework is complete. There is no fixed limit on how many quality checks a device may accumulate in the MVP.

**Owning context:** Quality control

**Must not be confused with:**

- _Inspection_ — quality check is a post-repair verification gate that produces a pass/fail outcome on functional correctness; inspection is a pre-repair diagnostic assessment that produces findings about a problem.
- _Safety screening_ — quality check verifies full functional correctness and gates the Refurbished status; safety screening verifies only that a device with known, accepted defects is not hazardous, and gates the Ready-for-as-is-sale status instead. The two are never the same record type and never substitute for one another.

---

### Safety screening

**Definition:** An immutable verification record created by a QUALITY_INSPECTOR that gates a device becoming eligible for as-is resale preparation (Ready-for-as-is-sale). Unlike a quality check, safety screening does not certify full functional correctness — it certifies only that the device is not hazardous to handle, store, or use in its current, documented condition. Known defects are recorded alongside a passing result rather than being treated as failures.

**Owning context:** Quality control

**Must not be confused with:**

- _Quality check_ — safety screening is a lighter-weight gate for devices that were never going to be functionally repaired; quality check certifies functional correctness after repair work.
- _Inspection_ — inspection happens earlier, before the repair-or-as-is-or- recycle decision is made, and diagnoses problems rather than certifying safety of a device already routed toward as-is sale.

---

### Refurbished device

**Definition:** A device that has completed repair work (if needed) and passed a quality check, making it eligible for resale preparation. This is one of several possible final dispositions, not the default expected outcome for every device.

**Owning context:** Device lifecycle

**Must not be confused with:**

- _Repaired_ (informal) — repair is the work; refurbished is the lifecycle status reached only after that work passes quality control.
- _Ready-for-as-is-sale device_ — refurbished devices were functionally certified via quality check; as-is devices were only safety-screened and carry documented known defects. A device on the as-is path is never described as refurbished.

---

### Ready-for-as-is-sale device

**Definition:** A device judged unrepairable but still sellable in its current condition, which has passed safety screening and carries documented known defects. Does not require full functional certification. This status is a readiness marker only — the MVP does not implement an actual sales transaction, so reaching this status does not mean the device has been sold.

**Owning context:** Device lifecycle

**Must not be confused with:**

- _Refurbished device_ — a refurbished device passed a full quality check after repair; a ready-for-as-is-sale device was only safety-screened and was never repaired.
- _Recycled device_ — as-is sale is a path for devices with remaining resale value despite defects; recycling is for devices with no further reusable or sellable value.

---

### Recycled device

**Definition:** A device whose terminal disposition is responsible recycling — either because it was never repairable, was not worth disassembling for parts, failed quality checks or safety screening beyond recovery, or had no resale or part-recovery value. This is a terminal status; a recycled device can never return to an active lifecycle through normal lifecycle operations, and no correction API exists for this status in the MVP.

**Owning context:** Device lifecycle

**Must not be confused with:**

- _Donor device / decommissioned_ — disassembly recovers reusable parts first; recycling is the disposal path for a device (or its remainder) with no further reusable value.

```

```
