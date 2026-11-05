# Java 25 parity test report: cpp-context-results

Stages 3 and 4 of [Java 25 upgrade with parity tests (team CHD)](https://hmcts.atlassian.net/wiki/spaces/CHD/pages/366839818).
BC references are from [Parity Testing Java17 → Java25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/279752455).

| | Java 25 copy | Java 17 copy |
|---|---|---|
| Branch | `dev/25.104-chd-j25-j17-parity` | `dev/CHD-2869-chd-parity-with-j25` (from `main`, never merged) |
| JDK | 25.0.5 | 17.0.9 (Corretto) |
| Hibernate in unit tests | 6.6.1.Final | 5.4.24.Final |
| H2 | 2.3.232 | 1.4.196 |

The Java 17 copy's production code is unchanged. On the Java 25 branch, production code has changed only to address the
PR #228 review, restoring `main`'s behaviour. Triage (Stage 5) and ITs (Stage 6) are not done.

**The JVM default zone is UTC, not UK time.** Earlier versions of this report took UK time (Europe/London) as fact.
The PR #228 review points out that the framework's `EntityManagerProducer.create()` calls `TimeZone.setDefault(UTC)`
(persistence-deltaspike 17.103.x and persistence-jpa 25.104.x), and the containers run in UTC, so `main`'s
`ZonedDateTime.now()` gives UTC on both stacks. `NCESDecisionHelper`, `HearingFinancialResultsTrackedListener`,
`InformantRegisterEventListener` and `DocumentGeneratorService` are back to `main`'s `ZonedDateTime.now()` calls, and the
CHD-04, CHD-08 and CHD-10 tests now set the JVM to UTC and assert UTC.

The review's rule is to keep `main`'s `now()` calls. `UtcClock` is `now(UTC).truncatedTo(MILLIS)`, so it also loses
Java 17's microsecond precision. The same rule was applied to four classes the review didn't name, which had the same
swap:

| Class | Restored to `main`'s |
|---|---|
| `HearingFinancialResultsAggregate` | `withCreatedTime(ZonedDateTime.now())` ×2 and `withUpdatedTime(now())`, replacing `now(UTC)` ×2 and `new UtcClock().now()` |
| `HearingFinancialResultsUpdatedListener` | `setUpdatedTime(ZonedDateTime.now())`, replacing `new UtcClock().now()` |
| `MigratedNcesEmailNotificationRequestedProcessor` | `withReceivedDateTime(ZonedDateTime.now())`, replacing `new UtcClock().now()` |
| `ResultsEventProcessor` | `withReceivedDateTime(ZonedDateTime.now())`, replacing `utcClock.now()`. The `UtcClock` field is on `main` too and stays |

`HearingFinancialResultsUpdatedListenerTest` and one `TestTemplates` (test-utilities) line are also back to `main`'s
`ZonedDateTime.now()`. A search of the production diff against `main` now finds no `UtcClock`, `now(UTC)` or
`Europe/London` changes. The one remaining zone change is CHD-02's `sharedTime` conversion, which restores Java 17's
output. Test code elsewhere still uses `new UtcClock().now()` to build test data, which doesn't affect production
behaviour.

## Results

| Run | Java 17 | Java 25 |
|---|---|---|
| Unit tests | 1,054 | 1,116 |
| Failed | **0** | **0** |
| Skipped | 2 | 2 |
| Parity tests (methods starting `chd`) | 18 | 18 |
| Parity tests green | **18** | **18** |
| Parity tests red | 0 | 0 |
| Other tests red | 0 | 0 |
| Hibernate 7.3.2 persistence run | n/a | **Not run** |

All 18 parity tests pass on both JDKs. The Java 25 figures come from a full `mvn -o install` on JDK 25.0.5 after the
PR #228 review fixes. Every viewstore repository was also checked against `main`'s DeltaSpike version: the other finders
all return lists, as on `main`, with the same queries. CHD-01, CHD-04, CHD-05, CHD-06, CHD-08 and CHD-10 were fixed in response to that review. CHD-02 and
CHD-07 were already green from earlier commits on this branch. Those rows below describe the code as it is now.

**The two copies have drifted and need realigning.** At Stage 4 a script confirmed all 18 method names matched and all
18 bodies were identical. Since then the CHD-04, CHD-08 (×2) and CHD-10 tests on the Java 25 branch were rewritten to
run on UK time and then moved to UTC, and the Java 17 copy has not been updated. Names still match, but those 4 bodies differ, and their
Java 17 results below come from the earlier version, which used far-from-UTC zones. See "Time zone tests". The other 14
bodies are unchanged.

One test is in a different class: `chd14_finderWithUnqualifiedAttributes_should_return_matching_rows` is in
`InformantRegisterRepositoryTest` on Java 17, because `InformantRegisterRepositoryRowValueQueryTest` doesn't exist on
`main`.

**Hibernate 7.3.2 gap:** the page requires this run, and WildFly 40.0.1 ships 7.3.2, but only 6.6.1 is in the local
Maven repository and the download did not complete. Every Java 25 persistence result here is Hibernate 6.6.1 on H2.

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
| CHD-03 (BC-24, Hibernate 5→6) | Medium (was High) | Times in zoneless columns could shift by an hour in BST if the JVM runs on a zone with daylight saving. The framework sets UTC, so this applies only if that default is overridden. The tests deliberately run the JVM on `Europe/London` to probe it | `DefendantGobAccountsEntityRepositoryTest.chd03_zonedDateTime_should_be_stored_as_jvm_local_wall_clock_in_zoneless_column`<br>`DefendantGobAccountsEntityRepositoryTest.chd03_existingRowWrittenAsLocalWallClock_should_read_back_as_the_same_instant` | GREEN ×2 | GREEN ×2. **Not reproduced on H2.** See "Corrections" | None yet. Confirm on PostgreSQL and Hibernate 7.3.2, because production differs from this H2 setup. Keep open until then |
| CHD-04 | High | A register generated between 00:00 and 01:00 BST gets a different `generatedDate` from Java 17, so `findByRegisterDate` looks in a different day | `InformantRegisterEventListenerTest.chd04_systemGeneratedRegister_should_take_generatedDate_from_jvm_zone` | GREEN (earlier version) | **GREEN.** Production code restored to `main`'s `now()` (×3) and `currentDateTime.toLocalDate()`. At 23:30 UTC on 1 July with the JVM on UTC: `2026-07-01`, as on Java 17 | **Fixed.** No change from `main`. `Europe/London` would have given `2026-07-02`, and `UtcClock` truncates to milliseconds |
| CHD-05 (BC-01) | Medium | No GOB account: an exception (likely 500) becomes a null payload (likely 404) | `DefendantGobAccountsEntityRepositoryTest.chd05_findAccountNumber_noMatch_should_throw_NoResultException` | GREEN | **GREEN.** `findAccountNumberByMasterDefendantIdAndHearingId` is back to Java 17's signature: it returns the entity and throws `NoResultException` when nothing matches (DeltaSpike's default `singleResult`). `DefendantGobAccountsQueryView` and its test are back to `main`'s code, including the unreachable `null` branch | **Fixed.** No change from `main`. The PR #228 CHD-01 review comment applied here too: `Optional.of(getSingleResult())` could never be empty |
| CHD-06 (BC-01, multi-match) | Medium | Two rows for one material: failure becomes an arbitrary row, so an NCES email could use the wrong recipient. No match also fails differently | `NcesEmailNotificationDetailsRepositoryTest.chd06_findByMaterialId_noMatch_should_throw_NoResultException`<br>`NcesEmailNotificationDetailsRepositoryTest.chd06_findByMaterialId_twoRowsForMaterial_should_throw_NonUniqueResultException` | GREEN ×2 | **GREEN ×2.** `findByMaterialId` is back to Java 17's signature: it returns the entity and throws `NoResultException` or `NonUniqueResultException`, as DeltaSpike's derived query did. `NcesEmailNotificationQueryView` and its test are back to `main`'s code | **Fixed.** No change from `main`. Same review point as CHD-05 |
| CHD-07 | Medium | A reference data response with no `prosecutors` key used to fail the command; now processing continues with default flags and police results could be skipped silently | `ReferenceDataServiceTest.chd07_responseWithoutProsecutorsKey_should_fail_not_return_empty` | GREEN | **GREEN.** The added null check is gone, so a missing key throws `NullPointerException` as on Java 17 | **Fixed** (earlier commit) |
| CHD-08 (BC-08) | Low (was Medium) | In-memory times change from the JVM zone to UTC | `NCESDecisionHelperTest.chd08_applicationResultsCreatedTime_should_be_in_jvm_zone`<br>`HearingFinancialResultsTrackedListenerTest.chd08_gobAccountAuditTimes_should_be_in_jvm_zone` | GREEN ×2 (earlier version) | **GREEN ×2.** Production code restored to `main`'s `ZonedDateTime.now()` in both classes; with the JVM default at UTC, as the framework sets it, the zone is `UTC` | **Fixed.** No change from `main`. `UtcClock` and `Europe/London` were dropped: `UtcClock` also truncates to milliseconds, which differs from Java 17's microseconds |
| CHD-09 | Low (was Medium) | `progression.version` moved to `17.104.306` | None. It's a build setting, so a unit test would only restate the pom | Not tested | Not tested. The query API and `add-court-document` schemas are unchanged; the command API adds only `remove-application-bdf` | **Keep the bump.** The enforcer requires it, and `main` needs the same change. See "Corrections" |
| CHD-10 | Low | PDF file name timestamps change from JVM time | `DocumentGeneratorServiceTest.chd10_generatedFileNameTimestamp_should_use_jvm_zone` (new class; none existed) | GREEN (earlier version) | **GREEN.** Production code restored to `main`'s `ZonedDateTime.now()`. At 23:30 UTC on 1 July with the JVM on UTC: `ENFDocumentOrder_20260701233000.pdf`, as on Java 17 | **Fixed.** No change from `main`. `Europe/London` would have made every BST file name an hour later |
| CHD-11 (BC-20) | Low | Rules could load as zero, so deny tests pass without proving anything | `ResultsCommandRuleExecutorTest.chd11_commandApiKieBase_should_load_rules`<br>`ResultsQueryApiAccessControlTest.chd11_queryApiKieBase_should_load_rules` | GREEN ×2 | GREEN ×2 | None. Keep the guards |
| CHD-12 (BC-13) | Low | Stricter schema validation could reject borderline payloads | `VerdictSchemaDependencyTest.chd12_dateTimeFormat_should_accept_and_reject_the_same_borderline_values`<br>`VerdictSchemaDependencyTest.chd12_integerType_should_treat_a_whole_decimal_payload_the_same` | GREEN ×2 | GREEN ×2 | None. Covers the engine with inline schemas, not every real schema |
| CHD-13 (BC-21) | Low | The Reflections change could alter the set of generated classes | None | Not tested | Not tested | Compare `target/generated-sources` file lists between the two copies. Both are now built, but this comparison hasn't been done |
| CHD-14 | Low | `hibernate.jpa.compliance.query=false` relaxes the whole persistence unit | `chd14_finderWithUnqualifiedAttributes_should_return_matching_rows` (class differs per copy, see above) | GREEN | GREEN | None for parity. Non-compliant JPQL still needs checking on 7.3.2 |
| CHD-15 | Low | Coverage gaps: MI contract unpack skipped, and persistence tests run on 6.6.1 while production uses 7.3.2 | None. This is a gap in test coverage, not a behaviour | n/a | **`ResultsSchemaContractValidationTest` (2 tests) no longer runs.** 7.3.2 run outstanding | Run on 7.3.2. Remove the skip once mi-reportdata releases a Java 25 test jar |
| CHD-16 (BC-12, 16, 17) | Low | REST round trips, `/internal/metrics`, `stream_error` hashes, and Java 17 aggregate snapshots | None. These need a live deployment | Not tested | Not tested. No RESTEasy in the WAR, and no `/internal/metrics` references in this repo | Check in Stage 6 |

