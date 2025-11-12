package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases;

import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.GobAccountHelper.hasPreviousCorrelation;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsCaseAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsCaseAmendment;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class implements a notification rule for case amendments with financial imposition changes. If there are
 * financial imposition changes to non financial changes, it builds a notification event with the updated imposition details.
 */
public class CaseFinToNonFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {

        final HearingFinancialResultRequest request = filteredCaseResults(input.request());
        final List<UUID> offenceIdList = request.getOffenceResults().stream()
                .map(OffenceResults::getOffenceId)
                .collect(Collectors.toList());

        final Boolean hasPreviousAccountCorrelation = hasPreviousCorrelation(input.correlationItemList(),
                request.getAccountCorrelationId(), offenceIdList, input.prevApplicationResultsDetails(), request.getHearingId());

        return !input.hasAccountCorrelation() && hasPreviousAccountCorrelation;
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {

        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        // Check if there are financial to non-financial amendments that require processing
        final boolean hasFineToNonFineAmendments = isFineToNonFineCaseAmendments(request, input.prevOffenceResultsDetails(), input.offenceDateMap());
        
        // Generate notification if there are financial to non-financial amendments
        // This rule handles cases where financial offences are being changed to non-financial
        if (hasFineToNonFineAmendments) {

            // Get original imposition offence details from the aggregate when needed
            final List<ImpositionOffenceDetails> originalImpositions = getOriginalOffenceResultsCaseAmendment(input.prevOffenceResultsDetails(), request.getOffenceResults()).stream()
                    .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                    .distinct().toList();

            final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList());

            final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsCaseAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails()).stream()
                    .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                    .toList();
            
            return Optional.of(
                    markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                            AMEND_AND_RESHARE,
                            originalImpositions,
                            input.isWrittenOffExists(),
                            input.originalDateOfOffenceList(),
                            input.originalDateOfOffenceList(),
                            newOffenceResults,
                            input.applicationResult(),
                            null,
                            null,
                            input.prevApplicationResultsDetails()));
        }

        //if offences has fin+nonfin and amendement happens to only non fine  - no correlation and no marked event required
        return Optional.empty();
    }
}
