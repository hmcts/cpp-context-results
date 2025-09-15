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
        final HearingFinancialResultRequest request = getFilteredApplicationResults(input.request());
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> nonNull(o.getIsParentFlag()) && o.getIsParentFlag())
                .filter(o -> nonNull(o.getIsDeemedServed()) && o.getIsDeemedServed())
                .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap())).distinct()
                .toList();

        final UUID applicationId = request.getOffenceResults().stream().filter(or -> nonNull(or.getApplicationId())).map(OffenceResults::getApplicationId).findFirst().orElse(null);
        final List<ImpositionOffenceDetails> impositionOffenceDetailsDeemedServedRemoved = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> nonNull(o.getIsParentFlag()) && o.getIsParentFlag())
                .filter(or -> !or.getIsDeemedServed() && isPrevOffenceResultDeemedServed(or.getOffenceId(), applicationId, input.prevApplicationOffenceResultsMap()))
                .filter(or -> Objects.nonNull(or.getAmendmentDate()))
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

    private boolean isPrevOffenceResultDeemedServed(final UUID offenceId, final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        return prevApplicationOffenceResultsMap.containsKey(applicationId)
                && prevApplicationOffenceResultsMap.get(applicationId).stream().anyMatch(or -> offenceId.equals(or.getOffenceId()));
    }
}
