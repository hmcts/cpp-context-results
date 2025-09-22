package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildOriginalOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasSentenceVaried;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.isApplicationDenied;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.shouldNotifyNCESForAppResultAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsAppAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsAppAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getPreviousOffenceResultsDetails;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper;
import uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.AbstractApplicationResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Rule for handling NCES notifications for application amendments related to account write-offs.
 */
public class ApplicationAmendmentAccWriteOffNotificationRule extends AbstractApplicationResultNotificationRule {

    private static final Predicate<OffenceResults> isApplicationAmended = o -> nonNull(o.getApplicationType()) && nonNull(o.getAmendmentDate());

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && input.isAmendmentFlow();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = getFilteredApplicationResults(input.request());

        final UUID currentApplicationId = request.getOffenceResults().stream().map(OffenceResults::getApplicationId).filter(Objects::nonNull).findFirst().orElse(null);
        // Get original imposition offence details from the aggregate.

        final List<ImpositionOffenceDetails> originalImpositionDetails = getOriginalOffenceResultsAppAmendment(input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails(), request.getOffenceResults())
                .stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                .distinct().toList();

        // Get imposition offence details for financial offences that are corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsFineToFine = request.getOffenceResults().stream()
                .filter(isApplicationAmended)
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(), currentApplicationId, input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails()))
                        .map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap()))
                .toList();

        // Get imposition offence details for non-financial offences corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsFineToNonFine = request.getOffenceResults().stream()
                .filter(isApplicationAmended)
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(), currentApplicationId, input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails()))
                        .map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap()))
                .toList();

        // Get imposition offence details for non-financial offences corresponding to original non-financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsNonFineToNonFine = request.getOffenceResults().stream()
                .filter(isApplicationAmended)
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(getPreviousOffenceResultsDetails(offenceFromRequest.getOffenceId(), currentApplicationId, input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails()))
                        .map(OffenceResultsDetails::getIsFinancial)
                        .map(isFinancial -> !isFinancial)
                        .orElse(true))
                .map(offenceResults -> buildImpositionOffenceDetailsFromRequest(offenceResults, input.offenceDateMap()))
                .toList();

        // Get original application results from the aggregate.
        final Optional<OriginalApplicationResults> originalResultsByApplication = request.getOffenceResults().stream()
                .filter(result -> Objects.nonNull(result.getApplicationId()))
                .map(offenceFromRequest -> input.prevApplicationResultsDetails().get(offenceFromRequest.getApplicationId()))
                .filter(Objects::nonNull)
                .map(ApplicationNCESEventsHelper::buildOriginalApplicationResultsFromAggregate)
                .findFirst();

        final NewApplicationResults newApplicationResults = buildNewApplicationResultsFromTrackRequest(request.getOffenceResults());
        final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsAppAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                .toList();

        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList());

        //Application only result amendment
        if (originalResultsByApplication.isPresent() && shouldNotifyNCESForAppResultAmendment(request) && newOffenceResults.isEmpty() && impositionOffenceDetailsNonFineToNonFine.isEmpty()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder
                            .buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                                    NCESDecisionConstants.AMEND_AND_RESHARE,
                                    input.correlationItemList().peekLast(),
                                    originalImpositionDetails,
                                    input.isWrittenOffExists(),
                                    input.originalDateOfOffenceList(),
                                    input.originalDateOfSentenceList(),
                                    newOffenceResults,
                                    input.applicationResult(),
                                    originalResultsByApplication.get(),
                                    newApplicationResults));
        }

        //has fp to nFP
        if (!impositionOffenceDetailsFineToNonFine.isEmpty() && impositionOffenceDetailsFineToFine.isEmpty()) {
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
                            originalResultsByApplication.orElse(null),
                            newApplicationResults));
        }


        //fp to fp
        if (!impositionOffenceDetailsFineToFine.isEmpty()) {
            return Optional.of(markedAggregateSendEmailEventBuilder
                    .buildMarkedAggregateWithOlds(request,
                            hasSentenceVaried(newOffenceResults) ? buildOriginalOffenceResultForSV(originalImpositionDetails) : originalImpositionDetails,
                            input.applicationResult(),
                            hasSentenceVaried(newOffenceResults) ? buildNewOffenceResultForSV(newOffenceResults) : newOffenceResults,
                            originalResultsByApplication.orElse(null),
                            newApplicationResults,
                            NCESDecisionConstants.AMEND_AND_RESHARE));
        }

        return Optional.empty();
    }

    private static OffenceResultsDetails getPreviousOffenceResultsDetailsRm(final UUID offenceId, final UUID currentApplicationId,
                                                                          final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        if (prevApplicationOffenceResultsMap.containsKey(currentApplicationId)) {
            return prevApplicationOffenceResultsMap.get(currentApplicationId).stream().filter(ord -> ord.getOffenceId().equals(offenceId)).findFirst().orElse(null);
        }
        return null;
    }
}
