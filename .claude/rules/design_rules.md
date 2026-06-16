# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (handler → aggregate → domain event)
       ↓ writes to event store (java:/app/results-service/DS.eventstore)

2. Event listener (projects events → viewstore tables)
       ↓ projects to java:/DS.results

3. Event processor (consumes domain events → emits public events)
       ↓ emits to public.event for other contexts
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift.

- **Command side** — RAML-declared commands hit `@Handles`-annotated handler classes (`ResultsCommandHandler`, `InformantRegisterHandler`, `StagingEnforcementResponseHandler`, `NcesDocumentNotificationCommandHandler`, `MigratedStagingEnforcementResponseHandler`, …) which load an aggregate, run validation/financial rules, and emit domain events. State is mutated only via the aggregate's `apply(event)` handlers.
- **Event listener** — projects domain events into the viewstore DB (`DS.results`). Lives under `results-event/results-event-listener`. Heavy use of converters mapping events → JPA entities.
- **Event processor** — consumes domain events and emits **public** events for downstream contexts. Lives under `results-event/results-event-processor`. Heavy use of converters mapping internal events → downstream public schemas.

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Results aggregate       | `ResultsAggregate` / `DefendantAggregate` / `ProsecutionAuthorityAggregate` — case-side resulting state.                                  |
| Financial results       | `HearingFinancialResultsAggregate`, `HearingFinancialResultGobAccountAggregate`, `MigratedInactiveHearingFinancialResultsAggregate` — fine/non-fine outcomes, GoB account linkage, migrated-inactive hearings. |
| Domain event            | Internal event written to the event store. Examples: `results.hearing-results-added`, `results.event.police-result-generated`, `results.event.hearing-financial-results-tracked`. |
| Public event            | Cross-context event emitted on `public.event`. Declared in `public-publications-descriptor.yaml` (e.g. `public.results.police-result-generated`). Consumed by downstream contexts. Has its own JSON schema. |
| Command                 | Inbound request via the `results.handler.command` queue. Declared in RAML, dispatched by `@Handles`.                                     |
| Listener                | Read-side projection — `*Listener` class extending the framework's listener base; projects events → viewstore JPA entities via converters. |
| Processor               | Public-event emitter — `*Processor` class extending the framework's processor base; maps domain events → public-event payloads via converters. |
| Notification flows      | Police/DVLA results, NCES email notifications, informant-register notifications, DCS document publishing — emitted by the processor based on resulting events. |
| Viewstore               | Read-model database `DS.results`, populated by listeners. Schema managed by the `results-viewstore-liquibase` module.                    |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Schema managed by `event-repository-liquibase`.                     |

## Three Subscription Sources

The processor and listener can be triggered by:

1. **Internal event topic** `results.event` — replay of this context's own domain events.
2. **Public event bus** `public.event` — events from other contexts (progression, material, sjp, staging contexts, etc.).
3. **Command queue** `results.handler.command` — RAML-declared commands.

## Schema Namespaces (read before touching any schema)

This service uses **three** distinct `schema_uri` / `id` namespaces. A schema in the wrong namespace fails dispatch even when otherwise correct. Match the namespace of sibling events in the same descriptor:

- `http://cpp.moj.gov.uk/hearing/json/schemas/...`
- `http://justice.gov.uk/json/schemas/...`
- `http://moj.gov.uk/cpp/json/schemas/...`

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `results-event-sources/src/yaml/event-sources.yaml` — internal + public topic declarations.
- `results-event/results-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `results-event/results-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `results-event/results-event-processor/src/yaml/public-publications-descriptor.yaml` — published public events.
- `results-command/results-command-handler/src/raml/results-command-handler.messaging.raml` — command → handler mapping.
- `results-command/results-command-handler/src/raml/results-private-event.messaging.raml` — private event mapping.
- Per-command/per-event JSON schemas: `src/raml/json/schema/` and `src/main/resources/json/schema/` under each module (notably `results-domain/results-domain-common/src/main/resources/json/schema/`).

