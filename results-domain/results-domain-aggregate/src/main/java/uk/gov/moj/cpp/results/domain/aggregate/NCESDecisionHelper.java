package uk.gov.moj.cpp.results.domain.aggregate;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AACA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AACD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ACSD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPEAL;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ASV;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AW;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.DISM;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.G;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.RFSD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ROPENED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.SENTENCE_VARIED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STDEC;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.SV_SENTENCE_VARIED;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WDRN;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.FINAL;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.INTERMEDIARY;

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

    private static final List<String> application_accepted_result_codes = asList(G, STDEC, ROPENED, AACA, AASA);
    private static final List<String> appeal_reopen_application_accepted_result_codes = asList(G, ROPENED, AACA, AASA);
    private static final List<String> statdec_application_accepted_result_codes = asList(G, STDEC);
    private static final List<String> stadec_reoopen_denied_result_codes = asList(DISM, RFSD, WDRN);
    private static final List<String> appeal_denied_result_codes = asList(ASV, APA, AW, AASD, RFSD, DISM, AACD, ACSD, WDRN);
    private static final List<String> application_denied_result_codes = asList(APA, AW, AASD, RFSD, DISM, AACD, ACSD, WDRN);


    public static boolean hasSentenceVaried(final List<NewOffenceByResult> newOffenceByResults) {
        return newOffenceByResults.stream().anyMatch(newOffenceByResult ->
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
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .anyMatch(offence -> appeal_denied_result_codes.contains(offence.getResultCode()));
    }

    public static boolean isNewApplicationGranted(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .anyMatch(offence -> application_accepted_result_codes.contains(offence.getResultCode()));
    }

    public static boolean isNewStatdecReopenApplicationDenied(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest
                .getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationId()))
                .filter(offence -> nonNull(offence.getApplicationType()) && STAT_DEC.equalsIgnoreCase(offence.getApplicationType()) || REOPEN.equalsIgnoreCase(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .anyMatch(offence -> stadec_reoopen_denied_result_codes.contains(offence.getResultCode()));
    }


    /**
     * Update notification would have sent out if the application previously resulted with ADJ with all cloned offences ADJ
     */
    public static boolean previousUpdateNotificationSent(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                         final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails,
                                                         final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        if (isNull(prevApplicationResultsDetails) || prevApplicationResultsDetails.isEmpty()
                || isNull(prevApplicationOffenceResultsMap) || prevApplicationOffenceResultsMap.isEmpty()) {
            return false;
        }

        final OffenceResults offenceResult = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationId()))
                .filter(result -> isNull(result.getAmendmentDate()))
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(result -> NCESDecisionConstants.APPLICATION_SUBJECT.containsKey(result.getApplicationType()))
                .findFirst().orElse(null);

        if (nonNull(offenceResult) && nonNull(offenceResult.getApplicationId())) {
            if (STAT_DEC.equals(offenceResult.getApplicationType())) {
                return isApplicationAdjourned(offenceResult.getApplicationId(), prevApplicationResultsDetails)
                        && isApplicationOffencesAdjourned(offenceResult.getApplicationId(), prevApplicationOffenceResultsMap);
            } else {
                return isApplicationAlreadyGranted(prevApplicationResultsDetails, offenceResult.getApplicationId())
                        && isApplicationOffencesAdjourned(offenceResult.getApplicationId(), prevApplicationOffenceResultsMap);
            }
        }

        return false;
    }

    private static boolean isApplicationAdjourned(final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        final List<OffenceResultsDetails> prevAppResultList = prevApplicationResultsDetails.get(applicationId);

        final List<String> resultCategoryList = isNotEmpty(prevAppResultList)
                ? prevAppResultList.stream()
                .map(OffenceResultsDetails::getApplicationResultsCategory)
                .filter(Objects::nonNull).toList()
                : emptyList();

        return resultCategoryList.stream().noneMatch(category -> category.equals(FINAL.name()))
                && resultCategoryList.stream().anyMatch(category -> category.equals(INTERMEDIARY.name()));
    }

    private static boolean isApplicationOffencesAdjourned(final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        final List<OffenceResultsDetails> prevAppOffenceResultList = prevApplicationOffenceResultsMap.get(applicationId);

        final List<String> resultCategoryList = isNotEmpty(prevAppOffenceResultList)
                ? prevAppOffenceResultList.stream()
                .map(OffenceResultsDetails::getOffenceResultsCategory)
                .filter(Objects::nonNull).toList()
                : emptyList();

        return resultCategoryList.stream().noneMatch(category -> category.equals(FINAL.name()))
                && resultCategoryList.stream().anyMatch(category -> category.equals(INTERMEDIARY.name()));
    }

    private static boolean isApplicationOffencesFinal(final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        final List<OffenceResultsDetails> prevAppOffenceResultList = prevApplicationOffenceResultsMap.get(applicationId);

        return isNotEmpty(prevAppOffenceResultList) && prevAppOffenceResultList.stream()
                .map(OffenceResultsDetails::getOffenceResultsCategory)
                .filter(Objects::nonNull)
                .allMatch(category -> category.equals(FINAL.name()));
    }

    /**
     * Overloaded check which also checks previous application results to avoid sending duplicate application notifications
     * when a notification for the same application has already been generated from aggregate state.
     */
    public static boolean isNewApplicationGranted(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                  final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails,
                                                  final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        final boolean incomingRequestIndicatesGrant = isNewApplicationGranted(hearingFinancialResultRequest);
        if (!incomingRequestIndicatesGrant) {
            return false;
        }

        if (isNull(prevApplicationResultsDetails) || prevApplicationResultsDetails.isEmpty()) {
            return true;
        }

        final OffenceResults offenceResult = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationId()))
                .filter(result -> isNull(result.getAmendmentDate()))
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(result -> NCESDecisionConstants.APPLICATION_SUBJECT.get(result.getApplicationType()).containsKey(result.getResultCode()))
                .findFirst().orElse(null);

        if (nonNull(offenceResult) && nonNull(offenceResult.getApplicationId())) {
            if (STAT_DEC.equals(offenceResult.getApplicationType())) {
                return !(isApplicationAlreadyGranted(prevApplicationResultsDetails, offenceResult.getApplicationId())
                        && isApplicationOffencesAdjourned(offenceResult.getApplicationId(), prevApplicationOffenceResultsMap));
            } else {
                return !(isApplicationAlreadyGranted(prevApplicationResultsDetails, offenceResult.getApplicationId())
                        && isApplicationOffencesFinal(offenceResult.getApplicationId(), prevApplicationOffenceResultsMap));
            }
        }
        return false;
    }

    private static boolean isApplicationAlreadyGranted(final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails, final UUID applicationId) {

        final List<OffenceResultsDetails> prevAppResultList = prevApplicationResultsDetails.get(applicationId);

        return isNotEmpty(prevAppResultList) && prevAppResultList.stream()
                .map(OffenceResultsDetails::getResultCode)
                .filter(Objects::nonNull)
                .anyMatch(application_accepted_result_codes::contains);
    }

    public static boolean isApplicationDenied(final List<OffenceResultsDetails> offenceResultsDetails) {
        return isNotEmpty(offenceResultsDetails) && offenceResultsDetails.stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .anyMatch(offence -> application_denied_result_codes.contains(offence.getResultCode()));
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
                .withApplicationResultsCategory(result.getApplicationResultsCategory())
                .withOffenceResultsCategory(result.getOffenceResultsCategory())
                .withCreatedTime(ZonedDateTime.now())
                .build())
        );
        return applicationResults;
    }

    public static boolean isNewAppealReopenApplicationOffencesAreConcluded(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final var filtered = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType())
                        && (APPEAL.equals(offence.getApplicationType()) || REOPEN.equals(offence.getApplicationType())))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode())) //???Narayan
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .filter(offence -> appeal_reopen_application_accepted_result_codes.contains(offence.getResultCode()))
                .toList();

        return !filtered.isEmpty() && filtered.stream()
                .allMatch(offence -> FINAL.name().equals(offence.getOffenceResultsCategory()));
    }


    public static boolean isNewStatdecApplicationConcluded(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()) && (STAT_DEC.equals(offence.getApplicationType())))
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .filter(offence -> statdec_application_accepted_result_codes.contains(offence.getResultCode())).toList();
        return !offenceResults.isEmpty() &&
                offenceResults.stream().allMatch(offenceResult -> FINAL.name().equals(offenceResult.getApplicationResultsCategory()));
    }

        public static boolean isNewAppealReopenApplicationOffencesAreAdjourned(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType())
                        && (APPEAL.equals(offence.getApplicationType()) || REOPEN.equals(offence.getApplicationType())))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .filter(offence -> appeal_reopen_application_accepted_result_codes.contains(offence.getResultCode()))
                .toList();

        return !offenceResults.isEmpty() &&
                offenceResults.stream().allMatch(offenceResult -> INTERMEDIARY.name().equals(offenceResult.getOffenceResultsCategory()));
    }

    public static boolean isNewStatdecApplicationAdjourned(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()) && (STAT_DEC.equals(offence.getApplicationType())))
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .toList();

        return !offenceResults.isEmpty() && offenceResults.stream()
                .allMatch(offence -> INTERMEDIARY.name().equals(offence.getApplicationResultsCategory()));
    }

}
