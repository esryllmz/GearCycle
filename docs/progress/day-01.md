# Day 01 — Project Foundation

## Goal

Define the GearCycle product scope, shared domain language, initial service
boundaries, device lifecycle, and foundational architecture decisions.

## Completed

- Created the GitHub repository.
- Cloned the repository locally.
- Defined Claude project instructions.
- Drafted the product scope.
- Drafted the domain glossary.
- Defined initial service boundaries.
- Defined the device lifecycle.
- Proposed the use of microservices for lifecycle bounded contexts.
- Proposed Keycloak for identity and access management.

## Decisions

- A repair order is created deliberately after inspection.
- Quality control is separate from initial inspection.
- Recovered parts enter a non-available state until verified.
- Device records and repair records belong to separate services.
- Terminal lifecycle statuses cannot be reversed through normal APIs.
- READY_FOR_AS_IS_SALE represents resale preparation, not an actual sale.

## Verification

- Checked service ownership for overlap.
- Reviewed lifecycle transition consistency.
- Reviewed inspection and quality-control terminology.
- Confirmed that application code remains outside Day 01 scope.

## Problems Encountered

- Maven is not yet installed or available on PATH.

## Deferred Work

- Install Maven or introduce Maven Wrapper.
- Finalize realm roles versus client roles.
- Implement repository tooling.
- Create the Spring Boot parent project.

## Next Day

Configure repository tooling, install the required build environment, and
prepare the initial Maven multi-module structure.
