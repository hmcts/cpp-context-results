package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationDenied;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class GobAccountHelper {

    public static List<String> getOldGobAccounts(final LinkedList<CorrelationItem> correlationItemList, final UUID accountCorrelationId, final List<UUID> offenceIdList,
                                                 final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails) {
        return offenceIdList.stream()
                .map(offenceId -> getOldGobAccount(correlationItemList, accountCorrelationId, offenceId, applicationResultsDetails))
                .filter(Objects::nonNull)
                .distinct()
                .collect(toList());
    }

    public static String getOldGobAccount(final LinkedList<CorrelationItem> correlationItemList, final UUID accountCorrelationId, final UUID offenceId,
                                          final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails) {
        correlationItemList.sort(comparing(CorrelationItem::getCreatedTime).reversed());

        //find previous matching correlationItem by offenceId; then ensure the offence isFinancial to return valid GobAccountNumber
        return correlationItemList.stream()
                .filter(correlationItem -> !correlationItem.getAccountCorrelationId().equals(accountCorrelationId))
                //filter to skip correlation if that is for an application that was declined
                .filter(correlationItem -> {
                    final UUID applicationId = correlationItem.getOffenceResultsDetailsList().stream()
                            .map(OffenceResultsDetails::getApplicationId)
                            .filter(Objects::nonNull).findFirst().orElse(null);
                    return !isApplicationDenied(applicationResultsDetails.get(applicationId));
                })
                .filter(correlationItem -> correlationItem.getOffenceResultsDetailsList().stream().anyMatch(o -> o.getOffenceId().equals(offenceId)))
                .findFirst()
                .map(correlationItem -> {
                    if (correlationItem.getOffenceResultsDetailsList().stream()
                            .filter(o -> o.getOffenceId().equals(offenceId))
                            .findFirst()
                            .filter(offenceResultsMatch -> Boolean.TRUE.equals(offenceResultsMatch.getIsFinancial())).isPresent()) {
                        return correlationItem.getAccountNumber();
                    }
                    return null;
                }).orElse(null);
    }

    public static CorrelationItem getOldCorrelation(final LinkedList<CorrelationItem> correlationItemList, final UUID currentCorrelationId, final List<UUID> currentOffenceIdList) {
        correlationItemList.sort(comparing(CorrelationItem::getCreatedTime).reversed());
        return currentOffenceIdList.stream()
                .map(offenceId -> getOldCorrelation(correlationItemList, currentCorrelationId, offenceId))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public static List<CorrelationItem> getOldCorrelations(final LinkedList<CorrelationItem> correlationItemList, final UUID currentCorrelationId, final List<UUID> currentOffenceIdList) {
        correlationItemList.sort(comparing(CorrelationItem::getCreatedTime).reversed());
        return currentOffenceIdList.stream()
                .map(offenceId -> getOldCorrelation(correlationItemList, currentCorrelationId, offenceId))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static CorrelationItem getOldCorrelation(final LinkedList<CorrelationItem> correlationItemList, final UUID currentCorrelationId, final UUID offenceId) {
        return correlationItemList.stream()
                .filter(correlationItem -> !correlationItem.getAccountCorrelationId().equals(currentCorrelationId))
                .filter(correlationItem ->
                        correlationItem.getOffenceResultsDetailsList().stream().anyMatch(o -> o.getOffenceId().equals(offenceId)))
                .findFirst().orElse(null);
    }
}
