package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationResultChanged;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * This rule is responsible for determining if an email notification should be sent when there is an amendment to an application result and cloned offences
 * had financial imposition prior to the current application(this means application notifications would have sent out to NCES),
 * and there is a change in the application result.
 * @see <a href="https://tools.hmcts.net/jira/browse/DD-35053">CCT-2389:DD-35053</a>
 */
public class ApplicationStatusAmendmentNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() &&
                input.isAmendmentFlow() &&
                input.hasFinancialImpositionPriorToThisApplication() &&
                !input.hasAccountCorrelation() &&
                isApplicationResultChanged(input.request(), input.prevApplicationResultsDetails());
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final List<OffenceResults> offenceResults = request.getOffenceResults();
        final LinkedList<CorrelationItem> correlationItems = input.correlationItemList();
        final String ncesEmail = input.ncesEmail();
        final UUID currentApplicationId = request.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        final Optional<OffenceResults> offenceForApplication = offenceResults.stream()
                .filter(offence -> APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .filter(offence -> APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .findFirst();

        // `finToNonFin` true means A&R notification is also sent, then lets not send another notification.
        final boolean finToNonFin = isFineToNonFineApplicationAmendment(input, request, currentApplicationId) && !isFineToFineApplicationAmendment(input, request, currentApplicationId);

        if (offenceForApplication.isPresent() && !finToNonFin) {
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
