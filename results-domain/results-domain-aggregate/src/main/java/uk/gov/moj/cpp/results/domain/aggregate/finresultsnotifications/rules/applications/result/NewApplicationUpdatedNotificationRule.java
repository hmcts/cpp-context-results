package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.previousUpdateNotificationSent;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Rule to handle notifications for new applications that have been updated, for example adjourned.
 */
public class NewApplicationUpdatedNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isNewApplication()
                && !input.isValidApplicationTypeWithAllowedResultCode()
                && !previousUpdateNotificationSent(input.request(), input.prevApplicationResultsDetails(), input.prevApplicationOffenceResultsMap());
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final Map<UUID, String> offenceDateMap = input.offenceDateMap();
        final String ncesEmail = input.ncesEmail();
        final String writtenOffExists = input.isWrittenOffExists();
        final List<NewOffenceByResult> newResultByOffence = input.newOffenceResultsFromHearing();
        final String applicationResult = input.applicationResult();

        final Optional<OffenceResults> offenceForApplication = request.getOffenceResults().stream()
                .filter(offence -> NCESDecisionConstants.APPLICATION_TYPES.containsKey(offence.getApplicationType()))
                .findFirst();

        return offenceForApplication.map(offence -> {
            List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = getApplicationImpositionOffenceDetails(
                    request,
                    offenceDateMap,
                    input.prevOffenceResultsDetails(),
                    input.prevApplicationOffenceResultsMap());
            if (!impositionOffenceDetailsForApplication.isEmpty()) {
                return Optional.of(
                        markedAggregateSendEmailEventBuilder(ncesEmail, input.correlationItemList())
                                .buildMarkedAggregateWithoutOldsForSpecificCorrelationIdWithEmail(
                                        request,
                                        NCESDecisionConstants.APPLICATION_UPDATED_SUBJECT.get(offence.getApplicationType()),
                                        impositionOffenceDetailsForApplication,
                                        ncesEmail,
                                        writtenOffExists,
                                        input.originalDateOfOffenceList(),
                                        input.originalDateOfSentenceList(),
                                        newResultByOffence,
                                        applicationResult,
                                        null,
                                        null,
                                        input.prevApplicationResultsDetails()));
            } else {
                return null;
            }
        }).orElse(Optional.empty());
    }
}
