# Tasks: CIMD-3915 — Include Verdict in SJP Results

**Input**: `specs/001-informant-register-local-schema/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**TDD discipline (Constitution Principle VIII)**: Every production code task MUST be preceded by its paired failing-test task. Write the test, confirm it fails for the right reason (assertion, not compilation), then write the production code.

> **Phase mapping — plan.md vs tasks.md**:
> | tasks.md phase | plan.md phase | Scope |
> |---------------|---------------|-------|
> | Phase 1 (Contracts) | Phase 0 | JSON schemas + subscription descriptors |
> | Phase 2 (POJOs) | Phase 1 | Local Java types |
> | Phase 3 (US1) | Phases 2 + 3 | Command handler + aggregate |
> | Phase 4 (US3) | Phase 6 | Query view |
> | Phase 5 (US2) | Phases 4 + 5 | Event listener + processor |
> | Phase 6 (US4) | Phase 7 | Pre-migration backward compat |
> | Phase 7 (Polish) | Phase 8 | Full build + hygiene |

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[Story]**: User story — US1/US2/US3/US4 (from spec.md)
- Exact file paths included in all descriptions

---

## Phase 1: Foundational — Contracts & Schemas (No Java)

**Purpose**: All JSON schemas and subscription descriptor entries MUST exist before any Java is written (Constitution Principle I: Contract First).

**⚠️ CRITICAL**: No Java task (Phase 2+) may start until T018 (build gate) passes.

- [ ] T001 Create directory `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/` and write `verdict.json` with `$id: http://justice.gov.uk/results/courts/informantRegisterDocument/verdict.json` — properties: `verdictCode`, `verdictDate`, `verdictType` (all optional strings)
- [ ] T002 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterOffence.json` — replaces flat `verdictCode` with `"verdict": { "$ref": "...verdict.json" }`; required: `offenceCode`, `orderIndex`, `offenceTitle`
- [ ] T003 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterResult.json` — properties: `resultText` (required), `cjsResultCode`, `resultData` ($ref to resultData)
- [ ] T004 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterResultData.json` — properties: `amount`, `nextHearingDate`, `nextCourtLocation`, `durationValue`, `durationUnit`, `durationStartDate`, `durationEndDate`, `secondaryDurationValue`, `secondaryDurationUnit` (all optional strings)
- [ ] T005 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterCaseOrApplication.json` — required: `caseOrApplicationReference`; offences array $ref to offence schema
- [ ] T006 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterDefendant.json` — required: `name`, `address1`; optional: address2-5, postCode, dateOfBirth, nationality, firstName, lastName; prosecutionCasesOrApplications array
- [ ] T007 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterHearing.json` — required: `courtRoom`, `hearingStartTime`, `defendants` (array, minItems 1)
- [ ] T008 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterHearingVenue.json` — required: `courtHouse`, `courtSessions` (array, minItems 1); optional: `ljaName`
- [ ] T009 [P] Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterRecipient.json` — required: `recipientName`, `emailAddress1`, `emailTemplateName`; optional: `emailAddress2`
- [ ] T010 Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/informantRegisterDocumentRequest.json` — required: `registerDate`, `hearingDate`, `hearingId`, `prosecutionAuthorityId`, `prosecutionAuthorityCode`, `fileName`, `hearingVenue`; $refs all local sub-schemas [after T001–T009]
- [ ] T011 Write `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/prosecutorResult.json` — required: `startDate`, `prosecutionAuthorityId`, `prosecutionAuthorityCode`; `hearingVenues` array $ref to local hearingVenue schema [after T010]
- [ ] T012 Write `results-domain/results-domain-common/src/main/resources/json/schema/results.event.informant-register-recorded-v2.json` — `$id: http://justice.gov.uk/results/courts/informant-register-recorded-v2.json`; required: `prosecutionAuthorityId`, `informantRegister` ($ref local documentRequest) [after T010]
- [ ] T013 Write `results-domain/results-domain-common/src/main/resources/json/schema/results.event.informant-register-generated-v2.json` — `$id: http://justice.gov.uk/results/courts/informant-register-generated-v2.json`; required: `informantRegisterDocumentRequests` (array $ref local documentRequest); optional: `systemGenerated` (boolean) [after T010]
- [ ] T014 Update `results-command/results-command-api/src/raml/json/schema/results.add-informant-register.json` — replace `$ref` to core-domain with `$ref: http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterDocumentRequest.json` [after T010]
- [ ] T015 Update `results-query/results-query-api/src/raml/json/schema/results.prosecutor-results.json` — replace `$ref` to core-domain `prosecutorResult.json` with `$ref: http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json` [after T011]
- [ ] T016 Add two V2 subscription entries to `results-event/results-event-listener/src/yaml/subscriptions-descriptor.yaml`: `results.event.informant-register-recorded-v2` (schema_uri: `http://justice.gov.uk/results/courts/informant-register-recorded-v2.json`) and `results.event.informant-register-generated-v2` (schema_uri: `http://justice.gov.uk/results/courts/informant-register-generated-v2.json`) [after T012, T013]
- [ ] T017 Add V2 subscription entry to `results-event/results-event-processor/src/yaml/subscriptions-descriptor.yaml`: `results.event.informant-register-generated-v2` (schema_uri: `http://justice.gov.uk/results/courts/informant-register-generated-v2.json`) [after T013]
- [ ] T018 Build gate: `mvn clean install -DskipTests` on affected modules must succeed — confirm zero compilation errors before proceeding to any Java task

