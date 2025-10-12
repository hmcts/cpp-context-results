package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationDenied;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GobAccountHelper {

    public static List<String> getOldGobAccounts(final LinkedList<CorrelationItem> correlationItemList, final UUID accountCorrelationId, final List<UUID> offenceIdList,
                                                 final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails) {

        correlationItemList.sort(comparing(CorrelationItem::getCreatedTime).reversed());
        final List<CorrelationItem> oldCorrelationItemsMatchingOffences = offenceIdList.stream()
                .map(offenceId -> getOldCorrelationItemMatch(correlationItemList, accountCorrelationId, offenceId, applicationResultsDetails))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        final boolean isSingleHearing = oldCorrelationItemsMatchingOffences.stream().map(CorrelationItem::getHearingId).filter(Objects::nonNull).distinct().count() == 1;
        if (isSingleHearing) {
            return isNotEmpty(oldCorrelationItemsMatchingOffences)
                    ? singletonList(oldCorrelationItemsMatchingOffences.get(oldCorrelationItemsMatchingOffences.size() - 1).getAccountNumber())
                    : emptyList();
        } else {
            return oldCorrelationItemsMatchingOffences.stream().map(CorrelationItem::getAccountNumber).filter(Objects::nonNull).distinct().toList();
        }
    }

    public static String getOldGobAccount(final LinkedList<CorrelationItem> correlationItemList, final UUID accountCorrelationId, final UUID offenceId,
                                          final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails) {
        correlationItemList.sort(comparing(CorrelationItem::getCreatedTime).reversed());
        final CorrelationItem oldCorrelationItemMatch = getOldCorrelationItemMatch(correlationItemList, accountCorrelationId, offenceId, applicationResultsDetails);
        return nonNull(oldCorrelationItemMatch) ? oldCorrelationItemMatch.getAccountNumber() : null;
    }

    private static CorrelationItem getOldCorrelationItemMatch(final LinkedList<CorrelationItem> correlationItemList, final UUID accountCorrelationId, final UUID offenceId,
                                                              final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails) {

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
                        return correlationItem;
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
