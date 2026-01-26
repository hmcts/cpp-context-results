package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalSjpReferredOffenceResultsApplication;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;


/**
 * This class implements a notification rule for sjp cases which moved to CC with reopen aplication in CC.
 */
public class SjpReferredReopenApplicationAcceptedNotificationRule extends AbstractCaseResultNotificationRule {

    public static final String REOPEN_APPLICATION_TYPE = "REOPEN";

    @Override
    public boolean appliesTo(final RuleInput input) {
        final Map<UUID, UUID> prevSjpReferralOffenceResultsDetails = input.prevSjpReferralOffenceResultsDetails();
        final Map<UUID,List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = input.prevApplicationOffenceResultsMap();
        return Boolean.FALSE.equals(input.request().getIsSJPHearing()) && !prevSjpReferralOffenceResultsDetails.isEmpty() &&
                input.request().getOffenceResults().stream()
                        .anyMatch(offenceResult -> isPrevSJPReopenApplication(offenceResult.getOffenceId(), prevSjpReferralOffenceResultsDetails, prevApplicationOffenceResultsMap));
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = input.request();

        final Map<UUID, UUID> prevSjpReferralOffenceResultsDetails = input.prevSjpReferralOffenceResultsDetails();
        final boolean allSjpOffenceHasFinancialResult = prevSjpReferralOffenceResultsDetails.entrySet().stream()
                .allMatch(sjpOffenceResult -> request.getOffenceResults().stream()
                        .filter(offenceResult -> offenceResult.getOffenceId().equals(sjpOffenceResult.getKey()))
                        .allMatch(offenceResult -> offenceResult.getIsFinancial() || StringUtils.startsWith(offenceResult.getImpositionOffenceDetails(), "OATS"))
                );
        if (allSjpOffenceHasFinancialResult) {
            final List<OffenceResults> sjpReferredOffences = request.getOffenceResults().stream()
                    .filter(offenceResult -> prevSjpReferralOffenceResultsDetails.keySet().contains(offenceResult.getOffenceId()))
                    .toList();

            final List<OffenceResultsDetails> originalOffenceResults = getOriginalSjpReferredOffenceResultsApplication(
                    input.prevOffenceResultsDetails(),
                    sjpReferredOffences);
            final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = originalOffenceResults.stream()
                    .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                    .distinct().toList();

            final List<NewOffenceByResult> newApplicationOffenceResults = getNewOffenceResultsApplication(
                    sjpReferredOffences,
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsMap()).stream()
                    .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap()))
                    .distinct().toList();


            return Optional.of(markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                    .buildMarkedAggregateGranted(request,
                            NCESDecisionConstants.APPLICATION_TO_REOPEN_GRANTED,
                            impositionOffenceDetailsForApplication,
                            input.ncesEmail(),
                            input.isWrittenOffExists(),
                            input.originalDateOfOffenceList(),
                            input.originalDateOfSentenceList(),
                            newApplicationOffenceResults,
                            buildNewApplicationResultsFromTrackRequest(sjpReferredOffences),
                            input.prevApplicationResultsDetails()));
        } else {
            return Optional.empty();
        }
    }

    private boolean isPrevSJPReopenApplication(final UUID offenceId, final Map<UUID, UUID> prevSjpReferralOffenceResultsDetails,
                                               final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
        return prevSjpReferralOffenceResultsDetails.containsKey(offenceId) &&
                prevApplicationOffenceResultsMap.get(prevSjpReferralOffenceResultsDetails.get(offenceId))
                        .stream().anyMatch(offenceResultsDetails -> REOPEN_APPLICATION_TYPE.equals(offenceResultsDetails.getApplicationType()));
    }
}