**Checkpoint**: All contract artefacts exist and the project compiles. Phase 2 may now begin.

---

## Phase 2: Foundational — Local POJO Types (TDD)

**Purpose**: Hand-written local POJO types in `uk.gov.moj.cpp.results.domain.informant.model` (no Lombok, no Spring). These POJOs are required by US1, US2, US3, and US4 — they are blocking prerequisites for all user story phases.

**⚠️ TDD**: Every test task must be written and confirmed to FAIL (assertion failure or compilation failure) BEFORE its paired production-code task begins.

- [ ] T019 [P] Write failing test `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/informant/model/VerdictTest.java` — assert builder creates instance with `verdictCode`, `verdictDate`, `verdictType`; assert null fields tolerated; assert Jackson deserialization roundtrip from `{"verdictCode":"G","verdictDate":"2026-04-13","verdictType":"FOUND_GUILTY"}` preserves all fields
- [ ] T020 Write `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/informant/model/Verdict.java` — explicit constructor, getters, nested `Builder`; `@JsonCreator`/`@JsonProperty`; makes T019 green
- [ ] T021 [P] Write failing test `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/informant/model/InformantRegisterOffenceTest.java` — assert builder sets `verdict` field (type `Verdict`); assert no `getVerdictCode()` method exists on the class; assert JSON with `verdict` object deserializes correctly; assert JSON with no `verdict` produces null verdict
- [ ] T022 Write `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/informant/model/InformantRegisterOffence.java` — `verdict: Verdict` field; required fields `offenceCode`, `offenceTitle`, `orderIndex`; no `verdictCode` field; builder + getters; makes T021 green
- [ ] T023 [P] Write failing tests `InformantRegisterResultTest.java` and `InformantRegisterResultDataTest.java` in `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/informant/model/` — assert builder/getters and JSON deserialization roundtrips
- [ ] T024 [P] Write `InformantRegisterResult.java` and `InformantRegisterResultData.java` in `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/informant/model/` — builder pattern, Jackson annotations; makes T023 green
- [ ] T025 [P] Write failing tests `InformantRegisterCaseOrApplicationTest.java`, `InformantRegisterDefendantTest.java`, `InformantRegisterHearingTest.java`, `InformantRegisterHearingVenueTest.java`, `InformantRegisterRecipientTest.java` in `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/informant/model/` — assert builders, getters, JSON deserialization roundtrips
- [ ] T026 [P] Write `InformantRegisterCaseOrApplication.java`, `InformantRegisterDefendant.java`, `InformantRegisterHearing.java`, `InformantRegisterHearingVenue.java`, `InformantRegisterRecipient.java` in `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/informant/model/` — builder pattern, Jackson annotations; makes T025 green
- [ ] T027 Write failing test `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/informant/model/InformantRegisterDocumentRequestTest.java` — assert builder sets all required fields; assert JSON deserialization of a nested document (hearingVenue → courtSessions → defendants → offences → verdict) produces correct object graph
- [ ] T028 Write `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/informant/model/InformantRegisterDocumentRequest.java` — required: `registerDate` (ZonedDateTime), `hearingDate`, `hearingId`, `prosecutionAuthorityId`, `prosecutionAuthorityCode`, `fileName`, `hearingVenue`; optional: `prosecutionAuthorityOuCode`, `majorCreditorCode`, `prosecutionAuthorityName`, `recipients`, `groupId`; builder + getters; makes T027 green
- [ ] T029 [P] Write failing test `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/informant/model/ProsecutorResultTest.java` — assert builder sets `startDate`, `prosecutionAuthorityId`, `prosecutionAuthorityCode`; optional fields nullable
- [ ] T030 [P] Write `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/informant/model/ProsecutorResult.java` — builder + getters; makes T029 green
- [ ] T031 Write failing test `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/event/InformantRegisterRecordedV2Test.java` — assert event name constant equals `"results.event.informant-register-recorded-v2"`; assert builder sets `prosecutionAuthorityId` (UUID) and `informantRegister` (local `InformantRegisterDocumentRequest`)
- [ ] T032 Write `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/event/InformantRegisterRecordedV2.java` — public static final `NAME = "results.event.informant-register-recorded-v2"`; fields: `prosecutionAuthorityId`, `informantRegister`; builder + getters; makes T031 green
- [ ] T033 Write failing test `results-domain/results-domain-common/src/test/java/uk/gov/moj/cpp/results/domain/event/InformantRegisterGeneratedV2Test.java` — assert event name constant equals `"results.event.informant-register-generated-v2"`; assert builder sets `informantRegisterDocumentRequests` list and optional `systemGenerated`
- [ ] T034 Write `results-domain/results-domain-common/src/main/java/uk/gov/moj/cpp/results/domain/event/InformantRegisterGeneratedV2.java` — public static final `NAME = "results.event.informant-register-generated-v2"`; fields: `informantRegisterDocumentRequests` (List), `systemGenerated` (Boolean); builder + getters; makes T033 green
- [ ] T035 Build gate: `mvn -pl results-domain/results-domain-common test` — all tests green

