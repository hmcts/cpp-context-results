package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasSentenceVaried;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.shouldNotifyNCESForAppAppealResult;

import uk.gov.justice.core.courts.CorrelationIdHistoryItem;
import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Rule to handle notifications for new applications that have resulted.
 * This rule checks if the application is new, valid, and not already resulted(this could happen when application is adjourned
 */
public class NewApplicationResultedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isNewApplication() &&
                input.isValidApplicationTypeWithAllowedResultCode() &&
                !input.isApplicationAlreadyResulted();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final List<OffenceResults> offenceResults = request.getOffenceResults();
        final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItems = input.correlationIdHistoryItemList();
        final String ncesEMail = input.ncesEmail();

        final Optional<OffenceResults> offenceForApplication = offenceResults.stream()
                .filter(offence -> NCESDecisionConstants.APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .findFirst();

        return offenceForApplication.map(offence -> {
            final Map<UUID, String> offenceDateMap = input.offenceDateMap();

            List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = getApplicationImpositionOffenceDetails(
                    request,
                    offenceDateMap,
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsDetails());
            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                final List<NewOffenceByResult> newApplicationOffenceResults = buildNewOffenceResultsFromTrackRequest(request.getOffenceResults(), offenceDateMap);
                final OriginalApplicationResults originalApplicationResults = buildApplicationResultsFromTrackRequest(offenceResults);
                final String writtenOffExists = input.isWrittenOffExists();
                final String originalDateOfOffenceList = input.originalDateOfOffenceList();
                final String originalDateOfSentenceList = input.originalDateOfSentenceList();

                final String applicationResult = input.applicationResult();
                if (shouldNotifyNCESForAppAppealResult(request)) {
                    return processAppealResults(request,
                            writtenOffExists,
                            originalDateOfOffenceList,
                            originalDateOfSentenceList,
                            newApplicationOffenceResults,
                            applicationResult,
                            offence,
                            impositionOffenceDetailsForApplication,
                            originalApplicationResults,
                            input.ncesEmail(),
                            input.correlationIdHistoryItemList());
                } else {
                    return Optional.of(
                            markedAggregateSendEmailEventBuilder(ncesEMail, correlationIdHistoryItems)
                                    .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(request,
                                            NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                                            correlationIdHistoryItems.peekLast(),
                                            impositionOffenceDetailsForApplication,
                                            ncesEMail,
                                            writtenOffExists,
                                            originalDateOfOffenceList,
                                            originalDateOfSentenceList,
                                            newApplicationOffenceResults,
                                            applicationResult,
                                            originalApplicationResults,
                                            null));
                }
            } else {
                return Optional.<MarkedAggregateSendEmailWhenAccountReceived>empty();
            }
        }).orElse(Optional.empty());
    }

    @SuppressWarnings("java:S107")
    private Optional<MarkedAggregateSendEmailWhenAccountReceived> processAppealResults(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                                       final String isWrittenOffExists,
                                                                                       final String originalDateOfOffenceList,
                                                                                       final String originalDateOfSentenceList,
                                                                                       final List<NewOffenceByResult> newResultByOffenceList,
                                                                                       final String applicationResult,
                                                                                       final OffenceResults offence,
                                                                                       final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication,
                                                                                       final OriginalApplicationResults originalApplicationResults,
                                                                                       final String ncesEmail,
                                                                                       final LinkedList<CorrelationIdHistoryItem> correlationIdHistoryItemList) {
        if (hasSentenceVaried(newResultByOffenceList)) {
            return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithOlds(hearingFinancialResultRequest,
                    impositionOffenceDetailsForApplication, applicationResult, buildNewOffenceResultForSV(newResultByOffenceList),
                    originalApplicationResults, null,
                    NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode())));
        } else {
            return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationIdHistoryItemList).buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                    NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                    correlationIdHistoryItemList.peekLast(), impositionOffenceDetailsForApplication, ncesEmail, isWrittenOffExists, originalDateOfOffenceList,
                    originalDateOfSentenceList, newResultByOffenceList, applicationResult, originalApplicationResults, null));
        }
    }
}
