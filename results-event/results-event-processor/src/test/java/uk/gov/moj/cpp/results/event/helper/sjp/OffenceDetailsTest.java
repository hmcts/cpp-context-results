package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.lang.String.valueOf;
import static java.time.LocalDate.parse;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.DATE_AND_TIME_OF_SESSION;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.SESSION_ID;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildAllocationDecision;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinition;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicSJPCaseResulted;

import uk.gov.justice.sjp.results.CaseOffence;
import uk.gov.justice.sjp.results.Plea;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceDetailsTest {

    @Mock
    private ReferenceCache referenceCache;

    @Test
    public void testBuildOffences() {

        when(referenceCache.getResultDefinitionById(any(), any(), any())).thenReturn(buildResultDefinition());
        when(referenceCache.getAllocationDecision(any(), anyString())).thenReturn(of(buildAllocationDecision()));


        final PublicSjpResulted publicSjpCaseResulted = basicSJPCaseResulted();
        final List<uk.gov.justice.sjp.results.CaseDetails> sjpCaseDetails = publicSjpCaseResulted.getCases();
        final List<uk.gov.justice.sjp.results.CaseDefendant> sjpCaseDefendants = sjpCaseDetails.get(0).getDefendants();
        final List<uk.gov.justice.core.courts.OffenceDetails> offenceDetailsList = new OffenceDetails(referenceCache).buildOffences(sjpCaseDefendants.get(0), DATE_AND_TIME_OF_SESSION, SESSION_ID);

        assertOffences(offenceDetailsList, sjpCaseDefendants.get(0).getOffences());

    }

    private void assertOffences(final List<uk.gov.justice.core.courts.OffenceDetails> offences, final List<CaseOffence> sjpOffences) {
        for (final uk.gov.justice.core.courts.OffenceDetails offenceDetails : offences) {
            final Optional<CaseOffence> sjpCaseOffenceOptional = sjpOffences.stream().filter(o -> o.getBaseOffenceDetails().getOffenceCode().equals(offenceDetails.getOffenceCode())).findFirst();
            assertThat(sjpCaseOffenceOptional.isPresent(), is(true));
            final CaseOffence sjpCaseOffence = sjpCaseOffenceOptional.get();
            if (null != offenceDetails) {
                assertThat(offenceDetails.getArrestDate(), is(sjpCaseOffence.getBaseOffenceDetails().getArrestDate()));
                assertThat(offenceDetails.getChargeDate(), is(sjpCaseOffence.getBaseOffenceDetails().getChargeDate()));
                assertThat(offenceDetails.getConvictionDate(), is(sjpCaseOffence.getConvictionDate().toLocalDate()));
                assertThat(offenceDetails.getEndDate(), is(sjpCaseOffence.getBaseOffenceDetails().getOffenceEndDate()));
                assertThat(offenceDetails.getFinalDisposal(), is("N"));
                assertThat(offenceDetails.getModeOfTrial(), is(valueOf(sjpCaseOffence.getModeOfTrial())));
                assertThat(offenceDetails.getConvictingCourt(), is(sjpCaseOffence.getConvictingCourt()));
                assertThat(offenceDetails.getOffenceSequenceNumber(), is(sjpCaseOffence.getBaseOffenceDetails().getOffenceSequenceNumber()));
                assertAllocationDecision(offenceDetails, sjpCaseOffence);
                assertOffenceFacts(offenceDetails, sjpCaseOffence);
                assertPlea(offenceDetails, sjpCaseOffence);

            }
        }
    }

    private void assertAllocationDecision(final uk.gov.justice.core.courts.OffenceDetails offenceDetails, final CaseOffence sjpCaseOffence) {
        if (null != offenceDetails.getAllocationDecision()) {
            assertThat(offenceDetails.getAllocationDecision().getOriginatingHearingId(), is(fromString("e4003b92-419b-4e47-b3f9-89a4bbd6741d")));
            assertThat(offenceDetails.getAllocationDecision().getOffenceId(), is(sjpCaseOffence.getBaseOffenceDetails().getOffenceId()));
            assertThat(offenceDetails.getAllocationDecision().getAllocationDecisionDate(), is(parse("2019-05-02")));
            assertThat(offenceDetails.getAllocationDecision().getMotReasonDescription(), is("motDescription"));
            assertThat(offenceDetails.getAllocationDecision().getMotReasonCode(), is("10"));
            assertThat(offenceDetails.getAllocationDecision().getSequenceNumber(), is(10));
        }
    }

    private void assertOffenceFacts(final uk.gov.justice.core.courts.OffenceDetails offenceDetails, final CaseOffence sjpCaseOffence) {
        assertThat(offenceDetails.getOffenceFacts().getAlcoholReadingAmount(), is(sjpCaseOffence.getBaseOffenceDetails().getAlcoholLevelAmount()));
        assertThat(offenceDetails.getOffenceFacts().getAlcoholReadingMethodCode(), is(sjpCaseOffence.getBaseOffenceDetails().getAlcoholLevelMethod()));
        assertThat(offenceDetails.getOffenceFacts().getVehicleRegistration(), is(sjpCaseOffence.getBaseOffenceDetails().getVehicleRegistrationMark()));
        assertThat(offenceDetails.getOffenceFacts().getVehicleCode(), is(nullValue()));
    }

    private void assertPlea(final uk.gov.justice.core.courts.OffenceDetails offenceDetails, final CaseOffence sjpCaseOffence) {
        final Plea plea = sjpCaseOffence.getPlea();
        assertThat(offenceDetails.getPlea().getPleaValue(), is(plea.getPleaType().toString()));
        assertThat(offenceDetails.getPlea().getPleaDate().getYear(), is(plea.getPleaDate().getYear()));
        assertThat(offenceDetails.getPlea().getPleaDate().getMonth(), is(plea.getPleaDate().getMonth()));
        assertThat(offenceDetails.getPlea().getPleaDate().getDayOfYear(), is(plea.getPleaDate().getDayOfYear()));
        assertThat(offenceDetails.getPlea().getOffenceId(), is(sjpCaseOffence.getBaseOffenceDetails().getOffenceId()));
    }

}