**Checkpoint**: All local types exist and are tested. User story phases may now begin.

---

## Phase 3: US1 — Add Informant Register Ingest (Priority: P1) 🎯 MVP

**Goal**: The `results.add-informant-register` command handler accepts offences with a structured `verdict` object, emits `InformantRegisterRecordedV2`, and the `ProsecutionAuthorityAggregate` applies the V2 event using local types.

**Independent Test**: Submit `results.add-informant-register` with an offence carrying a `verdict` object with `verdictCode="G"`, `verdictDate="2026-04-13"`, `verdictType="FOUND_GUILTY"` → confirm the emitted event is `results.event.informant-register-recorded-v2` and `payload.informantRegister.hearingVenue.courtSessions[0].defendants[0].prosecutionCasesOrApplications[0].offences[0].verdict.verdictCode` equals `"G"`.

- [ ] T036 [US1] Gap-fill `results-command/results-command-handler/src/test/java/uk/gov/moj/cpp/results/command/handler/InformantRegisterHandlerTest.java` — add missing scenarios: offence with no verdict (allowed), offence with null defendants (edge case), verify existing V1 tests still pass
- [ ] T037 [US1] Write failing test in `InformantRegisterHandlerTest.java` — `handleAddInformantRegisterToEventStream_withVerdictObject_shouldEmitV2RecordedEvent`: build a command envelope with local `InformantRegisterDocumentRequest` carrying an offence with `verdict.verdictCode="G"` → assert `eventRepository.create` called with event name `results.event.informant-register-recorded-v2` and payload contains `verdict.verdictCode = "G"`; confirm test fails (V2 event not yet emitted)
- [ ] T071 [US1] Write failing test in `InformantRegisterHandlerTest.java` — `handleAddInformantRegisterToEventStream_withVerdictCodeButNoVerdictDate_shouldFail` (FR-004 co-dependency): build command envelope where an offence has `verdictCode: "G"` but no `verdictDate`; assert the command is rejected (schema validation failure or explicit handler guard); confirm test fails (handler not yet guarding this case). Note: `verdict.json` schema now enforces co-dependency via `dependencies` — verify whether the framework rejects this pre-handler via schema validation, and if so document that as the passing mechanism (no additional production code needed)
- [ ] T038 [US1] Update `results-command/results-command-handler/src/main/java/uk/gov/moj/cpp/results/command/handler/InformantRegisterHandler.java` — change `handleAddInformantRegisterToEventStream` to deserialize payload to local `InformantRegisterDocumentRequest` and emit `InformantRegisterRecordedV2`; emit V2 only (V1 retained in event store for historical replay per spec.md Assumptions); makes T037 green
- [ ] T039 [US1] Gap-fill `results-domain/results-domain-aggregate/src/test/java/uk/gov/moj/cpp/results/domain/aggregate/ProsecutionAuthorityAggregateTest.java` — review existing `shouldReturnInformantRegisterNotified` and `shouldReturnInformantRegisterIgnored`; add missing scenarios for V1 generated event apply if not already covered
- [ ] T040 [US1] Write failing test in `ProsecutionAuthorityAggregateTest.java` — `apply_informantRegisterGeneratedV2_shouldSetRecipientsFromLocalTypes`: create `InformantRegisterGeneratedV2` event with local `InformantRegisterDocumentRequest` containing `InformantRegisterRecipient`; apply to aggregate; assert `informantRegisterRecipients` is populated correctly; confirm test fails
- [ ] T041 [US1] Update `results-domain/results-domain-aggregate/src/main/java/uk/gov/moj/cpp/results/domain/aggregate/ProsecutionAuthorityAggregate.java` — add `apply(InformantRegisterGeneratedV2 event)` method reading local `InformantRegisterDocumentRequest.getRecipients()`; update imports from core-domain to local types; makes T040 green
- [ ] T042 [US1] Build gate: `mvn -pl results-domain/results-domain-common,results-command/results-command-handler,results-domain/results-domain-aggregate -am test` — all tests green

