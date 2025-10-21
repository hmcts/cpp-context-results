package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.Optional;

/**
 * Rule for handling NCES notifications for application amendments related to account consolidation result codes.
 */
public class ApplicationAmendmentACONNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() &&
                input.isAmendmentFlow() &&
                hasACONAmendmentOffences(filteredApplicationResults(input.request()));
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = filteredApplicationResults(input.request());
        final boolean includeOlds = isNull(request.getAccountCorrelationId());
        return Optional.of(
                markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                        .buildMarkedAggregateWithoutOlds(request,
                                NCESDecisionConstants.ACON_EMAIL_SUBJECT,
                                getAppFinancialImpositionOffenceDetails(input, request),
                                includeOlds));
    }
}
