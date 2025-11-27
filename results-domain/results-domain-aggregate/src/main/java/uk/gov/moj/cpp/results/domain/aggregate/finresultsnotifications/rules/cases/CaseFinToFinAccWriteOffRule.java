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
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * This class implements a notification rule for case amendments with financial imposition changes. If there are
 * financial to financial, it builds a notification event with the updated imposition details.
 * <p>
 * Rule to handle notifications for
 * 1. Case -> offence amendments with financial to financial changes
 * 2. Case -> Partial offences result in financial penalties first,
 * and the remaining offences are processed later without altering the financial penalties that were already applied.
 */
public class CaseFinToFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        final boolean hasOverallFinancialToFinancial = isOverallFinancialToFinancialAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails(), request.getHearingId());

        return input.hasAccountCorrelation() && hasOverallFinancialToFinancial;
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

    private boolean isOverallFinancialToFinancialAmendment(final List<OffenceResults> offenceResults, final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetailsMap, final UUID hearingId) {

        final Boolean hasFinancial = offenceResults.stream()
                .anyMatch(offenceResult -> TRUE.equals(offenceResult.getIsFinancial()));

        if(!hasFinancial) return false;

        final Boolean hasPreviousFinancials = offenceResults.stream()
                .anyMatch(offenceResult -> {
                    final OffenceResultsDetails prevOffence = prevOffenceResultsDetailsMap.get(offenceResult.getOffenceId());
                    return  nonNull(prevOffence) &&
                            nonNull(prevOffence.getHearingId()) &&
                            prevOffence.getHearingId().equals(hearingId) &&
                            TRUE.equals(prevOffence.getIsFinancial());
                });

        return hasFinancial && hasPreviousFinancials;
    }
}
