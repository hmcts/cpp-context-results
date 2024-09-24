package uk.gov.moj.cpp.domains;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.domains.resultdetails.ApplicationResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.CaseResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.DefendantResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.JudicialResultAmendmentType;
import uk.gov.moj.cpp.domains.resultdetails.JudicialResultDetails;
import uk.gov.moj.cpp.domains.resultdetails.OffenceResultDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class ResultAmendmentDetailsHelper {
    private ResultAmendmentDetailsHelper() {
    }

    public static List<CaseResultDetails> buildHearingResultDetails(final Hearing hearing,  final Map<UUID, CaseResultDetails> caseResultDetailsList) {
        final List<CaseResultDetails> newCaseResultDetailsList = new ArrayList<>();
        final Set<UUID> prosecutinCaseIds = getAllProsecutionIds(hearing);

        prosecutinCaseIds.forEach(
            prosecutionCaseId -> {
                final Optional<CaseResultDetails> existingCaseResultDetails = ofNullable(caseResultDetailsList.get(prosecutionCaseId));
                final ProsecutionCase prosecutionCase = getProsecutionCase(hearing, prosecutionCaseId);

                final List<CourtApplication> courtApplications = getMatchingCourtApplications(prosecutionCase.getId(), hearing.getCourtApplications());
                buildCaseResultDetails(prosecutionCase, hearing.getDefendantJudicialResults(), courtApplications, existingCaseResultDetails)
                        .ifPresent(newCaseResultDetailsList::add);
            }
        );

        return newCaseResultDetailsList;
    }

    private static ProsecutionCase getProsecutionCase(final Hearing hearing, final UUID prosecutionCaseId) {
        if (isNull(hearing.getProsecutionCases())) {
            return buildProsecutionCaseForComparison(prosecutionCaseId);
        }

        return hearing.getProsecutionCases().stream()
                .filter(pc -> prosecutionCaseId.equals(pc.getId()))
                .findFirst().orElse(buildProsecutionCaseForComparison(prosecutionCaseId));
    }

    private static Set<UUID> getAllProsecutionIds(final Hearing hearing) {
        final Set<UUID> prosecutinCaseIds = new HashSet<>();

        if (nonNull(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases()
                    .forEach(pc -> prosecutinCaseIds.add(pc.getId()));
        }

        if (nonNull(hearing.getCourtApplications())) {
            hearing.getCourtApplications().stream()
                    .filter(courtApplication -> nonNull(courtApplication.getCourtApplicationCases()))
                    .flatMap(courtApplication -> courtApplication.getCourtApplicationCases().stream())
                    .map(CourtApplicationCase::getProsecutionCaseId)
                    .forEach(prosecutinCaseIds::add);

            hearing.getCourtApplications().stream()
                    .filter(courtApplication -> nonNull(courtApplication.getCourtOrder()) && nonNull(courtApplication.getCourtOrder().getCourtOrderOffences()))
                    .flatMap(courtApplication -> courtApplication.getCourtOrder().getCourtOrderOffences().stream())
                    .map(CourtOrderOffence::getProsecutionCaseId)
                    .forEach(prosecutinCaseIds::add);
        }

        return prosecutinCaseIds;
    }

    private static ProsecutionCase buildProsecutionCaseForComparison(final UUID prosecutionId) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionId)
                .withDefendants(
                        Collections.emptyList()
                ).build();
    }

    private static List<CourtApplication> getMatchingCourtApplications(UUID caseId, List<CourtApplication> courtApplications) {
        if (isNull(courtApplications)) {
            return Collections.emptyList();
        }

        final List<CourtApplication> result = new ArrayList<>();

        courtApplications.stream()
                .filter(courtApplication -> nonNull(courtApplication.getCourtApplicationCases()))
                .filter(courtApplication -> courtApplication.getCourtApplicationCases().stream()
                        .anyMatch(courtApplicationCase -> courtApplicationCase.getProsecutionCaseId().equals(caseId)))
                .forEach(result::add);

        courtApplications.stream()
                .filter(courtApplication -> nonNull(courtApplication.getCourtOrder()) && nonNull(courtApplication.getCourtOrder().getCourtOrderOffences()))
                .filter(courtApplication -> courtApplication.getCourtOrder().getCourtOrderOffences().stream()
                        .anyMatch(courtOrderOffence -> courtOrderOffence.getProsecutionCaseId().equals(caseId)))
                .filter(courtApplication -> result.stream().noneMatch(c -> Objects.equals(c.getId(), courtApplication.getId())))
                .forEach(result::add);

        return result;
    }

    private static Optional<CaseResultDetails> buildCaseResultDetails(final ProsecutionCase prosecutionCase, final List<DefendantJudicialResult> defendantJudicialResults,
                                                                      final List<CourtApplication> courtApplications, final Optional<CaseResultDetails> existingCaseResultDetails) {
        final List<DefendantResultDetails> defendantResultDetails = new ArrayList<>();
        prosecutionCase.getDefendants().forEach(
                defendant -> {
                    final Optional<DefendantResultDetails> existingDefendantResultDetails = existingCaseResultDetails
                            .flatMap(resultDetails -> resultDetails.getDefendantResultDetails().stream()
                                    .filter(d -> d.getDefendantId().equals(defendant.getId()))
                                    .findFirst());

                    buildDefendantResultDetails(defendant, defendantJudicialResults, existingDefendantResultDetails).ifPresent(defendantResultDetails::add);
                });


        final List<ApplicationResultDetails> applicationResultDetails = new ArrayList<>();
        courtApplications.forEach(courtApplication -> {
            final Optional<ApplicationResultDetails> existingApplicationResultDetails = existingCaseResultDetails
                    .flatMap(resultDetails -> resultDetails.getApplicationResultDetails().stream()
                            .filter(d -> d.getApplicationId().equals(courtApplication.getId()))
                            .findFirst());

            buildApplicationResultDetails(courtApplication, prosecutionCase.getId(), existingApplicationResultDetails).ifPresent(applicationResultDetails::add);
        });

        return Optional.of(new CaseResultDetails(prosecutionCase.getId(), defendantResultDetails, applicationResultDetails));
    }

    private static Optional<ApplicationResultDetails> buildApplicationResultDetails(final CourtApplication application, final UUID caseId, final Optional<ApplicationResultDetails> existingApplicationResultDetails) {
        final List<JudicialResultDetails> existingResultDetails = existingApplicationResultDetails.map(applicationResultDetails -> applicationResultDetails.getResults().stream()
                .filter(resultDetail -> resultDetail.getAmendmentType() != JudicialResultAmendmentType.DELETED)
                .collect(toList())).orElse(Collections.emptyList());

        final List<JudicialResultDetails> judicialResultDetails = buildResulDetails(application.getJudicialResults(), existingResultDetails);
        final List<OffenceResultDetails> courtApplicationCasesResultDetails = getCourtApplicationCasesOffenceResultDetails(application, caseId, existingApplicationResultDetails);
        final List<OffenceResultDetails> courtOrderOffenceResultDetails = getCourtOrderOffenceResultDetails(application, existingApplicationResultDetails);

        String applicationSubjectFirstName = null;
        String applicationSubjectLastName = null;

        if (nonNull(application.getSubject())
                && nonNull(application.getSubject().getMasterDefendant())
                && nonNull(application.getSubject().getMasterDefendant().getPersonDefendant())
                && nonNull(application.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails())) {
            final Person person = application.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails();
            applicationSubjectLastName = person.getLastName();
            applicationSubjectFirstName = person.getFirstName();
        }

        return Optional.of(new ApplicationResultDetails(application.getId(), application.getType().getType(), judicialResultDetails, courtOrderOffenceResultDetails, courtApplicationCasesResultDetails, applicationSubjectFirstName, applicationSubjectLastName));
    }

    private static List<OffenceResultDetails> getCourtApplicationCasesOffenceResultDetails(final CourtApplication application, final UUID caseId, final Optional<ApplicationResultDetails> existingApplicationResultDetails) {
        if (isNull(application.getCourtApplicationCases())) {
            return Collections.emptyList();
        }

        final List<OffenceResultDetails> courtApplicationResultDetails = new ArrayList<>();
        application.getCourtApplicationCases().stream()
                .filter(courtApplicationCase -> Objects.equals(caseId, courtApplicationCase.getProsecutionCaseId()) && nonNull(courtApplicationCase.getOffences()))
                .flatMap(courtApplicationCase -> courtApplicationCase.getOffences().stream())
                .forEach(offence -> {
                    final Optional<OffenceResultDetails> existingCourtApplicationCaseOffenceDetails;

                    if (existingApplicationResultDetails.isPresent() && isNotEmpty(existingApplicationResultDetails.get().getCourtApplicationCasesResultDetails())) {
                        existingCourtApplicationCaseOffenceDetails = existingApplicationResultDetails.get().getCourtApplicationCasesResultDetails().stream()
                                .filter(courtOrderOffenceDetail -> Objects.equals(courtOrderOffenceDetail.getOffenceId(), offence.getId()))
                                .findFirst();
                    }  else {
                        existingCourtApplicationCaseOffenceDetails = Optional.empty();
                    }

                    buildOffenceResultDetails(offence, Collections.emptyList(), Collections.emptyList(), existingCourtApplicationCaseOffenceDetails)
                            .ifPresent(courtApplicationResultDetails::add);
                });

        return courtApplicationResultDetails;
    }

    private static List<OffenceResultDetails> getCourtOrderOffenceResultDetails(final CourtApplication application, final Optional<ApplicationResultDetails> existingApplicationResultDetails) {
        if (isNull(application.getCourtOrder()) || isNull(application.getCourtOrder().getCourtOrderOffences())) {
            return Collections.emptyList();
        }

        final List<OffenceResultDetails> clonedOffenceResultDetails = new ArrayList<>();

        application.getCourtOrder().getCourtOrderOffences().stream()
                .map(CourtOrderOffence::getOffence)
                .forEach(offence -> {
                    final Optional<OffenceResultDetails> existingCourtOrderOffenceDetails;

                    if (existingApplicationResultDetails.isPresent()) {
                        existingCourtOrderOffenceDetails = existingApplicationResultDetails.get().getCourtOrderOffenceResultDetails().stream()
                                .filter(courtOrderOffenceDetail -> Objects.equals(courtOrderOffenceDetail.getOffenceId(), offence.getId()))
                                .findFirst();
                    }  else {
                        existingCourtOrderOffenceDetails = Optional.empty();
                    }

                    buildOffenceResultDetails(offence, Collections.emptyList(), Collections.emptyList(), existingCourtOrderOffenceDetails)
                            .ifPresent(clonedOffenceResultDetails::add);
                });

        return clonedOffenceResultDetails;
    }



    private static Optional<DefendantResultDetails> buildDefendantResultDetails(final Defendant defendant,
                                                                                final List<DefendantJudicialResult> defendantJudicialResultList,
                                                                                final Optional<DefendantResultDetails> existingDefendantResultDetails) {

        final List<OffenceResultDetails> offenceResultDetails = new ArrayList<>();
        defendant.getOffences().forEach(offence ->
                {
                    final List<JudicialResult> defendantJudicialResults = getDefendantJudicialResults(defendant.getMasterDefendantId(), offence.getId(), defendantJudicialResultList);
                    final List<JudicialResult> defendantCaseJudicialResults = getDefendantCaseJudicialResults(defendant, offence.getId());

                    final Optional<OffenceResultDetails> existingOffenceResultDetails = existingDefendantResultDetails
                            .flatMap(resultDetails -> resultDetails.getOffences().stream()
                                    .filter(o -> o.getOffenceId().equals(offence.getId()))
                                    .findFirst());

                    buildOffenceResultDetails(offence, defendantJudicialResults, defendantCaseJudicialResults, existingOffenceResultDetails).ifPresent(offenceResultDetails::add);
                }
        );

        if (offenceResultDetails.isEmpty()) {
            return Optional.empty();
        }

        final String defendantName;
        if (nonNull(defendant.getPersonDefendant())) {
            defendantName = defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName();
        } else {
            defendantName = defendant.getLegalEntityDefendant().getOrganisation().getName();
        }

        return Optional.of(new DefendantResultDetails(defendant.getId(), defendantName, offenceResultDetails));

    }

    private static List<JudicialResult> getDefendantJudicialResults(final UUID masterDefendantId, final UUID offenceId, final List<DefendantJudicialResult> defendantJudicialResultList) {
        if (isNull(defendantJudicialResultList)) {
            return Collections.emptyList();
        }

        return defendantJudicialResultList.stream()
                .filter(djr -> Objects.equals(djr.getMasterDefendantId(), masterDefendantId) && Objects.equals(djr.getJudicialResult().getOffenceId(), offenceId))
                .map(DefendantJudicialResult::getJudicialResult)
                .collect(Collectors.toList());

    }

    private static List<JudicialResult> getDefendantCaseJudicialResults(final Defendant defendant, final UUID offenceId) {
        if (isNull(defendant.getDefendantCaseJudicialResults())) {
            return Collections.emptyList();
        }

        return defendant.getDefendantCaseJudicialResults().stream()
                .filter(dcjr -> Objects.equals(dcjr.getOffenceId(), offenceId))
                .collect(toList());
    }

    private static Optional<OffenceResultDetails> buildOffenceResultDetails(final Offence offence,
                                                                            final List<JudicialResult> defendantJudicialResults,
                                                                            final List<JudicialResult> defendantCaseJudicialResults,
                                                                            final Optional<OffenceResultDetails> existingOffenceResultDetails) {
        final List<JudicialResultDetails> existingResultDetails = existingOffenceResultDetails.map(offenceResultDetails -> offenceResultDetails.getResults().stream()
                .filter(resultDetail -> resultDetail.getAmendmentType() != JudicialResultAmendmentType.DELETED)
                .collect(toList())).orElse(Collections.emptyList());

        final List<JudicialResult> allJudicialResults = new ArrayList<>();

        if (nonNull(offence.getJudicialResults())) {
            allJudicialResults.addAll(offence.getJudicialResults());
        }

        allJudicialResults.addAll(defendantJudicialResults);
        allJudicialResults.addAll(defendantCaseJudicialResults);

        final List<JudicialResultDetails> judicialResultDetails = buildResulDetails(allJudicialResults, existingResultDetails);

        if (judicialResultDetails.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new OffenceResultDetails(offence.getId(), offence.getOrderIndex(),  offence.getCount(), offence.getOffenceTitle(), judicialResultDetails));
    }


    private static List<JudicialResultDetails> buildResulDetails(final List<JudicialResult> judicialResults, final List<JudicialResultDetails> existingResultDetails) {
        final List<JudicialResult> newJudicialResults = isNull(judicialResults) ? new ArrayList<>(): judicialResults;

        final List<JudicialResultDetails> judicialResultDetails = new ArrayList<>();

        for (final JudicialResult judicialResult: newJudicialResults) {
            final boolean isAdded = existingResultDetails.stream().noneMatch(resultDetail -> Objects.equals(resultDetail.getResultId(), judicialResult.getJudicialResultId()));
            JudicialResultAmendmentType resultAmendmentType = JudicialResultAmendmentType.NONE;

            if (isAdded) {
                resultAmendmentType = JudicialResultAmendmentType.ADDED;
            }
            else if (Boolean.TRUE.equals(judicialResult.getIsNewAmendment())) {
                resultAmendmentType = JudicialResultAmendmentType.UPDATED;
            }
            judicialResultDetails.add(new JudicialResultDetails(judicialResult.getJudicialResultId(), judicialResult.getLabel(),  judicialResult.getJudicialResultTypeId(), resultAmendmentType));
        }


        final List<JudicialResultDetails> deletedResults = existingResultDetails.stream()
                .filter(r -> newJudicialResults.stream().noneMatch(jr -> Objects.equals(r.getResultId(), jr.getJudicialResultId())))
                .map(resultDetail -> new JudicialResultDetails(resultDetail.getResultId(), resultDetail.getTitle(), resultDetail.getResultTypeId(), JudicialResultAmendmentType.DELETED))
                .collect(toList());

        judicialResultDetails.addAll(deletedResults);

        return judicialResultDetails;
    }

}
