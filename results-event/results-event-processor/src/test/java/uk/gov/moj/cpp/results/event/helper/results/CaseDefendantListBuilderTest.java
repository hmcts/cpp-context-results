package uk.gov.moj.cpp.results.event.helper.results;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.results.test.TestTemplates.basicShareResultsTemplate;
import static uk.gov.moj.cpp.results.test.TestTemplates.buildJudicialResultList;

import org.mockito.Mock;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.test.TestTemplates;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.commons.collections.CollectionUtils;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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


        final PublicHearingResulted shareResultsMessage = TestTemplates.basicShareResultsV2Template(JurisdictionType.MAGISTRATES);
        final Hearing hearing = shareResultsMessage.getHearing();

        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final ProsecutionCase prosecutionCase = prosecutionCases.get(0);

        final List<Defendant> defendantsFromRequest = prosecutionCase.getDefendants();
        final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants = new CaseDefendantListBuilder(referenceCache).buildDefendantList(defendantsFromRequest, hearing, true);
        assertEquals(2, caseDetailsDefendants.size());
        assertEquals(caseDetailsDefendants.size(), defendantsFromRequest.size());
        assertDefendants(defendantsFromRequest, caseDetailsDefendants, hearing);
    }

    @Test
    public void shouldBuildDefendantsForApplication() {

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = TestTemplates.basicShareHearingTemplateWithCustomApplication(randomUUID(), JurisdictionType.MAGISTRATES,
                Collections.singletonList(CourtApplication.courtApplication()
                        .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicationReference("OFFENCE_CODE_REFERENCE")
                        .withType(TestTemplates.courtApplicationTypeTemplates())
                        .withApplicant(TestTemplates.courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .withSubject(TestTemplates.courtApplicationPartyTemplates())
                        .withCourtApplicationCases(Collections.singletonList(TestTemplates.createCourtApplicationCaseWithOffences()))
                        .withApplicationParticulars("bail application")
                        .withJudicialResults(buildJudicialResultList())
                        .withAllegationOrComplaintStartDate(now())
                        .withPlea(Plea.plea().withOffenceId(randomUUID()).withPleaDate(now()).withPleaValue("NOT_GUILTY").build())
                        .withVerdict(Verdict.verdict()
                                .withVerdictType(VerdictType.verdictType()
                                        .withId(fromString("3f0d69d0-2fda-3472-8d4c-a6248f661825"))
                                        .withCategory(STRING.next())
                                        .withCategoryType(STRING.next())
                                        .withCjsVerdictCode("N")
                                        .build())
                                .withOriginatingHearingId(randomUUID())
                                .withOffenceId(randomUUID())
                                .withVerdictDate(now())
                                .build())
                        .build()));

        final CourtApplication courtApplication = hearing.getCourtApplications().get(0);
        final CourtApplicationCase courtApplicationCase = courtApplication.getCourtApplicationCases().get(0);

        final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants = new CaseDefendantListBuilder(referenceCache)
                .buildDefendantList(courtApplicationCase, courtApplication, hearing, false);
        assertEquals(1, caseDetailsDefendants.size());
        assertDefendants(courtApplication, hearing.getCourtApplications().get(0).getSubject().getMasterDefendant(), caseDetailsDefendants, hearing,true);
    }

    @Test
    public void shouldBuildDefendantsWithDefenceCounselsForApplication() {

        when(referenceCache.getNationalityById(any())).thenReturn(getCountryNationality());

        final Hearing hearing = TestTemplates.basicShareHearingTemplateWithCustomApplication(randomUUID(), JurisdictionType.MAGISTRATES,
                Collections.singletonList(CourtApplication.courtApplication()
                        .withId(fromString("f8254db1-1683-483e-afb3-b87fde5a0a26"))
                        .withApplicationReceivedDate(FUTURE_LOCAL_DATE.next())
                        .withApplicationReference("OFFENCE_CODE_REFERENCE")
                        .withType(TestTemplates.courtApplicationTypeTemplates())
                        .withApplicant(TestTemplates.courtApplicationPartyTemplates())
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .withSubject(TestTemplates.courtApplicationPartyTemplates())
                        .withCourtApplicationCases(Collections.singletonList(TestTemplates.createCourtApplicationCaseWithOffences()))
                        .withApplicationParticulars("bail application")
                        .withJudicialResults(buildJudicialResultList())
                        .withAllegationOrComplaintStartDate(now())
                        .withPlea(Plea.plea().withOffenceId(randomUUID()).withPleaDate(now()).withPleaValue("NOT_GUILTY").build())
                        .withVerdict(Verdict.verdict()
                                .withVerdictType(VerdictType.verdictType()
                                        .withId(fromString("3f0d69d0-2fda-3472-8d4c-a6248f661825"))
                                        .withCategory(STRING.next())
                                        .withCategoryType(STRING.next())
                                        .withCjsVerdictCode("N")
                                        .build())
                                .withOriginatingHearingId(randomUUID())
                                .withOffenceId(randomUUID())
                                .withVerdictDate(now())
                                .build())

                        .build()));
        final Hearing hearinWithNoAttendance = Hearing.hearing().withValuesFrom(hearing) .withDefendantAttendance(Collections.emptyList()).build();

        final CourtApplication courtApplication = hearinWithNoAttendance.getCourtApplications().get(0);

        final CourtApplicationCase courtApplicationCase = courtApplication.getCourtApplicationCases().get(0);
        final List<DefenceCounsel> defenceCounsels = Arrays.asList(DefenceCounsel.defenceCounsel().withDefendants(Arrays.asList(randomUUID())).withAttendanceDays(hearinWithNoAttendance.getDefendantAttendance().stream().flatMap(attendance-> attendance.getAttendanceDays().stream()).map(days->days.getDay()).collect(Collectors.toList())).build());
        final Hearing hearingWithDefenceCounsels = Hearing.hearing().withValuesFrom(hearing).withDefenceCounsels(defenceCounsels) .withDefendantAttendance(Collections.emptyList()).build();


        final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants = new CaseDefendantListBuilder(referenceCache)
                .buildDefendantList(courtApplicationCase, courtApplication, hearingWithDefenceCounsels, false);

        assertEquals(1, caseDetailsDefendants.size());
        assertDefendants(courtApplication, hearingWithDefenceCounsels.getCourtApplications().get(0).getSubject().getMasterDefendant(), caseDetailsDefendants, hearingWithDefenceCounsels,false);
    }

    @Test
    public void shouldUpdateDefendantsNationalityAndASN() {
        final Hearing hearing = basicShareResultsTemplate(JurisdictionType.MAGISTRATES).getHearing();
        final List<Defendant> defendantsFromRequest = of(defendant().withOffences(of(offence().withModeOfTrial("1010").build())).withPersonDefendant(personDefendant().withPersonDetails(person().withNationalityCode("GBR").build()).withArrestSummonsNumber("1232324").build()).build());
        final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants = new CaseDefendantListBuilder(referenceCache).buildDefendantList(defendantsFromRequest, hearing, false);
        assertEquals(1, caseDetailsDefendants.size());
        final uk.gov.justice.core.courts.CaseDefendant caseDefendant = caseDetailsDefendants.get(0);
        assertEquals("GBR", caseDefendant.getIndividualDefendant().getPerson().getNationality());
        assertEquals("1232324", caseDefendant.getProsecutorReference());
    }

    private void assertDefendants(final List<Defendant> defendantsFromRequest, final List<uk.gov.justice.core.courts.CaseDefendant> caseDetailsDefendants, final Hearing hearing) {
        for (final uk.gov.justice.core.courts.CaseDefendant caseDetailsDefendant : caseDetailsDefendants) {
            final Optional<Defendant> defendantOptional = defendantsFromRequest.stream().filter(d -> d.getId().equals(caseDetailsDefendant.getDefendantId())).findFirst();
            assertTrue(defendantOptional.isPresent());
            final Defendant defendantFromRequest = defendantOptional.get();
            if (isNotEmpty(defendantFromRequest.getProsecutionAuthorityReference())) {
                assertEquals(caseDetailsDefendant.getProsecutorReference(),defendantFromRequest.getProsecutionAuthorityReference());
            } else {
                assertEquals("0800PP0100000000001H", caseDetailsDefendant.getProsecutorReference());
            }
            assertEquals(caseDetailsDefendant.getPncId(), defendantFromRequest.getPncId());
            assertEquals(caseDetailsDefendant.getJudicialResults(), defendantFromRequest.getDefendantCaseJudicialResults());
            assertEquals(caseDetailsDefendant.getCorporateDefendant(), defendantFromRequest.getDefenceOrganisation());
            if (null != defendantFromRequest.getAssociatedPersons()) {
                defendantFromRequest.getAssociatedPersons().forEach(a -> {
                    final Optional<AssociatedIndividual> associatedIndividualOptional = caseDetailsDefendant.getAssociatedPerson().stream().filter(a1 -> a1.getPerson().getLastName().equalsIgnoreCase(a.getPerson().getLastName())).findFirst();
                    assertTrue(associatedIndividualOptional.isPresent());
                    final AssociatedIndividual associatedIndividual = associatedIndividualOptional.get();
                    assertEquals("parentGuardian", associatedIndividual.getRole());
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

    private void assertDefendants(final CourtApplication courtApplication, final MasterDefendant defendant, final List<CaseDefendant> caseDetailsDefendants, final Hearing hearing, final boolean withPresence) {
        for (final uk.gov.justice.core.courts.CaseDefendant caseDetailsDefendant : caseDetailsDefendants) {
            assertEquals("ARREST_1234", caseDetailsDefendant.getProsecutorReference());
            assertEquals(caseDetailsDefendant.getPncId(), defendant.getPncId());
            assertEquals(caseDetailsDefendant.getCorporateDefendant(), defendant.getLegalEntityDefendant());
            if (null != defendant.getAssociatedPersons()) {
                defendant.getAssociatedPersons().forEach(a -> {
                    final Optional<AssociatedIndividual> associatedIndividualOptional = caseDetailsDefendant.getAssociatedPerson().stream().filter(a1 -> a1.getPerson().getLastName().equalsIgnoreCase(a.getPerson().getLastName())).findFirst();
                    assertTrue(associatedIndividualOptional.isPresent());
                    assertTrue(associatedIndividualOptional.isPresent());
                    final AssociatedIndividual associatedIndividual = associatedIndividualOptional.get();
                    assertEquals("parentGuardian", associatedIndividual.getRole());
                    assertPerson(associatedIndividual.getPerson(), a.getPerson());
                });
            }
            if (CollectionUtils.isNotEmpty(hearing.getDefendantAttendance())) {
                assertAttendanceDays(caseDetailsDefendant.getAttendanceDays(), hearing.getDefendantAttendance(), caseDetailsDefendant.getDefendantId());
            }
            if(TRUE.equals(withPresence)) {
                assertPresentAtHearing(caseDetailsDefendant);
            }
            assertDefendantPerson(caseDetailsDefendant.getIndividualDefendant(), defendant.getPersonDefendant());
            assertOffences(caseDetailsDefendant.getOffences(), courtApplication);
        }

    }

    private void assertPresentAtHearing(final uk.gov.justice.core.courts.CaseDefendant caseDetailsDefendant) {
        if (DEFAULT_DEFENDANT_ID1.equals(caseDetailsDefendant.getDefendantId()) || DEFAULT_DEFENDANT_ID4.equals(caseDetailsDefendant.getDefendantId())) {
            assertEquals("Y", caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing());
        }
        if (DEFAULT_DEFENDANT_ID2.equals(caseDetailsDefendant.getDefendantId())) {
            assertEquals("N", caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing());
        }
        if (DEFAULT_DEFENDANT_ID3.equals(caseDetailsDefendant.getDefendantId())) {
            assertEquals("A", caseDetailsDefendant.getIndividualDefendant().getPresentAtHearing());
        }
    }

    private void assertOffences(final List<uk.gov.justice.core.courts.OffenceDetails> offences, final List<Offence> defendantFromRequestOffences) {

        for (final OffenceDetails offence : offences) {
            final Optional<Offence> offenceOptional = defendantFromRequestOffences.stream().filter(o -> o.getId().equals(offence.getId())).findFirst();
            assertTrue(offenceOptional.isPresent());
            final Offence offenceFromRequest = offenceOptional.get();
            assertEquals(offence.getArrestDate(), offenceFromRequest.getArrestDate());
            assertEquals(offence.getChargeDate(), offenceFromRequest.getChargeDate());
            assertNull(offence.getConvictingCourt());
            assertEquals(offence.getConvictionDate(), offenceFromRequest.getConvictionDate());
            assertEquals(offence.getEndDate(), offenceFromRequest.getEndDate());
            assertEquals("Y", offence.getFinalDisposal());
            assertEquals("1010", offence.getModeOfTrial());
            assertEquals(offence.getOffenceCode(), offenceFromRequest.getOffenceCode());
            assertEquals(offence.getOffenceFacts(), offenceFromRequest.getOffenceFacts());
            assertEquals(offence.getOffenceSequenceNumber(), offenceFromRequest.getOrderIndex());
            assertEquals(offence.getStartDate(), offenceFromRequest.getStartDate());
            assertEquals(offence.getWording(), offenceFromRequest.getWording());
            assertAllocationDecision(offence, offenceFromRequest);
        }
    }

    private void assertOffences(final List<uk.gov.justice.core.courts.OffenceDetails> offences, final CourtApplication courtApplication) {
        assertEquals(2, offences.size());
        final OffenceDetails offence = offences.get(0);
        assertNull(offence.getArrestDate());
        assertEquals(offence.getChargeDate(), courtApplication.getApplicationReceivedDate());
        assertNull(offence.getConvictingCourt());
        assertEquals(offence.getConvictionDate(), courtApplication.getConvictionDate());
        assertEquals(offence.getEndDate(), courtApplication.getAllegationOrComplaintEndDate());
        assertEquals("Y", offence.getFinalDisposal());
        assertEquals("", offence.getModeOfTrial());
        assertEquals(offence.getOffenceCode(), courtApplication.getType().getCode());
        assertEquals(offence.getWording(), courtApplication.getApplicationParticulars());
        assertEquals(Integer.valueOf(0), offence.getOffenceSequenceNumber());
        assertEquals(offence.getStartDate(), courtApplication.getAllegationOrComplaintStartDate());
    }

    private void assertAllocationDecision(final OffenceDetails offenceDetail, final Offence offenceFromRequest) {
        if (nonNull(offenceDetail.getAllocationDecision())) {
            assertEquals(offenceDetail.getAllocationDecision().getMotReasonDescription(), offenceFromRequest.getAllocationDecision().getMotReasonDescription());
            assertEquals(offenceDetail.getAllocationDecision().getAllocationDecisionDate(), offenceFromRequest.getAllocationDecision().getAllocationDecisionDate());
            assertEquals(offenceDetail.getAllocationDecision().getMotReasonCode(), offenceFromRequest.getAllocationDecision().getMotReasonCode());
            assertEquals(offenceDetail.getAllocationDecision().getMotReasonId(), offenceFromRequest.getAllocationDecision().getMotReasonId());
            assertEquals(offenceDetail.getAllocationDecision().getOffenceId(), offenceFromRequest.getAllocationDecision().getOffenceId());
            assertEquals(offenceDetail.getAllocationDecision().getOriginatingHearingId(), offenceFromRequest.getAllocationDecision().getOriginatingHearingId());
            if (nonNull(offenceDetail.getAllocationDecision().getCourtIndicatedSentence())) {
                assertEquals(offenceDetail.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceTypeId(),
                        offenceFromRequest.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceTypeId());
                assertEquals(offenceDetail.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceDescription(),
                        offenceFromRequest.getAllocationDecision().getCourtIndicatedSentence().getCourtIndicatedSentenceDescription());
            }
        }
    }

    private void assertDefendantPerson(final IndividualDefendant individualDefendant, final PersonDefendant defendantFromRequest) {
        assertEquals(individualDefendant.getBailStatus(), defendantFromRequest.getBailStatus());
        assertEquals(individualDefendant.getBailConditions(), defendantFromRequest.getBailConditions());
        assertEquals(individualDefendant.getReasonForBailConditionsOrCustody(), defendantFromRequest.getBailReasons());
        assertPerson(individualDefendant.getPerson(), defendantFromRequest.getPersonDetails());
    }

    private void assertAttendanceDays(final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDays, final List<DefendantAttendance> defendantAttendance, final UUID defendantId) {
        final Optional<List<uk.gov.justice.core.courts.AttendanceDay>> attendanceDaysFromRequest = defendantAttendance.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().map(a -> a.getAttendanceDays());
        assertTrue(attendanceDaysFromRequest.isPresent());
        final List<uk.gov.justice.core.courts.AttendanceDay> attendanceDaysListFromRequest = attendanceDaysFromRequest.get();
        assertEquals(1, attendanceDaysListFromRequest.size());
        assertEquals(attendanceDays.size(), attendanceDaysListFromRequest.size());
        final uk.gov.justice.core.courts.AttendanceDay attendanceDay = attendanceDays.get(0);
        final AttendanceDay attendanceDayFromRequest = attendanceDaysListFromRequest.get(0);
        assertEquals(attendanceDay.getAttendanceType(), attendanceDayFromRequest.getAttendanceType());
        assertEquals(attendanceDay.getDay(), attendanceDayFromRequest.getDay());

    }

    private void assertPerson(final Individual associatedPerson, final Person person) {
        assertEquals(associatedPerson.getFirstName(), person.getFirstName());
        assertEquals(associatedPerson.getAddress(), person.getAddress());
        assertEquals(associatedPerson.getLastName(), person.getLastName());
        assertEquals(associatedPerson.getContact(), person.getContact());
        assertEquals(associatedPerson.getDateOfBirth(), person.getDateOfBirth());
        assertEquals(associatedPerson.getGender(), person.getGender());
        assertEquals(associatedPerson.getMiddleName(), person.getMiddleName());
        assertEquals("UK", associatedPerson.getNationality());
        assertEquals(associatedPerson.getTitle(), person.getTitle());
    }
}
