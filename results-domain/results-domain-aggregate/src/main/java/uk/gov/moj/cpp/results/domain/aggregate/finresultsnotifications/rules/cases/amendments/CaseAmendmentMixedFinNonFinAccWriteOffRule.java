package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsCaseAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsCaseAmendment;

import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Optional;

public class CaseAmendmentMixedFinNonFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(final RuleInput input) {
        return input.isCaseAmendment() && input.hasFinancialTransitionInTheCase();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {

        final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsCaseAmendment(input.request().getOffenceResults(),
                input.prevOffenceResultsDetails()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap())).distinct()
                .toList();

        final List<ImpositionOffenceDetails> originalImpositions = getOriginalOffenceResultsCaseAmendment(input.prevOffenceResultsDetails(), input.request().getOffenceResults())
                .stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                .distinct().toList();

        return Optional.of(
                markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                        .buildMarkedAggregateWithOlds(
                                input.request(),
                                originalImpositions,
                                input.applicationResult(),
                                newOffenceResults,
                                null,
                                null,
                                AMEND_AND_RESHARE,
                                input.prevApplicationResultsDetails()));
    }
}
