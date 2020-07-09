package uk.gov.moj.cpp.results.event.helper.results;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;

import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.AttendanceDay;
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
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDefendantListBuilderTest {

    private static final UUID NATIONALITY_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final String COUNTRY_ISO_CODE = "UK";
    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID2 = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID3 = fromString("dddd3333-1e20-4c21-916a-81a6c90239e5");
    private static final UUID DEFAULT_DEFENDANT_ID4 = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");

    @Mock
    private ReferenceCache referenceCache;

    private static Optional<JsonObject> getCountryNationality() {
        return Optional.of(createObjectBuilder()
                .add("isoCode", COUNTRY_ISO_CODE)
                .add("id", NATIONALITY_ID.toString())
                .build());
    }

    @Test
    public void shouldBuildDefendants() {

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());


        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsWithMagistratesTemplate();
        final Hearing hearing = shareResultsMessage.getHearing();

        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final ProsecutionCase prosecutionCase = prosecutionCases.get(0);

        final List<Defendant> defendantsFromRequest = prosecutionCase.getDefendants();
        final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants = new CaseDefendantListBuilder(referenceCache).buildDefendantList(defendantsFromRequest, hearing);
        assertThat(caseDetailsDefendants, hasSize(2));
        assertThat(caseDetailsDefendants, hasSize(defendantsFromRequest.size()));
        assertDefendants(defendantsFromRequest, caseDetailsDefendants, hearing);

    }

    @Test
    public void shouldUpdateDefendantsNationalityAndASN() {
        final Hearing hearing = TestTemplates.basicShareResultsWithMagistratesTemplate().getHearing();
        final List<Defendant> defendantsFromRequest = of(defendant().withOffences(of(offence().withModeOfTrial("1010").build())).withPersonDefendant(personDefendant().withPersonDetails(person().withNationalityCode("GBR").build()).withArrestSummonsNumber("1232324").build()).build());
        final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants = new CaseDefendantListBuilder(referenceCache).buildDefendantList(defendantsFromRequest, hearing);
        assertThat(caseDetailsDefendants, hasSize(1));
        final uk.gov.justice.core.courts.CaseDefendant caseDefendant = caseDetailsDefendants.get(0);
        assertThat(caseDefendant.getIndividualDefendant().getPerson().getNationality(), is("GBR"));
        assertThat(caseDefendant.getProsecutorReference(), is("1232324"));
    }

    private void assertDefendants(final List<Defendant> defendantsFromRequest, final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants, final Hearing hearing) {
        for (final uk.gov.justice.core.courts.CaseDefendant caseDetailsDefendant : caseDetailsDefendants) {
            final Optional<Defendant> defendantOptional = defendantsFromRequest.stream().filter(d -> d.getId().equals(caseDetailsDefendant.getDefendantId())).findFirst();
            assertThat(defendantOptional.isPresent(), is(true));
            final Defendant defendantFromRequest = defendantOptional.get();
            if (isNotEmpty(defendantFromRequest.getProsecutionAuthorityReference())) {
                assertThat(caseDetailsDefendant.getProsecutorReference(), is(defendantFromRequest.getProsecutionAuthorityReference()));
            } else {
                assertThat(caseDetailsDefendant.getProsecutorReference(), is("0800PP0100000000001H"));
            }
            assertThat(caseDetailsDefendant.getPncId(), is(defendantFromRequest.getPncId()));
            assertThat(caseDetailsDefendant.getJudicialResults(), is(defendantFromRequest.getDefendantCaseJudicialResults()));
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

    private void assertPresentAtHearing(final uk.gov.justice.core.courts.CaseDefendant caseDetailsDefendant) {
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

    private void assertOffences(final List<uk.gov.justice.core.courts.OffenceDetails> offences, final List<Offence> defendantFromRequestOffences) {

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
            assertThat(offence.getModeOfTrial(), is("1010"));
            assertThat(offence.getOffenceCode(), is(offenceFromRequest.getOffenceCode()));
            assertThat(offence.getOffenceFacts(), is(offenceFromRequest.getOffenceFacts()));
            assertThat(offence.getOffenceSequenceNumber(), is(offenceFromRequest.getCount()));
            assertThat(offence.getStartDate(), is(offenceFromRequest.getStartDate()));
            assertThat(offence.getWording(), is(offenceFromRequest.getWording()));
            assertAllocationDecision(offence,offenceFromRequest);

        }
    }

    private void assertAllocationDecision(final OffenceDetails offenceDetail, final Offence offenceFromRequest) {
        if(nonNull(offenceDetail.getAllocationDecision())) {
            assertThat(offenceDetail.getAllocationDecision().getMotReasonDescription(), is(offenceFromRequest.getAllocationDecision().getMotReasonDescription()));
            assertThat(offenceDetail.getAllocationDecision().getAllocationDecisionDate(), is(offenceFromRequest.getAllocationDecision().getAllocationDecisionDate()));
            assertThat(offenceDetail.getAllocationDecision().getMotReasonCode(), is(offenceFromRequest.getAllocationDecision().getMotReasonCode()));
            assertThat(offenceDetail.getAllocationDecision().getMotReasonId(), is(offenceFromRequest.getAllocationDecision().getMotReasonId()));
            assertThat(offenceDetail.getAllocationDecision().getOffenceId(), is(offenceFromRequest.getAllocationDecision().getOffenceId()));
            assertThat(offenceDetail.getAllocationDecision().getOriginatingHearingId(), is(offenceFromRequest.getAllocationDecision().getOriginatingHearingId()));
            if(nonNull(offenceDetail.getAllocationDecision().getCourtIndicatedSentence())) {
                assertThat(offenceDetail.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceTypeId(),
                        is(offenceFromRequest.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceTypeId()));
                assertThat(offenceDetail.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceDescription(),
                        is(offenceFromRequest.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceDescription()));
            }
        }
    }

    private void assertDefendantPerson(final IndividualDefendant individualDefendant, final PersonDefendant defendantFromRequest) {
        assertThat(individualDefendant.getBailStatus(), is(defendantFromRequest.getBailStatus()));
        assertThat(individualDefendant.getBailConditions(), is(defendantFromRequest.getBailConditions()));
        assertThat(individualDefendant.getReasonForBailConditionsOrCustody(), is(defendantFromRequest.getBailReasons()));
        assertPerson(individualDefendant.getPerson(), defendantFromRequest.getPersonDetails());
    }

    private void assertAttendanceDays(final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDays, final List<DefendantAttendance> defendantAttendance, final UUID defendantId) {
        final Optional<List<uk.gov.justice.core.courts.AttendanceDay>> attendanceDaysFromRequest = defendantAttendance.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().map(a -> a.getAttendanceDays());
        assertThat(attendanceDaysFromRequest.isPresent(), is(true));
        final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDaysListFromRequest = attendanceDaysFromRequest.get();
        assertThat(attendanceDaysListFromRequest, hasSize(1));
        assertThat(attendanceDays, hasSize(attendanceDaysListFromRequest.size()));
        final uk.gov.justice.core.courts.AttendanceDay attendanceDay = attendanceDays.get(0);
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
