package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildOriginalOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasSentenceVaried;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsAppAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsAppAmendment;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application amendment rule for financial to financial imposition changes.
 */
public class ApplicationAmendmentFinToFinAccWriteOffRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && input.isAmendmentFlow() && input.hasFinancialAmendments();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {

        final HearingFinancialResultRequest request = filteredApplicationResults(input.request());
        final UUID currentApplicationId = request.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);

        final boolean isMixedFinancialAmendment = isMixedFinancialApplicationAmendment(request.getOffenceResults(), input.prevApplicationOffenceResultsMap().get(currentApplicationId));

        // Check if there are valid financial amendments
        if (isFineToFineApplicationAmendment(input, request, currentApplicationId) || isMixedFinancialAmendment) {

            final List<ImpositionOffenceDetails> originalImpositionDetails = getOriginalOffenceResultsAppAmendment(input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails(), request.getOffenceResults())
                    .stream()
                    .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                    .distinct().toList();

            final Optional<OriginalApplicationResults> originalResultsByApplication = getOriginalApplicationResults(request, input.prevApplicationResultsDetails());

            final NewApplicationResults newApplicationResults = buildNewApplicationResultsFromTrackRequest(request.getOffenceResults());

            final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsAppAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails()).stream()
                    .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                    .toList();

            final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList());

            return Optional.of(markedAggregateSendEmailEventBuilder
                    .buildMarkedAggregateWithOlds(request,
                            hasSentenceVaried(newOffenceResults) ? buildOriginalOffenceResultForSV(originalImpositionDetails) : originalImpositionDetails,
                            input.applicationResult(),
                            hasSentenceVaried(newOffenceResults) ? buildNewOffenceResultForSV(newOffenceResults) : newOffenceResults,
                            originalResultsByApplication.orElse(null),
                            newApplicationResults,
                            NCESDecisionConstants.AMEND_AND_RESHARE,
                            input.prevApplicationResultsDetails()));
        }

        return Optional.empty();
    }
}
