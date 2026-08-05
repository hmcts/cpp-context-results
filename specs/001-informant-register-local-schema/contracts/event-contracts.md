# Event Contracts: Include Verdict in SJP Results

**Date**: 2026-06-16 | **Branch**: `CIMD-3915-informant-register-local-schema`

## New Domain Events

### `results.event.informant-register-recorded-v2`

Emitted by: `InformantRegisterHandler.handleAddInformantRegisterToEventStream`
Schema `$id`: `http://justice.gov.uk/results/courts/informant-register-recorded-v2.json`
Schema file: `results-domain/results-domain-common/src/main/resources/json/schema/results.event.informant-register-recorded-v2.json`
Subscription (listener): added to `results-event-listener/src/yaml/subscriptions-descriptor.yaml`
Subscription (processor): NOT subscribed (processor does not need to react to recorded V2)

```json
{
  "prosecutionAuthorityId": "<uuid>",
  "informantRegister": {
    "registerDate": "2026-04-13T10:00:00.000Z",
    "hearingDate": "2026-04-13T09:00:00.000Z",
    "hearingId": "<uuid>",
    "prosecutionAuthorityId": "<uuid>",
    "prosecutionAuthorityCode": "DVSA",
    "fileName": "informant-register-dvsa.csv",
    "hearingVenue": {
      "courtHouse": "Westminster Magistrates Court",
      "courtSessions": [
        {
          "courtRoom": "1",
          "hearingStartTime": "09:00",
          "defendants": [
            {
              "name": "John Smith",
              "address1": "1 Main Street",
              "prosecutionCasesOrApplications": [
                {
                  "caseOrApplicationReference": "EARL001390",
                  "offences": [
                    {
                      "offenceCode": "SF75060",
                      "offenceTitle": "Speed in excess of limit",
                      "orderIndex": 1,
                      "pleaValue": "GUILTY",
                      "verdict": {
                        "verdictCode": "G",
                        "verdictDate": "2026-04-13",
                        "verdictType": "FOUND_GUILTY"
                      },
                      "offenceResults": [
                        {
                          "resultText": "Fine",
                          "cjsResultCode": "1015"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  }
}
```

---

### `results.event.informant-register-generated-v2`

Emitted by: `InformantRegisterHandler.processRequests` (called from both generate handlers)
Schema `$id`: `http://justice.gov.uk/results/courts/informant-register-generated-v2.json`
Schema file: `results-domain/results-domain-common/src/main/resources/json/schema/results.event.informant-register-generated-v2.json`
Subscription (listener): added to `results-event-listener/src/yaml/subscriptions-descriptor.yaml`
Subscription (processor): added to `results-event-processor/src/yaml/subscriptions-descriptor.yaml`

```json
{
  "informantRegisterDocumentRequests": [
    {
      "registerDate": "2026-04-13T10:00:00.000Z",
      "hearingDate": "2026-04-13T09:00:00.000Z",
      "hearingId": "<uuid>",
      "prosecutionAuthorityId": "<uuid>",
      "prosecutionAuthorityCode": "DVSA",
      "fileName": "informant-register-dvsa.csv",
      "hearingVenue": { "...": "same structure as above" }
    }
  ],
  "systemGenerated": false
}
```

---

## Modified Command Schema

### `results.add-informant-register` (command)

Schema file: `results-command/results-command-api/src/raml/json/schema/results.add-informant-register.json`

**Before** (delegates entirely to core-domain):
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://moj.gov.uk/cpp/results/command/add-informant-register-request.json",
  "$ref": "http://justice.gov.uk/core/courts/informantRegisterDocument/informantRegisterDocumentRequest.json"
}
```

**After** (references local schema):
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://moj.gov.uk/cpp/results/command/add-informant-register-request.json",
  "$ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterDocumentRequest.json"
}
```

---

## Modified Query Schema

### `results.prosecutor-results` (query response)

Schema file: `results-query/results-query-api/src/raml/json/schema/results.prosecutor-results.json`

**Before** (delegates entirely to core-domain):
```json
{
  "$ref": "http://justice.gov.uk/core/courts/informantRegisterDocument/prosecutorResult.json"
}
```

**After** (references local schema with verdict):
```json
{
  "$ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/prosecutorResult.json"
}
```

The local `prosecutorResult.json` transitively includes the local `informantRegisterOffence.json` which has the `verdict` object.

---

## Subscription Descriptor Changes

### Listener (`results-event-listener/src/yaml/subscriptions-descriptor.yaml`)

Add after existing `results.event.informant-register-generated` entry:
```yaml
- name: results.event.informant-register-recorded-v2
  schema_uri: http://justice.gov.uk/results/courts/informant-register-recorded-v2.json

- name: results.event.informant-register-generated-v2
  schema_uri: http://justice.gov.uk/results/courts/informant-register-generated-v2.json
```

### Processor (`results-event-processor/src/yaml/subscriptions-descriptor.yaml`)

Add after existing `results.event.informant-register-generated` entry:
```yaml
- name: results.event.informant-register-generated-v2
  schema_uri: http://justice.gov.uk/results/courts/informant-register-generated-v2.json
```

---

## Verdict Object Schema (`verdict.json`)

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://justice.gov.uk/results/courts/informantRegisterDocument/verdict.json",
  "description": "Verdict recorded against an SJP offence",
  "type": "object",
  "properties": {
    "verdictCode": {
      "description": "Verdict code: G (FOUND_GUILTY), N (FOUND_NOT_GUILTY), PSJ (PROVED_SJP)",
      "type": "string"
    },
    "verdictDate": {
      "description": "Conviction date in yyyy-MM-dd format. Mandatory when verdictCode is present.",
      "type": "string"
    },
    "verdictType": {
      "description": "Verdict type resolved from reference data: FOUND_GUILTY, FOUND_NOT_GUILTY, PROVED_SJP",
      "type": "string"
    }
  },
  "additionalProperties": false
}
```

---

## Offence Schema Change (`informantRegisterOffence.json` — local)

**Key change**: `verdictCode: String` → `verdict: { verdictCode, verdictDate, verdictType }`

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "id": "http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterOffence.json",
  "description": "The details of a defendant offence for an informant register document",
  "type": "object",
  "properties": {
    "originatingCaseUrn": { "type": "string" },
    "offenceCode": { "type": "string" },
    "orderIndex": { "type": "integer", "minimum": 0 },
    "offenceTitle": { "type": "string" },
    "pleaValue": { "type": "string" },
    "verdict": {
      "$ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/verdict.json"
    },
    "offenceResults": {
      "type": "array",
      "minItems": 1,
      "items": {
        "$ref": "http://justice.gov.uk/results/courts/informantRegisterDocument/informantRegisterResult.json"
      }
    }
  },
  "required": ["offenceCode", "orderIndex", "offenceTitle"],
  "additionalProperties": false
}
```
