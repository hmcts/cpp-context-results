package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

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
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Rule to handle notifications for case amendments with financial to financial imposition changes.
 */
public class CaseAmendmentFinToFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        if (input.isCaseAmendment() && input.hasFinancialAmendments()) {
            final boolean hasTransitionedToFinancial = isOverallFinancialToFinancialAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails());
            if (hasTransitionedToFinancial) {
                return true;
            }

            final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsCaseAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails()).stream()
                    .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                    .toList();
            //newOffenceResults will be empty when no financial change from previous to new offence  - no marked event required
            //previous nonFine to new Fine - no marked event required
            if (newOffenceResults.isEmpty() || isNonFinToFinImposition(request, input.prevOffenceResultsDetails())) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {

        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsCaseAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                .toList();

        final List<ImpositionOffenceDetails> originalImpositions = getOriginalOffenceResultsCaseAmendment(input.prevOffenceResultsDetails(), request.getOffenceResults())
                .stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                .distinct().toList();

        return Optional.of(
                markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                        .buildMarkedAggregateWithOlds(
                                request,
                                originalImpositions,
                                input.applicationResult(),
                                newOffenceResults,
                                null,
                                null,
                                AMEND_AND_RESHARE,
                                input.prevApplicationResultsDetails()));

    }

    private boolean isOverallFinancialToFinancialAmendment(final List<OffenceResults> offenceResults, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetailsMap) {
        return offenceResults.stream().anyMatch(o -> TRUE.equals(o.getIsFinancial()))
                && nonNull(prevOffenceResultsDetailsMap) && prevOffenceResultsDetailsMap.values().stream().anyMatch(o -> TRUE.equals(o.getIsFinancial()));
    }

}
