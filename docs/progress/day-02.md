# Day 02 — Repository Tooling and Development Standards

## Goal

Prepare the repository for future Java/Spring Boot microservices and a
Next.js frontend by establishing minimal, professional repository tooling
and development standards — without implementing any application code,
infrastructure, or CI/CD.

## Completed Work

- Added a root Maven parent `pom.xml`:
  - Packaging `pom`, Java 21 (`maven.compiler.release`), UTF-8 encoding.
  - No `<modules>` declared and no service module POMs created.
  - No Spring Boot, Spring Cloud, or Testcontainers BOMs added.
  - `maven-compiler-plugin` pinned in `pluginManagement` (not yet bound to a
    module, since no module compiles code).
  - `maven-enforcer-plugin` pinned and bound with two minimal rules:
    `requireJavaVersion` ([21,22)) and `requireMavenVersion` ([3.9,)).
- Updated `README.md` with project overview, current status, planned
  architecture, planned technology stack, documentation links (matching the
  actual Day 01 file paths, including the `docs/architecture/` subdirectory
  and full ADR filenames), repository structure, local development status,
  and roadmap summary.
- Added `CONTRIBUTING.md` covering branch naming, Conventional Commits, pull
  request expectations, a review checklist, validation commands, secret
  handling, and the Claude-assisted development workflow.
- Added root-level `CLAUDE.md` consolidating project objective, architecture
  rules, coding standards, testing standards, Git rules, and scope-control
  rules into a version-controlled file.
- Added `.github/PULL_REQUEST_TEMPLATE.md` with summary, type of change,
  validation, an optional screenshots section, and a checklist.
- Added `.github/ISSUE_TEMPLATE/task.md` with goal, scope, out of scope,
  acceptance criteria, validation, and notes.
- Added `.gitignore` covering Java/Maven build output, Node/Next.js
  artifacts, IDE directories, OS files, and environment files — explicitly
  excluding Maven Wrapper files from being ignored.
- Added `.editorconfig` for consistent formatting across `.java`,
  `.ts`/`.tsx`, `.md`, `.yml`, and other text files.

## Decisions

- **Workflow documentation location:** a single root-level `CLAUDE.md` is
  used instead of `.claude/CLAUDE.md` or a separate `docs/claude-workflow.md`.
  A condensed Claude-assisted workflow section was added to `CONTRIBUTING.md`
  instead, to avoid duplicating the same content across multiple files.
- **`CODE_OF_CONDUCT.md`:** not included today. Low priority for the current
  stage of the project; can be added later without affecting build tooling.
- **`CODEOWNERS`:** not included today. Ownership boundaries are not yet
  meaningful with a single root POM and no service modules.
- **Maven Wrapper:** not generated today. `mvnw`, `mvnw.cmd`, and
  `.mvn/wrapper/*` are deferred until local Maven has been verified and the
  wrapper can be generated with the official Maven command, rather than
  hand-written. `.gitignore` was written so that, once generated, the
  wrapper JAR and scripts are committed rather than silently ignored.
- **Root `pom.xml` strategy — parent only, no module placeholders:** chosen
  over a parent POM with placeholder `<modules>` entries. Placeholder modules
  would require throwaway stub POMs for services that do not exist yet and
  would break `mvn validate` until those stub directories exist. A
  modules-free parent keeps the build valid today and makes adding a real
  service later a small, isolated, reviewable change.
- **`maven-enforcer-plugin` included and bound (not just pinned):** considered
  safe at this stage because it has exactly two built-in rules
  (`requireJavaVersion`, `requireMavenVersion`), no custom rule
  implementations, and no dependency convergence checks (there are no
  dependencies yet). Its only effect is failing the build fast on a
  mismatched local Java or Maven version — it does not constrain any future
  design decision.
- **No Checkstyle/Spotless/PMD/JaCoCo/Sonar:** deferred until real source
  code exists to format, lint, or measure coverage against.
- **No Spring Boot/Spring Cloud/Testcontainers BOM in the parent yet:**
  deferred until the first service module actually consumes those
  dependencies, to avoid version pins drifting from anything actually built.

## Validation

```bash
mvn -N validate
```

Expected result: `BUILD SUCCESS` against the single aggregator POM, with no
modules to resolve.

```bash
git status --ignored
```

Expected result: no `target/`, `node_modules/`, `.next/`, IDE directories, or
`.env*` files tracked or staged. Maven Wrapper files (`mvnw`, `mvnw.cmd`,
`.mvn/wrapper/*`) do not appear here today because they have not been
generated yet — they are not ignored, simply absent.

```bash
git diff --cached --name-only
```

Expected result: only the files explicitly listed for Day 02 appear in the
diff; no secrets or credentials present.

> Maven and Git command output should be captured here once actually run in
> the project's real environment. This log records what is expected to pass
> and why; see "Problems Encountered" for anything not verifiable in the
> current working environment.

## Problems Encountered

- This working session does not have a live checkout of the GearCycle Git
  repository or a local Maven installation to execute commands against.
  `mvn -N validate` and the `git` validation commands above were not actually
  executed against the real repository; they are provided as the commands to
  run once these files are placed in the real repository, and their expected
  outcomes are documented above. This should be confirmed and recorded with
  real output before merging.
- An earlier draft of this day's files used shortened documentation paths
  (e.g. `docs/service-boundaries.md`, `docs/adr/ADR-001.md`) that did not
  match the actual Day 01 file structure. This was corrected before
  finalizing: the README now links to `docs/architecture/service-boundaries.md`,
  `docs/architecture/device-lifecycle.md`,
  `docs/adr/ADR-001-use-microservices-for-lifecycle-bounded-contexts.md`, and
  `docs/adr/ADR-002-use-keycloak-for-identity-and-access-management.md`. An
  earlier draft of `.gitignore` also incorrectly ignored
  `.mvn/wrapper/maven-wrapper.jar`; this was removed so the wrapper JAR can
  be committed once generated.

## Deferred Work

- Maven Wrapper generation (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`) — to be
  generated with the official Maven command once local Maven is verified.
- `CODE_OF_CONDUCT.md` and `CODEOWNERS`.
- Checkstyle, Spotless, PMD, JaCoCo, SonarQube/SonarCloud configuration.
- Spring Boot BOM, Spring Cloud BOM, Testcontainers BOM in
  `dependencyManagement`.
- Any `<modules>` entry or service module `pom.xml`.
- Docker Compose, Keycloak, Kafka, database configuration.
- GitHub Actions / CI pipelines.
- Next.js frontend scaffolding.

## Next Day

Introduce the first service module (likely `device-service`) per the
approved roadmap: its own `pom.xml` registered as a single `<module>` entry
in the root POM, Spring Boot starter dependencies actually used by that
service, a minimal domain model, Flyway baseline migration, and the first
REST endpoint with corresponding unit, persistence (Testcontainers), and
controller tests — proposed first as an analysis per the Day 02 working
method, before any code is written.
