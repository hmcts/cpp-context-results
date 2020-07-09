package uk.gov.moj.cpp.results.event.helper.results;

import static com.google.common.collect.ImmutableList.of;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Offence;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class MoveDefendantJudicialResultsHelperTest {

    private static final UUID ID = randomUUID();
    private static final String OFFENCE_CODE = "offenceCode123";

    MoveDefendantJudicialResultsHelper moveDefendantJudicialResultsHelper = new MoveDefendantJudicialResultsHelper();


    @Test
    public void testBuildOffenceAndDefendantJudicialResults() {
        final List<Offence> offenceDetailsList =  getOffencesForNoneMatchWithNotInterimAndNotWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getCategory(), is(nullValue()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getTerminatesOffenceProceedings(), is(true));

        assertThat(updatedOffenceList.get(1).getJudicialResults().size(), is(1));
        assertThat(updatedOffenceList.get(1).getJudicialResults().get(0).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(1).getJudicialResults().get(0).getCategory(), is(Category.INTERMEDIARY));
        assertThat(updatedOffenceList.get(1).getJudicialResults().get(0).getTerminatesOffenceProceedings(), is(true));

        assertThat(updatedOffenceList.get(2).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getCategory(), is(Category.FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testNoneMatchBuildOffenceAndDefendantJudicialResults() {
        final List<Offence> offenceDetailsList =  getOffencesForNoneMatch();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.noneMatchBuildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(4));
        assertThat(updatedOffenceList.get(3).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(1).getCategory(), is(Category.FINAL));
        assertThat(updatedOffenceList.get(3).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testNoneMatchBuildOffenceOrDefendantJudicialResults() {
        final List<Offence> offenceDetailsList =  getOffencesForNoneMatchWithNotInterimAndNotWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Defendant> defendantsFromRequest = Arrays.asList(defendant, defendant);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.noneMatchBuildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getCategory(), is(Category.FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void when_NoneMatch_Success_shouldPreferOffence_With_NotInterim_And_NotWithrdawn() {
        final List<Offence> offenceDetailsList =  getOffencesToChooseFirstNoneMatchWithNotInterimAndNotWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Defendant> defendantsFromRequest = Arrays.asList(defendant, defendant);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.noneMatchBuildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(2).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getCategory(), is(Category.FINAL));
        assertThat(updatedOffenceList.get(2).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(false));
    }

    @Test
    public void testAllMatchBuildOffenceAndDefendantJudicialResults() {

        final List<Offence> offenceDetailsList = getOffences(true, Category.INTERMEDIARY);
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Defendant> defendantsFromRequest = Arrays.asList(defendant, defendant);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.allMatchBuildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(Category.INTERMEDIARY));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(true));

    }


    @Test
    public void testAllMatchBuildOffenceAndDefendantJudicialResultsWithAllINterimOrWithdrawn() {

        final List<Offence> offenceDetailsList = getOffencesWithAllInterimOrWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(Category.INTERMEDIARY));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(true));

    }

    @Test
    public void testAllMatchBuildOffenceAndDefendantJudicialResultsWithAllWithdrawn() {

        final List<Offence> offenceDetailsList = getOffencesWithAllWithdrawn();
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(3));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(2));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is("defendantLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getCategory(), is(nullValue()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(1).getTerminatesOffenceProceedings(), is(true));

    }

    @Test
    public void testIfNoneMatchAndAllMatchConditionsBothAreNotExecutedThenReturnOriginalOffences() {

        final List<Offence> offenceDetailsList = getOffences(null, null);
        final Defendant defendant = getDefendant(offenceDetailsList);
        final List<Offence> updatedOffenceList = moveDefendantJudicialResultsHelper.buildOffenceAndDefendantJudicialResults(offenceDetailsList, defendant.getDefendantCaseJudicialResults());

        assertThat(updatedOffenceList.size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().size(), is(1));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getLabel(), is("offenceLabel"));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getCategory(), is(nullValue()));
        assertThat(updatedOffenceList.get(0).getJudicialResults().get(0).getTerminatesOffenceProceedings(), is(nullValue()));

    }

    private Defendant getDefendant(final List<Offence> offenceDetails) {
        return Defendant.defendant().withOffences(offenceDetails).withDefendantCaseJudicialResults(buildDefendantJudicialResultList()).build();
    }

    private List<Offence> getOffences(Boolean terminatesOffenceProceedings, Category category) {
        return Arrays.asList(buildOffence(terminatesOffenceProceedings, category));
    }


    private List<Offence> getOffencesWithAllWithdrawn() {
        return Arrays.asList(buildOffence(true,null),buildOffence(true,null),buildOffence(true,null));
    }

    private List<Offence> getOffencesWithAllInterimOrWithdrawn() {
        return Arrays.asList(buildOffence(true,Category.INTERMEDIARY),buildOffence(true,Category.INTERMEDIARY),buildOffence(true,Category.INTERMEDIARY));
    }

    private List<Offence> getOffencesForNoneMatch() {
        return Arrays.asList(buildOffence(true,Category.INTERMEDIARY ),
                buildOffence(false,Category.INTERMEDIARY),
                buildOffence(true,Category.FINAL),
                buildOffence(false,Category.FINAL ) );
    }

    private List<Offence> getOffencesForNoneMatchWithNotInterimAndNotWithdrawn() {
        return Arrays.asList(buildOffence(true,null ),
                buildOffence(true,Category.INTERMEDIARY),
                buildOffence(false,Category.FINAL ));
    }

    private List<Offence> getOffencesToChooseFirstNoneMatchWithNotInterimAndNotWithdrawn() {
        return Arrays.asList(buildOffence(true,Category.ANCILLARY ),
                buildOffence(true,Category.INTERMEDIARY),
                buildOffence(false,Category.FINAL ));
    }

    private static List<JudicialResult> buildDefendantJudicialResultList() {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel("defendantLabel")
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .withJudicialResultPrompts(buildJudicialResultPrompt())
                .build());
    }

    private Offence buildOffence(final Boolean terminatesOffenceProceedings, final Category category) {
        return Offence.offence()
                .withId(ID)
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withJudicialResults(buildOffenceJudicialResultList(terminatesOffenceProceedings, category))
                .build();
    }

    private static List<JudicialResult> buildOffenceJudicialResultList(Boolean ternminatesOffenceProceedings, Category category) {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(category)
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel("offenceLabel")
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withTerminatesOffenceProceedings(ternminatesOffenceProceedings)
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


}
