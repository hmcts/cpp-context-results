package uk.gov.moj.cpp.results.domain.aggregate;


import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AACA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPEAL;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ASV;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AW;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.DISM;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.G;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.RFSD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ROPENED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.SENTENCE_VARIED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STDEC;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.SV_SENTENCE_VARIED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WDRN;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.event.ImpositionOffenceDetails;
import uk.gov.moj.cpp.results.domain.event.NewOffenceByResult;

import java.time.ZonedDateTime;
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

    public static boolean shouldNotifyNCESForAppResultAmendment(final HearingFinancialResultRequest request) {
        return request.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> nonNull(offence.getAmendmentDate()))
                .anyMatch(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()));
    }

    public static boolean isNewAppealApplicationDenied(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()) && APPEAL.equalsIgnoreCase(offence.getApplicationType()))
                .filter(offence -> nonNull(offence.getApplicationId()) && NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .filter(offence -> Objects.isNull(offence.getAmendmentDate()))
                .anyMatch(offence -> asList(ASV, APA, AW, AASD, RFSD, DISM).contains(offence.getResultCode()));
    }

    public static boolean isNewApplicationGranted(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .filter(offence -> Objects.isNull(offence.getAmendmentDate()))
                .anyMatch(offence -> asList(G, STDEC, ROPENED, AACA, AASA).contains(offence.getResultCode()));
    }

    public static boolean isApplicationDenied(final List<OffenceResultsDetails> offenceResultsDetails) {
        return isNotEmpty(offenceResultsDetails) && offenceResultsDetails.stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> !APPEAL.equalsIgnoreCase(offence.getApplicationType()))
                .anyMatch(offence -> asList(RFSD, WDRN, DISM).contains(offence.getResultCode()));
    }

    public static boolean isAppealApplicationWithNoOffenceResults(
            final UUID currentApplicationId,
            final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap,
            final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        final List<OffenceResultsDetails> offenceResultsDetails = prevApplicationResultsDetails.get(currentApplicationId);

        final List<OffenceResultsDetails> offenceResultsOnTheApplication = prevApplicationOffenceResultsMap.get(currentApplicationId);
        return isNotEmpty(offenceResultsDetails) && offenceResultsDetails.stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .anyMatch(offence -> APPEAL.equalsIgnoreCase(offence.getApplicationType()))
                && (isNull(offenceResultsOnTheApplication) || offenceResultsOnTheApplication.isEmpty());
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
                .withCreatedTime(ZonedDateTime.now())
                .build())
        );
        return applicationResults;
    }

}
