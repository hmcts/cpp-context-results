package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewReopenApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewStatdecApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasPreviousDeniedNotificationSent;
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
 * Rule to handle notifications for StatDec/Reopen that are denied.
 */
public class NewStatdecOrReopenAppDeniedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isNewApplication()
                && isNewStatdecApplicationDenied(input.request()) || isNewReopenApplicationDenied(input.request())
                && !hasPreviousDeniedNotificationSent(input.request(), input.prevApplicationResultsDetails(), input.prevApplicationOffenceResultsMap());
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
            final OffenceResults offence = offenceForApplication.get();
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

                return Optional.of(
                        markedAggregateSendEmailEventBuilder(ncesEmail, correlationItems)
                                .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(request,
                                        APPLICATION_SUBJECT.get(offence.getApplicationType()).get(offence.getResultCode()),
                                        impositionOffenceDetailsForApplication,
                                        ncesEmail,
                                        writtenOffExists,
                                        originalDateOfOffenceList,
                                        originalDateOfSentenceList,
                                        newApplicationOffenceResults,
                                        applicationResult,
                                        originalApplicationResults,
                                        null,
                                        input.prevApplicationResultsDetails()));
            }
        }
        return Optional.empty();
    }
}
