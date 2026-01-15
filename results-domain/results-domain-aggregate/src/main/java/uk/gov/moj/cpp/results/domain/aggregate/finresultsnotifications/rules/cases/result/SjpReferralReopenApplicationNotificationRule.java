package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result;

import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;


/**
 * This class implements a notification rule for sjp cases which moved to CC with reopen aplication in CC.
 */
public class SjpReferralReopenApplicationNotificationRule extends AbstractCaseResultNotificationRule {

    public static final String REOPEN_APPLICATION_TYPE = "REOPEN";

    @Override
    public boolean appliesTo(final RuleInput input) {
        final Map<UUID, OffenceResultsDetails> prevSjpReferralOffenceResultsDetails = input.prevSjpReferralOffenceResultsDetails();
        return Boolean.FALSE.equals(input.request().getIsSJPHearing()) && !prevSjpReferralOffenceResultsDetails.isEmpty() &&
                input.request().getOffenceResults().stream()
                        .anyMatch(offenceResult -> isOffenceHasReopened(offenceResult.getOffenceId(), prevSjpReferralOffenceResultsDetails));
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();

        final Map<UUID, OffenceResultsDetails> prevSjpReferralOffenceResultsDetails = input.prevSjpReferralOffenceResultsDetails();
        final boolean allSjpOffenceHasFinancialResult = prevSjpReferralOffenceResultsDetails.entrySet().stream()
                .allMatch(sjpOffenceResult -> request.getOffenceResults().stream()
                        .filter(offenceResult -> offenceResult.getOffenceId().equals(sjpOffenceResult.getKey()))
                        .allMatch(offenceResult -> offenceResult.getIsFinancial() || StringUtils.startsWith(offenceResult.getImpositionOffenceDetails(), "OATS"))
                );
        if (allSjpOffenceHasFinancialResult) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                            .buildMarkedAggregateWithoutOlds(request,
                                    NCESDecisionConstants.APPLICATION_TO_REOPEN_GRANTED,
                                    getAppFinancialImpositionOffenceDetails(input, request),
                                    Boolean.FALSE)
            );
        } else {
            return Optional.empty();
        }
    }

    private boolean isOffenceHasReopened(final UUID offenceId,
                                         final Map<UUID, OffenceResultsDetails> prevSjpReferralOffenceResultsDetails) {
        return prevSjpReferralOffenceResultsDetails.containsKey(offenceId) &&
                REOPEN_APPLICATION_TYPE.equals(prevSjpReferralOffenceResultsDetails.get(offenceId).getApplicationType());

    }

    private List<ImpositionOffenceDetails> getAppFinancialImpositionOffenceDetails(final RuleInput input, final HearingFinancialResultRequest request) {
        return input.prevSjpReferralOffenceResultsDetails().values().stream()
                .map(offenceResultsDetails -> buildImpositionOffenceDetailsFromAggregate(offenceResultsDetails, input.offenceDateMap())).distinct()
                .toList();
    }
}
