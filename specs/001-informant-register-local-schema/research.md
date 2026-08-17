# Research: Include Verdict in SJP Results — results context

**Date**: 2026-06-16 | **Branch**: `CIMD-3915-informant-register-local-schema`

## Decision 1: Event Versioning Strategy

**Decision**: Introduce new V2 domain events (`results.event.informant-register-recorded-v2`, `results.event.informant-register-generated-v2`) that carry the new local schema, while keeping existing V1 event handlers for backward-compatible replay of historical events.

**Rationale**: The existing event store contains pre-migration events carrying the old schema (core-domain references, flat `verdictCode`). These must remain replayable without errors (FR-012, SC-005). The project already uses this pattern — `InformantRegisterNotifiedV2` was introduced alongside `InformantRegisterNotified` — confirming it is the team's established approach.

**Alternatives considered**:
- In-place schema mutation (no versioning) — rejected: would break replay of historical events already in the event store.
- Migration job to rewrite historic events — rejected: out of scope; event sourcing requires immutable event history.

---

## Decision 2: Local POJO Strategy

**Decision**: Hand-write local POJO types in `results-domain/results-domain-common` under the package `uk.gov.moj.cpp.results.domain.informant.model` to replace the core-domain generated classes used in the informant-register flows. Each POJO uses explicit constructors, getters, and a nested `Builder` class (no Lombok, no Jackson codegen).

**Rationale**: The core-domain generated classes are produced by a codegen step that cannot be controlled from this context. Owning local POJOs is the only way to evolve the schema (e.g., replace `verdictCode` with `verdict` object) independently. The `results-domain-common` module already hosts shared domain helpers (`InformantRegisterHelper`) making it the correct home.

**Alternatives considered**:
- Keep using core-domain types and add a `verdict` enrichment adapter — rejected: still requires core-domain codegen to expose the new field, defeating the decoupling goal.
- Generate POJOs from local JSON schemas using the CPP codegen plugin — rejected: adds build complexity; constitution Principle III confirms hand-written POJOs are standard where codegen is not already wired.

**Types needed** (all in `uk.gov.moj.cpp.results.domain.informant.model`):

| Local type | Replaces core-domain type | Key change |
|------------|--------------------------|------------|
| `InformantRegisterDocumentRequest` | `uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest` | Local refs |
| `InformantRegisterHearingVenue` | `...InformantRegisterHearingVenue` | Local refs |
| `InformantRegisterHearing` | `...InformantRegisterHearing` | Local refs |
| `InformantRegisterDefendant` | `...InformantRegisterDefendant` | Local refs |
| `InformantRegisterCaseOrApplication` | `...InformantRegisterCaseOrApplication` | Local refs |
| `InformantRegisterOffence` | `...InformantRegisterOffence` | `verdict: Verdict` replaces `verdictCode: String` |
| `Verdict` | *(new)* | `verdictCode`, `verdictDate`, `verdictType` |
| `InformantRegisterResult` | `...InformantRegisterResult` | Local refs |
| `InformantRegisterResultData` | `...InformantRegisterResultData` | No change |
| `InformantRegisterRecipient` | `...InformantRegisterRecipient` | No change |
| `ProsecutorResult` | `...ProsecutorResult` | Uses local `InformantRegisterHearingVenue` |

---

## Decision 3: Local JSON Schema Layout

**Decision**: Place all local informant-register sub-schemas under `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/` with namespace `http://justice.gov.uk/results/courts/informantRegisterDocument/...` (matching the sibling V2 event namespace pattern). Command schema and query schema keep their existing file paths but replace the `$ref` with a local namespace ref.

**Rationale**: The existing V2 event schemas use the `http://justice.gov.uk/results/courts/...` namespace (confirmed from `informant-register-notified-v2.json`). New local sub-schemas should use `http://justice.gov.uk/results/courts/informantRegisterDocument/...` for consistency.

**Namespace mapping**:

| Schema file | Old `$id` / `$ref` | New `$id` |
|-------------|---------------------|-----------|
| `informantRegisterOffence.json` (local) | `http://justice.gov.uk/core/courts/informantRegisterDocument/informantRegisterOffence.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterOffence.json` |
| `informantRegisterDocumentRequest.json` (local) | `http://justice.gov.uk/core/courts/informantRegisterDocument/informantRegisterDocumentRequest.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterDocumentRequest.json` |
| `informant-register-recorded-v2.json` | *(new)* | `http://justice.gov.uk/results/courts/informant-register-recorded-v2.json` |
| `informant-register-generated-v2.json` | *(new)* | `http://justice.gov.uk/results/courts/informant-register-generated-v2.json` |
| `results.add-informant-register.json` (command) | `$ref` to core-domain | Self-contained with local refs |
| `results.prosecutor-results.json` (query) | `$ref` to core-domain `prosecutorResult.json` | Local inline with `verdict` object |

---

## Decision 4: Command Handler Emit Strategy

**Decision**: `handleAddInformantRegisterToEventStream` will emit the existing `InformantRegisterRecorded` event (V1, kept for backward compat with any downstream that still replays V1 events) AND a new `InformantRegisterRecordedV2` event carrying the local type. The listener adds a V2 handler alongside the existing V1 handler.

**Rationale**: Dual-emit ensures both old replays (via V1) and new downstream (via V2) work correctly. The `processRequests` method similarly emits `InformantRegisterGeneratedV2` for the generate commands.

**Alternatives considered**:
- Emit only V2 — rejected: breaks replay of the listener against historical V1 events (listener subscribed to V1 would miss new events; V2-only listener would miss historical events).
- Emit only V1 with an updated payload schema — rejected: backward-incompatible schema change on an existing event is a runtime risk.

---

## Decision 5: ProsecutorResultsQueryView Migration

**Decision**: `ProsecutorResultsQueryView` will be updated to use local `InformantRegisterDocumentRequest` and local `ProsecutorResult` types. The payload stored in `InformantRegisterEntity.payload` (as a JSON string) will be deserialized to the local `InformantRegisterDocumentRequest`, which includes the `verdict` object on offences. No viewstore schema change is required.

**Rationale**: The payload is stored as a raw JSON string in the `InformantRegisterEntity`. As long as newly ingested records contain the `verdict` object in the JSON, the query view will naturally return it. Pre-migration records (without `verdict`) will simply have no `verdict` field in the response — consistent with FR-003 (omit when absent).

**Alternatives considered**:
- Add a separate `verdictCode` column to the viewstore entity — rejected: the payload is stored as a JSON blob; extracting a field to a column adds Liquibase migration work and is unnecessary for read-only query purposes.

---

## Decision 6: InformantRegisterDocument CSV Model

**Decision**: The CSV `InformantRegisterDocument` model's `verdictCode` field is retained but now populated from `offence.getVerdict().getVerdictCode()` (null-safe, returning empty string if no verdict). The CSV format does not change.

**Rationale**: The CSV format is consumed by downstream file recipients and is out of scope for the verdict object change. The CSV only needs the `verdictCode` string, which is still available as `verdict.verdictCode`.

---

## Scope Confirmation — Three-Layer Impact

| Layer | Events affected | Action |
|-------|----------------|--------|
| Command side | `informant-register-recorded-v2` (new), `informant-register-generated-v2` (new) | Handler emits V2; aggregate uses local types |
| Event listener | `informant-register-recorded-v2` (new), `informant-register-generated-v2` (new); V1 handlers kept | Add V2 handler methods; keep V1 for backward compat |
| Event processor | `informant-register-generated-v2` (new); existing `informant-register-generated` V1 kept for CSV | Add V2 handler; update `buildOffenceDetails` to read `verdict` object |
| Query | `results.prosecutor-results` | Schema + view updated to include `verdict` object |

**Not affected** (explicitly out of scope):
- `informant-register-notified`, `informant-register-notified-v2`, `informant-register-notification-ignored` — carry no offence data
- Financial results aggregates, results aggregate, defendant aggregate
- All non-informant-register event flows
