# Feature Specification: Include Verdict in SJP Results — results context changes

**Feature Branch**: `CIMD-3915-informant-register-local-schema`
**Created**: 2026-06-16
**Status**: Draft
**Jira**: CIMD-3915
**Sprint**: 62
**Scope**: `cpp-context-results` only

## Overview

The results context must include structured verdict data in the `results.prosecutor-results` query response so that downstream consumers can accurately report the legal outcome for each SJP offence. Two enabling changes are required:

1. Decouple the informant register offence schema from the shared core-domain — own it locally so it can evolve independently.
2. Replace the flat `verdictCode` string on the offence with a structured `verdict` object (`verdictCode`, `verdictDate`, `verdictType`).

The delivery surface is the `results.prosecutor-results` query. The command side (`add-informant-register`) is the ingest path that must accept and store the enriched offence data.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add Informant Register Accepts and Stores Enriched Verdict Data (Priority: P1)

The `results.add-informant-register` command accepts an informant register document request containing offences with the structured `verdict` object and records it using a locally-owned schema.

**Why this priority**: This is the ingest entry point. Downstream generation and query flows depend on correctly stored data.

**Independent Test**: Submit `results.add-informant-register` with an offence carrying a `verdict` object; confirm the recorded domain event stores all three verdict fields. Confirm a request with the old flat `verdictCode` field fails validation against the new schema.

**Acceptance Scenarios**:

1. **Given** a valid `add-informant-register` command where an offence carries a `verdict` object with `verdictCode`, `verdictDate`, and `verdictType`, **When** the command is received, **Then** the domain event is stored with all three verdict fields present on the offence.

2. **Given** a valid `add-informant-register` command where an offence carries no `verdict` object, **When** the command is received, **Then** the event is stored without verdict data (verdict is optional on an offence).

3. **Given** an `add-informant-register` command where an offence carries a legacy flat `verdictCode` string (old schema), **When** the command is received, **Then** the request is rejected with a validation error.

---

### User Story 2 - Generate Informant Register Produces Enriched Generation Event (Priority: P2)

The `results.generate-informant-register` and `results.generate-informant-register-by-date` commands produce a generation event that carries informant register document requests using the locally-owned schema, including the structured verdict object on offences.

**Why this priority**: Generation reads from stored records and publishes events consumed downstream; it must propagate the enriched data correctly.

**Independent Test**: Trigger `results.generate-informant-register` against stored records with verdict data; confirm the generated domain event contains offences with the `verdict` object intact.

**Acceptance Scenarios**:

1. **Given** stored informant register records include offences with a `verdict` object, **When** `results.generate-informant-register` is triggered, **Then** the `informant-register-generated` domain event carries those offences with the structured verdict data.

2. **Given** stored informant register records include offences with no `verdict` object, **When** `results.generate-informant-register-by-date` is triggered, **Then** the `informant-register-generated` domain event carries those offences without a verdict field.

3. **Given** no informant register records exist in `RECORDED` status, **When** `results.generate-informant-register` is triggered, **Then** no events are emitted and the command completes silently.

---

### User Story 3 - Prosecutor Results Query Returns Structured Verdict on Offences (Priority: P1)

The `results.prosecutor-results` query response includes a `verdict` object on each offence where a verdict was recorded, using the verdict type mapping from CIMD-3915.

**Why this priority**: This is the primary delivery surface for this ticket within the results context.

**Independent Test**: Query `results.prosecutor-results` for a prosecution authority with stored offences covering FOUND_GUILTY, FOUND_NOT_GUILTY, PROVED_SJP, NO_VERDICT, and null verdict; confirm each offence is returned with the correct verdict shape per the mapping table.

**Acceptance Scenarios**:

1. **Given** an offence has verdict FOUND_GUILTY, **When** `results.prosecutor-results` is queried, **Then** the offence includes `verdict` with `verdictCode: "G"` (mandatory), `verdictDate` in `yyyy-MM-dd` (mandatory when verdictCode present, omitted if conviction date is null), and optionally `verdictType: "FOUND_GUILTY"`.

