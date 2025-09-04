package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
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
public class CaseAmendmentAccWriteOffNotificationRule extends AbstractCaseResultNotificationRule {

    private static final Predicate<OffenceResults> isCaseAmended = o -> isNull(o.getApplicationType()) && nonNull(o.getAmendmentDate());

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = getFilteredCaseResults(input.request());

        final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails = input.prevOffenceResultsDetails();
        final Map<UUID, String> offenceDateMap = input.offenceDateMap();

        final List<ImpositionOffenceDetails> originalImpositions = getOriginalOffenceResultsCaseAmendment(input.prevOffenceResultsDetails(), request.getOffenceResults()).stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, offenceDateMap))
                .distinct().toList();

        final List<ImpositionOffenceDetails> impositionOffenceDetailsFineToFine = request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();

        final List<ImpositionOffenceDetails> impositionOffenceDetailsFineToNonFine = request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();

        final List<ImpositionOffenceDetails> impositionOffenceDetailsNonFineToFine = request.getOffenceResults().stream()
                .filter(isCaseAmended)
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(ofr -> !TRUE.equals(ofr.getIsFinancial())).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();

        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList());
        final List<NewOffenceByResult> newOffenceResults = getNewOffenceResultsCaseAmendment(request.getOffenceResults(), input.prevOffenceResultsDetails()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, offenceDateMap)).distinct()
                .toList();

        //newOffenceResults will be empty when no financial change from previous to new offence  - no marked event required
        //previous nonFine to new Fine - no marked event required
        if (newOffenceResults.isEmpty() || !impositionOffenceDetailsNonFineToFine.isEmpty()) {
            return Optional.empty();
        } else {
            //f to f - without olds
            if (!impositionOffenceDetailsFineToNonFine.isEmpty() && impositionOffenceDetailsFineToFine.isEmpty()) {
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

            if (!impositionOffenceDetailsFineToFine.isEmpty()) {
                //f to nf   - with olds
                return Optional.of(
                        markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithOlds(request,
                                originalImpositions,
                                input.applicationResult(),
                                newOffenceResults,
                                null,
                                null,
                                AMEND_AND_RESHARE));
            }
        }

        return Optional.empty();
    }
}
