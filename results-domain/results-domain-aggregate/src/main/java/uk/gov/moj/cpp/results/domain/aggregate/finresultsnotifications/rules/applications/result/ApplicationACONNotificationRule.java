package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Optional;

/**
 * This class implements a notification rule for applications with ACON results.
 */
public class ApplicationACONNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && !input.isAmendmentFlow();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = filteredApplicationResults(input.request());

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForACON = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()) &&
                        Boolean.TRUE.equals(o.getIsParentFlag()) &&
                        nonNull(o.getImpositionOffenceDetails()) && o.getImpositionOffenceDetails().contains(ACON))
                .filter(OffenceResults::getIsFinancial)
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap())).distinct()
                .toList();
        if (!impositionOffenceDetailsForACON.isEmpty()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request,
                                    NCESDecisionConstants.ACON_EMAIL_SUBJECT,
                                    impositionOffenceDetailsForACON,
                                    Boolean.FALSE));
        }
        return Optional.empty();
    }
}
