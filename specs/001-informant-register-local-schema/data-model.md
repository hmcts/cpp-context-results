# Data Model: Include Verdict in SJP Results

**Date**: 2026-06-16 | **Branch**: `CIMD-3915-informant-register-local-schema`

## New Local Types (`uk.gov.moj.cpp.results.domain.informant.model`)

All types are hand-written POJOs with explicit builder, constructors, and getters. No Lombok. No Jackson annotations beyond `@JsonCreator` / `@JsonProperty` where needed for deserialization.

### `Verdict`
```
verdictCode  : String   (optional — "G", "N", "PSJ")
verdictDate  : String   (optional — yyyy-MM-dd; co-present with verdictCode)
verdictType  : String   (optional — "FOUND_GUILTY", "FOUND_NOT_GUILTY", "PROVED_SJP")
```
Constraints: `verdictCode` and `verdictDate` are co-dependent — both present or both absent.

### `InformantRegisterOffence` (replaces core-domain)
```
offenceCode            : String          (required)
offenceTitle           : String          (required)
orderIndex             : Integer         (required)
originatingCaseUrn     : String          (optional)
pleaValue              : String          (optional)
verdict                : Verdict         (optional — replaces flat verdictCode)
offenceResults         : List<InformantRegisterResult>  (optional, min 1 if present)
```

### `InformantRegisterResult` (replaces core-domain)
```
resultText   : String                      (required)
cjsResultCode : String                     (optional)
resultData   : InformantRegisterResultData (optional)
```

### `InformantRegisterResultData` (replaces core-domain)
```
amount                : String  (optional)
nextHearingDate       : String  (optional — date-time)
nextCourtLocation     : String  (optional)
durationValue         : String  (optional)
durationUnit          : String  (optional)
durationStartDate     : String  (optional — date-time)
durationEndDate       : String  (optional — date-time)
secondaryDurationValue : String (optional)
secondaryDurationUnit  : String (optional)
```

### `InformantRegisterCaseOrApplication` (replaces core-domain)
```
caseOrApplicationReference : String                             (required)
arrestSummonsNumber        : String                             (optional)
applicationParticulars     : String                             (optional)
offences                   : List<InformantRegisterOffence>     (required, min 1)
results                    : List<InformantRegisterResult>      (optional, min 1 if present)
```

### `InformantRegisterDefendant` (replaces core-domain)
```
name                         : String                                   (required)
address1                     : String                                   (required)
address2–5                   : String                                   (optional)
postCode                     : String                                   (optional)
dateOfBirth                  : String                                   (optional — date)
nationality                  : String                                   (optional)
title                        : String                                   (optional)
firstName                    : String                                   (optional)
lastName                     : String                                   (optional)
prosecutionCasesOrApplications : List<InformantRegisterCaseOrApplication> (optional, min 1 if present)
results                      : List<InformantRegisterResult>            (optional, min 1 if present)
```

### `InformantRegisterHearing` (replaces core-domain)
```
courtRoom         : String                          (required)
hearingStartTime  : String                          (required — time)
defendants        : List<InformantRegisterDefendant> (required, min 1)
```

### `InformantRegisterHearingVenue` (replaces core-domain)
```
ljaName        : String                         (optional)
courtHouse     : String                         (required)
courtSessions  : List<InformantRegisterHearing>  (required, min 1)
```

### `InformantRegisterRecipient` (replaces core-domain)
```
recipientName     : String  (required)
emailAddress1     : String  (required)
emailAddress2     : String  (optional)
emailTemplateName : String  (required)
```

### `InformantRegisterDocumentRequest` (replaces core-domain)
```
registerDate               : ZonedDateTime                   (required)
hearingDate                : ZonedDateTime                   (required)
hearingId                  : UUID                            (required)
prosecutionAuthorityId     : UUID                            (required)
prosecutionAuthorityCode   : String                          (required)
prosecutionAuthorityOuCode : String                          (optional)
majorCreditorCode          : String                          (optional)
prosecutionAuthorityName   : String                          (optional)
fileName                   : String                          (required)
recipients                 : List<InformantRegisterRecipient> (optional, min 1 if present)
hearingVenue               : InformantRegisterHearingVenue   (required)
groupId                    : UUID                            (optional)
```