2. **Given** an offence has verdict FOUND_NOT_GUILTY (decision type DISMISS), **When** `results.prosecutor-results` is queried, **Then** the offence includes `verdict` with `verdictCode: "N"`, `verdictDate`, and optionally `verdictType: "FOUND_NOT_GUILTY"`.

3. **Given** an offence has verdict PROVED_SJP (proved in absence, no plea), **When** `results.prosecutor-results` is queried, **Then** the offence includes `verdict` with `verdictCode: "PSJ"`, `verdictDate`, and optionally `verdictType: "PROVED_SJP"`.

4. **Given** an offence has verdict NO_VERDICT or null verdict, **When** `results.prosecutor-results` is queried, **Then** the offence does NOT contain a `verdict` field (omitted entirely — not null).

5. **Given** a case has multiple offences where some have verdicts and some do not, **When** `results.prosecutor-results` is queried, **Then** each offence is assessed independently — no verdict is mixed or defaulted from one offence to another.

---

### User Story 4 - Pre-migration Events Remain Replayable (Priority: P2)

Existing domain events stored before this change (carrying the old flat `verdictCode` string) can still be replayed against the aggregate and processed by the event listener without causing runtime errors.

**Why this priority**: Event sourcing requires backward-compatible replay. Failure here silently breaks historical reconstitution.

**Independent Test**: Replay a pre-migration `informant-register-recorded` event (with flat `verdictCode`) through the aggregate and event listener; confirm no exception is thrown and the state is consistent.

**Acceptance Scenarios**:

1. **Given** a pre-migration `informant-register-recorded` event exists in the event store with a flat `verdictCode` field on an offence, **When** the event is replayed, **Then** no runtime exception is thrown and the aggregate state is consistent.

2. **Given** a pre-migration `informant-register-generated` event exists with flat `verdictCode` offences, **When** the event is replayed by the event listener, **Then** no runtime exception is thrown.

---

### Edge Cases

- `verdictCode` and `verdictDate` are co-dependent: if one is present the other must be present; if one is absent both are absent.
- `verdictType` is optional — it is populated from reference data; if the lookup returns no match, `verdictType` is omitted but the rest of the verdict object (`verdictCode`, `verdictDate`) is still returned.
- An offence with an entirely absent `verdict` object is valid — verdict is optional on an offence.
- A `verdict` object with none of its fields set is equivalent to no verdict — the field should be omitted rather than serialised as `{}`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The `results.add-informant-register` command payload schema MUST be owned locally within the results context with no runtime `$ref` to any core-domain schema.
- **FR-002**: The `results.generate-informant-register` and `results.generate-informant-register-by-date` command handling MUST use the locally-owned informant register document types.
- **FR-003**: The local informant register offence schema MUST replace the flat `verdictCode` string field with an optional `verdict` object containing: `verdictCode` (string), `verdictDate` (string, `yyyy-MM-dd`), and `verdictType` (string) — all fields optional within the object.
- **FR-004**: `verdictCode` and `verdictDate` MUST be co-reported: both present or both absent.
- **FR-005**: New versioned domain events MUST be introduced for the add-informant-register and generate-informant-register flows that carry offences in the new local schema shape, coexisting with existing events in the event store.
- **FR-006**: The `ProsecutionAuthorityAggregate` MUST be updated to use locally-owned informant register document types.
- **FR-007**: The `results.prosecutor-results` query response MUST include the `verdict` object on offences per the verdict type mapping (FR-003), and MUST omit the field entirely (not null) when verdict is NO_VERDICT or null.
- **FR-008**: When verdict reference data is unavailable for an offence, the system MUST omit `verdictType` but still include `verdictCode` and `verdictDate` where available; the offence MUST still be returned and a warning logged.
- **FR-009**: Each offence MUST be assessed independently; verdict from one offence MUST NOT influence another.
- **FR-010**: All new local schemas MUST use the correct results-context namespace matching sibling schemas in the same descriptor.
- **FR-011**: Pre-migration events already stored in the event store MUST remain replayable without runtime errors — verified by unit test.

