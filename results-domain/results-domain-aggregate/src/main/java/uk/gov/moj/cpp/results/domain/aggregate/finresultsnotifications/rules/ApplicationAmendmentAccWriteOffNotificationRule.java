package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromAggregate;
import static uk.gov.moj.cpp.results.domain.aggregate.ImpositionOffenceDetailsBuilder.buildImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildOriginalOffenceResultForSV;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.hasSentenceVaried;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.shouldNotifyNCESForAppResultAmendment;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper;
import uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewApplicationResults;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;
import uk.gov.moj.cpp.results.domain.event.OriginalApplicationResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rule for handling NCES notifications for application amendments related to account write-offs.
 */
public class ApplicationAmendmentAccWriteOffNotificationRule extends AbstractApplicationResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.hasValidApplicationType() && input.isAmendmentFlow();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = getFilteredApplicationResults(input.request());

        // Get original imposition offence details from the aggregate.
        final List<ImpositionOffenceDetails> originalImpositionDetails = request.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationType()))
                .map(offenceFromRequest -> input.prevApplicationOffenceResultsDetails().get(offenceFromRequest.getOffenceId()))
                .filter(Objects::nonNull)
                .map(offenceResults -> buildImpositionOffenceDetailsFromAggregate(offenceResults, input.offenceDateMap())).distinct()
                .toList();

        // Get imposition offence details for financial offences that are corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial = new ArrayList<>();
        for (OffenceResults results : request.getOffenceResults()) {
            if (nonNull(results.getApplicationType()) && results.getIsFinancial() &&
                    nonNull(results.getAmendmentDate()) && ofNullable(input.prevApplicationOffenceResultsDetails().get(results.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false)) {
                ImpositionOffenceDetails impositionOffenceDetails = buildImpositionOffenceDetailsFromRequest(results, input.offenceDateMap());
                impositionOffenceDetailsForFinancial.add(impositionOffenceDetails);
            }
        }

        // Get imposition offence details for non-financial offences corresponding to original financial offences
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial = request.getOffenceResults().stream()
                .filter(o -> nonNull(o.getApplicationType()))
                .filter(o -> !o.getIsFinancial())
                .filter(o -> Objects.nonNull(o.getAmendmentDate()))
                .filter(offenceFromRequest -> ofNullable(input.prevApplicationOffenceResultsDetails().get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
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
        final List<NewOffenceByResult> newOffenceResults = buildNewOffenceResultsFromTrackRequest(input.request().getOffenceResults(), input.offenceDateMap());
        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationIdHistoryItemList());
        if (originalResultsByApplication.isPresent() &&
                impositionOffenceDetailsForNonFinancial.isEmpty() &&
                impositionOffenceDetailsForFinancial.isEmpty() &&
                shouldNotifyNCESForAppResultAmendment(request)) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder
                            .buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                                    NCESDecisionConstants.AMEND_AND_RESHARE,
                                    input.correlationIdHistoryItemList().peekLast(),
                                    originalImpositionDetails,
                                    input.isWrittenOffExists(),
                                    input.originalDateOfOffenceList(),
                                    input.originalDateOfSentenceList(),
                                    newOffenceResults,
                                    input.applicationResult(),
                                    originalResultsByApplication.orElse(null),
                                    newApplicationResults));
        }

        if (!impositionOffenceDetailsForNonFinancial.isEmpty()) {
            if (!impositionOffenceDetailsForFinancial.isEmpty()) {
                impositionOffenceDetailsForFinancial.addAll(impositionOffenceDetailsForNonFinancial);
            } else {
                return Optional.of(markedAggregateSendEmailEventBuilder
                        .buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                                NCESDecisionConstants.AMEND_AND_RESHARE,
                                input.correlationIdHistoryItemList().peekLast(),
                                originalImpositionDetails,
                                input.isWrittenOffExists(),
                                input.originalDateOfOffenceList(),
                                input.originalDateOfOffenceList(),
                                newOffenceResults,
                                input.applicationResult(),
                                originalResultsByApplication.orElse(null),
                                newApplicationResults));
            }
        }

        if (!impositionOffenceDetailsForFinancial.isEmpty()) {
            if (hasSentenceVaried(newOffenceResults)) {
                return Optional.of(markedAggregateSendEmailEventBuilder
                        .buildMarkedAggregateWithOlds(request,
                                buildOriginalOffenceResultForSV(originalImpositionDetails),
                                input.applicationResult(),
                                buildNewOffenceResultForSV(newOffenceResults),
                                originalResultsByApplication.orElse(null),
                                newApplicationResults,
                                NCESDecisionConstants.AMEND_AND_RESHARE));
            } else {
                return Optional.of(markedAggregateSendEmailEventBuilder
                        .buildMarkedAggregateWithOlds(request,
                                originalImpositionDetails,
                                input.applicationResult(),
                                newOffenceResults,
                                originalResultsByApplication.orElse(null),
                                newApplicationResults,
                                NCESDecisionConstants.AMEND_AND_RESHARE));
            }
        }

        return Optional.empty();
    }
}
