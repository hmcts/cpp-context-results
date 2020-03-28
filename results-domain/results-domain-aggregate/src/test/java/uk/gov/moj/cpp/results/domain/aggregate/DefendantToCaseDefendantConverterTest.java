package uk.gov.moj.cpp.results.domain.aggregate;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.AssociatedIndividual.associatedIndividual;
import static uk.gov.justice.core.courts.CourtIndicatedSentence.courtIndicatedSentence;
import static uk.gov.justice.core.courts.Individual.individual;
import static uk.gov.moj.cpp.domains.resultStructure.Result.result;
import static uk.gov.moj.cpp.results.domain.aggregate.DefendantToCaseDefendantConverter.convert;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.AssociatedIndividual;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.OrganisationDetails;
import uk.gov.justice.core.courts.PleaValue;
import uk.gov.justice.core.courts.VehicleCode;
import uk.gov.moj.cpp.domains.resultStructure.AttendanceDay;
import uk.gov.moj.cpp.domains.resultStructure.CorporateDefendant;
import uk.gov.moj.cpp.domains.resultStructure.Defendant;
import uk.gov.moj.cpp.domains.resultStructure.Offence;
import uk.gov.moj.cpp.domains.resultStructure.OffenceFacts;
import uk.gov.moj.cpp.domains.resultStructure.Person;
import uk.gov.moj.cpp.domains.resultStructure.Plea;
import uk.gov.moj.cpp.domains.resultStructure.Result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class DefendantToCaseDefendantConverterTest {

    private static final UUID DEFAULT_DEFENDANT_ID = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID RESULT_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID ORDERED_HEARING_ID = randomUUID();

    @Test
    public void testConvert() {
        final Defendant defendantFromAggregate = buildDefendant();
        final CaseDefendant caseDefendant  =  convert(defendantFromAggregate);
        assertThat(caseDefendant.getDefendantId(), is(defendantFromAggregate.getId()));
        assertThat(caseDefendant.getPncId(), is(defendantFromAggregate.getPncId()));
        assertThat(caseDefendant.getProsecutorReference(), is(defendantFromAggregate.getProsecutorReference()));
        assertOffences(caseDefendant.getOffences(), defendantFromAggregate.getOffences());
        assertCorporateDefendant(caseDefendant.getCorporateDefendant(), defendantFromAggregate.getCorporateDefendant());
        assertIndividualDefendant(caseDefendant, defendantFromAggregate);
    }

    private void assertIndividualDefendant(final CaseDefendant caseDefendant, final Defendant defendantFromAggregate) {
        final IndividualDefendant individualDefendant = caseDefendant.getIndividualDefendant();
        assertThat(defendantFromAggregate.getBailStatus(), is(individualDefendant.getBailStatus()));
        assertThat(defendantFromAggregate.getBailCondition(), is(individualDefendant.getBailConditions()));
        assertThat(defendantFromAggregate.getReasonForBailConditionsOrCustody(), is(individualDefendant.getReasonForBailConditionsOrCustody()));
        assertThat(defendantFromAggregate.getPerson().getFirstName(), is(individualDefendant.getPerson().getFirstName()));
        assertThat(defendantFromAggregate.getPerson().getMiddleName(), is(individualDefendant.getPerson().getMiddleName()));
        assertThat(defendantFromAggregate.getPerson().getLastName(), is(individualDefendant.getPerson().getLastName()));
        assertThat(defendantFromAggregate.getPerson().getDateOfBirth(), is(individualDefendant.getPerson().getDateOfBirth()));
        assertThat(defendantFromAggregate.getPerson().getGender(), is(individualDefendant.getPerson().getGender()));
        assertThat(defendantFromAggregate.getPerson().getTitle(), is(individualDefendant.getPerson().getTitle()));
        assertThat(defendantFromAggregate.getPerson().getNationality(), is(individualDefendant.getPerson().getNationality()));
        assertThat(defendantFromAggregate.getPerson().getAddress(), is(individualDefendant.getPerson().getAddress()));
        assertThat(defendantFromAggregate.getPerson().getContact(), is(individualDefendant.getPerson().getContact()));
    }

    private void assertCorporateDefendant(final OrganisationDetails corporateDefendant, final CorporateDefendant corporateDefendantFromRequest) {
        assertThat(corporateDefendant.getIncorporationNumber(), is(corporateDefendantFromRequest.getIncorporationNumber()));
        assertThat(corporateDefendant.getName(), is(corporateDefendantFromRequest.getName()));
        assertThat(corporateDefendant.getContact(), is(corporateDefendantFromRequest.getContact()));
    }

    private void assertOffences(final List<OffenceDetails> offenceDetails, final List<Offence> defendantFromRequestOffences) {
        for (final OffenceDetails offenceDetail : offenceDetails) {
            final Optional<Offence> offenceOptional = defendantFromRequestOffences.stream().filter(o -> o.getId().equals(offenceDetail.getId())).findFirst();
            assertThat(offenceOptional.isPresent(), is(true));
            final Offence offenceFromRequest = offenceOptional.get();
            assertThat(offenceDetail.getArrestDate(), is(offenceFromRequest.getArrestDate()));
            assertThat(offenceDetail.getChargeDate(), is(offenceFromRequest.getChargeDate()));
            assertThat(offenceDetail.getConvictingCourt(), is(24));
            assertThat(offenceDetail.getConvictionDate(), is(offenceFromRequest.getConvictionDate()));
            assertThat(offenceDetail.getEndDate(), is(offenceFromRequest.getEndDate()));
            assertThat(offenceDetail.getFinalDisposal(), is("N"));
            assertResults(offenceDetail.getJudicialResults(), offenceFromRequest.getResultDetails());
            assertThat(offenceDetail.getModeOfTrial(), is("1010"));

            assertAllocationDecision(offenceDetail, offenceFromRequest);
            assertThat(offenceDetail.getOffenceCode(), is(offenceFromRequest.getOffenceCode()));
            assertThat(offenceDetail.getOffenceSequenceNumber(), is(offenceFromRequest.getOffenceSequenceNumber()));
            assertThat(offenceDetail.getStartDate(), is(offenceFromRequest.getStartDate()));
            assertThat(offenceDetail.getWording(), is(offenceFromRequest.getWording()));
            assertThat(offenceDetail.getPlea().getOffenceId(), is(offenceFromRequest.getId()));
            assertThat(offenceDetail.getPlea().getPleaDate(), is(offenceFromRequest.getPlea().getDate()));
            assertThat(offenceDetail.getPlea().getPleaValue(), is(offenceFromRequest.getPlea().getValue()));
            assertThat(offenceDetail.getPlea().getOriginatingHearingId(), is(offenceFromRequest.getPlea().getEnteredHearingId()));
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

    private void assertResults(final List<JudicialResult> judicialResults, final List<Result> resultDetailsFromRequest) {
        for (final JudicialResult judicialResult : judicialResults) {
            final Optional<Result> resultOptional = resultDetailsFromRequest.stream().filter(j -> j.getResultId().toString().equals(judicialResult.getJudicialResultId().toString())).findFirst();
            assertThat(resultOptional.isPresent(), is(true));
            final Result resultFromRequest = resultOptional.get();
            assertThat(judicialResult.getAmendmentDate(), is(resultFromRequest.getAmendmentDate()));
            assertThat(judicialResult.getAmendmentReason(), is(resultFromRequest.getAmendmentReason()));
            assertThat(judicialResult.getApprovedDate(), is(resultFromRequest.getApprovedDate()));
            assertThat(judicialResult.getCategory(), is(resultFromRequest.getCategory()));
            assertThat(judicialResult.getCjsCode(), is(resultFromRequest.getCjsCode()));
            assertThat(judicialResult.getCourtClerk(), is(resultFromRequest.getCourtClerk()));
            assertThat(judicialResult.getDelegatedPowers().getFirstName(), is(resultFromRequest.getDelegatedPowers().getFirstName()));
            assertThat(judicialResult.getDelegatedPowers().getLastName(), is(resultFromRequest.getDelegatedPowers().getLastName()));
            assertThat(judicialResult.getDelegatedPowers().getUserId(), is(resultFromRequest.getDelegatedPowers().getUserId()));
            assertThat(judicialResult.getFourEyesApproval(), is(resultFromRequest.getFourEyesApproval()));
            assertThat(judicialResult.getIsAdjournmentResult(), is(resultFromRequest.getIsAdjournmentResult()));
            assertThat(judicialResult.getIsAvailableForCourtExtract(), is(resultFromRequest.getIsAvailableForCourtExtract()));
            assertThat(judicialResult.getIsConvictedResult(), is(resultFromRequest.getIsConvictedResult()));
            assertThat(judicialResult.getIsFinancialResult(), is(resultFromRequest.getIsFinancialResult()));
            assertThat(judicialResult.getJudicialResultPrompts(), is(resultFromRequest.getJudicialResultPrompts()));
            assertThat(judicialResult.getLabel(), is(resultFromRequest.getLabel()));
            assertThat(judicialResult.getLastSharedDateTime(), is(resultFromRequest.getLastSharedDateTime()));
            assertThat(judicialResult.getOrderedDate(), is(resultFromRequest.getOrderedDate()));
            assertThat(judicialResult.getOrderedHearingId(), is(resultFromRequest.getOrderedHearingId()));
            assertThat(judicialResult.getRank(), is(resultFromRequest.getRank()));
            assertThat(judicialResult.getWelshLabel(), is(resultFromRequest.getWelshLabel()));
            assertThat(judicialResult.getUsergroups(), is(resultFromRequest.getUsergroups()));
            assertThat(judicialResult.getIsDeleted(), is(resultFromRequest.getDeleted()));
        }
    }
    private Defendant buildDefendant() {
        final Defendant defendant = new Defendant(DEFAULT_DEFENDANT_ID);
        defendant.setPresentAtHearing("T");
        defendant.setAssociatedIndividuals(buildAssociatedIndividuals());
        defendant.setBailCondition("bailCondition");
        defendant.setCorporateDefendant(CorporateDefendant.corporateDefendant().build());
        defendant.setPerson(Person.person().withFirstName("John")
                .withMiddleName("Smith")
                .withLastName("Row")
                .withGender(Gender.MALE)
                .withTitle("Mr")
                .withDateOfBirth(LocalDate.of(2000, 1, 1))
                .withNationality("GBR")
                .withContact(buildContactNumber())
                .withAddress(buildAddress())
                .build());
        defendant.setPncId("pncId002");
        defendant.setProsecutorReference("reference");
        defendant.setReasonForBailConditionsOrCustody("reason");
        defendant.setAttendanceDays(buildAttendanceDays());
        defendant.setOffences(buildOffences());
        defendant.setResults(buildListOfResults());
        return defendant;
    }

    private List<AttendanceDay> buildAttendanceDays() {
        final List<AttendanceDay> attendanceDays =  new ArrayList<>();
        final AttendanceDay attendanceDay = new AttendanceDay(LocalDate.of(2018, 5, 2), true);
        attendanceDays.add(attendanceDay);
        return attendanceDays;
    }

    private List<AssociatedIndividual> buildAssociatedIndividuals() {
        final List<AssociatedIndividual> associatedIndividuals =  new ArrayList<>();
        associatedIndividuals.add(associatedIndividual()
                .withRole("role")
                .withPerson(individual()
                        .withAddress(buildAddress())
                        .withTitle("Mr")
                        .withContact(buildContactNumber())
                        .withDateOfBirth(LocalDate.of(2018, 5, 2))
                        .withFirstName("JohnSmith")
                        .withLastName("LastName")
                        .withMiddleName("MiddleName")
                        .withNationality("UK")
                        .build())
                .build());
        return associatedIndividuals;
    }

    private ContactNumber buildContactNumber() {
        return ContactNumber.contactNumber()
                .withPrimaryEmail("primaryemail@gmail.com")
                .withSecondaryEmail("secondary@gmail.com")
                .withHome("9999999999")
                .withMobile("88888888888")
                .withWork("7777777777")
                .withFax("5555555555")
                .build();
    }

    private static Address buildAddress() {
        return address()
                .withAddress1("Fitzalan Place")
                .withAddress2("Cardiff")
                .withAddress3("addressline3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("CF24 0RZ")
                .build();
    }


    private List<Offence> buildOffences() {
        final List<Offence> offences = new ArrayList<>();
        final Offence offence = new Offence(OFFENCE_ID,"offenceCode" ,1 ,"offenceWording"
                ,2 , now(),now() ,now() , now(), buildOffenceFacts(), buildPlea(),"1010" ,24,
                LocalDate.of(2018, 5, 2) ,"N" ,buildListOfResults() ,"finding", buildAllocationDecision());
        offences.add(offence);
        return offences;
    }

    private AllocationDecision buildAllocationDecision() {
        return allocationDecision()
                    .withAllocationDecisionDate(LocalDate.of(2019,10,01))
                    .withMotReasonCode("motReasonCode")
                    .withSequenceNumber(1)
                    .withOriginatingHearingId(randomUUID())
                    .withMotReasonId(randomUUID())
                    .withMotReasonDescription("motReasonDescription")
                    .withOffenceId(OFFENCE_ID)
                    .withCourtIndicatedSentence(courtIndicatedSentence().
                            withCourtIndicatedSentenceTypeId(randomUUID())
                            .withCourtIndicatedSentenceDescription("description").build())
                    .build();
    }

    private Plea buildPlea() {
       return new Plea(randomUUID(), LocalDate.of(2018, 5, 2), PleaValue.GUILTY, randomUUID());
    }

    private DelegatedPowers buildDelegatPowers() {
       return DelegatedPowers.delegatedPowers().withUserId(USER_ID)
                .withLastName("lastName").withFirstName("firstName").build();
    }

    private  Result buildResult() {
        return  result()
                .withAmendmentDate(LocalDate.of(2018, 6, 2))
                .withAmendmentReason("reason")
                .withApprovedDate(LocalDate.of(2018, 7, 2))
                .withCategory(Category.ANCILLARY)
                .withCjsCode("0007")
                .withCourtClerk(buildDelegatPowers())
                .withDelegatedPowers(buildDelegatPowers())
                .withFourEyesApproval(buildDelegatPowers())
                .withIsAdjournmentResult(true)
                .withIsAvailableForCourtExtract(true)
                .withIsConvictedResult(true)
                .withIsFinancialResult(true)
                .withJudicialResultPrompts(buildJudicialPrompts())
                .withLabel("label")
                .withLastSharedDateTime("2019-05-24")
                .withOrderedDate(LocalDate.of(2018, 8, 2))
                .withAmendmentReason("reason")
                .withAmendmentDate(LocalDate.of(2018, 9, 2))
                .withOrderedHearingId(ORDERED_HEARING_ID)
                .withRank(new BigDecimal(23))
                .withResultId(RESULT_ID)
                .withUsergroups(asList("usergroup1","usergroup2"))
                .withWelshLabel("WS")
                .withIsDeleted(true)
                .withApprovedDate(LocalDate.of(2018, 10, 2)).build();
    }

    private List<JudicialResultPrompt> buildJudicialPrompts() {
       final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        final JudicialResultPrompt judicialResultPrompt = JudicialResultPrompt.judicialResultPrompt().build();
        judicialResultPrompts.add(judicialResultPrompt);
        return  judicialResultPrompts;
    }

    private List<Result> buildListOfResults() {
       final List<Result> results = new ArrayList<>();
       results.add(buildResult());
        return results;
    }

   private  OffenceFacts buildOffenceFacts() {
    OffenceFacts offenceFacts = new OffenceFacts(1,"alcoholreadingMethod", VehicleCode.LARGE_GOODS_VEHICLE,"12345");
       return offenceFacts;
   }
}
