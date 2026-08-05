# Quickstart: CIMD-3915 — Include Verdict in SJP Results

**Branch**: `CIMD-3915-informant-register-local-schema`
**Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

## What this change does

Replaces the core-domain dependency for informant-register schemas with locally-owned schemas and POJOs. Adds a structured `verdict` object (`verdictCode`, `verdictDate`, `verdictType`) to offences, replacing the flat `verdictCode` string. Exposes verdict data through the `results.prosecutor-results` query API.

## Key commands

```bash
# Run unit tests on a single module
mvn -pl results-domain/results-domain-common test
mvn -pl results-domain/results-domain-aggregate test
mvn -pl results-command/results-command-handler test
mvn -pl results-event/results-event-listener test
mvn -pl results-event/results-event-processor test
mvn -pl results-query/results-query-view test

# Full build with unit tests
mvn clean install

# Full build, skip tests (for iterating on compile errors)
mvn clean install -DskipTests
```

## Implementation order (TDD)

Follow the phases in [plan.md](plan.md) strictly:

1. **Phase 0** — Write all JSON schemas and update subscription descriptors first (no Java)
2. **Phase 1** — Write local POJO tests, then POJOs (deepest types first: `Verdict` → `InformantRegisterOffence` → …)
3. **Phase 2** — Command handler: gap-fill tests, then V2 emit tests, then production handler changes
4. **Phase 3** — Aggregate: gap-fill tests, then V2 apply test, then production aggregate change
5. **Phase 4** — Event listener: gap-fill tests, then V2 handler tests, then production handlers
6. **Phase 5** — Event processor: gap-fill tests, then verdict extraction test, then V2 handler test, then production changes
7. **Phase 6** — Query view: gap-fill tests, then verdict-in-response test, then production view change
8. **Phase 7** — Backward compat tests (V1 replay)
9. **Phase 8** — `mvn clean install` (full build gate)

## New local packages

| Package | Purpose |
|---------|---------|
| `uk.gov.moj.cpp.results.domain.informant.model` | 11 hand-written POJO types (in `results-domain-common`) |
| `uk.gov.moj.cpp.results.domain.event` | `InformantRegisterRecordedV2`, `InformantRegisterGeneratedV2` |

## New JSON schemas

All under `results-domain/results-domain-common/src/main/resources/json/schema/`:

- `informantRegisterDocument/verdict.json` — new
- `informantRegisterDocument/informantRegisterOffence.json` — local copy with `verdict` object
- 9 other sub-schemas — local copies of core-domain sub-schemas
- `results.event.informant-register-recorded-v2.json` — new V2 event schema
- `results.event.informant-register-generated-v2.json` — new V2 event schema

## What is NOT changing

- V1 event handlers (`informant-register-recorded`, `informant-register-generated`) — kept for backward-compat replay
- Viewstore schema — no DDL change; verdict lives in the existing `payload` JSON column
- `informant-register-notified` / `informant-register-notified-v2` — out of scope (carry no offence data)
- CSV format — `verdictCode` string is still present; now sourced from `verdict.verdictCode`

## Constitution gates (must not skip)

- Contracts before Java (Principle I) — Phase 0 must be complete before Phase 1
- Three-layer discipline (Principle II) — all three layers must be tested and updated
- TDD (Principle VIII) — failing test before every production code change
- Schema-subscription symmetry (Principle VI) — every new event has both a schema file AND a subscription entry
