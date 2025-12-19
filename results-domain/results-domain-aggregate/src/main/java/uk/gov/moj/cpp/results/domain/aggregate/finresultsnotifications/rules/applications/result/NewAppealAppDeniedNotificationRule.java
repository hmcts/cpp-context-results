package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewAppealApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.previousDeniedNotificationSent;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.previousGrantedNotificationSent;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsApplication;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
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
 * Rule to handle notifications for APPEAL denied results that have been processed.
 * This rule specifically handles appeal results with sentence variation scenarios.
 */
public class NewAppealAppDeniedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isNewApplication()
                && isNewAppealApplicationDenied(input.request())
                && previousDeniedNotificationSent(input.request(), input.prevApplicationResultsDetails(), input.prevApplicationOffenceResultsMap());
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final List<OffenceResults> offenceResults = request.getOffenceResults();
        final LinkedList<CorrelationItem> correlationItems = input.correlationItemList();
        final String ncesEmail = input.ncesEmail();

        final Optional<OffenceResults> offenceForApplication = offenceResults.stream()
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .findFirst();

        if (offenceForApplication.isPresent()) {
            final String subject =  APPLICATION_SUBJECT.get(offenceForApplication.get().getApplicationType()).get(offenceForApplication.get().getResultCode());
            final Map<UUID, String> offenceDateMap = input.offenceDateMap();
            final List<OffenceResultsDetails> originalOffenceResults = getOriginalOffenceResultsApplication(
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsMap(),
                    request.getOffenceResults());

            final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = originalOffenceResults.stream()
                    .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, offenceDateMap))
                    .distinct().toList();

            final List<NewOffenceByResult> newApplicationOffenceResults = getNewOffenceResultsApplication(
                    request.getOffenceResults(),
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsMap()).stream()
                    .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, offenceDateMap))
                    .distinct().toList();

            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                final OriginalApplicationResults originalApplicationResults = buildApplicationResultsFromTrackRequest(offenceResults);
                final String writtenOffExists = input.isWrittenOffExists();
                final String originalDateOfOffenceList = input.originalDateOfOffenceList();
                final String originalDateOfSentenceList = input.originalDateOfSentenceList();
                final String applicationResult = input.applicationResult();

                return processAppealResults(request,
                        writtenOffExists,
                        originalDateOfOffenceList,
                        originalDateOfSentenceList,
                        newApplicationOffenceResults,
                        applicationResult,
                        subject,
                        impositionOffenceDetailsForApplication,
                        originalApplicationResults,
                        ncesEmail,
                        correlationItems,
                        input.prevApplicationResultsDetails());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("java:S107")
    private Optional<MarkedAggregateSendEmailWhenAccountReceived> processAppealResults(
            final HearingFinancialResultRequest hearingFinancialResultRequest,
            final String isWrittenOffExists,
            final String originalDateOfOffenceList,
            final String originalDateOfSentenceList,
            final List<NewOffenceByResult> newResultByOffenceList,
            final String applicationResult,
            final String subject,
            final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication,
            final OriginalApplicationResults originalApplicationResults,
            final String ncesEmail,
            final LinkedList<CorrelationItem> correlationItemList,
            final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        if (!newResultByOffenceList.isEmpty()) {
            return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationItemList)
                    .buildMarkedAggregateWithOlds(hearingFinancialResultRequest,
                            impositionOffenceDetailsForApplication,
                            applicationResult,
                            buildNewOffenceResultForSV(newResultByOffenceList),
                            originalApplicationResults,
                            null,
                            subject,
                            prevApplicationResultsDetails));
        } else {
            return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationItemList)
                    .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(hearingFinancialResultRequest,
                            subject,
                            impositionOffenceDetailsForApplication,
                            ncesEmail,
                            isWrittenOffExists,
                            originalDateOfOffenceList,
                            originalDateOfSentenceList,
                            newResultByOffenceList,
                            applicationResult,
                            originalApplicationResults,
                            null,
                            prevApplicationResultsDetails));
        }
    }
}
