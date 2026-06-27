# GearCycle

GearCycle is a portfolio-grade microservices platform for managing the
**repair, recovery, refurbishment, resale preparation, and recycling**
lifecycle of electronic devices.

The project is built to demonstrate production-oriented software engineering
practices expected from a Java backend or full-stack developer: clear service
boundaries, explicit architecture rules, disciplined testing, and incremental,
reviewable delivery. Complexity is introduced only when justified by a real
domain or operational requirement — not to make the stack look more advanced
than the problem requires.

## Current Status

**Day 02 — Repository tooling and development standards.**

| Area                                                           | Status               |
| -------------------------------------------------------------- | -------------------- |
| Documentation (scope, glossary, boundaries, lifecycle, ADRs)   | Done (Day 01)        |
| Repository tooling (Maven parent, contributor docs, templates) | In progress (Day 02) |
| Spring Boot services                                           | Not started          |
| Next.js frontend                                               | Not started          |
| Docker Compose / local infrastructure                          | Not started          |
| Keycloak / security                                            | Not started          |
| Kafka / async events                                           | Not started          |
| CI/CD                                                          | Not started          |
| Kubernetes                                                     | Not started          |

No application code, infrastructure, or CI/CD exists yet. The repository
currently contains documentation and Maven build tooling only.

## Planned Architecture

GearCycle follows a microservices architecture with strict service autonomy:

- Each service owns its own database; no service queries another service's
  database directly.
- Domain entities are never shared between services.
- **REST** is used for synchronous operations that require an immediate
  response.
- **Kafka** is used for asynchronous domain events between services.
- Distributed transactions are not used. Cross-service consistency is achieved
  through the **Transactional Outbox** pattern and **idempotent consumers**.
- Domain events are versioned to allow producers and consumers to evolve
  independently.
- Controllers contain no business logic; business rules live in the
  application/domain layers, and transaction boundaries are defined at the
  application layer.
- JPA entities are never returned directly from APIs; dedicated request and
  response models are used at service boundaries.
- Domain invariants are enforced in the domain model itself, independent of
  Bean Validation.

See [`docs/adr/`](docs/adr) for the full architectural reasoning, including
ADR-001 and ADR-002.

## Planned Technology Stack

**Backend**

- Java 21
- Spring Boot 3.5.x (Spring Web MVC, Spring Data JPA, Spring Security OAuth2
  Resource Server)
- PostgreSQL
- Flyway
- Maven
- Testcontainers, JUnit 5, AssertJ, Mockito
- MapStruct
- springdoc-openapi
- ArchUnit

**Frontend**

- Next.js (App Router)
- TypeScript (strict mode)
- Tailwind CSS
- TanStack Query
- React Hook Form + Zod
- Vitest, Playwright

**Infrastructure** (introduced progressively, only when justified)

- Docker Compose
- Keycloak
- Apache Kafka
- Redis (only if a concrete caching/locking need arises)
- OpenTelemetry, Prometheus, Grafana
- GitHub Actions
- Kubernetes (after the Docker Compose version is stable)

## Planned Service Boundaries

- `api-gateway`
- `device-service`
- `repair-service`
- `inventory-service`
- `notification-service`
- `gearcycle-web`

Services are introduced one at a time according to the approved development
roadmap — not created up front as empty scaffolding.

## Documentation Links

- [`docs/product-scope.md`](docs/product-scope.md) — product scope
- [`docs/domain-glossary.md`](docs/domain-glossary.md) — domain glossary
- [`docs/architecture/service-boundaries.md`](docs/architecture/service-boundaries.md) —
  service boundaries
- [`docs/architecture/device-lifecycle.md`](docs/architecture/device-lifecycle.md) —
  device lifecycle
- [`docs/adr/ADR-001-use-microservices-for-lifecycle-bounded-contexts.md`](docs/adr/ADR-001-use-microservices-for-lifecycle-bounded-contexts.md) —
  ADR-001: use microservices for lifecycle bounded contexts
- [`docs/adr/ADR-002-use-keycloak-for-identity-and-access-management.md`](docs/adr/ADR-002-use-keycloak-for-identity-and-access-management.md) —
  ADR-002: use Keycloak for identity and access management
- [`docs/progress/`](docs/progress) — daily progress log
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution workflow and standards
- [`CLAUDE.md`](CLAUDE.md) — rules governing Claude-assisted development on
  this repository

## Repository Structure
