package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result;

import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.Optional;

/**
 * Rule to handle notifications for cases where the deemed served result code is applied.
 * This rule checks if the case result is present and not part of an amendment process.
 */
public class CaseDeemedServedNotificationRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.hasCaseResult() && !input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());
        if (hasDeemedServedOffences(request) && input.isFinancial()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request,
                                    WRITE_OFF_ONE_DAY_DEEMED_SERVED,
                                    getCaseFinancialImpositionOffenceDetails(input, request),
                                    Boolean.FALSE)
            );
        }
        return Optional.empty();
    }
}