## Module Layout

- `results-command/results-command-api` — command RAML + schemas
- `results-command/results-command-handler` — `@Handles` command handlers + messaging RAML
- `results-domain/results-domain-aggregate` — aggregates + validation/financial rules
- `results-domain/results-domain-common` — shared types + event JSON schemas
- `results-domain/results-domain-event` — domain event types
- `results-domain/results-domain-transformations` — stream-transformation-tool wiring (e.g. anonymisation)
- `results-event/results-event-listener` — listeners + converters → viewstore
- `results-event/results-event-processor` — processors + converters → public events; subscription + publication descriptors
- `results-event-sources` — `event-sources.yaml`
- `results-query/results-query-api` — query RAML
- `results-query/results-query-view` — read services over the viewstore (`src/main/raml/results-query-view.raml`)
- `results-viewstore/results-viewstore-liquibase` — Liquibase changelogs for `DS.results`
- `results-viewstore/results-viewstore-persistence` — JPA entities / repositories for the viewstore
- `results-json` — shared JSON resources
- `results-service` — packaging WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources / queues / topics (matcher `/results-[^/]+`)
- `results-healthchecks` — healthchecks
- `results-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`
- `results-performance-test` — performance tests
- `test-utilities` — shared test helpers

## Adding a New Command

1. **RAML first.** Add the command operation to `results-command-handler.messaging.raml` (or the appropriate query RAML).
2. **JSON schema.** Add the command payload schema under the command-api module's `src/raml/json/schema/`, in the correct namespace.
3. **Handler.** Add a method with `@Handles("<command-name>")` on a class annotated `@ServiceComponent(COMMAND_HANDLER)`. Method takes `Envelope<CommandPayload>`.
4. **Aggregate.** If the command mutates state, the handler loads the aggregate and invokes its command method; the aggregate validates and emits a domain event, applied via `apply(event)`.
5. **Listener.** If the new event is consumed by the listener: subscription entry + JSON schema + listener method + converter.
6. **Processor.** If the new event triggers a public event: subscription entry + JSON schema + processor method + converter + `public-publications-descriptor.yaml` entry + public-event JSON schema.
7. **Tests.** Failing unit tests for handler, aggregate, listener (if touched), processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

Same as "Adding a New Command" steps 4–7, plus:

- Add the event's JSON schema under the owning module's `src/main/resources/json/schema/`, in the correct namespace
- Update both listener AND processor `subscriptions-descriptor.yaml` files with the new event entry (or document explicitly which is unaffected)
- Update `event-sources.yaml` if a new internal topic is introduced

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to listener and/or processor `subscriptions-descriptor.yaml` for the relevant context's `public.event` source.
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version) under the consuming module's `src/main/resources/json/schema/`.
3. **Listener / processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → either a viewstore entity (listener) or a domain command (if it triggers a state change).
5. **Tests.** Unit tests for the listener/processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and JPA repositories
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`) and Lombok — this service uses neither
- Cross-context coupling beyond declared public events — never call another context's REST API for command-side traffic; consume their public events instead

## Common Gotchas

1. **Schema-subscription drift** — adding a subscription entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Wrong schema namespace** — a schema whose `id`/`schema_uri` is in the wrong one of the three namespaces fails dispatch even when the file is otherwise valid. Match the sibling events in the same descriptor.
3. **Three-layer drift** — modifying a domain event without updating listener AND processor is the most common silent-data-drift bug. Constitution Principle II makes this a review-blocker.
4. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) means it never applies in CI's IT setup.
5. **Cross-context pin drift** — bumping `coredomain` / `referencedata` / `notification.notify` / `progression` / `material` / `sjp` / `staging.dcs` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version, otherwise schema validation fails at runtime.
6. **Wrong `@ServiceComponent` value** — `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.
