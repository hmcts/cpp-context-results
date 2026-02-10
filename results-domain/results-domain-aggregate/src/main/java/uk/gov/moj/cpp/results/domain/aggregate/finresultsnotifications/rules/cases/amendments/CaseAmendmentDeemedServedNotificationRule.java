package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
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
        return input.hasCaseResult() && input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        if (hasDeemedServedAmendmentOffences(request) && input.isFinancial()) {
            final boolean includeOlds = isNull(request.getAccountCorrelationId());
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED,
                                    getCaseFinancialImpositionOffenceDetails(input, request),
                                    includeOlds)
            );
        }
        if (hasDeemedServedRemovedOffences(request, input.prevOffenceResultsDetails())) {
            final List<ImpositionOffenceDetails> impositionOffenceDetailsDeemedServedRemoved = request.getOffenceResults().stream()
                    .filter(or -> !or.getIsDeemedServed() && isPrevOffenceResultDeemedServed(or.getOffenceId(), input.prevOffenceResultsDetails()))
                    .filter(or -> Objects.nonNull(or.getAmendmentDate()))
                    .map(or -> this.buildImpositionOffenceDetailsFromRequest(or, input.offenceDateMap())).distinct()
                    .toList();

            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request, WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED,
                                    impositionOffenceDetailsDeemedServedRemoved,
                                    Boolean.TRUE)
            );
        }
        return Optional.empty();
    }

}
