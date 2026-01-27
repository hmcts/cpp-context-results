package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result;

import static uk.gov.moj.cpp.results.domain.aggregate.ApplicationNCESEventsHelper.buildNewApplicationResultsFromTrackRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.MarkedAggregateSendEmailEventBuilder.markedAggregateSendEmailEventBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.buildNewImpositionOffenceDetailsFromRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getNewOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver.getOriginalSjpReferredOffenceResultsApplication;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.FINAL;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.AbstractCaseResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.utils.ApplicationMetadata;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


/**
 * This class implements a notification rule for sjp cases which moved to CC with reopen aplication in CC.
 */
public class SjpReferredReopenApplicationAcceptedNotificationRule extends AbstractCaseResultNotificationRule {

    public static final String REOPEN_APPLICATION_TYPE = "REOPEN";

    @Override
    public boolean appliesTo(final RuleInput input) {
        final Map<UUID, ApplicationMetadata> prevSjpApplicationOffences = input.prevSjpApplicationOffences();
        final List<OffenceResults> offenceResults = input.request().getOffenceResults();
        return Boolean.FALSE.equals(input.request().getIsSJPHearing()) && !prevSjpApplicationOffences.isEmpty() &&
                !input.isCaseAmendment() &&
                isAllSjpApplicationOffencesInRequestAreFinalAndReopen(prevSjpApplicationOffences, offenceResults);
    }

    @Override
    public Optional<MarkedAggregateSendEmailWhenAccountReceived> apply(final RuleInput input) {
        final HearingFinancialResultRequest request = input.request();
        final Map<UUID, ApplicationMetadata> prevSjpReferralOffenceResultsDetails = input.prevSjpApplicationOffences();
        final List<OffenceResults> sjpReferredOffences = request.getOffenceResults().stream()
                .filter(offenceResult -> prevSjpReferralOffenceResultsDetails.containsKey(offenceResult.getOffenceId()))
                .toList();

        final List<OffenceResultsDetails> originalOffenceResults = getOriginalSjpReferredOffenceResultsApplication(
                input.prevOffenceResultsDetails(),
                sjpReferredOffences);
        final List<ImpositionOffenceDetails> impositionOffenceDetailsForApplication = originalOffenceResults.stream()
                .map(oor -> buildImpositionOffenceDetailsFromAggregate(oor, input.offenceDateMap()))
                .distinct().toList();

        final List<NewOffenceByResult> newApplicationOffenceResults = getNewOffenceResultsApplication(
                sjpReferredOffences,
                input.prevOffenceResultsDetails(),
                input.prevApplicationOffenceResultsMap()).stream()
                .map(nor -> buildNewImpositionOffenceDetailsFromRequest(nor, input.offenceDateMap()))
                .distinct().toList();

        return Optional.of(markedAggregateSendEmailEventBuilder(input.ncesEmail(), input.correlationItemList())
                .buildMarkedAggregateGranted(request,
                        NCESDecisionConstants.APPLICATION_TO_REOPEN_GRANTED,
                        impositionOffenceDetailsForApplication,
                        input.ncesEmail(),
                        input.isWrittenOffExists(),
                        input.originalDateOfOffenceList(),
                        input.originalDateOfSentenceList(),
                        newApplicationOffenceResults,
                        buildNewApplicationResultsFromTrackRequest(sjpReferredOffences),
                        input.prevApplicationResultsDetails()));
    }

    private static boolean isAllSjpApplicationOffencesInRequestAreFinalAndReopen(final Map<UUID, ApplicationMetadata> prevSjpApplicationOffences, final List<OffenceResults> offenceResults) {
        final List<OffenceResults> sjpOffenceResults = offenceResults.stream()
                .filter(result -> prevSjpApplicationOffences.containsKey(result.getOffenceId()))
                .toList();
        return !sjpOffenceResults.isEmpty() && sjpOffenceResults.stream()
                .allMatch(result -> FINAL.name().equals(result.getOffenceResultsCategory()) &&
                        REOPEN_APPLICATION_TYPE.equals(prevSjpApplicationOffences.get(result.getOffenceId()).applicationType()));
    }

}
