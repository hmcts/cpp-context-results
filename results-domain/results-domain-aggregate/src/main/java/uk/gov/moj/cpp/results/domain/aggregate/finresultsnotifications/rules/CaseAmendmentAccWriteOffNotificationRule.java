package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewOffenceResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AMEND_AND_RESHARE;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * This class implements a notification rule for case amendments with financial imposition changes. If there are
 * financial imposition changes, it builds a notification event with the updated imposition details.
 */
public class CaseAmendmentAccWriteOffNotificationRule extends AbstractCaseResultNotificationRule {

    @Override
    public boolean appliesTo(RuleInput input) {
        return input.isCaseAmendmentProcess();
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = getFilteredCaseResults(input.request());

        final Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails = input.prevOffenceResultsDetails();
        final Map<UUID, String> offenceDateMap = input.offenceDateMap();

        final List<ImpositionOffenceDetails> originalImpositions = request.getOffenceResults().stream()
                .filter(result -> isNull(result.getApplicationType()))
                .map(offenceFromRequest -> prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId()))
                .filter(Objects::nonNull)
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromAggregate(offenceResults, offenceDateMap)).distinct()
                .toList();

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForFinancial = request.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(OffenceResults::getIsFinancial)
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .collect(toList());

        final List<ImpositionOffenceDetails> impositionOffenceDetailsForNonFinancial = request.getOffenceResults().stream()
                .filter(o -> isNull(o.getApplicationType()))
                .filter(o -> Objects.nonNull(o.getAmendmentDate()))
                .filter(o -> !o.getIsFinancial())
                .filter(offenceFromRequest -> ofNullable(prevOffenceResultsDetails.get(offenceFromRequest.getOffenceId())).map(OffenceResultsDetails::getIsFinancial).orElse(false))
                .map(offenceResults -> this.buildImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();

        final MarkedAggregateSendEmailEventBuilder markedAggregateSendEmailEventBuilder = markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationIdHistoryItemList());
        final List<NewOffenceByResult> newOffenceResults = buildNewOffenceResultsFromTrackRequest(input.request().getOffenceResults(), input.offenceDateMap());

        if (!impositionOffenceDetailsForNonFinancial.isEmpty()) {
            if (!impositionOffenceDetailsForFinancial.isEmpty()) {
                impositionOffenceDetailsForFinancial.addAll(impositionOffenceDetailsForNonFinancial);
            } else {
                return Optional.of(
                        markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithoutOldsForSpecificCorrelationId(request,
                                AMEND_AND_RESHARE,
                                input.correlationIdHistoryItemList().peekLast(),
                                originalImpositions,
                                input.isWrittenOffExists(),
                                input.originalDateOfOffenceList(),
                                input.originalDateOfOffenceList(),
                                newOffenceResults,
                                input.applicationResult(),
                                null,
                                null));
            }
        }

        if (!impositionOffenceDetailsForFinancial.isEmpty()) {
            return Optional.of(
                    markedAggregateSendEmailEventBuilder.buildMarkedAggregateWithOlds(request,
                            originalImpositions,
                            input.applicationResult(),
                            newOffenceResults,
                            null,
                            null,
                            AMEND_AND_RESHARE));
        }
        return Optional.empty();
    }
}