**Checkpoint**: US1 fully implemented and tested. The command ingest path accepts and stores verdict data.

---

## Phase 4: US3 — Prosecutor Results Query (Priority: P1)

**Goal**: The `results.prosecutor-results` query returns the structured `verdict` object on offences where a verdict was recorded; omits the field entirely when absent.

**Independent Test**: Store an `InformantRegisterEntity` with a payload JSON containing an offence with `"verdict": {"verdictCode": "G", "verdictDate": "2026-04-13", "verdictType": "FOUND_GUILTY"}`; call `getProsecutorResults`; assert response JSON includes `hearingVenues[0].courtSessions[0].defendants[0].prosecutionCasesOrApplications[0].offences[0].verdict.verdictCode = "G"`. Store a second entity with an offence without a `verdict` key; assert response offence has no `verdict` field.

- [ ] T043 [US3] Gap-fill `results-query/results-query-view/src/test/java/uk/gov/moj/cpp/results/query/view/ProsecutorResultsQueryViewTest.java` — add any missing scenarios for current behaviour (e.g., empty result set, multiple hearing venues, null defendants)
- [ ] T044 [US3] Write failing test in `ProsecutorResultsQueryViewTest.java` — `getProsecutorResults_whenOffenceHasVerdict_shouldIncludeVerdictInResponse`: store `InformantRegisterEntity` with payload JSON containing offence with `verdict.verdictCode: "G"`; call query; assert returned `ProsecutorResult` includes `offence.verdict.verdictCode = "G"`, `offence.verdict.verdictDate` present; confirm test fails (current code uses core-domain type with no `verdict` field)
- [ ] T045 [US3] Write failing test in `ProsecutorResultsQueryViewTest.java` — `getProsecutorResults_whenOffenceHasNoVerdict_shouldOmitVerdictField`: store entity with offence JSON that has no `verdict` key; assert response offence object serializes without `verdict` key (not `"verdict": null`)
- [ ] T046 [US3] Write failing test in `ProsecutorResultsQueryViewTest.java` — `getProsecutorResults_whenOffenceVerdictIsNull_shouldOmitVerdictField`: store entity with `"verdict": null` in offence JSON; assert response offence has no `verdict` key
- [ ] T072 [US3] Write failing test in `ProsecutorResultsQueryViewTest.java` — `getProsecutorResults_whenVerdictTypeUnavailable_shouldLogWarningAndStillReturnOffence` (FR-008): store entity with offence carrying `verdictCode: "G"` and `verdictDate`; configure verdict-type reference lookup to return empty/null for that code; call query; assert response offence includes `verdict.verdictCode` and `verdict.verdictDate` but omits `verdictType`; assert SLF4J logger captured a WARN message; confirm test fails (no warning log in current production code)
- [ ] T073 [US3] Write failing test in `ProsecutorResultsQueryViewTest.java` — `getProsecutorResults_whenVerdictIsEmptyObject_shouldOmitVerdictField`: store entity with offence JSON `"verdict": {}`; call query; assert response offence has no `verdict` key at all (neither `{}` nor `null`); confirm test fails. Note: may require `@JsonInclude(NON_EMPTY)` rather than `NON_NULL` on `InformantRegisterOffence.verdict`
- [ ] T047 [US3] Update `results-query/results-query-view/src/main/java/uk/gov/moj/cpp/results/query/view/ProsecutorResultsQueryView.java` — replace core-domain `InformantRegisterDocumentRequest` and `ProsecutorResult` with local types; add SLF4J WARN log when verdict-type lookup returns no result (FR-008); use `@JsonInclude(NON_EMPTY)` on `InformantRegisterOffence.verdict` to omit null and empty-object cases (T046 + T073); makes T044–T046, T072, T073 green
- [ ] T048 [US3] Build gate: `mvn -pl results-query/results-query-view -am test` — all tests green

