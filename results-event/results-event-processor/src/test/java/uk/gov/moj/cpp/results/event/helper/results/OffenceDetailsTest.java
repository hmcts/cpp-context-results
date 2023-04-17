package uk.gov.moj.cpp.results.event.helper.results;

import static com.google.common.collect.ImmutableList.of;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareHearingTemplateWithApplication;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class OffenceDetailsTest {

    public static final String MODE_OF_TRIAL = "1010";
    private static final String OFFENCE_WORDING = "offenceWording";
    private static final UUID ID = randomUUID();
    private static final String OFFENCE_CODE = "offenceCode123";

    private static ImmutableList<JudicialResult> buildJudicialResultList() {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(JudicialResultCategory.INTERMEDIARY)
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel("label")
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .withTerminatesOffenceProceedings(Boolean.TRUE)
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

    private static List<JudicialResult> buildDefendantJudicialResultList() {
        return of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withTerminatesOffenceProceedings(Boolean.TRUE)
                .withCategory(JudicialResultCategory.INTERMEDIARY)
                .withCjsCode("cjsCode")
                .withIsAdjournmentResult(false)
                .withIsAvailableForCourtExtract(false)
                .withIsConvictedResult(false)
                .withIsFinancialResult(false)
                .withLabel("label")
                .withOrderedHearingId(ID)
                .withOrderedDate(now())
                .withRank(BigDecimal.ZERO)
                .withWelshLabel("welshLabel")
                .withJudicialResultPrompts(buildJudicialResultPrompt())
                .build());
    }

    @Test
    public void testBuildOffences() {

        final Hearing hearing = TestTemplates.basicShareResultsTemplate(JurisdictionType.CROWN).getHearing();

        final List<DefendantJudicialResult>  defendantJudicialResults = hearing.getDefendantJudicialResults();
        final JudicialResult judicialResult = judicialResult().withValuesFrom(defendantJudicialResults.get(0)
                .getJudicialResult()).withLabel("label")
                .withCategory(JudicialResultCategory.INTERMEDIARY).build();

        DefendantJudicialResult defendantJudicialResult = DefendantJudicialResult.defendantJudicialResult().withValuesFrom(hearing.getDefendantJudicialResults().get(0)).withJudicialResult(judicialResult).build();
        defendantJudicialResults.set(0, defendantJudicialResult);

        List<Offence> offenceDetails = getOffences();
        Defendant defendant = getDefendant(offenceDetails);

        List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList = new OffenceDetails().buildOffences(defendant, defendantJudicialResults);
        uk.gov.justice.core.courts.OffenceDetails offenceDetailsFromRequest = offenceDetailsList.get(0);
        assertOffenceDetails(offenceDetailsList, offenceDetailsFromRequest, offenceDetails.get(0).getOrderIndex());
    }

    @Test
    public void prepareSpiOutApplicationPayloadOnlyWhenCJSCodeIsSet() {
        Hearing hearing = basicShareHearingTemplateWithApplication(randomUUID(), JurisdictionType.MAGISTRATES);
        final CourtApplication courtApplication = hearing.getCourtApplications().get(0);
        final CourtApplicationType finalCourtApplicationType = courtApplicationType().withValuesFrom(courtApplication.getType())
                .withCode("CJS").withSpiOutApplicableFlag(true).build();
        final CourtApplication finalCourtApplication = courtApplication().withValuesFrom(courtApplication)
                        .withType(finalCourtApplicationType)
                .withJudicialResults(Collections.singletonList(judicialResult().build())).build();

        hearing().withValuesFrom(hearing).withCourtApplications(Collections.singletonList(finalCourtApplication)).build();

        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetails = new OffenceDetails().buildOffences(finalCourtApplication.getCourtApplicationCases().get(0), finalCourtApplication);
        assertEquals(2, offenceDetails.size());
        assertEquals("CJS", offenceDetails.get(0).getOffenceCode());
        assertEquals(finalCourtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getOffenceCode(), offenceDetails.get(1).getOffenceCode());

    }

    @Test
    public void shouldNotPrepareSpiOutApplicationPayloadWhenCJSCodeIsNotSet() {
        Hearing hearing = basicShareHearingTemplateWithApplication(randomUUID(), JurisdictionType.MAGISTRATES);
        final CourtApplication courtApplication = hearing.getCourtApplications().get(0);

        final CourtApplicationType finalCourtApplicationType = courtApplicationType().withValuesFrom(courtApplication.getType())
                .withCode(null).withSpiOutApplicableFlag(true).build();
        final CourtApplication finalCourtApplication = courtApplication().withValuesFrom(courtApplication)
                .withType(finalCourtApplicationType)
                .withJudicialResults(Collections.singletonList(judicialResult().build())).build();
         hearing().withValuesFrom(hearing).withCourtApplications(Collections.singletonList(finalCourtApplication)).build();

        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetails = new OffenceDetails().buildOffences(courtApplication.getCourtApplicationCases().get(0), courtApplication);
        assertEquals(1, offenceDetails.size());
        assertEquals(courtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getOffenceCode(), offenceDetails.get(0).getOffenceCode());

    }

    private Defendant getDefendant(final List<Offence> offenceDetails) {
        return Defendant.defendant().withOffences(offenceDetails).withDefendantCaseJudicialResults(buildDefendantJudicialResultList()).build();
    }

    private void assertOffenceDetails(final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList, final uk.gov.justice.core.courts.OffenceDetails offenceDetailsFromRequest, final int orderIndex) {
        assertThat(offenceDetailsFromRequest.getId(), is(ID));
        assertThat(offenceDetailsFromRequest.getOffenceCode(), is(OFFENCE_CODE));
        assertThat(offenceDetailsList.size(), is(1));
        assertThat(offenceDetailsFromRequest.getWording(), is(OFFENCE_WORDING));
        assertThat(offenceDetailsFromRequest.getModeOfTrial(), is(MODE_OF_TRIAL));
        assertThat(offenceDetailsFromRequest.getFinalDisposal(), is("N"));
        assertThat(offenceDetailsFromRequest.getOffenceSequenceNumber(), is(orderIndex));
        assertThat(offenceDetailsFromRequest.getIndicatedPlea().getIndicatedPleaValue(), is(IndicatedPleaValue.INDICATED_GUILTY));
        assertResult(offenceDetailsFromRequest.getJudicialResults(), offenceDetailsList.get(0).getJudicialResults());
    }

    private void assertResult(final List<JudicialResult> results, final List<JudicialResult> judicialResults) {

        assertThat(results.size(), is(judicialResults.size()));
        judicialResults.forEach(judicialResult -> {
            final Optional<JudicialResult> baseResultOptional = results.stream().filter(r -> r.getJudicialResultId().equals(judicialResult.getJudicialResultId())).findFirst();
            assertThat(baseResultOptional.isPresent(), is(true));
            assertThat(judicialResult.getCjsCode(), is("cjsCode"));
            assertThat(judicialResult.getLabel(), is("label"));
            assertThat(judicialResult.getCategory(), is(JudicialResultCategory.INTERMEDIARY));
            assertThat(judicialResult.getIsAvailableForCourtExtract(), is(false));
            assertThat(judicialResult.getIsAdjournmentResult(), is(false));
            assertThat(judicialResult.getIsConvictedResult(), is(false));
            assertThat(judicialResult.getIsFinancialResult(), is(false));
            assertThat(judicialResult.getOrderedHearingId(), is(notNullValue()));
            assertThat(judicialResult.getRank(), is(BigDecimal.valueOf(0)));
            assertThat(judicialResult.getUsergroups(), nullValue());
            assertThat(judicialResult.getWelshLabel(), is("welshLabel"));
            assertPrompt(judicialResult.getJudicialResultPrompts());
        });
    }

    private void assertPrompt(final List<JudicialResultPrompt> judicialResultPrompts) {
        final Optional<JudicialResultPrompt> judicialResultPromptOptional = judicialResultPrompts.stream().findFirst();
        assertThat(judicialResultPromptOptional.isPresent(), is(true));
        final JudicialResultPrompt judicialResultPrompt = judicialResultPromptOptional.get();
        assertThat(judicialResultPrompt.getIsFinancialImposition(), is(true));
        assertThat(judicialResultPrompt.getTotalPenaltyPoints(), is(new BigDecimal(10)));
        assertThat(judicialResultPrompt.getLabel(), is("label"));
        assertThat(judicialResultPrompt.getValue(), is("value"));


    }

    private List<Offence> getOffences() {
        return Arrays.asList(Offence.offence()
                .withId(ID)
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceTitle(STRING.next())
                .withWording(OFFENCE_WORDING)
                .withStartDate(now())
                .withEndDate(now())
                .withArrestDate(now())
                .withChargeDate(now())
                .withConvictionDate(now())
                .withEndDate(now())
                .withModeOfTrial(MODE_OF_TRIAL)
                .withOrderIndex(12)
                .withOrderIndex(65)
                .withCount(434)
                .withIsDisposed(false)
                .withJudicialResults(buildJudicialResultList())
                .withIndicatedPlea(IndicatedPlea.indicatedPlea().withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY).build())
                .build());
    }

}
