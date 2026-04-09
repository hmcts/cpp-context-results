package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationResultChanged;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * This rule is responsible for determining if an email notification should be sent when there is an amendment to an application result and cloned offences
 * had financial imposition prior to the current application(this means application notifications would have sent out to NCES),
 * and there is a change in the application result.
 *
 * @see <a href="https://tools.hmcts.net/jira/browse/DD-35053">CCT-2389:DD-35053</a>
 */
public class ApplicationStatusAmendmentNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() &&
                input.isAmendmentFlow() &&
                input.hasFinancialImpositionPriorToThisApplication() &&
                !input.hasAccountCorrelation() &&
                isApplicationResultChanged(input.request(), input.prevApplicationResultsDetails());
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final UUID currentApplicationId = input.request().getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);
        // `finToNonFin` true means A&R notification is also sent, then lets not send another notification.
        final boolean finToNonFin = isFineToNonFineApplicationAmendment(input, request, currentApplicationId) && !isFineToFineApplicationAmendment(input, request, currentApplicationId);

        if (finToNonFin) {
            return Optional.empty();
        }
        return applicationResultedNotification(input);
    }
}
