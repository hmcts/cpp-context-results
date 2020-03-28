package uk.gov.moj.cpp.results.domain.aggregate;

import static uk.gov.justice.core.courts.CaseDefendant.caseDefendant;
import static uk.gov.justice.core.courts.Individual.individual;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.OffenceDetails.offenceDetails;
import static uk.gov.justice.core.courts.OrganisationDetails.organisationDetails;
import static uk.gov.justice.core.courts.Plea.plea;

import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.Individual;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.OffenceDetails;
import uk.gov.justice.core.courts.OrganisationDetails;
import uk.gov.justice.core.courts.Plea;
import uk.gov.moj.cpp.domains.resultStructure.CorporateDefendant;
import uk.gov.moj.cpp.domains.resultStructure.Defendant;
import uk.gov.moj.cpp.domains.resultStructure.Offence;
import uk.gov.moj.cpp.domains.resultStructure.Person;
import uk.gov.moj.cpp.domains.resultStructure.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefendantToCaseDefendantConverter {

    private DefendantToCaseDefendantConverter() {

    }

    public static CaseDefendant convert(final Defendant defendant) {

        return caseDefendant().withPncId(defendant.getPncId())
                .withProsecutorReference(defendant.getProsecutorReference())
                .withOffences(buildOffences(defendant.getOffences()))
                .withJudicialResults(defendant.getResults().isEmpty() ? null : buildJudicialResults(defendant.getResults()))
                .withCorporateDefendant(null != defendant.getCorporateDefendant() ? buildCorporateDefendant(defendant.getCorporateDefendant(), defendant) : null)
                .withIndividualDefendant(null != defendant.getPerson() ? buildIndividualDefendant(defendant) : null)
                .withAssociatedPerson(defendant.getAssociatedIndividuals())
                .withDefendantId(defendant.getId())
                .build();
    }

    private static IndividualDefendant buildIndividualDefendant(final Defendant defendant) {
        return IndividualDefendant.individualDefendant()
                .withPresentAtHearing(defendant.getPresentAtHearing())
                .withReasonForBailConditionsOrCustody(defendant.getReasonForBailConditionsOrCustody())
                .withBailConditions(defendant.getBailCondition())
                .withBailStatus(defendant.getBailStatus())
                .withPerson(buildIndividual(defendant.getPerson()))
                .build();
    }

    private static Individual buildIndividual(final Person person) {
        return individual()
                .withLastName(person.getLastName())
                .withGender(person.getGender())
                .withMiddleName(person.getMiddleName())
                .withNationality(person.getNationality())
                .withFirstName(person.getFirstName())
                .withDateOfBirth(person.getDateOfBirth())
                .withContact(person.getContact())
                .withTitle(person.getTitle())
                .withAddress(person.getAddress())
                .build();
    }

    private static OrganisationDetails buildCorporateDefendant(final CorporateDefendant corporateDefendant, final Defendant defendant) {
        return organisationDetails()
                .withAddress(corporateDefendant.getAddress())
                .withContact(corporateDefendant.getContact())
                .withIncorporationNumber(corporateDefendant.getIncorporationNumber())
                .withName(corporateDefendant.getName())
                .withPresentAtHearing(defendant.getPresentAtHearing())
                .build();
    }

    private static List<OffenceDetails> buildOffences(final List<Offence> offences) {

        final List<OffenceDetails> offenceDetails = new ArrayList<>();
        for (final Offence offence : offences) {
            offenceDetails.add(offenceDetails().withArrestDate(offence.getArrestDate())
                    .withChargeDate(offence.getChargeDate())
                    .withConvictingCourt(offence.getConvictingCourt())
                    .withConvictionDate(offence.getConvictionDate())
                    .withEndDate(offence.getEndDate())
                    .withFinalDisposal(offence.getFinalDisposal())
                    .withId(offence.getId())
                    .withJudicialResults(buildJudicialResults(offence.getResultDetails()))
                    .withAllocationDecision(offence.getAllocationDecision())
                    .withModeOfTrial(offence.getModeOfTrial())
                    .withOffenceCode(offence.getOffenceCode())
                    .withOffenceDateCode(offence.getOffenceDateCode())
                    .withOffenceSequenceNumber(offence.getOffenceSequenceNumber())
                    .withPlea(buildPlea(offence.getPlea(), offence.getId()))
                    .withStartDate(offence.getStartDate())
                    .withWording(offence.getWording())
                    .build());
        }
        return offenceDetails;
    }

    private static Plea buildPlea(final uk.gov.moj.cpp.domains.resultStructure.Plea plea, final UUID offenceId) {
        if (null != plea) {
            return plea()
                    .withOriginatingHearingId(plea.getEnteredHearingId())
                    .withPleaDate(plea.getDate())
                    .withPleaValue(plea.getValue())
                    .withOffenceId(offenceId)
                    .build();
        }
        return null;
    }

    private static List<JudicialResult> buildJudicialResults(final List<Result> resultDetailsFromDefendant) {

        final List<JudicialResult> judicialResults = new ArrayList<>();

        for (final Result resultDetail : resultDetailsFromDefendant) {
            judicialResults.add(judicialResult()
                    .withJudicialResultId(resultDetail.getResultId())
                    .withAmendmentDate(resultDetail.getAmendmentDate())
                    .withAmendmentReason(resultDetail.getAmendmentReason())
                    .withApprovedDate(resultDetail.getApprovedDate())
                    .withCategory(resultDetail.getCategory())
                    .withCjsCode(resultDetail.getCjsCode())
                    .withCourtClerk(resultDetail.getCourtClerk())
                    .withDelegatedPowers(resultDetail.getDelegatedPowers())
                    .withFourEyesApproval(resultDetail.getFourEyesApproval())
                    .withIsAdjournmentResult(resultDetail.getIsAdjournmentResult())
                    .withIsAvailableForCourtExtract(resultDetail.getIsAvailableForCourtExtract())
                    .withIsConvictedResult(resultDetail.getIsConvictedResult())
                    .withIsFinancialResult(resultDetail.getIsFinancialResult())
                    .withJudicialResultPrompts(resultDetail.getJudicialResultPrompts())
                    .withLabel(resultDetail.getLabel())
                    .withLastSharedDateTime(resultDetail.getLastSharedDateTime())
                    .withOrderedDate(resultDetail.getOrderedDate())
                    .withOrderedHearingId(resultDetail.getOrderedHearingId())
                    .withRank(resultDetail.getRank())
                    .withUsergroups(resultDetail.getUsergroups())
                    .withIsDeleted(resultDetail.getDeleted())
                    .withWelshLabel(resultDetail.getWelshLabel())
                    .withLifeDuration(resultDetail.getLifeDuration())
                    .withResultText(resultDetail.getResultText())
                    .build());
        }
        return judicialResults;
    }

}