**Checkpoint**: US3 fully implemented and tested. Query API returns structured verdict data.

---

## Phase 5: US2 — Generate Informant Register (Priority: P2)

**Goal**: `results.generate-informant-register` and `results.generate-informant-register-by-date` emit `InformantRegisterGeneratedV2`; the event listener and processor handle V2 generated events, propagating the structured verdict through to the CSV output.

**Independent Test**: Trigger `results.generate-informant-register` → confirm emitted event is `results.event.informant-register-generated-v2`; send that event to the listener → confirm entity status set to GENERATED; send to processor → confirm CSV row `verdictCode` equals `"G"` when offence verdict is `FOUND_GUILTY`.

- [ ] T050 [US2] Gap-fill `InformantRegisterHandlerTest.java` then write failing test `processRequests_shouldEmitInformantRegisterGeneratedV2`: review existing `processRequests` test coverage; add missing scenarios (e.g., empty document-requests list, null recipients); then write the V2 failing test — mock dependencies, confirm `processRequests` emits event with name `results.event.informant-register-generated-v2` and payload `informantRegisterDocumentRequests` list contains local `InformantRegisterDocumentRequest` with verdict data; confirm test currently fails (V2 not yet emitted)
- [ ] T051 [US2] Update `results-command/results-command-handler/src/main/java/uk/gov/moj/cpp/results/command/handler/InformantRegisterHandler.java` — change `processRequests` to emit `InformantRegisterGeneratedV2` using local types; makes T050 green
- [ ] T052 [US2] Gap-fill `results-event/results-event-listener/src/test/java/uk/gov/moj/cpp/results/event/listener/InformantRegisterEventListenerTest.java` — review existing test coverage; add missing scenarios if any (e.g., missing test for status transition)
- [ ] T053 [US2] Write failing test in `InformantRegisterEventListenerTest.java` — `saveInformantRegisterV2_shouldSaveEntityFromLocalDocumentRequest`: build `results.event.informant-register-recorded-v2` envelope with local `InformantRegisterDocumentRequest`; assert listener saves `InformantRegisterEntity` with correct `prosecutionAuthorityId`, `registerDate`, and raw `payload` JSON; confirm fails (no V2 handler yet)
- [ ] T054 [US2] Write failing test in `InformantRegisterEventListenerTest.java` — `generateInformantRegisterV2_shouldSetEntityStatusToGenerated`: build `results.event.informant-register-generated-v2` envelope; assert entity `status` updated to GENERATED; confirm fails
- [ ] T055 [US2] Add `@Handles("results.event.informant-register-recorded-v2")` method `saveInformantRegisterV2` to `results-event/results-event-listener/src/main/java/uk/gov/moj/cpp/results/event/listener/InformantRegisterEventListener.java`; deserializes local `InformantRegisterDocumentRequest`; reuses existing entity-save logic; makes T053 green
- [ ] T056 [US2] Add `@Handles("results.event.informant-register-generated-v2")` method `generateInformantRegisterV2` to `InformantRegisterEventListener.java`; updates entity status to GENERATED; makes T054 green
- [ ] T057 [US2] Gap-fill `results-event/results-event-processor/src/test/java/uk/gov/moj/cpp/results/event/processor/InformantRegisterEventProcessorTest.java` — add missing coverage: `buildOffenceDetails` with offence that has a `verdict` object but `verdictCode = null` (should produce empty string); `buildOffenceDetails` with no verdict object at all
- [ ] T058 [US2] Write failing test in `InformantRegisterEventProcessorTest.java` — `generateInformantRegister_whenOffenceHasVerdictCode_shouldPopulateVerdictCodeInCsvRow`: build event with offence `verdict.verdictCode = "G"`; assert `InformantRegisterDocument.verdictCode = "G"` in the CSV model; confirm fails (current code calls `offence.getVerdictCode()` which does not exist on local type)
- [ ] T059 [US2] Update `buildOffenceDetails` in `results-event/results-event-processor/src/main/java/uk/gov/moj/cpp/results/event/processor/InformantRegisterEventProcessor.java` — replace `offence.getVerdictCode()` with null-safe `offence.getVerdict() != null ? offence.getVerdict().getVerdictCode() : null`; makes T058 green
- [ ] T060 [US2] Write failing test in `InformantRegisterEventProcessorTest.java` — `generateInformantRegisterV2_shouldProduceSameCsvOutputAsV1Handler`: build `results.event.informant-register-generated-v2` envelope with the same data as a V1 test; assert the same CSV rows are produced; confirm fails (no V2 handler yet)
- [ ] T061 [US2] Add `@Handles("results.event.informant-register-generated-v2")` method `generateInformantRegisterV2` to `results-event/results-event-processor/src/main/java/uk/gov/moj/cpp/results/event/processor/InformantRegisterEventProcessor.java`; delegates to same CSV-generation logic as V1 handler; makes T060 green
- [ ] T062 [US2] Build gate: `mvn -pl results-command/results-command-handler,results-event/results-event-listener,results-event/results-event-processor -am test` — all tests green

