package uk.gov.moj.cpp.results.event.helper;

import static com.google.common.collect.ImmutableList.of;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildAllocationDecision;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinition;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicSJPCaseResulted;

import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.sjp.results.BaseResult;
import uk.gov.justice.sjp.results.CaseOffence;
import uk.gov.justice.sjp.results.PublicSjpResulted;

import java.util.List;
import java.util.Optional;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDetailsConverterForSjpTest {

    @InjectMocks
    CaseDetailsConverterForSjp caseDetailsConverterForSjp;
    @Mock
    private ReferenceCache referenceCache;

    @Test
    public void testCaseDetailsConverter() {

        when(referenceCache.getResultDefinitionById(any(), any(), any())).thenReturn(buildResultDefinition());
        final BailStatus bailStatus = mock(BailStatus.class);
        when(referenceCache.getBailStatusObjectByCode(any(), any())).thenReturn(Optional.of(bailStatus));

        when(referenceCache.getAllocationDecision(any(), anyString())).thenReturn(Optional.of(buildAllocationDecision()));

        final PublicSjpResulted publicSjpCaseResulted = basicSJPCaseResulted();
        final List<uk.gov.justice.sjp.results.CaseDetails> sjpCaseDetails = publicSjpCaseResulted.getCases();

        for (final uk.gov.justice.sjp.results.CaseDetails sjpCaseDetail : sjpCaseDetails) {
            final List<uk.gov.justice.sjp.results.CaseDefendant> sjpCaseDefendants = sjpCaseDetail.getDefendants();


            final List<CaseDetails> caseDetailsList = caseDetailsConverterForSjp.convert(publicSjpCaseResulted);
            assertThat(caseDetailsList, hasSize(1));
            for (final CaseDetails caseDetails : caseDetailsList) {
                final List<CaseDefendant> caseDefendantList = caseDetails.getDefendants();
                assertThat(caseDefendantList, hasSize(2));
                assertThat(caseDetails.getUrn(), is(sjpCaseDetail.getUrn()));
                assertThat(caseDetails.getCaseId(), is(sjpCaseDetail.getCaseId()));
                assertThat(caseDetails.getProsecutionAuthorityCode(), is(sjpCaseDetail.getProsecutionAuthorityCode()));
                assertDefendants(caseDefendantList, sjpCaseDefendants);
            }
        }
    }

    private void assertDefendants(final List<CaseDefendant> caseDefendants, final List<uk.gov.justice.sjp.results.CaseDefendant> sjpCaseDefendants) {
        for (final CaseDefendant caseDefendant : caseDefendants) {
            final Optional<uk.gov.justice.sjp.results.CaseDefendant> sjpdefendantOptional = sjpCaseDefendants.stream().filter(d -> d.getDefendantId().equals(caseDefendant.getDefendantId())).findFirst();
            assertThat(sjpdefendantOptional.isPresent(), Is.is(true));
            final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant = sjpdefendantOptional.get();

            assertThat(caseDefendant.getProsecutorReference(), is(sjpDefendant.getProsecutorReference()));
            if (null != caseDefendant.getCorporateDefendant()) {
                assertThat(caseDefendant.getCorporateDefendant().getName(), is(sjpDefendant.getCorporateDefendant().getOrganisationName()));
            }
            assertAssociatedPerson(caseDefendant, sjpDefendant);
            assertIndividualDefendant(caseDefendant, sjpDefendant);
            assertOffences(caseDefendant.getOffences(), sjpDefendant.getOffences());
        }
    }

    private void assertOffences(final List<OffenceDetails> offences, final List<CaseOffence> sjpOffences) {
        for (final OffenceDetails offenceDetails : offences) {
            final Optional<CaseOffence> sjpCaseOffenceOptional = sjpOffences.stream().filter(o -> o.getBaseOffenceDetails().getOffenceCode().equals(offenceDetails.getOffenceCode())).findFirst();
            assertThat(sjpCaseOffenceOptional.isPresent(), Is.is(true));
            final CaseOffence sjpCaseOffence = sjpCaseOffenceOptional.get();
            assertThat(offenceDetails.getArrestDate(), is(sjpCaseOffence.getBaseOffenceDetails().getArrestDate()));
            assertThat(offenceDetails.getChargeDate(), is(sjpCaseOffence.getBaseOffenceDetails().getChargeDate()));
            assertThat(offenceDetails.getConvictionDate(), is(sjpCaseOffence.getConvictionDate().toLocalDate()));
            assertThat(offenceDetails.getEndDate(), is(sjpCaseOffence.getBaseOffenceDetails().getOffenceEndDate()));
            assertThat(offenceDetails.getFinalDisposal(), is("N"));
            assertThat(offenceDetails.getModeOfTrial(), is(String.valueOf(sjpCaseOffence.getModeOfTrial())));
            assertThat(offenceDetails.getConvictingCourt(), is(sjpCaseOffence.getConvictingCourt()));
            assertOffenceFacts(offenceDetails, sjpCaseOffence);
            assertResult(sjpCaseOffence.getResults(), offenceDetails.getJudicialResults());
        }
    }

    private void assertResult(final List<BaseResult> results, final List<JudicialResult> judicialResults) {
        assertThat(results.size(), is(judicialResults.size()));
        judicialResults.forEach(judicialResult -> {
            final Optional<BaseResult> baseResultOptional = results.stream().filter(r -> r.getId().equals(judicialResult.getJudicialResultId())).findFirst();
            assertThat(baseResultOptional.isPresent(), is(true));
            assertThat(judicialResult.getCjsCode(), is("cjsCode"));
            assertThat(judicialResult.getLabel(), is("label"));
            assertThat(judicialResult.getCategory(), is(Category.ANCILLARY));
            assertThat(judicialResult.getIsAvailableForCourtExtract(), is(true));
            assertThat(judicialResult.getIsAdjournmentResult(), is(true));
            assertThat(judicialResult.getIsConvictedResult(), is(true));
            assertThat(judicialResult.getIsFinancialResult(), is(true));
            assertThat(judicialResult.getOrderedHearingId(), is(notNullValue()));
            assertThat(judicialResult.getRank(), is(valueOf(1)));
            assertThat(judicialResult.getUsergroups(), is(of("1", "2")));
            assertThat(judicialResult.getWelshLabel(), is("welshLabel"));
        });

    }

    private void assertOffenceFacts(final OffenceDetails offenceDetails, final CaseOffence sjpCaseOffence) {
        assertThat(offenceDetails.getOffenceFacts().getAlcoholReadingAmount(), is(sjpCaseOffence.getBaseOffenceDetails().getAlcoholLevelAmount()));
        assertThat(offenceDetails.getOffenceFacts().getAlcoholReadingMethodCode(), is(sjpCaseOffence.getBaseOffenceDetails().getAlcoholLevelMethod()));
        assertThat(offenceDetails.getOffenceFacts().getVehicleRegistration(), is(sjpCaseOffence.getBaseOffenceDetails().getVehicleRegistrationMark()));
        assertThat(offenceDetails.getOffenceFacts().getVehicleCode(), is(nullValue()));
    }

    private void assertIndividualDefendant(final CaseDefendant caseDefendant, final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant) {
        if (null != caseDefendant.getIndividualDefendant()) {
            assertThat(caseDefendant.getIndividualDefendant().getReasonForBailConditionsOrCustody(), is(nullValue()));
            assertThat(caseDefendant.getIndividualDefendant().getBailConditions(), is(nullValue()));
            assertThat(caseDefendant.getIndividualDefendant().getPresentAtHearing(), is("N"));
            assertPerson(caseDefendant, sjpDefendant);
        }
    }

    private void assertPerson(final CaseDefendant caseDefendant, final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant) {
        if (null != caseDefendant.getIndividualDefendant().getPerson()) {
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getTitle(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getPersonTitle()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getGender().toString(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getGender().toString()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getFirstName(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getFirstName()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getLastName(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getLastName()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getDateOfBirth(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getBirthDate().toLocalDate()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getAddress(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getContact().getHome(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getTelephoneNumberHome()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getContact().getMobile(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getTelephoneNumberMobile()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getContact().getWork(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getTelephoneNumberBusiness()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getContact().getPrimaryEmail(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getEmailAddress1()));
            assertThat(caseDefendant.getIndividualDefendant().getPerson().getContact().getSecondaryEmail(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getEmailAddress2()));
        }
    }

    private void assertAssociatedPerson(final CaseDefendant caseDefendant, final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant) {
        final List<AssociatedIndividual> associatedIndividualList = caseDefendant.getAssociatedPerson();
        final Optional<AssociatedIndividual> associatedIndividualOptional = associatedIndividualList.stream().filter(a -> a.getPerson().getLastName().equals(sjpDefendant.getParentGuardianDetails().getLastName())).findFirst();
        assertThat(associatedIndividualOptional.isPresent(), Is.is(true));
        final AssociatedIndividual associatedIndividualFromRequest = associatedIndividualOptional.get();

        if (null != associatedIndividualFromRequest.getPerson()) {
            assertAddress(sjpDefendant, associatedIndividualFromRequest);
            assertThat(associatedIndividualFromRequest.getPerson().getDateOfBirth(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getBirthDate().toLocalDate()));
            assertThat(associatedIndividualFromRequest.getPerson().getLastName(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getLastName()));
            assertThat(associatedIndividualFromRequest.getPerson().getFirstName(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getFirstName()));
            assertThat(associatedIndividualFromRequest.getPerson().getTitle(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getPersonTitle()));
        }

    }

    private void assertAddress(final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant, final AssociatedIndividual associatedIndividualFromRequest) {
        assertThat(associatedIndividualFromRequest.getPerson().getAddress().getAddress1(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress().getAddress1()));
        assertThat(associatedIndividualFromRequest.getPerson().getAddress().getAddress2(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress().getAddress2()));
        assertThat(associatedIndividualFromRequest.getPerson().getAddress().getAddress3(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress().getAddress3()));
        assertThat(associatedIndividualFromRequest.getPerson().getAddress().getAddress4(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress().getAddress4()));
        assertThat(associatedIndividualFromRequest.getPerson().getAddress().getAddress5(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress().getAddress5()));
        assertThat(associatedIndividualFromRequest.getPerson().getAddress().getPostcode(), is(sjpDefendant.getIndividualDefendant().getBasePersonDetails().getAddress().getPostcode()));
    }
}