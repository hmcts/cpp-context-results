# Java 25 parity test report: cpp-context-results

Started at Stage 3 of [Java 25 upgrade with parity tests (team CHD)](https://hmcts.atlassian.net/wiki/spaces/CHD/pages/366839818).
BC references are from [Parity Testing Java17 → Java25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/279752455).

| | |
|---|---|
| Branch | `dev/25.104-chd-j25-j17-parity`, based on `e2e608ab1` (CHD-2869 Upgrade java25) |
| JDK | 25.0.5 |
| Hibernate in unit tests | 6.6.1.Final (with H2 2.3.232) |
| Production code changed | None |
| Status | Java 25 copy only. Java 17 copy (Stage 4), triage (Stage 5) and ITs (Stage 6) not done |

## Java 25 results

| Run | Tests | Green | Red | Skipped |
|---|---|---|---|---|
| Full unit run (`mvn -o -B clean install -Dmaven.test.failure.ignore=true`) | 1,116 | 1,101 | 13 | 2 |
| Parity tests only (methods starting `chd`) | 18 | 5 | 13 | 0 |
| Tests that existed before this work | 1,098 | 1,096 | 0 | 2 |
| Hibernate 7.3.2 persistence run | **Not run** | | | |

All 13 red tests are parity tests. Each red is a finding to triage in Stage 5, not a broken build.

In the full run, one CHD-03 test failed on a harness error (the H2 table already existed). I fixed the harness
and reran `DefendantGobAccountsEntityRepositoryTest` only: 12 tests, 3 red, all assertion failures. The full run
was not repeated after that one-line fix.

**Hibernate 7.3.2 gap:** the page requires this run, and WildFly 40.0.1 ships 7.3.2, but only 6.6.1 is in the local
Maven repository and the download did not complete. Every persistence result below is Hibernate 6.6.1 on H2.

## Risks

| ID | Severity | What could change | Test | Java 25 result | Suggested fix |
|---|---|---|---|---|---|
| CHD-01 (BC-01) | High | A query for a hearing date with no document returns the latest day's results with a 200, instead of failing | `HearingResultedDocumentRepositoryTest.chd01_findByHearingIdAndHearingDay_noDocumentForThatDay_should_throw_NoResultException`<br>`HearingServiceTest.chd01_hearingDateWithNoDocument_should_not_fall_back_to_latest_hearing_day` | **RED ×2.** No exception thrown; `findByHearingIdAndLatestHearingDay` was called. The wrong-day fallback is proven | **Fix.** Restore `getSingleResult()`, or stop the fallback in `ResultsQueryView.java:121-122`. Returning another day's results can't be accepted either way |
| CHD-02 (BC-08) | High | `sharedTime` in the query response changes from `...Z[UTC]` to `...Z` | `ResultsQueryViewTest.chd02_sharedTimeReadFromStoredPayload_should_keep_java17_string_form` | **RED.** Expected `2018-12-05T10:00Z[UTC]`, got `2018-12-05T10:00Z` | **Fix or Accept**, depending on consumers. `...Z` is valid `date-time` and `Z[UTC]` isn't, so Accept may be right, with owner sign-off. Either way restore the assertion weakened with `toInstant()` at `HearingResultedIT.java:555` |
| CHD-03 (BC-24, Hibernate 5→6) | High | Times in zoneless columns shift by an hour on a UK-time JVM in BST | `DefendantGobAccountsEntityRepositoryTest.chd03_zonedDateTime_should_be_stored_as_jvm_local_wall_clock_in_zoneless_column`<br>`DefendantGobAccountsEntityRepositoryTest.chd03_existingRowWrittenAsLocalWallClock_should_read_back_as_the_same_instant` | **RED ×2.** Write: stored `10:00:00`, expected `11:00:00`. Read: existing row gave `11:00Z`, expected `10:00Z`. Hibernate 6 treats the column as UTC, so rows written on Java 17 in BST read an hour late | **Fix.** First confirm the production JVM zone, then repeat on PostgreSQL and Hibernate 7.3.2. If it reproduces, restore the JVM-zone mapping (for example `hibernate.jdbc.time_zone` or a time-zone storage setting) |
| CHD-04 | High | A register generated between 00:00 and 01:00 BST gets the previous day's `generatedDate`, so `findByRegisterDate` looks in the wrong day | `InformantRegisterEventListenerTest.chd04_systemGeneratedRegister_should_take_generatedDate_from_jvm_zone` | **RED.** Expected `2026-11-03` (JVM date), got `2026-11-02` (UTC date) | **Fix.** Derive the date in `Europe/London`, as `getLocalLondonZoneDate` already does in `ResultsEventListener`, rather than relying on the JVM zone |
| CHD-05 (BC-01) | Medium | No GOB account: an exception (likely 500) becomes a null payload (likely 404) | `DefendantGobAccountsEntityRepositoryTest.chd05_findAccountNumber_noMatch_should_throw_NoResultException` | **RED.** No exception thrown | **Accept candidate.** The `null` branch in the view shows this was the intent, but it was dead code on Java 17. Needs owner sign-off, and the status codes need confirming in Stage 6 |
| CHD-06 (BC-01, multi-match) | Medium | Two rows for one material: failure becomes an arbitrary row, so an NCES email could use the wrong recipient. No match also fails differently | `NcesEmailNotificationDetailsRepositoryTest.chd06_findByMaterialId_noMatch_should_throw_NoResultException`<br>`NcesEmailNotificationDetailsRepositoryTest.chd06_findByMaterialId_twoRowsForMaterial_should_throw_NonUniqueResultException` | **RED ×2.** No exception thrown in either case | **Fix.** End the finder with `getSingleResult()` |
| CHD-07 | Medium | A reference data response with no `prosecutors` key used to fail the command; now processing continues with default flags and police results could be skipped silently | `ReferenceDataServiceTest.chd07_responseWithoutProsecutorsKey_should_fail_not_return_empty` | **RED.** `NullPointerException` expected, nothing thrown | **Fix.** Remove the added null check, unless the owner decides a missing key should be tolerated |
| CHD-08 (BC-08) | Low (was Medium) | In-memory times change from the JVM zone to UTC | `NCESDecisionHelperTest.chd08_applicationResultsCreatedTime_should_be_in_jvm_zone`<br>`HearingFinancialResultsTrackedListenerTest.chd08_gobAccountAuditTimes_should_be_in_jvm_zone` | **RED ×2.** Expected `Pacific/Kiritimati`, got `Z` | **Accept candidate.** Same instant, and serialised JSON is identical on both stacks. See "Correction" below |
| CHD-09 | Medium | `progression.version` moved to `17.104.306`; neither `main` nor `team/25.104.x` has this | None. It's a build setting, so a unit test would only restate the pom | Not tested. The query API and `add-court-document` schemas are unchanged; the command API adds only `remove-application-bdf` | **Owner decision.** Revert to `17.0.286` unless the bump is intended |
| CHD-10 | Low | PDF file name timestamps change from JVM time to UTC | `DocumentGeneratorServiceTest.chd10_generatedFileNameTimestamp_should_use_jvm_zone` (new class; none existed) | **RED.** 50,400 seconds (14 hours) from the JVM-zone time | **Accept candidate**, once nothing is found depending on the name's time |
| CHD-11 (BC-20) | Low | Rules could load as zero, so deny tests pass without proving anything | `ResultsCommandRuleExecutorTest.chd11_commandApiKieBase_should_load_rules`<br>`ResultsQueryApiAccessControlTest.chd11_queryApiKieBase_should_load_rules` | **GREEN ×2** | None. Keep the guards |
| CHD-12 (BC-13) | Low | Stricter schema validation could reject borderline payloads | `VerdictSchemaDependencyTest.chd12_dateTimeFormat_should_accept_and_reject_the_same_borderline_values`<br>`VerdictSchemaDependencyTest.chd12_integerType_should_treat_a_whole_decimal_payload_the_same` | **GREEN ×2.** A separate probe gave the same results on both stacks (see below) | None. Covers the engine with inline schemas, not every real schema |
| CHD-13 (BC-21) | Low | The Reflections change could alter the set of generated classes | None. Needs a Java 17 build to compare with | Not tested | Compare `target/generated-sources` file lists in Stage 4 |
| CHD-14 | Low | `hibernate.jpa.compliance.query=false` relaxes the whole persistence unit | `InformantRegisterRepositoryRowValueQueryTest.chd14_finderWithUnqualifiedAttributes_should_return_matching_rows` | **GREEN** | None for parity. Non-compliant JPQL still needs checking on 7.3.2 |
| CHD-15 | Low | Coverage gaps: MI contract unpack skipped, and persistence tests run on 6.6.1 while production uses 7.3.2 | None. This is a gap in test coverage, not a behaviour | Not tested. 7.3.2 run still outstanding | Run on 7.3.2. Remove the skip once mi-reportdata releases a Java 25 test jar |
| CHD-16 (BC-12, 16, 17) | Low | REST round trips, `/internal/metrics`, `stream_error` hashes, and Java 17 aggregate snapshots | None. These need a live deployment | Not tested. No RESTEasy in the WAR, and no `/internal/metrics` references in this repo | Check in Stage 6 |

## Limits of these results

- **Java 17 expectations aren't all proven yet.** Stage 4 must confirm them, and a Java 17 failure means the
  expectation is wrong. Sources so far:
  - CHD-02 and CHD-12: measured on the real 17.104.5 stack with a throwaway probe.
  - CHD-01, CHD-05, CHD-06: DeltaSpike 1.9.6 bytecode, the skill's bytecode table, and tests on `main`.
  - CHD-04, CHD-07, CHD-08, CHD-10: read from the code on `main`.
  - CHD-03: reasoned from Hibernate 5 behaviour. **This is the weakest expectation.**
- **CHD-03 is H2, not PostgreSQL.** It shows a real difference in Hibernate's handling, but the size of the effect
  on PostgreSQL still has to be measured. Its harness uses a new test persistence unit,
  `results-test-production-timezone-unit`, because the shared unit sets `hibernate.jdbc.time_zone=UTC`, which
  production doesn't.
- **CHD-03, CHD-04 and CHD-10 depend on the production JVM zone.** The upgrade skill says SIT and production run on
  UK time; this repo doesn't confirm it. CHD-04, CHD-08 and CHD-10 use far-from-UTC zones so they fail on any date;
  they show JVM-zone dependence, not a UK-specific result. London equals UTC outside BST.
- **CHD-01 test harness.** `stubNoDocumentForHearingDay` returns `Optional.empty()` here. The Java 17 copy must throw
  `NoResultException` instead, because that's what the Java 17 repository does.

## Correction to the Stage 2 review

CHD-08 said event timestamps would change from `+01:00` to `Z`. The probe shows that's wrong for serialised output:
the framework mapper writes `2018-07-05T10:00:00.000Z` on both stacks. Only the in-memory zone differs, and it's
visible only through `toString()` (which is CHD-02). CHD-08 is lowered from Medium to Low.

Probe results for the same input:

| Input | Java 17 `toString()` | Java 25 `toString()` | Serialised (both) |
|---|---|---|---|
| `2018-12-05T10:00:00.000Z` | `2018-12-05T10:00Z[UTC]` | `2018-12-05T10:00Z` | `2018-12-05T10:00:00.000Z` |
| `2018-07-05T11:00:00+01:00` | `2018-07-05T10:00Z[UTC]` | `2018-07-05T10:00Z` | `2018-07-05T10:00:00.000Z` |

Schema probe, identical on both stacks: `...00.000Z` and `...00Z` accepted; `...Z[UTC]` and a bare date rejected;
integer `1` accepted; `1.5` and `1.0` rejected. Library versions differ: Jackson 2.12.7 → 2.21.8 and org.json
20231013 → 20251224; everit is 1.6.0 on both.

## Other observation

The build logs 3 `Skipping resource` warnings for `hearing/enforceFinancialImposition.json`,
`hearing/sharedResultLine.json` and `hearing/target.json`. No schema file under `results-json` differs from `main`
(only the pom version), so these files aren't part of the upgrade, but I haven't checked whether the warnings also
appear on Java 17. Check in Stage 4.

## How to run

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home \
PATH=$JAVA_HOME/bin:$PATH \
mvn -o -B clean install -Dmaven.test.failure.ignore=true
```

`jenv` has no JDK 25 registered on this machine, so `JAVA_HOME` is set directly. Use
`-Dmaven.test.failure.ignore=true` while parity tests are expected to be red, or the build stops at the first
failing module.
