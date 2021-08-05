package uk.gov.moj.cpp.results.event.helper.results;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultCategory.ANCILLARY;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.core.courts.JudicialResultCategory.INTERMEDIARY;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.justice.core.courts.Offence.offence;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Offence;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class MoveDefendantJudicialResultsHelperTest {

    private static final UUID ID = randomUUID();
    private static final String OFFENCE_CODE = "offenceCode123";
    private static final String CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL = "case defendant " + randomAlphabetic(10);
    private static final String OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL = "offence " + randomAlphabetic(10);

    MoveDefendantJudicialResultsHelper moveDefendantJudicialResultsHelper = new MoveDefendantJudicialResultsHelper();

    @Test
    public void testBuildOffenceAndDefendantJudicialResults() {

        final List<Offence> offenceDetailsList = getOffencesForNoneMatchWithNotInterimAndNotWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getCategory(), is(nullValue()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getTerminatesOffenceProceedings(), is(true));

        assertThat(updatedOffenceList.get(1).getJudicialResults().size(), is(1));
        assertThat(updatedOffenceList.get(1).getJudicialResults().get(0).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(1).getJudicialResults().get(0).getCategory(), is(INTERMEDIARY));
        assertThat(updatedOffenceList.get(1).getJudicialResults().get(0).getTerminatesOffenceProceedings(), is(false));

        final DefendantJudicialResult relevantDefendantJudicialResult = defendantJudicialResults.stream().filter(djr -> djr.getMasterDefendantId() == masterDefendantId).findFirst().orElse(null);
        assertThat(updatedOffenceList.get(2).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getCategory(), is(FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(2).getLabel(), is(relevantDefendantJudicialResult.getJudicialResult().getLabel()));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(2).getCategory(), is(FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(2).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testBuildOffenceAndDefendantJudicialResults_NoOffenceLevelResultsPresent() {

        final List<Offence> offenceDetailsList = singletonList(buildOffenceWithNoJudicialResult());
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(1));
        final DefendantJudicialResult relevantDefendantJudicialResult = defendantJudicialResults.stream().filter(djr -> djr.getMasterDefendantId() == masterDefendantId).findFirst().orElse(null);
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is(relevantDefendantJudicialResult.getJudicialResult().getLabel()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(FINAL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testNoneMatchBuildOffenceAndDefendantJudicialResults() {
        final List<Offence> offenceDetailsList = getOffencesForNoneMatch();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.noneMatchBuildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(4));
        assertThat(updatedOffenceList.get(3).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(1).getCategory(), is(FINAL));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testNoneMatchBuildOffenceOrDefendantJudicialResults() {

        final List<Offence> offenceDetailsList = getOffencesForNoneMatchWithNotInterimAndNotWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.noneMatchBuildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getCategory(), is(FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void when_NoneMatch_Success_shouldPreferOffence_With_NotInterim_And_NotWithrdawn() {
        final List<Offence> offenceDetailsList = getOffencesToChooseFirstNoneMatchWithNotInterimAndNotWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.noneMatchBuildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getCategory(), is(FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testAllMatchBuildOffenceAndDefendantJudicialResults() {
        final List<Offence> offenceDetailsList = getOffences(true, INTERMEDIARY);
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.allMatchBuildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(INTERMEDIARY));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(true));

    }


    @Test
    public void testAllMatchBuildOffenceAndDefendantJudicialResultsWithAllINterimOrWithdrawn() {
        final List<Offence> offenceDetailsList = getOffencesWithAllInterimOrWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(INTERMEDIARY));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(true));

    }

    @Test
    public void testAllMatchBuildOffenceAndDefendantJudicialResultsWithAllWithdrawn() {
        final List<Offence> offenceDetailsList = getOffencesWithAllWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(nullValue()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(true));

    }

    @Test
    public void testIfNoneMatchAndAllMatchConditionsBothAreNotExecutedThenReturnOriginalOffences() {
        final List<Offence> offenceDetailsList = getOffences(null, null);
        final Defendant defendant = getDefendant(offenceDetailsList);
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        final List<DefendantJudicialResult> defendantJudicialResults = getDefendantLevelJudicialResults(masterDefendantId, randomUUID());
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(Optional.of(masterDefendantId), offenceDetailsList, defendant.getDefendantCaseJudicialResults(), defendantJudicialResults);

        assertThat(updatedOffenceList.size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getCategory(), is(nullValue()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getTerminatesOffenceProceedings(), is(nullValue()));

    }

    private Defendant getDefendant(final List<Offence> offenceDetails) {
        return defendant().withMasterDefendantId(randomUUID())
                .withOffences(offenceDetails)
                .withDefendantCaseJudicialResults(buildCaseDefendantJudicialResultList())
                .build();
    }

    private List<Offence> getOffences(Boolean terminatesOffenceProceedings, JudicialResultCategory category) {
        return singletonList(buildOffence(terminatesOffenceProceedings, category));
    }


    private List<Offence> getOffencesWithAllWithdrawn() {
        return newArrayList(buildOffence(true, null),
                buildOffence(true, null),
                buildOffence(true, null));
    }

    private List<Offence> getOffencesWithAllInterimOrWithdrawn() {
        return newArrayList(buildOffence(true, INTERMEDIARY),
                buildOffence(true, INTERMEDIARY),
                buildOffence(true, INTERMEDIARY));
    }

    private List<Offence> getOffencesForNoneMatch() {
        return newArrayList(buildOffence(true, INTERMEDIARY),
                buildOffence(false, INTERMEDIARY),
                buildOffence(true, FINAL),
                buildOffence(false, FINAL));
    }

    private List<Offence> getOffencesForNoneMatchWithNotInterimAndNotWithdrawn() {
        return newArrayList(buildOffence(true, null),
                buildOffence(false, INTERMEDIARY),
                buildOffence(false, FINAL));
    }

    private List<Offence> getOffencesToChooseFirstNoneMatchWithNotInterimAndNotWithdrawn() {
        return newArrayList(buildOffence(true, ANCILLARY),
                buildOffence(true, INTERMEDIARY),
                buildOffence(false, FINAL));
    }

    private static List<JudicialResult> buildCaseDefendantJudicialResultList() {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel(CASE_DEFENDANT_LEVEL_JUDICIAL_RESULT_LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .withJudicialResultPrompts(buildJudicialResultPrompt())
                .build());
    }

    private Offence buildOffence(final Boolean terminatesOffenceProceedings, final JudicialResultCategory category) {
        return offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withJudicialResults(buildOffenceJudicialResultList(terminatesOffenceProceedings, category))
                .build();
    }

    private Offence buildOffenceWithNoJudicialResult() {
        return offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .build();
    }

    private static List<JudicialResult> buildOffenceJudicialResultList(final Boolean terminatesOffenceProceedings, final JudicialResultCategory category) {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(category)
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel(OFFENCE_LEVEL_JUDICIAL_RESULT_LABEL)
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withTerminatesOffenceProceedings(terminatesOffenceProceedings)
                .withWelshLabel("welshLabel")
                .withJudicialResultPrompts(buildJudicialResultPrompt())
                .build());
    }

    private static List<JudicialResultPrompt> buildJudicialResultPrompt() {
        return of(judicialResultPrompt()
                .withTotalPenaltyPoints(new BigDecimal(10))
                .withIsFinancialImposition(true)
                .withValue("value")
                .withLabel("label")
                .build());
    }

    private List<DefendantJudicialResult> getDefendantLevelJudicialResults(final UUID... masterDefendantIds) {
        return Arrays.stream(masterDefendantIds).map(MoveDefendantJudicialResultsHelperTest::getDefendantLevelJudicialResult).collect(Collectors.toList());
    }

    private static DefendantJudicialResult getDefendantLevelJudicialResult(final UUID masterDefendantId) {
        final String randomStringSuffix = randomAlphabetic(10);

        return defendantJudicialResult()
                .withMasterDefendantId(masterDefendantId)
                .withJudicialResult(judicialResult()
                        .withJudicialResultId(randomUUID())
                        .withCategory(JudicialResultCategory.FINAL)
                        .withCjsCode(RandomStringUtils.randomNumeric(4))
                        .withIsAdjournmentResult(false)
                        .withIsAvailableForCourtExtract(false)
                        .withIsConvictedResult(false)
                        .withIsFinancialResult(false)
                        .withLabel("defendant level label " + randomStringSuffix)
                        .withOrderedHearingId(ID)
                        .withOrderedDate(now())
                        .withRank(BigDecimal.ZERO)
                        .withWelshLabel("result definition welsh label " + randomStringSuffix)
                        .withResultText("result text " + randomStringSuffix)
                        .withLifeDuration(false)
                        .withTerminatesOffenceProceedings(Boolean.FALSE)
                        .withLifeDuration(false)
                        .withPublishedAsAPrompt(false)
                        .withExcludedFromResults(false)
                        .withAlwaysPublished(false)
                        .withUrgent(false)
                        .withD20(false)
                        .withPublishedForNows(false)
                        .withRollUpPrompts(false)
                        .withJudicialResultTypeId(randomUUID())
                        .withJudicialResultPrompts(singletonList(judicialResultPrompt()
                                .withIsFinancialImposition(true)
                                .withJudicialResultPromptTypeId(randomUUID())
                                .withCourtExtract("Y")
                                .withLabel("prompt label " + randomStringSuffix)
                                .withValue("value " + randomStringSuffix)
                                .withTotalPenaltyPoints(BigDecimal.TEN)
                                .build()))
                        .build())
                .build();
    }


}
