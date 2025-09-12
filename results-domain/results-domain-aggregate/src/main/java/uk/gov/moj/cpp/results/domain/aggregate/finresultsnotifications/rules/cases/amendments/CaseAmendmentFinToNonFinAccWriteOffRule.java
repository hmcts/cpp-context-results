package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.Objects.isNull;
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
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * This class implements a notification rule for case amendments with financial imposition changes. If there are
 * financial imposition changes, it builds a notification event with the updated imposition details.
 */
public class CaseAmendmentFinToNonFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    private static final Predicate<OffenceResults> isCaseAmended = o -> isNull(o.getApplicationType()) && nonNull(o.getAmendmentDate());

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = filteredCaseResults(input.request());

        final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails = input.prevOffenceResultsDetails();
        final Map<UUID, String> offenceDateMap = input.offenceDateMap();

        final List<ImpositionOffenceDetails> originalImpositions = getOriginalOffenceResultsCaseAmendment(input.prevOffenceResultsDetails(), request.getOffenceResults()).stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, offenceDateMap))
                .distinct().toList();
        final List<ImpositionOffenceDetails> impositionOffenceDetailsFinToFin = getImpositionOffenceDetailsFinToFin(request, prevOffenceResultsDetails, offenceDateMap);
        final List<ImpositionOffenceDetails> impositionOffenceDetailsFinToNonFin = getImpositionOffenceDetailsFinToNonFin(request, prevOffenceResultsDetails, offenceDateMap);

        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList());
        final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsCaseAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, offenceDateMap)).distinct()
                .toList();

        //newOffenceResults will be empty when no financial change from previous to new offence  - no marked event required
        //previous nonFine to new Fine - no marked event required
        if (newOffenceResults.isEmpty() || isNonFinToFinImposition(request, prevOffenceResultsDetails, offenceDateMap)) {
            return Optional.empty();
        }

        //f to f - without olds
        if (!impositionOffenceDetailsFinToNonFin.isEmpty() && impositionOffenceDetailsFinToFin.isEmpty()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                            AMEND_AND_RESHARE,
                            input.correlationItemList().peekLast(),
                            originalImpositions,
                            input.isWrittenOffExists(),
                            input.originalDateOfOffenceList(),
                            input.originalDateOfOffenceList(),
                            newOffenceResults,
                            input.applicationResult(),
                            null,
                            null));
        }


        return Optional.empty();
    }
}
