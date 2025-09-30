package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ApplicationAmendmentDeemedServedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && input.isAmendmentFlow();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = filteredApplicationResults(input.request());
        final UUID applicationId = request.getOffenceResults().stream().filter(or -> nonNull(or.getApplicationId())).map(OffenceResults::getApplicationId).findFirst().orElse(null);
        
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> nonNull(o.getIsParentFlag()) && o.getIsParentFlag())
                .filter(o -> nonNull(o.getIsDeemedServed()) && o.getIsDeemedServed())
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .filter(o -> nonNull(o.getAmendmentDate())) // Only include offences that are being amended
                .filter(o -> isDeemedServedStatusChanged(o.getOffenceId(), o.getIsDeemedServed(), applicationId, input.prevApplicationOffenceResultsMap()))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap())).distinct()
                .toList();
        final List<ImpositionOffenceDetails> impositionOffenceDetailsDeemedServedRemoved = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> nonNull(o.getIsParentFlag()) && o.getIsParentFlag())
                .filter(or -> !or.getIsDeemedServed() && isPrevOffenceResultDeemedServed(or.getOffenceId(), applicationId, input.prevApplicationOffenceResultsMap()))
                .filter(or -> Objects.nonNull(or.getAmendmentDate()))
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(or -> buildImpositionOffenceDetailsFromRequest(or, input.offenceDateMap())).distinct()
                .toList();

        final boolean isDeemedServedAddedOrAmended = !impositionOffenceDetailsForDeemed.isEmpty();
        final boolean isDeemedServedRemoved = !impositionOffenceDetailsDeemedServedRemoved.isEmpty();

        if (isDeemedServedAddedOrAmended) {
            final boolean includeOlds = isNull(request.getAccountCorrelationId());
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED,
                                    impositionOffenceDetailsForDeemed, includeOlds));
        }

        if (isDeemedServedRemoved) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED,
                                    impositionOffenceDetailsDeemedServedRemoved, Boolean.TRUE));
        }
        return Optional.empty();
    }

   /* private boolean isPrevOffenceResultDeemedServed(final UUID offenceId, final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        return prevApplicationOffenceResultsMap.containsKey(applicationId)
                && prevApplicationOffenceResultsMap.get(applicationId).stream()
                .filter(or -> offenceId.equals(or.getOffenceId()))
                .findFirst()
                .map(OffenceResultsDetails::getIsDeemedServed)
                .orElse(false);
    }*/

    private boolean isPrevOffenceResultDeemedServed(final UUID offenceId, final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        return prevApplicationOffenceResultsMap.containsKey(applicationId)
                && prevApplicationOffenceResultsMap.get(applicationId).stream().filter(or -> offenceId.equals(or.getOffenceId())).anyMatch(OffenceResultsDetails::getIsDeemedServed);
    }

    private boolean isDeemedServedStatusChanged(final UUID offenceId, final Boolean currentIsDeemedServed, final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        if (applicationId == null || !prevApplicationOffenceResultsMap.containsKey(applicationId)) {
            return currentIsDeemedServed; // If no previous application results, treat as a change if current is deemed served
        }
        
        final List<OffenceResultsDetails> prevOffenceResults = prevApplicationOffenceResultsMap.get(applicationId);
        final Optional<OffenceResultsDetails> prevOffenceResult = prevOffenceResults.stream()
                .filter(or -> offenceId.equals(or.getOffenceId()))
                .findFirst();
        
        return prevOffenceResult
                .map(prev -> !Objects.equals(prev.getIsDeemedServed(), currentIsDeemedServed))
                .orElse(currentIsDeemedServed); // If no previous result for this offence, treat as a change if current is deemed served
    }
}
