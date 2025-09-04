package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasNoCorrelationIdForAmendedApplication;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Optional;

public class ApplicationDeemedServedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType();
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
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            if (hasNoCorrelationIdForAmendedApplication(request)) {
                return Optional.of(
                        markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                                .buildMarkedAggregateWithoutOlds(request,
                                        NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED,
                                        impositionOffenceDetailsForDeemed,
                                        Boolean.TRUE));
            } else {
                return Optional.of(
                        markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                                .buildMarkedAggregateWithoutOlds(request,
                                        NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED,
                                        impositionOffenceDetailsForDeemed,
                                        Boolean.FALSE));
            }
        }
        return Optional.empty();
    }
}
