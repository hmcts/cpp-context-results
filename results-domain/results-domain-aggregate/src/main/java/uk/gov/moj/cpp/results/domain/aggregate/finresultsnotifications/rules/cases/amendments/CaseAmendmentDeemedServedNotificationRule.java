package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class implements a notification rule for case amendments with deemed served results.
 */
public class CaseAmendmentDeemedServedNotificationRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = getFilteredCaseResults(input.request());
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForDeemed = request.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsDeemedServed)
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap()))
                .toList();
        if (!impositionOffenceDetailsForDeemed.isEmpty()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED, impositionOffenceDetailsForDeemed, Boolean.FALSE)
            );
        }
        return Optional.empty();
    }
}
