package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Optional;

/**
 * This class implements a notification rule for cases with ACON results.
 */
public class CaseACONNotificationRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.hasCaseResult() && !input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForAcon = request.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap()))
                .toList();
        if (!impositionOffenceDetailsForAcon.isEmpty()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request,
                                    NCESDecisionConstants.ACON_EMAIL_SUBJECT,
                                    getCaseFinancialImpositionOffenceDetails(input, request),
                                    Boolean.FALSE)
            );
        }
        return Optional.empty();
    }
}