### Key Entities

- **InformantRegisterOffence (local)**: `offenceCode` (required), `offenceTitle` (required), `orderIndex` (required), `originatingCaseUrn` (optional), `pleaValue` (optional), `verdict` object (optional), `offenceResults` (optional).
- **Verdict**: `verdictCode` (string — "G", "N", "PSJ"), `verdictDate` (string — `yyyy-MM-dd`), `verdictType` (string — "FOUND_GUILTY", "FOUND_NOT_GUILTY", "PROVED_SJP"). All optional; `verdictCode` and `verdictDate` are co-dependent.
- **InformantRegisterDocumentRequest (local)**: Full document — hearing venue, defendants, cases/applications, offences. No external schema references.

### Verdict Type Mapping

| Internal verdict type | Verdict code | `verdictType` in payload | Verdict field in payload |
|-----------------------|--------------|--------------------------|--------------------------|
| FOUND_GUILTY          | G            | FOUND_GUILTY (optional)  | Included                 |
| FOUND_NOT_GUILTY      | N            | FOUND_NOT_GUILTY (opt.)  | Included                 |
| PROVED_SJP            | PSJ          | PROVED_SJP (optional)    | Included                 |
| NO_VERDICT            | (absent)     | (absent)                 | Omitted entirely         |
| null (not set)        | (absent)     | (absent)                 | Omitted entirely         |

## Success Criteria *(mandatory)*

- **SC-001**: All unit tests for the command handler, aggregate, event listener, event processor, and query view pass with no failures.
- **SC-002**: `mvn clean install` on all affected modules completes with zero compilation errors and zero test failures.
- **SC-003**: Zero `$ref` references to `http://justice.gov.uk/core/courts/informantRegisterDocument/` remain in any command API schema or domain event schema for the informant-register flows.
- **SC-004**: The `results.prosecutor-results` query response schema validates against a payload containing the structured `verdict` object on an offence.
- **SC-005**: A unit test confirms replaying a pre-migration event (with flat `verdictCode`) does not throw an exception.

## Development Constraints

- **TDD is mandatory (Constitution Principle VIII)**: Every production code change MUST be preceded by a failing unit test that fails for the correct reason (assertion failure, not a compilation error). The commit history must show failing tests authored at or before the corresponding production code. This applies to: new/updated command handler methods, aggregate command and apply methods, event listener converters, event processor converters, query view methods, and all new local POJO types.
- **Missing tests first**: Before any schema migration or production code change is made, any gap in existing test coverage for the affected classes must be filled with failing tests. Existing tests for `InformantRegisterHandlerTest`, `ProsecutionAuthorityAggregateTest`, `InformantRegisterEventListenerTest`, `InformantRegisterEventProcessorTest`, and `ProsecutorResultsQueryViewTest` must be reviewed and supplemented where coverage is absent.
- **Three-layer discipline**: Every change touching a domain event must be assessed across all three layers — command side, event listener, event processor — with tests at each layer before production code.

## Assumptions

- `results.generate-informant-register-by-date.json` command schema is already self-contained (no core-domain reference) — no schema migration needed for that file.
- `InformantRegisterNotified`, `InformantRegisterNotifiedV2`, and `InformantRegisterNotificationIgnored` events are out of scope — they carry no offence data.
- Local Java types are hand-written POJOs (builder pattern, no Lombok) replacing the core-domain generated classes for the informant-register flows.
- The integration test (`InformantRegisterDocumentRequestIT`) is deferred — it will need updating separately after the unit-test-driven implementation is complete.
- `verdictType` is populated via a reference data lookup; if unavailable, it is omitted from the verdict object without failing the response.
- The plea value reporting issue noted in the Jira comments is out of scope for this ticket.
- **Dual-emit decision**: The command handler emits **V2 only** for all new ingest (both `add-informant-register` and `processRequests`). V1 events already persisted in the event store are handled by the existing V1 listener/processor handlers and are not re-emitted. V1 handlers are retained for backward-compatible replay (US4) but receive no new writes after this change.
