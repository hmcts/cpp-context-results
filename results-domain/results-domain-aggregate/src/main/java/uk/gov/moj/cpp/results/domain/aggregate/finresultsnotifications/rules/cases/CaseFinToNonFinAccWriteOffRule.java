package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsCaseAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsCaseAmendment;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * This class implements a notification rule for case amendments with financial imposition changes. If there are
 * financial imposition changes to non financial changes, it builds a notification event with the updated imposition details.
 */
public class CaseFinToNonFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {

        final HearingFinancialResultRequest request = filteredCaseResults(input.request());
        final boolean hasOverallFinancialToNonFinancial = isOverallFinancialToNonFinancialAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails(), request.getHearingId());
        return !input.hasAccountCorrelation() && hasOverallFinancialToNonFinancial;
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {

        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        final boolean hasFineToNonFineAmendments = isFineToNonFineCaseAmendments(request, input.prevOffenceResultsDetails(), input.offenceDateMap());

        if (hasFineToNonFineAmendments) {
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
        return Optional.empty();
    }

    private boolean isOverallFinancialToNonFinancialAmendment(final List<OffenceResults> offenceResults, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetailsMap, final UUID hearingId) {

        return offenceResults.stream()
                .allMatch(offenceResult -> !TRUE.equals(offenceResult.getIsFinancial()))
                && prevOffenceResultsDetailsMap.values().stream()
                .filter(prevOffenceResult -> nonNull(prevOffenceResult) &&
                        nonNull(prevOffenceResult.getHearingId())
                        && prevOffenceResult.getHearingId().equals(hearingId))
                .anyMatch(prev -> TRUE.equals(prev.getIsFinancial()));
    }
}
