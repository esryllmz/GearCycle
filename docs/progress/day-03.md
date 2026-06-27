# Day 03 — Device Service Skeleton

## Goal

Introduce `device-service` as the first real Maven module in the GearCycle
multi-module build: a buildable, testable Spring Boot application skeleton
with no business logic, no persistence, and no web layer.

## Completed Work

- Updated root `pom.xml`:
  - Added `<modules><module>services/device-service</module></modules>`.
  - Added `<spring-boot.version>3.5.3</spring-boot.version>` property.
  - Added `spring-boot-dependencies` BOM to `<dependencyManagement>` (imported
    with `scope=import`, `type=pom`) — deferred from Day 02 until the first
    service module actually consumed it.
  - Added `spring-boot-maven-plugin` to `<pluginManagement>` (pinned to
    `${spring-boot.version}`).

- Created `services/device-service/pom.xml`:
  - Parent: `com.gearcycle:gearcycle-parent:0.1.0-SNAPSHOT` (inherits enforcer
    rules, compiler config, and BOM).
  - `artifactId`: `device-service`, `version`: `0.1.0-SNAPSHOT`, `packaging`:
    `jar`.
  - Dependencies: `spring-boot-starter` (compile) and
    `spring-boot-starter-test` (test) — no versions declared, resolved from
    the BOM.
  - Build: `spring-boot-maven-plugin` bound (no version, resolved from
    `pluginManagement`).

- Created `services/device-service/src/main/java/com/gearcycle/device/DeviceServiceApplication.java`:
  - `@SpringBootApplication` entry point; no additional annotations or
    configuration.

- Created `services/device-service/src/main/resources/application.yml`:
  - Sets `spring.application.name: device-service` only.

- Created `services/device-service/src/test/java/com/gearcycle/device/DeviceServiceApplicationTests.java`:
  - `@SpringBootTest` context load test with a single `contextLoads()` method.
  - Uses JUnit 5 (`@Test` from `org.junit.jupiter.api`).

- Created `docs/progress/day-03.md` (this file).

## Decisions

- **Spring Boot BOM placed in the root POM, not the child:** any future service
  module inheriting from `gearcycle-parent` will automatically receive the same
  managed Spring Boot version without repeating it, and version upgrades are a
  single-line change.
- **`spring-boot-starter` only, no `spring-boot-starter-web`:** the skeleton
  intentionally has no HTTP layer. A non-web `ApplicationContext` starts faster
  and avoids binding a port in tests.
- **`spring-boot-maven-plugin` in `pluginManagement`, not in the root `<plugins>`
  block:** it must not execute at the parent level (the parent is a `pom`
  module with nothing to repackage). The child POM opts in by declaring the
  plugin in its own `<build><plugins>` block.
- **No Lombok, no additional starters:** per Day 03 scope constraints.

## Validation

```
mvn validate
```

Result: `BUILD SUCCESS` — both `GearCycle Parent` and `GearCycle Device
Service` pass the enforcer rules (Java 21, Maven ≥ 3.9); BOM dependencies
resolve from Maven Central.

```
mvn -pl services/device-service test
```

Result: `BUILD SUCCESS` — `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
(context load test passes).

```
git diff --check
```

Result: no output (no trailing whitespace or mixed line-ending issues).

## Out of Scope

The following were explicitly excluded from Day 03:

- REST controllers, domain model, lifecycle logic.
- JPA, Flyway, PostgreSQL, Testcontainers, H2.
- Kafka, Keycloak, Docker / Docker Compose.
- Actuator, web layer (`spring-boot-starter-web`).
- Maven Wrapper files.
- GitHub Actions / CI pipelines.
- Additional service modules (repair-service, inventory-service,
  notification-service, api-gateway).
- Changes to Day 01 architecture decisions or ADRs.

## Risks and Follow-Up Items

- **Spring Boot 3.5.3 / Spring Framework 6.2.8:** these are the versions
  resolved from Maven Central as of this session. Pin them explicitly in any
  lock file or CI cache strategy once CI is introduced.
- **Enforcer Java range `[21,22)`:** requires exactly Java 21 on the developer
  machine; Java 22+ will fail the build. This is an intentional constraint from
  Day 02 and is unchanged.
- **No `dependencyManagement` for Testcontainers BOM yet:** will be needed when
  integration tests are introduced in a later day.
- **`application.yml` is minimal:** `server.port`, datasource URL, and other
  runtime properties will be added when the corresponding layers are
  introduced.

## Next Day

Introduce the first domain object and a minimal REST endpoint within
`device-service`, with unit tests — proposed first as an analysis before any
code is written, per the established Day 02 working method.