### `ProsecutorResult` (replaces core-domain)
```
startDate                  : LocalDate                        (required)
endDate                    : LocalDate                        (optional)
prosecutionAuthorityId     : UUID                             (required)
prosecutionAuthorityCode   : String                           (required)
prosecutionAuthorityName   : String                           (optional)
prosecutionAuthorityOuCode : String                           (optional)
majorCreditorCode          : String                           (optional)
hearingVenues              : List<InformantRegisterHearingVenue> (optional)
```

---

## New Domain Event Types

### `InformantRegisterRecordedV2` (new — in results-domain-common or domain-event)
```
prosecutionAuthorityId  : UUID                           (required)
informantRegister       : InformantRegisterDocumentRequest (local, required)
```
Event name: `results.event.informant-register-recorded-v2`

### `InformantRegisterGeneratedV2` (new)
```
informantRegisterDocumentRequests : List<InformantRegisterDocumentRequest> (local, required)
systemGenerated                   : Boolean                                (optional)
```
Event name: `results.event.informant-register-generated-v2`

---

## JSON Schema Files (new / modified)

### New local sub-schemas under `results-domain/results-domain-common/src/main/resources/json/schema/informantRegisterDocument/`

| File | `$id` namespace |
|------|-----------------|
| `verdict.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/verdict.json` |
| `informantRegisterOffence.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterOffence.json` |
| `informantRegisterResult.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterResult.json` |
| `informantRegisterResultData.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterResultData.json` |
| `informantRegisterCaseOrApplication.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterCaseOrApplication.json` |
| `informantRegisterDefendant.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterDefendant.json` |
| `informantRegisterHearing.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterHearing.json` |
| `informantRegisterHearingVenue.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterHearingVenue.json` |
| `informantRegisterRecipient.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterRecipient.json` |
| `informantRegisterDocumentRequest.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterDocumentRequest.json` |
| `prosecutorResult.json` | `http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json` |

### New V2 event schemas under `results-domain/results-domain-common/src/main/resources/json/schema/`

| File | `$id` namespace |
|------|-----------------|
| `results.event.informant-register-recorded-v2.json` | `http://justice.gov.uk/results/courts/informant-register-recorded-v2.json` |
| `results.event.informant-register-generated-v2.json` | `http://justice.gov.uk/results/courts/informant-register-generated-v2.json` |

### Modified command schema under `results-command/results-command-api/src/raml/json/schema/`

| File | Change |
|------|--------|
| `results.add-informant-register.json` | Replace `$ref` to core-domain with local `$ref` to `informantRegisterDocumentRequest.json` |

### Modified query schema under `results-query/results-query-api/src/raml/json/schema/`

| File | Change |
|------|--------|
| `results.prosecutor-results.json` | Replace `$ref` to core-domain `prosecutorResult.json` with local `$ref` including `verdict` object |

---

## State Transitions (Informant Register lifecycle — unchanged)

```
RECORDED → GENERATED → NOTIFIED
```
The lifecycle is unaffected by this change. The verdict enrichment only alters the payload shape within each state.

---

## Backward Compatibility

Pre-migration events in the event store carry `informantRegister.hearingVenue.courtSessions[*].defendants[*].prosecutionCasesOrApplications[*].offences[*].verdictCode` as a flat string. When these are replayed:

- V1 listener handlers (`informant-register-recorded`, `informant-register-generated`) still deserialize correctly using the core-domain types — these handlers are not removed.
- The local `InformantRegisterOffence` POJO must tolerate a missing `verdict` field when deserialized from stored JSON — `verdict` is optional.
- The `InformantRegisterEntity.payload` (stored as raw JSON string) for pre-migration records will not have a `verdict` object — the query view returns those offences without a `verdict` field (correct per FR-003).