**Checkpoint**: US2 fully implemented and tested. Generate flow emits V2 events; listener and processor handle them correctly.

---

## Phase 6: US4 — Pre-migration Event Replay (Priority: P2)

**Goal**: V1 events already in the event store (`informant-register-recorded`, `informant-register-generated`) replay through the listener without runtime errors, even though their offences carry a flat `verdictCode` string (old schema).

**Independent Test**: Replay a V1 `informant-register-recorded` event JSON (old schema with flat `verdictCode: "G"` on an offence) through the V1 `saveInformantRegister` listener handler → confirm no exception thrown and entity saved correctly.

- [ ] T063 [US4] Write passing regression test in `InformantRegisterEventListenerTest.java` — `saveInformantRegister_withPreMigrationFlatVerdictCode_shouldNotThrow`: build V1 `informant-register-recorded` envelope JSON where offence has `"verdictCode": "G"` (flat string, old core-domain schema); send to V1 listener handler; assert no exception; assert entity saved with correct `prosecutionAuthorityId`. This test is expected to PASS without new production code — it is a backward-compat regression guard, not a TDD failing test
- [ ] T064 [US4] Confirm T063 passes without new production code — V1 handler uses core-domain types that already support flat `verdictCode`; document this confirmation explicitly (no production change needed)
- [ ] T065 [US4] Write failing test in `InformantRegisterEventListenerTest.java` — `generateInformantRegister_withPreMigrationFlatVerdictCode_shouldNotThrow`: build V1 `informant-register-generated` envelope with offences carrying flat `verdictCode` string; send to V1 listener handler; assert no exception
- [ ] T066 [US4] Confirm T065 passes without new production code — document confirmation; no production change needed
- [ ] T067 [US4] Build gate: `mvn test` on all affected modules — full unit-test suite green

**Checkpoint**: US4 verified. Pre-migration V1 events remain replayable with no production code changes.

---

## Phase 7: Polish & Full Build

**Purpose**: Final verification and code hygiene checks across all changes.

- [ ] T068 Full build gate: `mvn clean install` — zero compilation errors, zero test failures across entire reactor
- [ ] T069 [P] Code hygiene review: verify all new/modified Java files in `results-domain-common`, `results-command-handler`, `results-domain-aggregate`, `results-event-listener`, `results-event-processor`, `results-query-view` have no wildcard imports, no `System.out`/`System.err`, no Spring annotations, no Lombok annotations, SLF4J logging only
- [ ] T070 [P] Schema-subscription symmetry check (Constitution Principle VI + SC-003): verify `results.event.informant-register-recorded-v2.json` exists AND appears in listener `subscriptions-descriptor.yaml`; verify `results.event.informant-register-generated-v2.json` exists AND appears in both listener AND processor `subscriptions-descriptor.yaml`; verify zero `$ref` to `http://justice.gov.uk/core/courts/informantRegisterDocument/` remains in any command or event schema for informant-register flows (SC-003); confirm `results.prosecutor-results.json` schema accepts a sample payload containing a `verdict` sub-object with `verdictCode`, `verdictDate`, `verdictType` (SC-004)

