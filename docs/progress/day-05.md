# Day 05 — Device Persistence

## Goal

Wire the `Device` domain aggregate to PostgreSQL using Flyway migrations, a JPA entity in the
infrastructure layer, and a repository adapter that satisfies the domain port. All persistence
tests run against a real PostgreSQL instance via Testcontainers.

## What was built

### Dependencies added (`device-service/pom.xml`)

- `spring-boot-starter-data-jpa` — Hibernate, Spring Data JPA
- `postgresql` (runtime) — JDBC driver
- `flyway-core` + `flyway-database-postgresql` — schema migrations
- `spring-boot-testcontainers` (test) — `@ServiceConnection` wiring
- `org.testcontainers:junit-jupiter` (test) — `@Testcontainers` / `@Container`
- `org.testcontainers:postgresql` (test) — `PostgreSQLContainer`

### Flyway migration

`V1__create_devices_table.sql` creates the `devices` table:

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `category` | VARCHAR(50) | NOT NULL |
| `lifecycle_status` | VARCHAR(50) | NOT NULL |
| `received_at` | TIMESTAMPTZ | NOT NULL |
| `notes` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |
| `version` | BIGINT | NOT NULL |

### New files

| File | Role |
|---|---|
| `domain/port/DeviceRepository.java` | Hexagonal port — pure Java interface |
| `infrastructure/persistence/DeviceJpaEntity.java` | JPA entity; `@PrePersist`/`@PreUpdate` set audit timestamps; `@Version` for optimistic locking |
| `infrastructure/persistence/SpringDataDeviceJpaRepository.java` | Spring Data JPA interface (package-private) |
| `infrastructure/persistence/JpaDeviceRepository.java` | Adapter implementing the domain port |

### Modified files

| File | Change |
|---|---|
| `domain/model/Device.java` | Added `reconstitute(DeviceId, DeviceCategory, DeviceLifecycleStatus, Instant, String)` static factory; refactored to a single private constructor |
| `DeviceServiceApplicationTests.java` | Added `@Testcontainers` + `@Container @ServiceConnection PostgreSQLContainer` so the context-load test passes without local PostgreSQL |
| `application.yml` | Added datasource (safe `${VAR:default}` env placeholders), JPA (`ddl-auto: validate`), and Flyway config |

### Tests

- `DeviceServiceApplicationTests.contextLoads()` — full Spring context load against Testcontainer PostgreSQL
- `JpaDeviceRepositoryTest` (6 tests) — persistence integration tests covering:
  - persist and retrieve by ID
  - `createdAt`/`updatedAt` population on insert
  - `version` starts at 0
  - `version` increments on update
  - `updatedAt` advances on update, `createdAt` stays fixed
  - `findById` returns empty for unknown ID
  - lifecycle status persisted correctly after transition

## Design decisions

- **Domain stays pure**: No JPA annotations in `com.gearcycle.device.domain.model`. The JPA entity is an infrastructure concern confined to the `infrastructure.persistence` package.
- **`lifecycle_status` column name**: Matches the domain concept; avoids the generic `status` name.
- **Audit via `@PrePersist`/`@PreUpdate`**: No database triggers. Timestamps are set by JPA lifecycle callbacks before SQL is issued.
- **Optimistic locking**: `@Version Long version` on `DeviceJpaEntity`. Concurrent updates on the same device row throw `OptimisticLockingFailureException`.
- **No H2**: Tests use real PostgreSQL via Testcontainers to avoid dialect mismatches.
- **`SpringDataDeviceJpaRepository` is package-private**: Only `JpaDeviceRepository` (same package) calls it. The domain port is the only public contract.
