package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.Optional;

/**
 * This class implements a notification rule for case amendments with ACON results.
 */
public class CaseAmendmentACONNotificationRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(ResultNotificationRule.RuleInput input) {
        return input.isCaseAmendment() &&
                hasACONAmendmentOffences(filteredCaseResults(input.request()));
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());
        final boolean includeOlds = isNull(request.getAccountCorrelationId());
        return Optional.of(
                markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                        .buildMarkedAggregateWithoutOlds(request,
                                NCESDecisionConstants.ACON_EMAIL_SUBJECT,
                                getCaseFinancialImpositionOffenceDetails(input, request),
                                includeOlds)
        );
    }
}
