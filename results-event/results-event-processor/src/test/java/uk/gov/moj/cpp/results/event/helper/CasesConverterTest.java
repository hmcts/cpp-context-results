package uk.gov.moj.cpp.results.event.helper;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CasesConverterTest {

    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID2 = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID4 = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");

    private static final String COUNTRY_ISO_CODE = "UK";

    @Mock
    private ReferenceCache referenceCache;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CasesConverter casesConverter;

    public static Optional<JsonObject> getCountryNationality() {
        return Optional.of(createObjectBuilder()
                .add("isoCode", COUNTRY_ISO_CODE)
                .add("id", NATIONALITY_ID.toString())
                .build());
    }

    @Test
    public void testConverter() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithMagistratesTemplate();
        final Hearing hearing = shareResultsMessage.getHearing();
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        when(referenceDataService.getSpiOutFlagForProsecutorOucode(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(2));
        assertThat(caseDetailsList, hasSize(prosecutionCases.size()));
        for (final CaseDetails caseDetails : caseDetailsList) {
            final Optional<ProsecutionCase> prosecutionCaseOptional = prosecutionCases.stream().filter(p -> p.getId().equals(caseDetails.getCaseId())).findFirst();
            assertThat(prosecutionCaseOptional.isPresent(), is(true));
            final ProsecutionCase prosecutionCase = prosecutionCaseOptional.get();
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            if (isNotEmpty(prosecutionCaseIdentifier.getCaseURN())) {
                assertThat(caseDetails.getUrn(), is(prosecutionCaseIdentifier.getCaseURN()));
            } else if (isNotEmpty(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                assertThat(caseDetails.getUrn(), is("authorityReference"));
            } else {
                assertThat(caseDetails.getUrn(), is("00PP0000008"));
            }
            final List<Defendant> defendantsFromRequest = prosecutionCase.getDefendants();
            final List<CaseDefendant> caseDetailsDefendants = caseDetails.getDefendants();
            assertThat(caseDetailsDefendants, hasSize(2));
            assertThat(caseDetailsDefendants, hasSize(defendantsFromRequest.size()));
            assertDefendants(defendantsFromRequest, caseDetailsDefendants, hearing);
        }
    }

    @Test
    public void testConverter_MissingHearing() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithMagistratesTemplate();
        shareResultsMessage.setHearing(null);
        when(referenceDataService.getSpiOutFlagForProsecutorOucode(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(0));
    }

    @Test
    public void testConverter_MissingProsecutionCases() {
        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithMagistratesTemplate();
        final Hearing hearing = shareResultsMessage.getHearing();
        hearing.setProsecutionCases(null);
        when(referenceDataService.getSpiOutFlagForProsecutorOucode(any())).thenReturn(true);
        final List<CaseDetails> caseDetailsList = casesConverter.convert(shareResultsMessage);
        assertThat(caseDetailsList, hasSize(0));
    }

    private void assertDefendants(final List<Defendant> defendantsFromRequest, final List<CaseDefendant> caseDetailsDefendants, final Hearing hearing) {
        for (final CaseDefendant caseDetailsDefendant : caseDetailsDefendants) {
            final Optional<Defendant> defendantOptional = defendantsFromRequest.stream().filter(d -> d.getId().equals(caseDetailsDefendant.getDefendantId())).findFirst();
            assertThat(defendantOptional.isPresent(), is(true));
            final Defendant defendantFromRequest = defendantOptional.get();
            if (isNotEmpty(defendantFromRequest.getProsecutionAuthorityReference())) {
                assertThat(caseDetailsDefendant.getProsecutorReference(), is(defendantFromRequest.getProsecutionAuthorityReference()));
            } else {
                assertThat(caseDetailsDefendant.getProsecutorReference(), is("0800PP0100000000001H"));
            }
            assertThat(caseDetailsDefendant.getPncId(), is(defendantFromRequest.getPncId()));
            assertThat(caseDetailsDefendant.getCorporateDefendant(), is(defendantFromRequest.getDefenceOrganisation()));
            if (null != defendantFromRequest.getAssociatedPersons()) {
                defendantFromRequest.getAssociatedPersons().forEach(a -> {
                    final Optional<AssociatedIndividual> associatedIndividualOptional = caseDetailsDefendant.getAssociatedPerson().stream().filter(a1 -> a1.getPerson().getLastName().equalsIgnoreCase(a.getPerson().getLastName())).findFirst();
                    assertThat(associatedIndividualOptional.isPresent(), is(true));
                    final AssociatedIndividual associatedIndividual = associatedIndividualOptional.get();
                    assertThat(associatedIndividual.getRole(), is("parentGuardian"));
                    assertPerson(associatedIndividual.getPerson(), a.getPerson());
                });
            }
            if (null != hearing.getDefendantAttendance()) {
                assertAttendanceDays(caseDetailsDefendant.getAttendanceDays(), hearing.getDefendantAttendance(), caseDetailsDefendant.getDefendantId());
            }
            assertPresentAtHearing(caseDetailsDefendant);
            assertDefendantPerson(caseDetailsDefendant.getIndividualDefendant(), defendantFromRequest.getPersonDefendant());
            assertOffences(caseDetailsDefendant.getOffences(), defendantFromRequest.getOffences());
        }

    }

    private void assertPresentAtHearing(final CaseDefendant caseDetailsDefendant) {
        if (DEFAULT_DEFENDANT_ID1.equals(caseDetailsDefendant.getDefendantId()) || DEFAULT_DEFENDANT_ID4.equals(caseDetailsDefendant.getDefendantId())) {
            assertThat(caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing(), is("Y"));
        }
        if (DEFAULT_DEFENDANT_ID2.equals(caseDetailsDefendant.getDefendantId())) {
            assertThat(caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing(), is("N"));
        }
        if (DEFAULT_DEFENDANT_ID3.equals(caseDetailsDefendant.getDefendantId())) {
            assertThat(caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing(), is("A"));
        }
    }

    private void assertOffences(final List<OffenceDetails> offences, final List<Offence> defendantFromRequestOffences) {

        for (final OffenceDetails offence : offences) {
            final Optional<Offence> offenceOptional = defendantFromRequestOffences.stream().filter(o -> o.getId().equals(offence.getId())).findFirst();
            assertThat(offenceOptional.isPresent(), is(true));
            final Offence offenceFromRequest = offenceOptional.get();
            assertThat(offence.getArrestDate(), is(offenceFromRequest.getArrestDate()));
            assertThat(offence.getChargeDate(), is(offenceFromRequest.getChargeDate()));
            assertThat(offence.getConvictingCourt(), is(nullValue()));
            assertThat(offence.getConvictionDate(), is(offenceFromRequest.getConvictionDate()));
            assertThat(offence.getEndDate(), is(offenceFromRequest.getEndDate()));
            assertThat(offence.getFinalDisposal(), is("Y"));
            assertThat(offence.getModeOfTrial(), is(offenceFromRequest.getModeOfTrial()));
            assertThat(offence.getOffenceCode(), is(offenceFromRequest.getOffenceCode()));
            assertThat(offence.getOffenceFacts(), is(offenceFromRequest.getOffenceFacts()));
            assertThat(offence.getOffenceSequenceNumber(), is(offenceFromRequest.getOrderIndex()));
            assertThat(offence.getStartDate(), is(offenceFromRequest.getStartDate()));
            assertThat(offence.getWording(), is(offenceFromRequest.getWording()));
        }
    }

    private void assertDefendantPerson(final IndividualDefendant individualDefendant, final PersonDefendant defendantFromRequest) {
        assertThat(individualDefendant.getReasonForBailConditionsOrCustody(), is(defendantFromRequest.getBailReasons()));
        assertThat(individualDefendant.getBailStatus(), is(defendantFromRequest.getBailStatus()));
        assertThat(individualDefendant.getBailConditions(), is(defendantFromRequest.getBailConditions()));
        assertPerson(individualDefendant.getPerson(), defendantFromRequest.getPersonDetails());
    }

    private void assertAttendanceDays(final List<AttendanceDay> attendanceDays, final List<DefendantAttendance> defendantAttendance, final UUID defendantId) {
        final Optional<List<AttendanceDay>> attendanceDaysFromRequest = defendantAttendance.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().map(a -> a.getAttendanceDays());
        assertThat(attendanceDaysFromRequest.isPresent(), is(true));
        final List<AttendanceDay> attendanceDaysListFromRequest = attendanceDaysFromRequest.get();
        assertThat(attendanceDaysListFromRequest, hasSize(1));
        assertThat(attendanceDays, hasSize(attendanceDaysListFromRequest.size()));
        final AttendanceDay attendanceDay = attendanceDays.get(0);
        final AttendanceDay attendanceDayFromRequest = attendanceDaysListFromRequest.get(0);
        assertThat(attendanceDay.getAttendanceType(), is(attendanceDayFromRequest.getAttendanceType()));
        assertThat(attendanceDay.getDay(), is(attendanceDayFromRequest.getDay()));

    }

    private void assertPerson(final Individual associatedPerson, final Person person) {
        assertThat(associatedPerson.getFirstName(), is(person.getFirstName()));
        assertThat(associatedPerson.getAddress(), is(person.getAddress()));
        assertThat(associatedPerson.getLastName(), is(person.getLastName()));
        assertThat(associatedPerson.getContact(), is(person.getContact()));
        assertThat(associatedPerson.getDateOfBirth(), is(person.getDateOfBirth()));
        assertThat(associatedPerson.getGender(), is(person.getGender()));
        assertThat(associatedPerson.getMiddleName(), is(person.getMiddleName()));
        assertThat(associatedPerson.getNationality(), is("UK"));
        assertThat(associatedPerson.getTitle(), is(person.getTitle()));
    }

}