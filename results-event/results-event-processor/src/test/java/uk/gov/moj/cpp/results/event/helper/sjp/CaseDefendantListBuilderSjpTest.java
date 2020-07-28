package uk.gov.moj.cpp.results.event.helper.sjp;

import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.DATE_AND_TIME_OF_SESSION;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.SESSION_ID;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildAllocationDecision;
import static uk.gov.moj.cpp.results.event.helper.TestTemplate.buildResultDefinition;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicSJPCaseResulted;

import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;

import java.util.List;
import java.util.Optional;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDefendantListBuilderSjpTest {

    @Mock
    private ReferenceCache referenceCache;

    final Optional<BailStatus> bailStatusMock = of(mock(BailStatus.class));

    @Before
    public void setUp() {
        when(referenceCache.getResultDefinitionById(any(), any(), any())).thenReturn(buildResultDefinition());
        when(referenceCache.getBailStatusObjectByCode(any(), any())).thenReturn(bailStatusMock);
        when(referenceCache.getAllocationDecision(any(), anyString())).thenReturn(of(buildAllocationDecision()));
    }

    @Test
    public void testBuildCaseDefendants() {
        uk.gov.justice.sjp.results.CaseDetails sjpCaseDetail = getSjpCaseDetail();
        final List<uk.gov.justice.core.courts.CaseDefendant> caseDefendantList = new CaseDefendantListBuilderSjp(referenceCache).buildDefendantList(sjpCaseDetail, DATE_AND_TIME_OF_SESSION, SESSION_ID);
        verifyMocks();
        assertDefendants(caseDefendantList, sjpCaseDetail.getDefendants(), bailStatusMock.get());
    }

    @Test
    public void shouldNotFailWhenPersonDateOfBirthIsNull() {
        final uk.gov.justice.sjp.results.CaseDetails sjpCaseDetail = getSjpCaseDetail();
        sjpCaseDetail.getDefendants().forEach(defendant -> {
            defendant.getIndividualDefendant().getBasePersonDetails().setBirthDate(null);
            defendant.getParentGuardianDetails().setBirthDate(null);
        });
    }

    private uk.gov.justice.sjp.results.CaseDetails getSjpCaseDetail() {
        final PublicSjpResulted publicSjpCaseResulted = basicSJPCaseResulted();
        final List<uk.gov.justice.sjp.results.CaseDetails> sjpCaseDetails = publicSjpCaseResulted.getCases();
        return sjpCaseDetails.get(0);
    }

    private void verifyMocks() {
        verify(referenceCache, atLeastOnce()).getResultDefinitionById(any(), any(), any());
        verify(referenceCache, atLeastOnce()).getBailStatusObjectByCode(any(), any());
        verify(referenceCache, atLeastOnce()).getAllocationDecision(any(), anyString());
    }

    private void assertDefendants(final List<CaseDefendant> caseDefendants, final List<uk.gov.justice.sjp.results.CaseDefendant> sjpCaseDefendants, final BailStatus bailStatus) {
        for (final CaseDefendant caseDefendant : caseDefendants) {
            final Optional<uk.gov.justice.sjp.results.CaseDefendant> sjpdefendantOptional = sjpCaseDefendants.stream().filter(d -> d.getDefendantId().equals(caseDefendant.getDefendantId())).findFirst();
            assertThat(sjpdefendantOptional.isPresent(), Is.is(true));
            final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant = sjpdefendantOptional.get();

            assertThat(caseDefendant.getProsecutorReference(), is(sjpDefendant.getProsecutorReference()));
            if (null != caseDefendant.getCorporateDefendant()) {
                assertThat(caseDefendant.getCorporateDefendant().getName(), is(sjpDefendant.getCorporateDefendant().getOrganisationName()));
            }
            assertAssociatedPerson(caseDefendant, sjpDefendant);
            assertIndividualDefendant(caseDefendant, sjpDefendant, bailStatus);

        }
    }

    private void assertIndividualDefendant(final CaseDefendant caseDefendant, final uk.gov.justice.sjp.results.CaseDefendant sjpDefendant, final BailStatus bailStatus) {
        if (null != caseDefendant.getIndividualDefendant()) {
            assertThat(caseDefendant.getIndividualDefendant().getBailStatus(), is(bailStatus));
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
