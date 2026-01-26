package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.OrganisationDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StandaloneApplicationHelper {

    private static final String POLICE_ASN_DEFAULT_VALUE = "0800PP0100000000001H";
    private StandaloneApplicationHelper() {
    }

    public static CaseDefendant buildDefendantFromSubject(final CourtApplication application, final Hearing hearing) {
        final CourtApplicationParty subject = application.getSubject();
        if(isNull(subject)){
            return null;
        }

        return CaseDefendant.caseDefendant()
                .withDefendantId(subject.getId())
                .withCorporateDefendant(buildCorporateDefendant(application.getSubject()))
                .withIndividualDefendant(buildIndividualDefendant(application.getSubject()))
                .withAttendanceDays(ofNullable(hearing.getDefendantAttendance()).map(defendantAttendances -> buildAttendance(defendantAttendances, application.getSubject().getId())).orElse(emptyList()))
                .withProsecutorReference(ofNullable(application.getDefendantASN()).orElse(POLICE_ASN_DEFAULT_VALUE))//Police flag already checked in upper level. If the flow reaches upto here, prosecutor should be police.
                .withOffences(buildOffenceList(application))
                .withPncId(null)
                .withAssociatedPerson(null)
                .withJudicialResults(null)//not used in SPI OUT. Results are picked up from offence.
                .build();
    }


    private static List<OffenceDetails> buildOffenceList(final CourtApplication application) {
        return singletonList(OffenceDetails.offenceDetails()
                .withOffenceSequenceNumber(1)
                .withOffenceCode(application.getType().getCode())
                .withFinalDisposal(setFinalDisposal(application.getJudicialResults()))
                .withOffenceDateCode(1)
                .withJudicialResults(application.getJudicialResults())
                .withStartDate(application.getApplicationReceivedDate())
                .withWording(getWording(application))
                .withAllocationDecision(null)
                .withArrestDate(null)
                .withChargeDate(null)
                .withCivilOffence(null)
                .withConvictingCourt(null)
                .withConvictionDate(null)
                .withEndDate(null)
                .withFinding(null)
                .withId(randomUUID())
                .withIndicatedPlea(null)
                .withModeOfTrial(null)
                .withOffenceFacts(null)
                .withPlea(null)
                .build()
        );
    }

    private static String getWording(final CourtApplication application) {
        if(nonNull(application.getApplicationParticulars())){
            return application.getApplicationParticulars();
        }
        if(nonNull(application.getType().getApplicationWording())){
            return application.getType().getApplicationWording();
        }
        return application.getType().getType();
    }

    private static String setFinalDisposal(final List<JudicialResult> judicialResults) {
        return isCategoryTypeFinalPresentInJudicialResult(judicialResults) ? "Y" : "N";
    }

    private static boolean isCategoryTypeFinalPresentInJudicialResult(final List<JudicialResult> judicialResultsList) {

        if (isNotEmpty(judicialResultsList)) {
            return judicialResultsList
                    .stream()
                    .filter(judicialResult -> nonNull(judicialResult.getCategory()))
                    .anyMatch(judicialResult -> judicialResult.getCategory().equals(JudicialResultCategory.FINAL));
        }
        return false;

    }

    private static OrganisationDetails buildCorporateDefendant(final CourtApplicationParty subject){
        if(isNull(subject.getOrganisation())){
            return null;
        }
        return OrganisationDetails.organisationDetails()
                .withName(subject.getOrganisation().getName())
                .withAddress(subject.getOrganisation().getAddress())
                .withContact(subject.getOrganisation().getContact())
                .withIncorporationNumber(subject.getOrganisation().getIncorporationNumber())
                .withRegisteredCharityNumber(subject.getOrganisation().getRegisteredCharityNumber())
                .withPresentAtHearing(null)//standalone applications does not have attendance information. A Default value is set in staging-spi.
                .build();
    }

    private static IndividualDefendant buildIndividualDefendant(final CourtApplicationParty subject){
        if(isNull(subject.getPersonDetails())){
            return null;
        }
        return IndividualDefendant.individualDefendant()
                .withBailConditions(null)//standalone applications do not have defendant
                .withBailStatus(BailStatus.bailStatus()
                        .withId(randomUUID())
                        .withCode("A")
                        .withDescription("Not Applicable")
                        .build())//for all standalone application, value set to A - not applicable
                .withReasonForBailConditionsOrCustody(null)//standalone applications do not have defendant
                .withPerson(Individual.individual()
                        .withAddress(subject.getPersonDetails().getAddress())
                        .withContact(subject.getPersonDetails().getContact())
                        .withDateOfBirth(subject.getPersonDetails().getDateOfBirth())
                        .withFirstName(subject.getPersonDetails().getFirstName())
                        .withMiddleName(subject.getPersonDetails().getMiddleName())
                        .withLastName(subject.getPersonDetails().getLastName())
                        .withGender(subject.getPersonDetails().getGender())
                        .withNationality(subject.getPersonDetails().getNationalityDescription())
                        .withTitle(subject.getPersonDetails().getTitle())
                        .build())
                .withPresentAtHearing(null)//standalone applications does not have attendance information. A Default value is set in staging-spi.
                .build();
    }

    public static List<uk.gov.justice.core.courts.AttendanceDay> buildAttendance(final List<DefendantAttendance> defendantAttendance, final UUID defendantId) {
        final Optional<List<uk.gov.justice.core.courts.AttendanceDay>> attendanceDays = defendantAttendance.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().map(a -> a.getAttendanceDays());
        return attendanceDays.orElse(null);
    }


}
