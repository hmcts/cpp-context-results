# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service is

`results` is one of the HMCTS CPP bounded contexts — the **resulting** context. It receives hearing and financial results, persists them as event-sourced aggregates, projects them into a read-model, and publishes downstream: police/DVLA results, financial results and GoB enforcement, DCS document publishing, NCES email notifications, and informant-register notifications.

Built on the CPP framework (`uk.gov.moj.cpp.common:service-parent-pom`), packaged as WARs, deployed to WildFly. Java 17. CDI for DI — no Spring, no Lombok.

## Build, test, run

```bash
# Full build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Build + unit tests
mvn clean install

# Single module (with deps)
mvn -pl results-domain/results-domain-aggregate -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName
```

### Integration tests

The `results-integration-test` module is **not** run by `mvn verify`. It needs WildFly + Postgres + ActiveMQ in Docker first:

```bash
./runIntegrationTests.sh
```

Prerequisites:
- `CPP_DOCKER_DIR` env var pointing at a local checkout of `hmcts/cpp-developers-docker`.
- Docker daemon running and authenticated to the `crmdvrepo01` registry.

The script runs Liquibase (event log, aggregate snapshot, event buffer, viewstore, system, event tracking, file service) → deploys WireMock stubs → deploys WARs → healthchecks → runs ITs.

Once the env is up, run a single IT against it:

```bash
mvn -pl results-integration-test test -Dit.test=ClassNameIT
```

### Framework JMX commands

```bash
./runSystemCommand.sh           # help
./runSystemCommand.sh --list    # list available commands (CATCHUP, etc.)
./runSystemCommand.sh CATCHUP   # run one
```

### CI

Azure DevOps (`azure-pipelines.yaml`):
- PR builds → `pipelines/context-verify.yaml` (Sonar + unit tests).
- `IndividualCI` on `main` / `team/*` → `pipelines/context-validation.yaml` with `serviceName=results` and `itTestFolder=results-integration-test`.
- `dev/release-*` branches are excluded.
- Agent pool: `MDV-ADO-AGENT-AKS-01`, demand `centos8-j17`.
- SonarQube project: `uk.gov.moj.cpp.results:results-parent`.

## Architecture — the three layers you must reason across

Every change touching events needs to be reasoned about across **three layers**. Breaking one without the others produces silent data drift:

1. **Command side** — RAML-declared commands hit `@Handles`-annotated handler classes (`ResultsCommandHandler`, `InformantRegisterHandler`, `StagingEnforcementResponseHandler`, `NcesDocumentNotificationCommandHandler`, `MigratedStagingEnforcementResponseHandler`, …) which load an aggregate, run validation/financial rules, and emit domain events. State is mutated only via the aggregate's `apply(event)` handlers (framework `Aggregate` event-sourcing — no separate mutator class).
2. **Event listener** — projects domain events into the viewstore DB (`DS.results`). Lives under `results-event/results-event-listener`. Heavy use of converters mapping events → JPA entities.
3. **Event processor** — consumes domain events and emits **public** events for downstream contexts. Lives under `results-event/results-event-processor`. Heavy use of converters mapping internal events → downstream public schemas.

### Aggregates

- Case-side: `ResultsAggregate`, `DefendantAggregate`, `ProsecutionAuthorityAggregate`.
- Financial: `HearingFinancialResultsAggregate`, `HearingFinancialResultGobAccountAggregate`, `MigratedInactiveHearingFinancialResultsAggregate` (fine/non-fine outcomes, GoB account linkage, SJP/NCES, migrated-inactive hearings).

### Three subscription sources

The processor and listener can be triggered by:

1. **Internal event topic** `results.event` — replay of this context's own domain events.
2. **Public event bus** `public.event` — events from other contexts (progression, material, sjp, staging contexts).
3. **Command queue** `results.handler.command` — RAML-declared commands.

### Authoritative routing files (always re-read before reasoning about a flow)

- `results-event-sources/src/yaml/event-sources.yaml` — internal + public topic declarations.
- `results-event/results-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `results-event/results-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `results-event/results-event-processor/src/yaml/public-publications-descriptor.yaml` — published public events (e.g. `public.results.police-result-generated`).
- `results-command/results-command-handler/src/raml/results-command-handler.messaging.raml` — command → handler mapping.
- Per-command/per-event JSON schemas: `src/raml/json/schema/` and `src/main/resources/json/schema/` under each module (notably `results-domain/results-domain-common/src/main/resources/json/schema/`).

### Data stores

- `java:/app/results-service/DS.eventstore` — event store (event-repository-liquibase + aggregate-snapshot-repository-liquibase).
- `java:/DS.results` — viewstore (event-buffer-liquibase + `results-viewstore-liquibase`).

## Critical gotchas — when adding/removing an event

1. **Always update both** the relevant `subscriptions-descriptor.yaml` (and `public-publications-descriptor.yaml` for published events) **and** the JSON schema under `*/src/main/resources/json/schema/`. A subscription without a matching schema produces a runtime 500 on dispatch.
2. **Mind the namespace.** This service uses **three** schema `id` / `schema_uri` namespaces — `http://cpp.moj.gov.uk/hearing/json/schemas/...`, `http://justice.gov.uk/json/schemas/...`, `http://moj.gov.uk/cpp/json/schemas/...`. A schema in the wrong namespace fails dispatch even if otherwise valid. Match the sibling events in the same descriptor.

## Module layout (high-level)

- `results-command` → `-command-api` (RAML + schemas), `-command-handler` (`@Handles` handlers + messaging RAML).
- `results-domain` → `-domain-aggregate` (aggregates + rules), `-domain-common` (shared types + event schemas), `-domain-event`, `-domain-transformations` (stream-transformation-tool wiring).
- `results-event` → `-event-listener`, `-event-processor`.
- `results-event-sources` — `event-sources.yaml`.
- `results-query` → `-query-api` (RAML), `-query-view` (read services over the viewstore).
- `results-viewstore` → `-viewstore-liquibase` (changelogs for `DS.results`), `-viewstore-persistence` (JPA entities/repos).
- `results-json` — shared JSON resources.
- `results-service` — packaging WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources/queues/topics, service mapping `/results-[^/]+`.
- `results-healthchecks` — healthchecks.
- `results-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`; `results-performance-test`; `test-utilities`.

## Key version pins (in `pom.xml`)

Parent `service-parent-pom:17.103.9`. Current artifact `results-parent:17.103.108-SNAPSHOT`. Notable upstream pins: `coredomain=17.103.13`, `referencedata=17.103.131`, `notification.notify=17.104.44`, `systemdocgenerator=17.104.111`, `progression=17.0.252`, `material=17.0.79`, `usersgroups=17.104.47`, `sjp=17.103.169`, `staging.dcs=17.103.11`. When bumping any of these, also check the matching schema/RAML classifier dep is on the same version.

## Java style

No wildcard imports. Always use explicit per-class imports. No Spring, no Lombok.

## Spec-driven workflow (Spec Kit)

This repo uses Spec Kit. For non-trivial work use:
`/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement → /speckit-analyze`.
`/speckit-specify` triggers the `before_specify` hook which creates a Jira-prefixed feature branch (`DD-XXXXX-<slug>`). The authoritative principles live in `.specify/memory/constitution.md`; `.claude/rules/*.md` are quick-reference and must be kept in sync. Reviewer agents (`code-reviewer`, `qa`, `spec-validator`) report only — the primary agent or a human applies fixes.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
