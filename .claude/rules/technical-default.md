# Service Identity

- **Service:** cpp-context-results
- **Description:** The resulting context of the Crime Common Platform. Receives hearing and financial results, persists them as event-sourced aggregates, projects them into a read-model, and publishes downstream — police/DVLA results, financial results and GoB enforcement, DCS document publishing, NCES email notifications, and informant-register notifications.
- **Bounded context:** `results` (one of many CPP contexts).
- **Programme:** Crime Common Platform (CPP).
- **Organisation:** HMCTS / Ministry of Justice.

## Technology Stack

| Component         | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Build tool        | Maven (multi-module reactor; root `pom.xml`)                          |
| Language          | Java 17 (CI demand `centos8-j17`; local `mvn17` alias)                |
| Framework         | CPP `service-parent-pom:17.103.x` (JEE/CDI-style)                     |
| Packaging         | WAR → WildFly via Docker                                              |
| Annotations       | `@ServiceComponent`, `@Handles`, `@ApplicationScoped`                 |
| Persistence       | Liquibase changelogs (event-store, aggregate-snapshot, viewstore, event-buffer) |
| Messaging         | ActiveMQ (Docker for ITs); JMS topics + queues                        |
| Tests             | JUnit + Mockito (unit); framework's IT harness (`runIntegrationTests.sh`) |
| CI                | Azure DevOps Pipelines (`azure-pipelines.yaml`)                       |
| Quality gate      | SonarQube in CI (no local Checkstyle/PMD enforcement)                 |
| Java packaging    | Root namespace `uk.gov.moj.cpp.results.*`                             |

## Constraints

- Maven is the current build tool. Future migration to Gradle is allowed but requires coordinating constitution + rule files + CI pipeline together (see Constitution Principle V).
- Java 17 only — do not use `var` outside method-local scope where the type is non-obvious; prefer explicit types in public APIs
- Use the CPP framework's `@ServiceComponent` + `@Handles` for command/event handling — NOT hand-rolled JMS listeners
- Aggregate state mutation flows through the framework `Aggregate` `apply(...)` event-handling pipeline — do not write events directly to the event store, and do not mutate aggregate fields outside `apply`
- Event listeners and processors must use converter classes — NOT inline mapping in the listener/processor body
- Contracts (RAML, JSON schemas, `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`, `event-sources.yaml`) update FIRST, Java second (Constitution Principle I)
- Schema additions / removals / renames update both the subscription descriptor AND JSON schema in lockstep, and the schema `id` / `schema_uri` must use the correct one of this service's three namespaces (Constitution Principle VI)
- Logging via SLF4J only — no `System.out` / `System.err` (Constitution Principle VII)
- Test-Driven Development is mandatory (Constitution Principle VIII)
- No Spring, no Lombok — this codebase uses neither (0 imports of each)
- No wildcard imports

## Build & Test Commands

```bash
# Full build + unit tests
mvn clean install

# Build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Single module with deps
mvn -pl results-domain/results-domain-aggregate -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName

# Integration tests (requires Dockerised env up; CPP_DOCKER_DIR must be set)
./runIntegrationTests.sh

# Single IT against running env
mvn -pl results-integration-test test -Dit.test=ClassNameIT

# Framework JMX commands
./runSystemCommand.sh           # help
./runSystemCommand.sh --list    # list available commands
./runSystemCommand.sh CATCHUP   # run one
```

## Key version pins (`pom.xml`)

- Parent: `uk.gov.moj.cpp.common:service-parent-pom:17.103.x` (currently 17.103.9)
- Cross-context pins to keep aligned: `coredomain`, `referencedata`, `notification.notify`, `systemdocgenerator`, `progression`, `material`, `usersgroups`, `sjp`, `staging.dcs`
- When bumping any of these, also check the matching schema/RAML classifier dep is on the same version
