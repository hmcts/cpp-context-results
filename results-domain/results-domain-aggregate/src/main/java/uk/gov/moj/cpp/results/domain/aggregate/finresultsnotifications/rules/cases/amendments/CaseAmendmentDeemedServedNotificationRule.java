package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * This class implements a notification rule for case amendments with deemed served results.
 */
public class CaseAmendmentDeemedServedNotificationRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.hasCaseResult() && input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = request.getOffenceResults().stream()
                .filter(OffenceResults::getIsDeemedServed)
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .filter(offence -> isDeemedServedChanged(offence.getOffenceId(), offence.getIsDeemedServed(), input.prevOffenceResultsDetails()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap()))
                .toList();

        //TBD: check as part of CCT-2357
        final List<ImpositionOffenceDetails> impositionOffenceDetailsDeemedServedRemoved = request.getOffenceResults().stream()
                .filter(or -> !or.getIsDeemedServed() && isPrevOffenceResultDeemedServed(or.getOffenceId(), input.prevOffenceResultsDetails()))
                .filter(or -> Objects.nonNull(or.getAmendmentDate()))
                .map(or -> this.buildImpositionOffenceDetailsFromRequest(or, input.offenceDateMap())).distinct()
                .toList();

        final boolean isDeemedServedAddedOrAmended = !impositionOffenceDetailsForDeemed.isEmpty();
        final boolean isDeemedServedRemoved = !impositionOffenceDetailsDeemedServedRemoved.isEmpty();

        if (isDeemedServedAddedOrAmended) {
            final boolean includeOlds = isNull(request.getAccountCorrelationId());
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed, includeOlds)
            );
        }

        //TBD: check as part of CCT-2357
        if (isDeemedServedRemoved) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED, impositionOffenceDetailsDeemedServedRemoved, Boolean.TRUE)
            );
        }
        return Optional.empty();
    }

    private boolean isPrevOffenceResultDeemedServed(final UUID offenceId, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetailsMap) {
        return prevOffenceResultsDetailsMap.containsKey(offenceId) && prevOffenceResultsDetailsMap.get(offenceId).getIsDeemedServed();
    }

    private boolean isDeemedServedChanged(final UUID offenceId, final Boolean currentIsDeemedServed, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetailsMap) {
        if (!prevOffenceResultsDetailsMap.containsKey(offenceId)) {
            return currentIsDeemedServed; // If no previous result, treat as a change if current is deemed served
        }
        
        final OffenceResultsDetails prevOffenceResult = prevOffenceResultsDetailsMap.get(offenceId);
        final boolean statusChanged = !Objects.equals(prevOffenceResult.getIsDeemedServed(), currentIsDeemedServed);
        
        // Also consider it a change if:
        // 1. The status actually changed, OR
        // 2. Current is deemed served and this is the first amendment to a deemed served offence
        //    (previous was deemed served but had no amendment date, indicating it was the original result)
        final boolean isFirstAmendmentToDeemedServed = currentIsDeemedServed && 
                prevOffenceResult.getIsDeemedServed() && 
                (prevOffenceResult.getAmendmentDate() == null || prevOffenceResult.getAmendmentDate().isEmpty());
        
        return statusChanged || isFirstAmendmentToDeemedServed;
    }
}
