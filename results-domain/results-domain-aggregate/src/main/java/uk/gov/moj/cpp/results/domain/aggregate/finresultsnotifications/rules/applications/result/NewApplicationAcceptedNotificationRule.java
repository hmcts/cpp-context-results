package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewAppealReopenApplicationGranted;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.previousGrantedNotificationSent;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isNewStatdecApplicationGranted;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_SUBJECT;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPLICATION_TYPES;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.isNcesNotificationForNewApplication;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Rule to handle notifications for approved result codes of STATDEC/REOPEN/APPEAL that have been processed.
 * This rule checks if the application is a new application with a valid application type and result code
 */
public class NewApplicationAcceptedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return (isNewStatdecApplicationGranted(input.request()) || isNewAppealReopenApplicationGranted(input.request())) &&
                previousGrantedNotificationSent(input.request(), input.prevApplicationResultsDetails(), input.prevApplicationOffenceResultsMap());
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
                final String writtenOffExists = input.isWrittenOffExists();
                final String originalDateOfOffenceList = input.originalDateOfOffenceList();
                final String originalDateOfSentenceList = input.originalDateOfSentenceList();

                if (isResultedWithOffences(request.getOffenceResults())
                        && isNcesNotificationForNewApplication(request.getOffenceResults(),
                                input.prevOffenceResultsDetails(),
                                input.prevApplicationOffenceResultsMap())) {
                    return Optional.of(markedAggregateSendEmailEventBuilder(ncesEmail, correlationItems)
                            .buildMarkedAggregateGranted(request,
                                    getSubject(request, offence),
                                    impositionOffenceDetailsForApplication,
                                    ncesEmail,
                                    writtenOffExists,
                                    originalDateOfOffenceList,
                                    originalDateOfSentenceList,
                                    newApplicationOffenceResults,
                                    buildNewApplicationResultsFromTrackRequest(offenceResults),
                                    input.prevApplicationResultsDetails()));
                }
            }
        }
        return Optional.empty();
    }

    private static String getSubject(final HearingFinancialResultRequest request, final OffenceResults offence) {
        return request.getOffenceResults().stream()
                .filter(applicationResults -> applicationResults.getApplicationId() != null && applicationResults.getResultCode() != null)
                .map(r -> APPLICATION_SUBJECT
                        .get(offence.getApplicationType())
                        .get(r.getResultCode()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean isResultedWithOffences(final List<OffenceResults> offenceResults) {
        return nonNull(offenceResults) && offenceResults.stream()
                .filter(result -> nonNull(result.getApplicationType()) && nonNull(result.getImpositionOffenceDetails()))
                .anyMatch(result -> Boolean.TRUE.equals(result.getIsParentFlag()));
    }
}
