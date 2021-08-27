package uk.gov.moj.cpp.results.event.helper.results;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;
import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.Offence.offence;

public class MoveDefendantJudicialResultsHelperTest {

    private static final UUID ID = randomUUID();
    private static final String OFFENCE_CODE = "offenceCode123";
    private static final String CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL = "case defendant " + randomAlphabetic(10);
    private static final String OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL = "offence " + randomAlphabetic(10);

    private MoveDefendantJudicialResultsHelper moveDefendantJudicialResultsHelper = new MoveDefendantJudicialResultsHelper();

    @Test
    public void shouldReturnSameOffencesAndJRsWhenNoOtherJRsToMatch() {

        UUID offenceId = randomUUID();
        UUID offenceId2 = randomUUID();
        final List<Offence> expectedOffenceList = of(buildOffence(offenceId), buildOffence(offenceId2));
        List<JudicialResult> caseDefendantJudicialResultList = of(buildCaseDefendantJudicialResultList(offenceId, randomUUID()));
        final Defendant defendant = getDefendant(expectedOffenceList, caseDefendantJudicialResultList);

        List<Offence> actualOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(defendant.getOffences());

        assertThat(actualOffenceList, is(expectedOffenceList));

        assertActualContainsMatchingJudicialResults(actualOffenceList.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList()),
                expectedOffenceList.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList()));
    }

    @Test
    public void shouldAddMatchingCaseDefendantLevelJRsToDefendantOffencesWhenOffenceIdMatch() {
        UUID offenceId = randomUUID();
        UUID offenceId2 = randomUUID();
        UUID caseDefendantJudicialResultId = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceId), buildOffence(offenceId2));
        List<JudicialResult> caseDefendantJudicialResultList = of(buildCaseDefendantJudicialResultList(offenceId, caseDefendantJudicialResultId));

        final Defendant defendant = getDefendant(originalOffenceList, caseDefendantJudicialResultList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantId), originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), emptyList());

        assertThat(updatedOffences.size(), is(2));

        Optional<List<JudicialResult>> actualOffenceJudicialResults = updatedOffences.stream().filter(o -> o.getId().equals(offenceId))
                .map(Offence::getJudicialResults)
                .findFirst();

        assertThat(updatedOffences.size(), is(2));
        assertThat(actualOffenceJudicialResults.get().stream()
                .filter(jr -> jr.getJudicialResultId().equals(caseDefendantJudicialResultId))
                .anyMatch(jr -> jr.equals(caseDefendantJudicialResultList.stream().filter(cdjr -> cdjr.getJudicialResultId().equals(jr.getJudicialResultId())).findFirst().get())), is(true));
    }

    @Test
    public void shouldAddMultipleMatchingCaseDefendantLevelJRsToDefendantOffencesWhenOffenceIdsMatch() {
        UUID offenceId = randomUUID();

        UUID offenceId1 = randomUUID();
        UUID caseDefendantJudicialResultId1 = randomUUID();
        UUID offenceId2 = randomUUID();
        UUID caseDefendantJudicialResultId2 = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceId), buildOffence(offenceId1), buildOffence(offenceId2));
        List<JudicialResult> caseDefendantJudicialResultList = of(buildCaseDefendantJudicialResultList(offenceId1, caseDefendantJudicialResultId1),
                buildCaseDefendantJudicialResultList(offenceId2, caseDefendantJudicialResultId2));

        final Defendant defendant = getDefendant(originalOffenceList, caseDefendantJudicialResultList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantId), originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), emptyList());

        assertThat(updatedOffences.size(), is(3));

        List<JudicialResult> matchingJRs = updatedOffences.stream()
                .filter(o -> o.getId().equals(offenceId1) || o.getId().equals(offenceId2))
                .flatMap(o -> o.getJudicialResults().stream())
                .collect(Collectors.toList());
        assertThat(matchingJRs.size(), is(4));

        assertThat(matchingJRs.stream()
                .map(JudicialResult::getJudicialResultId)
                .collect(Collectors.toList()), hasItems(caseDefendantJudicialResultId1, caseDefendantJudicialResultId2));
        assertActualContainsMatchingJudicialResults(matchingJRs, caseDefendantJudicialResultList);
    }

    @Test
    public void shouldAddMatchingDefendantLevelJRsToDefendantOffencesWhenMasterDefendantIdAndOffenceIdMatch() {

        UUID masterDefendantId = randomUUID();
        UUID offenceId = randomUUID();
        UUID offenceIdMatch = randomUUID();
        final List<Offence> originalOffenceList = of(buildOffence(offenceId), buildOffence(offenceIdMatch));
        final Defendant defendant = getDefendant(masterDefendantId, originalOffenceList, emptyList());
        final List<DefendantJudicialResult> defendantJudicialResults = of(getDefendantLevelJudicialResult(masterDefendantId, offenceIdMatch));

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantId),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffences.size(), is(2));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(3));
        assertActualContainsMatchingJudicialResults(actualOffenceJudicialResults,
                defendantJudicialResults.stream().map(DefendantJudicialResult::getJudicialResult).collect(Collectors.toList()));
    }

    @Test
    public void shouldAddMatchingMultipleDefendantLevelJRsToDefendantOffencesWhenMasterDefendantIdAndOffenceIdMatch() {

        UUID offenceIdMatch = randomUUID();
        UUID masterDefendantIdMatch = randomUUID();

        UUID offenceId1 = randomUUID();
        UUID masterDefendantId1 = randomUUID();
        UUID offenceId2 = randomUUID();
        UUID masterDefendantId2 = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceIdMatch), buildOffence(offenceId1), buildOffence(offenceId2));
        final Defendant defendant = getDefendant(masterDefendantIdMatch, originalOffenceList, emptyList());
        final List<DefendantJudicialResult> defendantJudicialResults = of(getDefendantLevelJudicialResult(masterDefendantIdMatch, offenceIdMatch),
                getDefendantLevelJudicialResult(masterDefendantId1, offenceId1),
                getDefendantLevelJudicialResult(masterDefendantId2, offenceId2));

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantIdMatch),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffences.size(), is(3));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(4));
        assertActualContainsMatchingJudicialResults(actualOffenceJudicialResults,
                defendantJudicialResults.stream().filter(djr -> djr.getMasterDefendantId().equals(masterDefendantIdMatch))
                        .map(DefendantJudicialResult::getJudicialResult).collect(Collectors.toList()));
    }

    @Test
    public void shouldNotAddDefendantLevelJRsToDefendantOffencesWhenMasterDefendantIdDoNotMatchButOffenceIdMatch() {

        UUID offenceIdMatch = randomUUID();
        UUID masterDefendantIdMatch = randomUUID();

        UUID masterDefendantId1 = randomUUID();
        UUID masterDefendantId2 = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceIdMatch));
        final Defendant defendant = getDefendant(masterDefendantIdMatch, originalOffenceList, emptyList());
        final List<DefendantJudicialResult> defendantJudicialResults = of(getDefendantLevelJudicialResult(masterDefendantId1, offenceIdMatch),
                getDefendantLevelJudicialResult(masterDefendantId2, offenceIdMatch));

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantIdMatch),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffences.size(), is(1));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(1));
        assertActualDoesNotContainMatchingJudicialResults(actualOffenceJudicialResults,
                defendantJudicialResults.stream().map(DefendantJudicialResult::getJudicialResult).collect(Collectors.toList()));
    }

    @Test
    public void shouldNotAddDefendantLevelJRsToDefendantOffencesWhenMasterDefendantIdMatchButOffenceIdDoNotMatch() {

        UUID offenceIdMatch = randomUUID();
        UUID masterDefendantIdMatch = randomUUID();

        UUID offenceId1 = randomUUID();
        UUID offenceId2 = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceIdMatch), buildOffence(randomUUID()));
        final Defendant defendant = getDefendant(masterDefendantIdMatch, originalOffenceList, emptyList());
        final List<DefendantJudicialResult> defendantJudicialResults = of(getDefendantLevelJudicialResult(masterDefendantIdMatch, offenceId1),
                getDefendantLevelJudicialResult(masterDefendantIdMatch, offenceId2));

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantIdMatch),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffences.size(), is(2));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(2));
        assertActualDoesNotContainMatchingJudicialResults(actualOffenceJudicialResults,
                defendantJudicialResults.stream().map(DefendantJudicialResult::getJudicialResult).collect(Collectors.toList()));
    }

    @Test
    public void shouldAddCaseDefendantAndDefendantLevelJRsToDefendantOffencesWhenMatch() {

        UUID offenceIdCaseDefMatch = randomUUID();
        UUID offenceIdDefMatch = randomUUID();
        UUID masterDefendantIdMatch = randomUUID();
        UUID caseDefendantJudicialResultId = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceIdDefMatch), buildOffence(offenceIdCaseDefMatch));
        List<JudicialResult> caseDefendantJudicialResultList = of(buildCaseDefendantJudicialResultList(offenceIdCaseDefMatch, caseDefendantJudicialResultId));
        final Defendant defendant = getDefendant(masterDefendantIdMatch, originalOffenceList, caseDefendantJudicialResultList);

        final List<DefendantJudicialResult> defendantJudicialResults = of(getDefendantLevelJudicialResult(masterDefendantIdMatch, offenceIdDefMatch));

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantIdMatch),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffences.size(), is(2));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(4));

        assertActualContainsMatchingJudicialResults(actualOffenceJudicialResults,
                defendantJudicialResults.stream().map(DefendantJudicialResult::getJudicialResult).collect(Collectors.toList()));
        assertActualContainsMatchingJudicialResults(actualOffenceJudicialResults, caseDefendantJudicialResultList);
    }

    @Test
    public void shouldAddCaseDefendantLevelJRsWithNoOffenceToDefendantOffencesWhenMasterDefendantIdAndResultCategoryInterim() {

        UUID offenceIdMatch = randomUUID();
        UUID masterDefendantIdMatch = randomUUID();

        UUID offenceId1 = randomUUID();
        UUID offenceId2 = randomUUID();

        List<JudicialResult> interimJR = buildOffenceJudicialResultList(offenceId2, JudicialResultCategory.INTERMEDIARY);
        final List<Offence> originalOffenceList = of(buildOffence(offenceIdMatch),
                buildOffence(offenceId1),
                buildOffenceWithJudicialResults(offenceId2, interimJR));

        UUID ddchCaseDefendantJudicialResultId = randomUUID();
        List<JudicialResult> caseDefendantJudicialResultList = of(buildCaseDefendantJudicialResultList(null, ddchCaseDefendantJudicialResultId));

        final Defendant defendant = getDefendant(masterDefendantIdMatch, originalOffenceList, caseDefendantJudicialResultList);

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantIdMatch),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), emptyList());

        assertThat(updatedOffences.size(), is(3));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(4));

        List<JudicialResult> expectedOffenceJrs = updatedOffences.stream().filter(o -> o.getId().equals(offenceId2))
                .map(Offence::getJudicialResults).findFirst().get();
        assertThat(expectedOffenceJrs.size(), is(2));
        assertThat(expectedOffenceJrs.stream().anyMatch(jrm -> jrm.equals(caseDefendantJudicialResultList.get(0))), is(true));
    }

    @Test
    public void shouldAddCaseDefendantLevelJRsWithNoOffenceHavingInterimJrsToTheFirstDefendantOffences() {

        UUID offenceIdMatch = randomUUID();
        UUID masterDefendantIdMatch = randomUUID();

        UUID offenceId1 = randomUUID();
        UUID offenceId2 = randomUUID();

        final List<Offence> originalOffenceList = of(buildOffence(offenceIdMatch), buildOffence(offenceId1), buildOffence(offenceId2));
        UUID ddchCaseDefendantJudicialResultId = randomUUID();
        List<JudicialResult> caseDefendantJudicialResultList = of(buildCaseDefendantJudicialResultList(null, ddchCaseDefendantJudicialResultId));
        final Defendant defendant = getDefendant(masterDefendantIdMatch, originalOffenceList, caseDefendantJudicialResultList);

        List<Offence> updatedOffences = moveDefendantJudicialResultsHelper.buildOffenceWithCaseDefendantAndDefendantLevelJudicialResults(Optional.of(masterDefendantIdMatch),
                originalOffenceList,
                defendant.getDefendantCaseJudicialResults(), emptyList());

        assertThat(updatedOffences.size(), is(3));

        List<JudicialResult> actualOffenceJudicialResults = updatedOffences.stream().flatMap(o -> o.getJudicialResults().stream()).collect(Collectors.toList());
        assertThat(actualOffenceJudicialResults.size(), is(4));

        List<JudicialResult> expectedOffenceJrs = updatedOffences.stream().findFirst().get().getJudicialResults();
        assertThat(expectedOffenceJrs.size(), is(2));
        assertThat(expectedOffenceJrs.stream().anyMatch(jrm -> jrm.equals(caseDefendantJudicialResultList.get(0))), is(true));
    }

    private void assertActualContainsMatchingJudicialResults(List<JudicialResult> actualJRs, List<JudicialResult> expectedJRs) {
        assertThat(actualJRs.isEmpty(), is(false));
        assertThat(expectedJRs.isEmpty(), is(false));
        assertThat(actualJRs.stream().map(JudicialResult::getJudicialResultId).collect(Collectors.toList()),
                hasItems(expectedJRs.stream().map(JudicialResult::getJudicialResultId).toArray()));
    }

    private void assertActualDoesNotContainMatchingJudicialResults(List<JudicialResult> actualJRs, List<JudicialResult> expectedJRs) {
        assertThat(actualJRs.isEmpty(), is(false));
        assertThat(expectedJRs.isEmpty(), is(false));
        assertThat(actualJRs.stream().map(JudicialResult::getJudicialResultId).collect(Collectors.toList()),
                not(hasItems(expectedJRs.stream().map(JudicialResult::getJudicialResultId).toArray())));
    }

    private Defendant getDefendant(List<Offence> originalOffenceList, List<JudicialResult> caseDefendantJudicialResultList) {
        return defendant()
                .withMasterDefendantId(randomUUID())
                .withOffences(originalOffenceList)
                .withDefendantCaseJudicialResults(caseDefendantJudicialResultList)
                .build();
    }

    private Defendant getDefendant(UUID masterDefendantId, List<Offence> originalOffenceList, List<JudicialResult> caseDefendantJudicialResultList) {
        return defendant()
                .withMasterDefendantId(masterDefendantId)
                .withOffences(originalOffenceList)
                .withDefendantCaseJudicialResults(caseDefendantJudicialResultList)
                .build();
    }

    private static JudicialResult buildCaseDefendantJudicialResultList(UUID offenceId, UUID judicialResultId) {
        return judicialResult()
                .withJudicialResultId(judicialResultId)
                .withCjsCode("cjsCode")
                .withOffenceId(offenceId)
                .withLabel(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .build();
    }

    private Offence buildOffence(final UUID offenceId) {
        return offence()
                .withId(offenceId)
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withJudicialResults(buildOffenceJudicialResultList(offenceId))
                .build();
    }

    private Offence buildOffenceWithJudicialResults(final UUID offenceId, List<JudicialResult> judicialResults) {
        return offence()
                .withId(offenceId)
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withJudicialResults(judicialResults)
                .build();
    }

    private static List<JudicialResult> buildOffenceJudicialResultList(UUID offenceId) {
        return of(judicialResult()
                .withOffenceId(offenceId)
                .withJudicialResultId(randomUUID())
                .withCjsCode("cjsCode")
                .withLabel(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .build());
    }

    private static List<JudicialResult> buildOffenceJudicialResultList(UUID offenceId, JudicialResultCategory judicialResultCategory) {
        return of(judicialResult()
                .withOffenceId(offenceId)
                .withJudicialResultId(randomUUID())
                .withCjsCode("cjsCode")
                .withCategory(judicialResultCategory)
                .withLabel(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .build());
    }

    private static DefendantJudicialResult getDefendantLevelJudicialResult(final UUID masterDefendantId, final UUID offenceId) {

        return defendantJudicialResult()
                .withMasterDefendantId(masterDefendantId)
                .withJudicialResult(judicialResult()
                        .withOffenceId(offenceId)
                        .withJudicialResultId(randomUUID())
                        .withCategory(JudicialResultCategory.FINAL)
                        .withCjsCode(RandomStringUtils.randomNumeric(4))
                        .withOrderedHearingId(ID)
                        .withOrderedDate(now())
                        .withRank(BigDecimal.ZERO)
                        .withJudicialResultTypeId(randomUUID())
                        .build())
                .build();
    }


}