**Checkpoint**: Build green, code clean, schema-subscription symmetry confirmed. Ready for code-reviewer and qa agents.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Contracts)**: No dependencies — start immediately
- **Phase 2 (POJOs)**: Requires T018 (Phase 1 gate) — BLOCKS all user story phases
- **Phase 3 (US1)**: Requires T035 (Phase 2 gate) — first P1 story, no cross-story dependency
- **Phase 4 (US3)**: Requires T035 (Phase 2 gate) — can start in parallel with Phase 3 (different modules)
- **Phase 5 (US2)**: Requires T035 (Phase 2 gate) AND T042 (US1 gate, command handler V2 emit) — processor depends on V2 events being emitted
- **Phase 6 (US4)**: Requires T035 (Phase 2 gate) — tests V1 handlers (no new production code); can start after Phase 2
- **Phase 7 (Polish)**: Requires T042, T048, T062, T067 — all user story gates passed

### User Story Dependencies

| Story | Priority | Depends on | Blocked by |
|-------|----------|-----------|------------|
| US1 — Add Ingest | P1 | Phase 2 complete | — |
| US3 — Query | P1 | Phase 2 complete | — |
| US2 — Generate | P2 | Phase 2 + US1 gate | T042 |
| US4 — Compat | P2 | Phase 2 complete | — |

### Within Each Phase

1. TDD rule: Test task written → confirmed failing → production code written → confirmed passing
2. In Phase 2: deepest types first (`Verdict` → `InformantRegisterOffence` → … → `InformantRegisterDocumentRequest` → V2 events)
3. In Phase 3: gap-fill → failing V2 test → production command handler → failing aggregate test → production aggregate
4. In Phase 4: gap-fill → failing verdict tests (T044–T046) → production view
5. In Phase 5: gap-fill → failing handler test → production handler → failing listener tests → production listener → failing processor tests → production processor

---

## Parallel Opportunities

### Phase 1 (Contracts) — run T002–T009 in parallel

```
T001 (verdict.json — no deps)
T002–T009 [all in parallel — different schema files, no cross-deps]
Then: T010, T011, T012, T013, T014, T015, T016, T017 (sequentially or grouped by dep)
Then: T018 (build gate)
```

### Phase 2 (POJOs) — run test+production pairs in parallel where types are independent

```
T019 + T020 (Verdict) in parallel with T021 + T022 (Offence — once Verdict exists)
T023 + T024 (Result + ResultData) in parallel with T025 + T026 (remaining mid-tier types)
T027 + T028 (DocumentRequest — depends on all above)
T029 + T030 (ProsecutorResult) in parallel with T031 + T032 (RecordedV2 event)
T033 + T034 (GeneratedV2 event) after T028
T035 (gate)
```

### Phase 3 + Phase 4 — run in parallel (different modules)

```
After T035:
  [Track A] Phase 3: T036–T041 (command handler + aggregate)
  [Track B] Phase 4: T043–T047 (query view — different module, no dependency on Track A)
  T042 (Track A gate) + T048 (Track B gate) — then Phase 5 can begin
```

---

## Implementation Strategy

### MVP (US1 + US3 only — both P1)

1. Phase 1: Contracts (T001–T018)
2. Phase 2: POJOs (T019–T035)
3. Phase 3: US1 — Add Ingest (T036–T042)
4. Phase 4: US3 — Query (T043–T048)
5. **STOP and VALIDATE**: `mvn test` on all P1 modules green; US1 and US3 independently testable
6. Deliver MVP — query returns verdict data for newly ingested records

### Full Delivery (add P2 stories)

7. Phase 5: US2 — Generate (T050–T062) in parallel with Phase 6: US4 — Compat (T063–T067)
8. Phase 7: Full build (T068–T070)

---

## Notes

- [P] tasks involve different files with no shared dependencies — safe to parallelize
- [US*] label maps task to user story for traceability
- TDD gate per story: each story's tests must be green before moving to the next
- Never skip T018 or T035 build gates — they are the Principle I and Phase 2 completeness checkpoints
- Commits: after each build gate (T018, T035, T042, T048, T062, T067, T068) at minimum
- Integration test (`InformantRegisterDocumentRequestIT`) deferred — not part of this task list (see spec.md Assumptions)
