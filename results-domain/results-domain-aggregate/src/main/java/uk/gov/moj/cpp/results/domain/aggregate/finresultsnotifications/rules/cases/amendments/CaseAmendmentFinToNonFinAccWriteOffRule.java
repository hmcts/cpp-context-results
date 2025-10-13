package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsCaseAmendment;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalOffenceResultsCaseAmendment;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
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

/**
 * This class implements a notification rule for case amendments with financial imposition changes. If there are
 * financial imposition changes, it builds a notification event with the updated imposition details.
 */
public class CaseAmendmentFinToNonFinAccWriteOffRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isCaseAmendmentProcess() && !input.hasFinancialAmendments();
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

        //if offences has fin+nonfin and amendement happens to only non fine  - no correlation and no marked event required
        //f to nf - without olds
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
                            null,
                            input.prevApplicationResultsDetails()));
        }


        return Optional.empty();
    }
}
