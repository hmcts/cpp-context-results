package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewReopenApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewStatdecApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isPreviousApplicationFinalisedNotificationSent;

import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.Optional;

/**
 * Rule to handle notifications for StatDec/Reopen that are denied.
 */
public class NewStatdecOrReopenAppDeniedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isNewApplication()
                && isNewStatdecApplicationDenied(input.request()) || isNewReopenApplicationDenied(input.request())
                && !isPreviousApplicationFinalisedNotificationSent(input.request(), input.prevApplicationResultsDetails(), input.prevApplicationOffenceResultsMap());
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        return applicationResultedNotification(input);
    }
}
