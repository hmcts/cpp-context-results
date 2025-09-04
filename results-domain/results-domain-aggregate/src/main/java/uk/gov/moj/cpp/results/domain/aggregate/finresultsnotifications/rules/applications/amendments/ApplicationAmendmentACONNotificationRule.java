package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACON;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rule for handling NCES notifications for application amendments related to account consolidation result codes.
 */
public class ApplicationAmendmentACONNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && input.isAmendmentFlow();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = getFilteredApplicationResults(input.request());

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForACON = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> Boolean.TRUE.equals(o.getIsParentFlag()) && o.getIsFinancial()
                        && nonNull(o.getImpositionOffenceDetails()))
                .filter(offence -> ACON.equals(offence.getResultCode()))
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
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
