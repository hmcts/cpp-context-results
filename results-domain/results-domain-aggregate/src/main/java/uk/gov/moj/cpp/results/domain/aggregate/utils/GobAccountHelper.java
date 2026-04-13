package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationDenied;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.event.OldAccountDetails;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

public class GobAccountHelper {

    public static OldAccountDetailsWrapper getOldAccountCorrelations(final LinkedList<CorrelationItem> correlationItemList, final UUID accountCorrelationId, final List<UUID> offenceIdList,
                                                                     final Map<UUID, List<OffenceResultsDetails>> applicationResultsDetails) {

        correlationItemList.sort(comparing(CorrelationItem::getCreatedTime).reversed());
        final Map<UUID, List<CorrelationItem>> hearingIdCorrelationItemsMap = offenceIdList.stream()
                .map(offenceId -> getOldCorrelationItemMatch(correlationItemList, accountCorrelationId, offenceId, applicationResultsDetails))
                .filter(Objects::nonNull)
                .distinct()
                .collect(groupingBy(CorrelationItem::getHearingId));

        final List<OldAccountDetails> oldAccountDetails = hearingIdCorrelationItemsMap.values().stream()
                .filter(CollectionUtils::isNotEmpty)
                .map(GobAccountHelper::toOldAccountCorrelations)
                .distinct()
                .toList();

        return new OldAccountDetailsWrapper(oldAccountDetails);
    }

    private static OldAccountDetails toOldAccountCorrelations(final List<CorrelationItem> ciList) {
        ciList.sort(comparing(CorrelationItem::getCreatedTime).reversed());
        final CorrelationItem correlationItem = ciList.get(0);
        return OldAccountDetails.oldAccountDetails()
                .withAccountCorrelationId(correlationItem.getAccountCorrelationId())
                .withGobAccountNumber(correlationItem.getAccountNumber())
                .withDivisionCode(correlationItem.getAccountDivisionCode())
                .withCreatedTime(correlationItem.getCreatedTime())
                .build();
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
                    final List<OffenceResultsDetails> prevAppDetails = applicationId == null ? null : applicationResultsDetails.get(applicationId);
                    return !isApplicationDenied(prevAppDetails);
                })
                .filter(correlationItem -> correlationItem.getOffenceResultsDetailsList().stream().anyMatch(o -> o.getOffenceId().equals(offenceId)))
                .findFirst()
                .map(correlationItem -> {
                    if (correlationItem.getOffenceResultsDetailsList().stream()
                            .filter(o -> o.getOffenceId().equals(offenceId))
                            .findFirst()
                            .filter(offenceResultsMatch -> Boolean.TRUE.equals(offenceResultsMatch.getIsFinancial())).isPresent()
                    ) {
                        return correlationItem;
                    }
                    return null;
                }).orElse(null);
    }
}