## Corrections to earlier findings

**CHD-03: the earlier result was wrong.** The first Java 25 run reported both CHD-03 tests red, and said rows written
on Java 17 in BST read an hour late. That came from my harness, not from Hibernate. H2 caches the JVM zone, so
`TimeZone.setDefault` never reached it, and the same two tests failed the same way on Java 17, which exposed the
problem. With `DateTimeUtils.resetCalendar()` added to the harness, both tests pass on both JDKs, and the method
bodies did not change. The risk is lowered from High to Medium, not closed: this is H2, and production uses PostgreSQL
with Hibernate 7.3.2.

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
| CHD-03 factory | Test persistence unit `results-test-production-timezone-unit` | Hibernate `Configuration` in code, because a test `persistence.xml` would interfere with OpenEJB |
| CHD-03 table | `uuid` columns | `binary(16)` columns, as Hibernate 5 maps UUID on H2 |
| CHD-03 helper | `createdHearingFinancialDetailsEntity(UUID)` exists | Overload added |
| CHD-14 flush | Test class already holds an `EntityManager` | `@Inject EntityManager` field added |

## Remaining limits

- **The CHD-01 service-level test is partly harness-defined on both JDKs.** Its stub throws, so what proves the
  repository really throws is the repository-level CHD-01 test, which runs against real Hibernate.
- **Status codes in CHD-05 and CHD-06 are still inferred.** Stage 6 needs to observe them.

## Time zone tests

All these tests set the JVM default zone to `UTC`, as `EntityManagerProducer` does, and restore it afterwards.

- **CHD-04 and CHD-10** use Mockito `mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)` to stub only the no-arg
  `ZonedDateTime.now()`, returning `2026-07-01T23:30:00Z` in the JVM zone. That instant is already 2 July in the UK,
  so a change back to `Europe/London` would fail them. They assert exact expected values.
- **CHD-08** asserts the zone itself, so it needs no fixed time. Both tests expect `UTC`.

**Java 17 copy still to do:** Java 17 production code also calls `ZonedDateTime.now()`, so the Java 25 test bodies for
CHD-04, CHD-08 and CHD-10 should carry over unchanged, apart from `javax`/`jakarta` imports. They haven't been run on
Java 17 yet.

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
```

`jenv` has no JDK 25 registered on this machine, so `JAVA_HOME` is set directly. Use
`-Dmaven.test.failure.ignore=true` while parity tests are expected to be red, or the build stops at the first
failing module.
