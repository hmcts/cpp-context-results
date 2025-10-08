package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.shouldNotifyNCESForAppResultAmendment;
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
 * Rule for application amendments from financial to non-financial impositions.
 */
public class ApplicationAmendmentFinToNonFinAccWriteOffRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && input.isAmendmentFlow();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = filteredApplicationResults(input.request());

        final UUID currentApplicationId = request.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);
        // Get original imposition offence details from the aggregate.

        final List<ImpositionOffenceDetails> originalImpositionDetails = getOriginalOffenceResultsAppAmendment(input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails(), request.getOffenceResults())
                .stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                .distinct().toList();

        // Get imposition offence details for financial offences that are corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsFineToFine = getImpositionOffenceDetailsFineToFine(input, request, currentApplicationId);

        // Get imposition offence details for non-financial offences corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsFineToNonFine = getImpositionOffenceDetailsFineToNonFine(input, request, currentApplicationId);

        // Get imposition offence details for non-financial offences corresponding to original non-financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsNonFineToNonFine = getImpositionOffenceDetailsNonFineToNonFine(input, request, currentApplicationId);

        final Optional<OriginalApplicationResults> originalApplicationResults = getOriginalApplicationResults(request, input.prevApplicationResultsDetails());

        final NewApplicationResults newApplicationResults = buildNewApplicationResultsFromTrackRequest(request.getOffenceResults());
        final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsAppAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                .toList();

        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList());
        //has application amendments
        final boolean appResultsOnly = originalApplicationResults.isPresent() && shouldNotifyNCESForAppResultAmendment(request) && newOffenceResults.isEmpty();
        //has fp to nFP
        final boolean finToNonFin = !impositionOffenceDetailsFineToNonFine.isEmpty() && impositionOffenceDetailsFineToFine.isEmpty();
        //has nFP to nFP
        final boolean nonFinToNonFin = !impositionOffenceDetailsNonFineToNonFine.isEmpty();

        if ((appResultsOnly && !nonFinToNonFin) || finToNonFin) {
            return Optional.of(markedAggregateSendEmailEventBuilder
                    .buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                            NCESDecisionConstants.AMEND_AND_RESHARE,
                            input.correlationItemList().peekLast(),
                            originalImpositionDetails,
                            input.isWrittenOffExists(),
                            input.originalDateOfOffenceList(),
                            input.originalDateOfOffenceList(),
                            newOffenceResults,
                            input.applicationResult(),
                            originalApplicationResults.orElse(null),
                            newApplicationResults,
                            input.prevApplicationResultsDetails()));
        }

        return Optional.empty();
    }
}
