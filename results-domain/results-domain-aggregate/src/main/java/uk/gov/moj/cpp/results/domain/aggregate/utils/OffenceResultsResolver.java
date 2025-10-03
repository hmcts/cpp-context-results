package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isAppealApplicationWithNoOffenceResults;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationDenied;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class OffenceResultsResolver {

    public static List<OffenceResultsDetails> getOriginalOffenceResultsCaseAmendment(final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                                     final List<OffenceResults> newOffenceResults) {

        return newOffenceResults.stream()
                .filter(reqOffenceResult -> hasFinancialChanges(reqOffenceResult, previousCaseOffenceResultsMap.get(reqOffenceResult.getOffenceId())))
                .map(reqOffenceResult -> previousCaseOffenceResultsMap.get(reqOffenceResult.getOffenceId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<OffenceResultsDetails> getOriginalOffenceResultsApplication(final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                                   final Map<UUID, List<OffenceResultsDetails>> previousApplicationOffenceResultsMap,
                                                                                   final List<OffenceResults> newOffenceResults) {

        final UUID currentApplicationId = newOffenceResults.stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        return newOffenceResults.stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(newOffenceResult -> ofNullable(getPreviousOffenceResultsDetails(newOffenceResult.getOffenceId(), previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, currentApplicationId))
                        .filter(previousOffenceResult -> hasFinancialChanges(newOffenceResult, previousOffenceResult))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<OffenceResultsDetails> getOriginalOffenceResultsAppAmendment(final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                                    final Map<UUID, List<OffenceResultsDetails>> previousApplicationOffenceResultsMap,
                                                                                    final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails,
                                                                                    final List<OffenceResults> newOffenceResults) {

        final UUID currentApplicationId = newOffenceResults.stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        return newOffenceResults.stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(newOffenceResult -> ofNullable(getPreviousOffenceResultsDetails(newOffenceResult.getOffenceId(), currentApplicationId, previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, prevApplicationResultsDetails))
                        .filter(previousOffenceResult -> hasFinancialChanges(newOffenceResult, previousOffenceResult))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public static OffenceResultsDetails getPreviousOffenceResultsDetails(final UUID offenceId, final UUID currentApplicationId,
                                                                          final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                          final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap,
                                                                          final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        if (isApplicationDenied(prevApplicationResultsDetails.get(currentApplicationId)) || (isAppealApplicationWithNoOffenceResults(currentApplicationId, prevApplicationOffenceResultsMap, prevApplicationResultsDetails))) {
            return getPreviousOffenceResultsDetails(offenceId, previousCaseOffenceResultsMap, prevApplicationOffenceResultsMap, currentApplicationId);
        }

        if (prevApplicationOffenceResultsMap.containsKey(currentApplicationId)) {
            return prevApplicationOffenceResultsMap.get(currentApplicationId).stream().filter(ord -> ord.getOffenceId().equals(offenceId)).findFirst().orElse(null);
        }
        return null;
    }

    public static List<OffenceResults> getNewOffenceResultsCaseAmendment(final List<OffenceResults> newOffenceResults,
                                                                         final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap) {

        return newOffenceResults.stream()
                .filter(o -> isNull(o.getApplicationType()))
                .map(newOffenceResult -> {
                    final OffenceResultsDetails previousOffenceResultsDetails = previousCaseOffenceResultsMap.get(newOffenceResult.getOffenceId());
                    return hasFinancialChanges(newOffenceResult, previousOffenceResultsDetails) ? newOffenceResult : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<OffenceResults> getNewOffenceResultsAppAmendment(final List<OffenceResults> newOffenceResults,
                                                                        final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                        final Map<UUID, List<OffenceResultsDetails>> previousApplicationOffenceResultsMap,
                                                                        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        final UUID currentApplicationId = newOffenceResults.stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        return newOffenceResults.stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> nonNull(o.getIsParentFlag()) && (o.getIsParentFlag()))
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(newOffenceResult -> {
                    final OffenceResultsDetails previousOffenceResult = getPreviousOffenceResultsDetails(newOffenceResult.getOffenceId(), currentApplicationId, previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, prevApplicationResultsDetails);
                    if (nonNull(currentApplicationId) && nonNull(previousOffenceResult) && hasFinancialChanges(newOffenceResult, previousOffenceResult)) {
                        return newOffenceResult;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<OffenceResults> getNewOffenceResultsApplication(final List<OffenceResults> newOffenceResults,
                                                                       final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                                       final Map<UUID, List<OffenceResultsDetails>> previousApplicationOffenceResultsMap) {

        final UUID currentApplicationId = newOffenceResults.stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        return newOffenceResults.stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(o -> TRUE.equals(o.getIsParentFlag()))
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(newOffenceResult -> {
                    final OffenceResultsDetails previousOffenceResultsDetails = getPreviousOffenceResultsDetails(newOffenceResult.getOffenceId(), previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, currentApplicationId);
                    return hasFinancialChanges(newOffenceResult, previousOffenceResultsDetails) ? newOffenceResult : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static boolean isNcesNotificationForNewApplication(final List<OffenceResults> newOffenceResults,
                                                              final Map<UUID, OffenceResultsDetails> previousCaseOffenceResultsMap,
                                                              final Map<UUID, List<OffenceResultsDetails>> previousApplicationOffenceResultsMap) {

        final UUID currentApplicationId = newOffenceResults.stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        final List<OffenceResultsDetails> matchingPreviousOffenceResultsDetails = newOffenceResults.stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(o -> TRUE.equals(o.getIsParentFlag()))
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(newOffenceResult -> getPreviousOffenceResultsDetails(newOffenceResult.getOffenceId(), previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, currentApplicationId))
                .filter(Objects::nonNull)
                .distinct().toList();

        if (isNotEmpty(matchingPreviousOffenceResultsDetails)
                && matchingPreviousOffenceResultsDetails.stream().noneMatch(po -> Boolean.TRUE.equals(po.getIsFinancial()))) {
            return false;
        }

        return newOffenceResults.stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(o -> TRUE.equals(o.getIsParentFlag()))
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(newOffenceResult -> {
                    final OffenceResultsDetails previousOffenceResultsDetails = getPreviousOffenceResultsDetails(newOffenceResult.getOffenceId(), previousCaseOffenceResultsMap, previousApplicationOffenceResultsMap, currentApplicationId);
                    return hasFinancialChangesNewApplication(newOffenceResult, previousOffenceResultsDetails) ? newOffenceResult : null;
                })
                .anyMatch(Objects::nonNull);
    }

    private static OffenceResultsDetails getPreviousOffenceResultsDetails(final UUID offenceId, final Map<UUID, OffenceResultsDetails> caseOffenceResultsDetails,
                                                                          final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap,
                                                                          final UUID currentApplicationId) {

        final List<OffenceResultsDetails> allOffenceResults = new ArrayList<>();

        //add case level results for that offence
        if (caseOffenceResultsDetails.containsKey(offenceId)) {
            allOffenceResults.add(caseOffenceResultsDetails.get(offenceId));
        }

        //add all previous application level results for that offence
        if (nonNull(currentApplicationId)) {
            prevApplicationOffenceResultsMap.forEach((applicationId, offenceResults) -> {
                if (!currentApplicationId.equals(applicationId)) {
                    allOffenceResults.addAll(offenceResults.stream().filter(ord -> offenceId.equals(ord.getOffenceId())).toList());
                }
            });
        }

        allOffenceResults.sort(comparing(OffenceResultsDetails::getCreatedTime).reversed());

        return !allOffenceResults.isEmpty() ? allOffenceResults.get(0) : null;
    }

    private static boolean hasFinancialChanges(final OffenceResults newOffenceResult, final OffenceResultsDetails previousOffenceResult) {
        if (nonNull(newOffenceResult) && nonNull(previousOffenceResult)) {
            return previousOffenceResult.getIsFinancial() && newOffenceResult.getIsFinancial()
                    || previousOffenceResult.getIsFinancial() && !newOffenceResult.getIsFinancial()
                    || !previousOffenceResult.getIsFinancial() && newOffenceResult.getIsFinancial();
        }
        return true;
    }

    private static boolean hasFinancialChangesNewApplication(final OffenceResults newOffenceResult, final OffenceResultsDetails previousOffenceResult) {
        if (nonNull(newOffenceResult) && nonNull(previousOffenceResult)) {
            return previousOffenceResult.getIsFinancial() && newOffenceResult.getIsFinancial()
                    || previousOffenceResult.getIsFinancial() && !newOffenceResult.getIsFinancial();
        }
        return true;
    }

}
