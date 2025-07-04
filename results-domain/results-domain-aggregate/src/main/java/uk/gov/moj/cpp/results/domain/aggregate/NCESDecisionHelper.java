package uk.gov.moj.cpp.results.domain.aggregate;


import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ASV;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AW;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.SENTENCE_VARIED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.SV_SENTENCE_VARIED;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class NCESDecisionHelper {


    public static boolean hasSentenceVaried(final List<NewOffenceByResult> newResultByOffence) {
        return newResultByOffence.stream().anyMatch(newOffenceByResult ->
                newOffenceByResult.getDetails().contains(SV_SENTENCE_VARIED));
    }

    public static List<NewOffenceByResult> buildNewOffenceResultForSV(final List<NewOffenceByResult> newOffenceByResults) {
        return newOffenceByResults.stream()
                .map(newOffenceByResult -> {
                    String details = newOffenceByResult.getDetails();
                    String title = newOffenceByResult.getTitle();
                    if (details.contains(SV_SENTENCE_VARIED)) {
                        details = details.replace(SV_SENTENCE_VARIED, "").trim();
                        title = title + SENTENCE_VARIED;
                    }
                    return new NewOffenceByResult(details, newOffenceByResult.getOffenceDate(), newOffenceByResult.getOffenceId(), title);
                })
                .toList();
    }

    public static List<ImpositionOffenceDetails> buildOriginalOffenceResultForSV(final List<ImpositionOffenceDetails> originalOffenceResults) {
        return originalOffenceResults.stream()
                .map(originalOffenceByResult -> {
                    String details = originalOffenceByResult.getDetails();
                    String title = originalOffenceByResult.getTitle();
                    if (details.contains(SV_SENTENCE_VARIED)) {
                        details = details.replace(SV_SENTENCE_VARIED, "").trim();
                        title = title + SENTENCE_VARIED;
                    }
                    return new ImpositionOffenceDetails(details, originalOffenceByResult.getOffenceDate(), originalOffenceByResult.getOffenceId(), title);
                })
                .toList();
    }

    public static boolean shouldNotifyNCESForAppResultAmendment(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> Objects.nonNull(offence.getAmendmentDate()))
                .anyMatch(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()));
    }

    public static boolean shouldNotifyNCESForAppAppealResult(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .filter(offence -> Objects.isNull(offence.getAmendmentDate()))
                .anyMatch(offence -> asList(ASV, APA, AW, AASD).contains(offence.getResultCode()));
    }

    public static NewOffenceByResult buildNewImpositionOffenceDetailsFromRequest(final OffenceResults offencesFromRequest, final Map<UUID, String> offenceDateMap) {
        return NewOffenceByResult.newOffenceByResult()
                .withOffenceId(offencesFromRequest.getOffenceId())
                .withDetails(offencesFromRequest.getImpositionOffenceDetails())
                .withOffenceDate(offenceDateMap.get(offencesFromRequest.getOffenceId()))
                .withTitle(offencesFromRequest.getOffenceTitle())
                .build();
    }

    public static List<OffenceResultsDetails> buildApplicationResultsDetailsFromOffenceResults(List<OffenceResults> resultFromRequest) {
        List<OffenceResultsDetails> applicationResults = new ArrayList<>();
        resultFromRequest.forEach(result -> applicationResults.add(offenceResultsDetails().withApplicationTitle(result.getApplicationTitle())
                .withApplicationId(result.getApplicationId())
                .withResultCode(result.getResultCode())
                .withApplicationType(result.getApplicationType())
                .withApplicationResultType(result.getApplicationResultType())
                .build())
        );
        return applicationResults;
    }

    public static List<NewOffenceByResult> buildNewOffenceResultsFromTrackRequest(final List<OffenceResults> offenceResultsDetails, final Map<UUID, String> offenceDateMap) {
        List<NewOffenceByResult> newImpositionOffenceDetails;

        newImpositionOffenceDetails = offenceResultsDetails.stream()
                .filter(o -> isNull(o.getApplicationType()))
                .map(offenceResults -> buildNewImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap))
                .toList();

        if (newImpositionOffenceDetails.isEmpty()) {
            newImpositionOffenceDetails = offenceResultsDetails.stream()
                    .filter(o -> nonNull(o.getApplicationType()))
                    .filter(o -> nonNull(o.getIsParentFlag()) && (o.getIsParentFlag()))
                    .filter(o -> nonNull(o.getImpositionOffenceDetails()))
                    .map(offenceResults -> buildNewImpositionOffenceDetailsFromRequest(offenceResults, offenceDateMap)).distinct()
                    .toList();
        }
        return newImpositionOffenceDetails;
    }

    public static boolean hasNoCorrelationIdForAmendedApplication(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return isNull(hearingFinancialResultRequest.getAccountCorrelationId()) && (hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .anyMatch(offence -> nonNull(offence.getAmendmentDate())));
    }

}
