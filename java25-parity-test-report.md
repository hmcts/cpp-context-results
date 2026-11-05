# Java 25 parity test report: cpp-context-results

Stages 3 and 4 of [Java 25 upgrade with parity tests (team CHD)](https://hmcts.atlassian.net/wiki/spaces/CHD/pages/366839818).
BC references are from [Parity Testing Java17 → Java25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/279752455).

| | Java 25 copy | Java 17 copy |
|---|---|---|
| Branch | `dev/25.104-chd-j25-j17-parity` | `dev/CHD-2869-chd-parity-with-j25` (from `main`, never merged) |
| JDK | 25.0.5 | 17.0.9 (Corretto) |
| Hibernate in unit tests | 6.6.1.Final (persistence module also run on 7.3.2.Final) | 5.4.24.Final |
| H2 | 2.3.232 | 1.4.196 |

The Java 17 copy's production code is unchanged. On the Java 25 branch, production code has changed only to address the
PR #228 review. Triage (Stage 5) is not done. Stage 6 integration tests have been run on the final commit; see
"Stage 6 integration tests".

**The JVM default zone is UTC, not UK time.** Earlier versions of this report took UK time (Europe/London) as fact.
The PR #228 review points out that the framework's `EntityManagerProducer.create()` calls `TimeZone.setDefault(UTC)`
(persistence-deltaspike 17.103.x and persistence-jpa 25.104.x), and the containers run in UTC, so the default-zone
`ZonedDateTime.now()` gives UTC on both stacks.

**`now()` calls match `team/25.104.x`.** Narayan advised keeping every `now()` exactly as on `team/25.104.x`, the
platform team's Java 25 branch, rather than swapping in `UtcClock` (`now(UTC).truncatedTo(MILLIS)`, which also loses
Java 17's microsecond precision), `now(UTC)` or `Europe/London`. For these calls, `team/25.104.x` is the same as `main`.
What was restored:

| Area | Restored to `team/25.104.x` |
|---|---|
| Production, named in the review | `NCESDecisionHelper`, `HearingFinancialResultsTrackedListener`, `InformantRegisterEventListener`, `DocumentGeneratorService` |
| Production, same swap but not named | `HearingFinancialResultsAggregate` (`ZonedDateTime.now()` ×2, `now()`), `HearingFinancialResultsUpdatedListener`, `MigratedNcesEmailNotificationRequestedProcessor`, `ResultsEventProcessor` (the `UtcClock` field stays, as on `team/25.104.x`) |
| Unit tests and ITs | 175 lines in 25 files that had `ZonedDateTime.now()`, `now()`, `now(UTC)` or `LocalDateTime.now()` swapped for `new UtcClock().now()`, plus one `TestTemplates` (test-utilities) line |
| Other test differences from `team/25.104.x` | `GobAccountHelperTest` (a `CREATED_TIME_BASE` counter replacing `ZonedDateTime.now()`), `TestTemplate` and `CommonMethodsTest` (`ZoneOffset.UTC` replacing `ZoneId.systemDefault()`), and `InformantRegisterRepositoryRowValueQueryTest` (`now()`). All four now match `team/25.104.x` exactly, apart from the added `chd14_` test |

A per-file comparison of every line that calls `now()`, `UtcClock`, `ZoneId`, `ZoneOffset.UTC`, `TimeZone` or
`withZoneSameInstant` now finds only these differences from `team/25.104.x`:

- **`HearingResultedEventProcessorTest`.** Its five extra `clock.now()` lines come from feature work merged from `main`
  (CRA-221), not from the Java 25 change, and they match `main`.
- **CHD-02 in `ResultsQueryView`.** It converts `sharedTime` with `withZoneSameInstant(ZoneId.of("UTC"))` so the
  response keeps Java 17's `...Z[UTC]` form. This isn't a `now()` change and isn't on `team/25.104.x`. Without it, CHD-02
  goes red and the response becomes `...Z`. Keep it, or align with `team/25.104.x` and accept the `...Z` form, once the
  query's consumers have been checked.

## Results

| Run | Java 17 | Java 25 |
|---|---|---|
| Unit tests | 1,048 | 1,110 |
| Failed | **0** | **0** |
| Skipped | 2 | 2 |
| Parity tests (methods starting `chd`) | 12 | 12 |
| Parity tests green | **12** | **12** |
| Parity tests red | 0 | 0 |
| Other tests red | 0 | 0 |
| Hibernate 7.3.2 persistence run | n/a | **47 of 47 green**, including the 5 persistence parity tests |
| Integration tests (Stage 6) | Not run | **66 of 66 green** on commit `e22dfeab0` |

All 12 parity tests pass on both JDKs. The Java 25 figures come from a full `mvn -o install` on JDK 25.0.5 after the
PR #228 review fixes (1,116 tests), less the six parity tests since removed (see "Removed parity tests"). Every viewstore repository was also checked against `main`'s DeltaSpike version: the other finders
all return lists, as on `main`, with the same queries. CHD-01, CHD-05 and CHD-06 were fixed in response to that review. For CHD-04, CHD-08 and CHD-10 the production code was restored to `team/25.104.x`, and their tests were later removed. CHD-02 and
CHD-07 were already green from earlier commits on this branch. Those rows below describe the code as it is now.

**The two copies are aligned.** CHD-14's helper call uses `now()` on both branches, and a script confirmed all
method names and bodies are identical. The Java 17 full build on Corretto 17.0.9 gave 1,054 tests, 0 failures,
2 skipped. The figures above are that run less the six parity tests since removed. After the removals, every module
that lost a test was rerun on both JDKs and is green: aggregate, event listener, event processor and persistence.

One test is in a different class: `chd14_finderWithUnqualifiedAttributes_should_return_matching_rows` is in
`InformantRegisterRepositoryTest` on Java 17, because `InformantRegisterRepositoryRowValueQueryTest` doesn't exist on
`main`.

**Hibernate 7.3.2 run:** see "Hibernate 7.3.2 persistence run". All 47 persistence tests pass on 7.3.2.Final.

**Java 17 build note:** `main` doesn't build as it stands, because `enforce-moj-latest-interfaces` fails on
`results-event-processor`: progression `17.0.286` is older than released `17.104.306`. The parent hard-codes
`<skip>false</skip>`, so `-Denforcer.skip` has no effect. The Java 17 run used `-Dprogression.version=17.104.306` on
the command line; the pom is unchanged.

### Test count difference

Excluding parity tests, Java 25 has 62 more tests (1,098 against 1,036). Per-class differences:

| Change | Classes |
|---|---|
| +48 | Classes that report no tests on Java 17: `EventStreamTransformTest` 12, `JudicialResultsTransformerTest` 10, `EventPayloadTransformerTest` 8, `EventToTransformTest` 5, `MaterialUrlGeneratorTest` 4, `JsonPathHelperTest` 3, `InformantRegisterRepositoryRowValueQueryTest` 3, and 1 each for `CourtProceedingsInitiatedEventTransformerTest`, `MasterDefendantIdEventTransformerTest` and `DomainTest`. I haven't established whether each is new or simply never ran on Java 17 |
| +16 | Added to repository test classes: `InformantRegisterRepositoryTest` 7, `DefendantTrackingStatusRepositoryTest` 3, `DefendantGobAccountsEntityRepositoryTest` 3, `NcesEmailNotificationDetailsRepositoryTest` 2, `HearingResultedDocumentRepositoryTest` 1 |
| **−2** | **Lost:** `ResultsSchemaContractValidationTest` ran 2 tests on Java 17 and none on Java 25. This is CHD-15 |

Together these give the +62 difference.

## Risks

| ID | Severity | What could change | Test | Java 17 | Java 25 | Suggested fix |
|---|---|---|---|---|---|---|
| CHD-01 (BC-01) | High | A query for a hearing date with no document returns the latest day's results with a 200, instead of failing | `HearingResultedDocumentRepositoryTest.chd01_findByHearingIdAndHearingDay_noDocumentForThatDay_should_throw_NoResultException`<br>`HearingServiceTest.chd01_hearingDateWithNoDocument_should_not_fall_back_to_latest_hearing_day` | GREEN ×2 | **GREEN ×2.** `HearingResultedDocumentRepository` is back to Java 17's signatures: `findByHearingIdAndHearingDay` returns the entity and throws `NoResultException` (DeltaSpike's default `singleResult`), and `findByHearingIdAndLatestHearingDay` returns `null` when nothing matches (DeltaSpike's `OPTIONAL`). `HearingService.findHearing` is back to `main`'s `Supplier<HearingResultedDocument>` with the null check. `shouldReturnNullWhenFindByHearingIdAndLatestHearingDayNoHearingPresent` covers the `null` case | **Fixed.** No change from `main` in behaviour or in `HearingService` |
| CHD-02 (BC-08) | High | `sharedTime` in the query response changes from `...Z[UTC]` to `...Z` | `ResultsQueryViewTest.chd02_sharedTimeReadFromStoredPayload_should_keep_java17_string_form` | GREEN | **GREEN.** `ResultsQueryView` converts `sharedTime` with `withZoneSameInstant(ZoneId.of("UTC"))` before `toString()`, giving `2018-12-05T10:00Z[UTC]` as on Java 17 | **Fixed** (earlier commit). Still restore the assertion weakened with `toInstant()` at `HearingResultedIT.java:555` |
| CHD-03 (BC-24, Hibernate 5→6) | Removed | Times in zoneless columns could shift by an hour in BST if the JVM runs on a zone with daylight saving | None. Both tests removed | n/a | n/a | **Removed from scope** on Narayan's advice. The framework sets the JVM default to UTC, so the risk needs that default to be overridden. See "CHD-03 removed" |
| CHD-04 | Removed | A register generated between 00:00 and 01:00 BST gets a different `generatedDate` from Java 17, so `findByRegisterDate` looks in a different day | None. Test removed | n/a | n/a | **Removed from scope** on Narayan's advice. Production code is `team/25.104.x`'s (the same as `main`'s) `now()` (×3) and `currentDateTime.toLocalDate()`, so there is nothing left to compare. See "Removed parity tests" |
| CHD-05 (BC-01) | Medium | No GOB account: an exception (500) becomes a null payload (likely 404) | `DefendantGobAccountsEntityRepositoryTest.chd05_findAccountNumber_noMatch_should_throw_NoResultException` | GREEN | **GREEN** (also on Hibernate 7.3.2). `findAccountNumberByMasterDefendantIdAndHearingId` is back to Java 17's signature: it returns the entity and throws `NoResultException` when nothing matches (DeltaSpike's default `singleResult`). `DefendantGobAccountsQueryView` and its test are back to `main`'s code, including the unreachable `null` branch. **Observed in Stage 6:** no match returns HTTP 500 | **Fixed.** No change from `main`. The PR #228 CHD-01 review comment applied here too: `Optional.of(getSingleResult())` could never be empty |
| CHD-06 (BC-01, multi-match) | Medium | Two rows for one material: failure becomes an arbitrary row, so an NCES email could use the wrong recipient. No match also fails differently | `NcesEmailNotificationDetailsRepositoryTest.chd06_findByMaterialId_noMatch_should_throw_NoResultException`<br>`NcesEmailNotificationDetailsRepositoryTest.chd06_findByMaterialId_twoRowsForMaterial_should_throw_NonUniqueResultException` | GREEN ×2 | **GREEN ×2** (also on Hibernate 7.3.2). `findByMaterialId` is back to Java 17's signature: it returns the entity and throws `NoResultException` or `NonUniqueResultException`, as DeltaSpike's derived query did. `NcesEmailNotificationQueryView` and its test are back to `main`'s code. **Observed in Stage 6:** no match and two rows both return HTTP 500 | **Fixed.** No change from `main`. Same review point as CHD-05 |
| CHD-07 | Medium | A reference data response with no `prosecutors` key used to fail the command; now processing continues with default flags and police results could be skipped silently | `ReferenceDataServiceTest.chd07_responseWithoutProsecutorsKey_should_fail_not_return_empty` | GREEN | **GREEN.** The added null check is gone, so a missing key throws `NullPointerException` as on Java 17 | **Fixed** (earlier commit) |
| CHD-08 (BC-08) | Removed | In-memory times change from the JVM zone to UTC | None. Both tests removed | n/a | n/a | **Removed from scope** on Narayan's advice. `NCESDecisionHelper` and `HearingFinancialResultsTrackedListener` use `team/25.104.x`'s (the same as `main`'s) `ZonedDateTime.now()`. See "Removed parity tests" |
| CHD-09 | Low (was Medium) | `progression.version` moved to `17.104.306` | None. It's a build setting, so a unit test would only restate the pom | Not tested | Not tested. The query API and `add-court-document` schemas are unchanged; the command API adds only `remove-application-bdf` | **Keep the bump.** The enforcer requires it, and `main` needs the same change. See "Corrections" |
| CHD-10 | Removed | PDF file name timestamps change from JVM time | None. Test and its new class `DocumentGeneratorServiceTest` removed | n/a | n/a | **Removed from scope** on Narayan's advice. `DocumentGeneratorService` uses `team/25.104.x`'s (the same as `main`'s) `ZonedDateTime.now()`. See "Removed parity tests" |
| CHD-11 (BC-20) | Low | Rules could load as zero, so deny tests pass without proving anything | `ResultsCommandRuleExecutorTest.chd11_commandApiKieBase_should_load_rules`<br>`ResultsQueryApiAccessControlTest.chd11_queryApiKieBase_should_load_rules` | GREEN ×2 | GREEN ×2 | None. Keep the guards |
| CHD-12 (BC-13) | Low | Stricter schema validation could reject borderline payloads | `VerdictSchemaDependencyTest.chd12_dateTimeFormat_should_accept_and_reject_the_same_borderline_values`<br>`VerdictSchemaDependencyTest.chd12_integerType_should_treat_a_whole_decimal_payload_the_same` | GREEN ×2 | GREEN ×2 | None. Covers the engine with inline schemas, not every real schema |
| CHD-13 (BC-21) | Low | The Reflections change could alter the set of generated classes | None. Compared `target/generated-sources` between the two builds | n/a | **Same set.** 438 generated files in the same 6 modules on both stacks, none missing on either side. 429 are identical apart from imports and formatting. Of the other 9, 8 SJP result classes differ only in `serialVersionUID`, and `RemoteEventProcessor2SjpCommandApi` has one extra method from the newer SJP pin. See "Generated sources (CHD-13)" | **Closed.** No class generated on one stack is missing on the other |
| CHD-14 | Low | `hibernate.jpa.compliance.query=false` relaxes the whole persistence unit | `chd14_finderWithUnqualifiedAttributes_should_return_matching_rows` (class differs per copy, see above) | GREEN | GREEN | None for parity. Non-compliant JPQL still needs checking on 7.3.2 |
| CHD-15 | Low | Coverage gaps: MI contract unpack skipped, and persistence tests run on 6.6.1 while production uses 7.3.2 | None. This is a gap in test coverage, not a behaviour | n/a | **`ResultsSchemaContractValidationTest` (2 tests) no longer runs.** The 7.3.2 gap is closed: all 47 persistence tests pass on 7.3.2 | Remove the skip once mi-reportdata releases a Java 25 test jar |
| CHD-16 (BC-12, 16, 17) | Low | REST round trips, `/internal/metrics`, `stream_error` hashes, and Java 17 aggregate snapshots | None. These need a live deployment | Not tested | Not tested. No RESTEasy in the WAR, and no `/internal/metrics` references in this repo | Check in Stage 6 |

## Removed parity tests

Narayan advised that the time zone tests are no longer needed. They only mattered if the JVM ran on a zone with
daylight saving or if the `now()` calls changed. The framework's `EntityManagerProducer` sets the default to UTC,
and every `now()` now matches `team/25.104.x`. Six tests were removed from both copies:

| ID | Removed | Other cleanup |
|---|---|---|
| CHD-03 ×2 | `DefendantGobAccountsEntityRepositoryTest`: both `chd03_` tests and the `inZonelessTableOnLondonJvm` harness | Java 25: the `results-test-production-timezone-unit` persistence unit in the test `persistence.xml`. Java 17: the `createdHearingFinancialDetailsEntity(UUID)` overload |
| CHD-04 | `InformantRegisterEventListenerTest.chd04_` and its `generateSystemRegisterAt` / `generateSystemRegister` helpers | Mockito `mockStatic` and time zone imports |
| CHD-08 ×2 | `NCESDecisionHelperTest.chd08_` and `HearingFinancialResultsTrackedListenerTest.chd08_` | Time zone imports |
| CHD-10 | `DocumentGeneratorServiceTest`, a class added only for this test, deleted | None |

The Java 25 files now match `team/25.104.x`: the three test classes and `persistence.xml` exactly, and
`DefendantGobAccountsEntityRepositoryTest` apart from its CHD-05 changes. The Java 17 files match `main`, except
`DefendantGobAccountsEntityRepositoryTest`, which keeps its CHD-05 test. Before removal all six tests were green on
both JDKs. The CHD-03 PostgreSQL check that was outstanding is no longer needed.

## Corrections to earlier findings

**CHD-08: the review overstated it.** It said event timestamps would change from `+01:00` to `Z`. A probe shows the
framework mapper writes `2018-07-05T10:00:00.000Z` on both stacks. Only the in-memory zone differs, and it's visible
only through `toString()`, which is CHD-02. Lowered from Medium to Low.

**CHD-09: the review's advice was wrong.** It suggested reverting to `17.0.286` unless the bump was intended. The
enforcer rejects that version now `17.104.306` is released, so reverting would break the build. Lowered to Low.

Probe results for the same input:

| Input | Java 17 `toString()` | Java 25 `toString()` | Serialised (both) |
|---|---|---|---|
| `2018-12-05T10:00:00.000Z` | `2018-12-05T10:00Z[UTC]` | `2018-12-05T10:00Z` | `2018-12-05T10:00:00.000Z` |
| `2018-07-05T11:00:00+01:00` | `2018-07-05T10:00Z[UTC]` | `2018-07-05T10:00Z` | `2018-07-05T10:00:00.000Z` |

Library versions differ: Jackson 2.12.7 → 2.21.8 and org.json 20231013 → 20251224; everit is 1.6.0 on both.

## Harness differences in the Java 17 copy

| Area | Java 25 | Java 17 |
|---|---|---|
| Imports | `jakarta.persistence` | `javax.persistence` |
| CHD-01 stub (`stubNoDocumentForHearingDay`) | Throws `NoResultException`, as the repository does again | Same |
| CHD-14 flush | Test class already holds an `EntityManager` | `@Inject EntityManager` field added |

## Remaining limits

- **The CHD-01 service-level test is partly harness-defined on both JDKs.** Its stub throws, so what proves the
  repository really throws is the repository-level CHD-01 test, which runs against real Hibernate.
- **CHD-05 and CHD-06 status codes are observed on Java 25 only.** Stage 6 saw HTTP 500 in all three no-match and
  multi-match cases. The Java 17 500s are inferred: on Java 17 the same exceptions escape the same query views, but
  there is no Java 17 deployment to call.
- **The Hibernate 7.3.2 run is H2, not PostgreSQL.** No remaining parity test depends on database-specific behaviour.

## Hibernate 7.3.2 persistence run

WildFly 40.0.1 ships Hibernate 7.3.2, but the framework BOM pins 6.6.1.Final for unit tests. To run on 7.3.2, the
`results-viewstore-persistence` pom was pinned to `hibernate-core` 7.3.2.Final for the run only, then reverted.

| | Hibernate 6.6.1 | Hibernate 7.3.2 |
|---|---|---|
| Persistence tests | 47 of 47 | **47 of 47** |
| Parity tests in the module (CHD-01, CHD-05, CHD-06 ×2, CHD-14) | 5 green | **5 green** |

- **Test harness only:** Hibernate 7 moved classpath entity scanning out of `hibernate-core` into
  `hibernate-scan-jandex`. Without it, the test persistence units (`<jar-file>../classes</jar-file>` with
  `hibernate.archive.autodetection=class`) find no entities, and all tests fail with `UnknownEntityException`. In
  WildFly the container scans for entities, so production isn't affected. If the BOM moves to 7.x, add
  `hibernate-scan-jandex` as a test dependency of this module.
- **Download:** 7.3.2 isn't in the internal Artifactory mirror, which can't be reached from this machine. It was
  downloaded from Maven Central with a settings file that has no mirror. The run used `-o -llr` so Maven would accept it.
- **Rerun after removing CHD-03:** 47 of 47, with `hibernate-core-7.3.2.Final` confirmed on the surefire classpath.

## Stage 6 integration tests

Run against a local `cpp-developers-docker` (`java-25` branch) environment with WildFly, PostgreSQL and Artemis. The
code deployed is the same as the final commit `e22dfeab0`.

| Run | Result |
|---|---|
| `./runIntegrationTests.sh` | Build, Liquibase and deploy succeeded. Stopped at healthchecks: 5 of 6 passed (see below) |
| Full IT suite, first run | 60 of 66. 6 `StagingEnforcementIT` tests failed in a chain (see below) |
| `StagingEnforcementIT` alone | 12 of 12 |
| Full IT suite, second run | **66 of 66** |

- **Healthcheck:** `informant-register-queue-configuration-healthcheck` (from CRA-221) asks `FeatureControlGuard`,
  which calls the Azure feature manager at `java:global/featureManagerConnectionString`
  (`http://localhost:8080/azure/featuremanager/kv/`). Nothing stubs that URL locally, so it returns 404 and the
  script stops. The ITs were then run with the script's own step:
  `mvn verify -pl results-integration-test -Presults-integration-test -DINTEGRATION_HOST_KEY=localhost`. A permanent
  fix needs a feature-manager stub, an entry in `ResultsIgnoredHealthcheckNamesProvider`, or a healthcheck that copes
  when the feature manager can't be reached.
- **`StagingEnforcementIT` flake:** the class shares static message consumers across its tests. When one test misses
  its message in time, each later test reads the previous test's message. It passed on its own and in the next full
  run, with no code change. A fix would drain the consumers between tests.
- **CHD-05 and CHD-06 status codes,** observed by calling the query API on the deployed service:

| Case | Request | Status | Body |
|---|---|---|---|
| CHD-05: no GOB account | `GET /results-query-api/query/api/rest/results/defendant-gob-account?masterDefendantId=<random>&hearingId=<random>` | **500** | `No result found for query [SELECT * FROM defendant_gob_accounts …]` |
| CHD-06: no row for the material | `GET /results-query-api/query/api/rest/results/nces-email-notification/<random>` | **500** | `No result found for query [select n from NcesEmailNotificationDetailsEntity …]` |
| CHD-06: two rows for one material | Same, after inserting two `nces_email_notification` rows with one `material_id` | **500** | `Query did not return a unique result: 2 results were returned` |
| CHD-06 control: one row | Same, with one row | 200 | |

  The two probe rows were deleted afterwards.
- **CHD-02:** `HearingResultedIT` passes, but its assertion at line 555 is still the weakened `toInstant()` one, so it
  doesn't prove the `...Z[UTC]` form end to end.

## Generated sources (CHD-13)

Both copies were built with `mvn -o clean install`, and `target/generated-sources` and
`target/generated-test-sources` were compared module by module.

| Module | Java 25 | Java 17 | Only on one side |
|---|---|---|---|
| `results-command-api` | 20 | 20 | 0 |
| `results-command-handler` | 8 | 8 | 0 |
| `results-domain-common` | 341 | 341 | 0 |
| `results-event-listener` | 7 | 7 | 0 |
| `results-event-processor` | 25 | 25 | 0 |
| `results-query-api` | 37 | 37 | 0 |
| **Total** | **438** | **438** | **0** |

Contents were compared after mapping `jakarta.` to `javax.` and ignoring imports and whitespace. 429 files are
identical. The other 9:

- **8 SJP result classes** (`uk.gov.justice.sjp.results.BaseOffense`, `BasePersonDetail`, `BaseResult`,
  `CaseDefendant`, `CaseOffence`, `IndividualDefendant`, `Plea`, `Prompts`) differ only in the generated
  `serialVersionUID`. Nothing in results uses Java serialization on them, so this has no effect.
- **`RemoteEventProcessor2SjpCommandApi`** has one extra `@Handles("sjp.update-session-bdf")` method on Java 25. It
  comes from the newer SJP RAML (`sjp.version` 17.104.197 on Java 25 against 17.103.174 on the Java 17 copy), not
  from the Reflections change. Results doesn't send that command.

## Resolved observation

The 3 `Skipping resource` warnings (`hearing/enforceFinancialImposition.json`, `hearing/sharedResultLine.json`,
`hearing/target.json`) appear on both JDKs, so they're pre-existing and not an upgrade change.

## How to run

```bash
# Java 25, on dev/25.104-chd-j25-j17-parity
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home PATH=$JAVA_HOME/bin:$PATH \
mvn -o -B clean install -Dmaven.test.failure.ignore=true

# Java 17, on dev/CHD-2869-chd-parity-with-j25
JAVA_HOME=$HOME/Library/Java/JavaVirtualMachines/corretto-17.0.9/Contents/Home PATH=$JAVA_HOME/bin:$PATH \
mvn -o -B clean install -Dprogression.version=17.104.306 -Dmaven.test.failure.ignore=true

# Hibernate 7.3.2 persistence run (Java 25): temporarily add <version>7.3.2.Final</version> to hibernate-core and a
# test-scoped org.hibernate.orm:hibernate-scan-jandex:7.3.2.Final in results-viewstore-persistence/pom.xml, then
mvn -o -llr -B -pl results-viewstore/results-viewstore-persistence clean test

# Stage 6 ITs against a running cpp-developers-docker environment (after ./runIntegrationTests.sh has deployed)
mvn -B verify -pl results-integration-test -Presults-integration-test -DINTEGRATION_HOST_KEY=localhost
```

`jenv` has no JDK 25 registered on this machine, so `JAVA_HOME` is set directly. Use
`-Dmaven.test.failure.ignore=true` while parity tests are expected to be red, or the build stops at the first
failing module.
