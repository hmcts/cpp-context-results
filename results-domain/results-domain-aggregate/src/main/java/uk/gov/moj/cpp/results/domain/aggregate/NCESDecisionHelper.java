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
import java.util.Optional;
import java.util.UUID;

public class NCESDecisionHelper {

    private static final List<String> appeal_reopen_application_accepted_result_codes = asList(G, ROPENED, AACA, AASA);
    private static final List<String> statdec_application_accepted_result_codes = asList(G, STDEC);
    private static final List<String> stadec_reopen_denied_result_codes = asList(DISM, RFSD, WDRN);
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
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(APPEAL),
                appeal_denied_result_codes);
        return isFinalResultsOnApplicationOrOffences(offenceResults);
    }

    public static boolean isNewReopenApplicationDenied(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(REOPEN),
                stadec_reopen_denied_result_codes);
        return isFinalResultsOnApplicationOrOffences(offenceResults);
    }

    public static boolean isNewStatdecApplicationDenied(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(STAT_DEC, REOPEN),
                stadec_reopen_denied_result_codes);
        return !offenceResults.isEmpty() && offenceResults.stream().allMatch(offence -> FINAL.name().equals(offence.getApplicationResultsCategory()));
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
        final OffenceResults offenceResult = getOffenceResultForApplication(hearingFinancialResultRequest);
        if (nonNull(offenceResult) && nonNull(offenceResult.getApplicationId())) {
            if (STAT_DEC.equals(offenceResult.getApplicationType())) {
                return isApplicationAdjourned(offenceResult.getApplicationId(), prevApplicationResultsDetails);
            } else {
                return isApplicationOffencesAdjourned(offenceResult.getApplicationId(), prevApplicationOffenceResultsMap);
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

    private static boolean areApplicationOffenceResultsAlreadyFinalised(final UUID applicationId, final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {
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
    public static boolean isPreviousGrantedNotificationSent(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                            final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails,
                                                            final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap) {

        if (isNull(prevApplicationResultsDetails) || prevApplicationResultsDetails.isEmpty()) {
            return false;
        }
        final OffenceResults offenceResult = getOffenceResultForApplication(hearingFinancialResultRequest);
        if (nonNull(offenceResult) && nonNull(offenceResult.getApplicationId())) {
            if (STAT_DEC.equals(offenceResult.getApplicationType())) {
                return areAllApplicationResultsAlreadyFinalised(prevApplicationResultsDetails, offenceResult.getApplicationId());
            } else {
                return areApplicationOffenceResultsAlreadyFinalised(offenceResult.getApplicationId(), prevApplicationOffenceResultsMap);
            }
        }
        return false;
    }

    /**
     * Overloaded check which also checks previous application results to avoid sending duplicate application notifications
     * when a notification for the same application has already been generated from aggregate state.
     */
    public static boolean isPreviousDeniedNotificationSent(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                           final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails) {

        if (isNull(prevApplicationResultsDetails) || prevApplicationResultsDetails.isEmpty()) {
            return false;
        }
        final OffenceResults offenceResult = getOffenceResultForApplication(hearingFinancialResultRequest);
        return areAllApplicationResultsAlreadyFinalised(prevApplicationResultsDetails, offenceResult.getApplicationId());
    }

    public static boolean isApplicationDenied(final List<OffenceResultsDetails> offenceResultsDetails) {
        return isNotEmpty(offenceResultsDetails) && offenceResultsDetails.stream()
                .filter(offence -> nonNull(offence.getApplicationType()))
                .filter(offence -> NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()).containsKey(offence.getResultCode()))
                .anyMatch(offence -> nonNull(offence.getApplicationId()) && application_denied_result_codes.contains(offence.getResultCode()));
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

    public static boolean isNewAppealOrReopenApplicationGranted(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(APPEAL, REOPEN),
                appeal_reopen_application_accepted_result_codes);
        return !offenceResults.isEmpty() && offenceResults.stream()
                .allMatch(offence -> FINAL.name().equals(offence.getOffenceResultsCategory()));
    }

    public static boolean isNewStatdecApplicationGranted(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(STAT_DEC),
                statdec_application_accepted_result_codes);
        return !offenceResults.isEmpty() && offenceResults.stream()
                .allMatch(offence -> FINAL.name().equals(offence.getApplicationResultsCategory()));
    }

    public static boolean isNewAppealOrReopenApplicationOffencesAreAdjourned(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(APPEAL, REOPEN),
                appeal_reopen_application_accepted_result_codes);
        return !offenceResults.isEmpty() && offenceResults.stream()
                .anyMatch(offence -> INTERMEDIARY.name().equals(offence.getOffenceResultsCategory()));
    }

    public static boolean isNewStatdecApplicationAdjourned(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        final List<OffenceResults> offenceResults = getFilteredOffenceResults(hearingFinancialResultRequest, asList(STAT_DEC),
                null);
        return !offenceResults.isEmpty() && offenceResults.stream()
                .allMatch(offence -> INTERMEDIARY.name().equals(offence.getApplicationResultsCategory()));
    }

    /**
     * Filters offence results based on application type, result codes, and offence state.
     * - Offence must have a valid application type from the provided list and must be mapped in APPLICATION_SUBJECT
     * - When resultCodes provided: check if resultCode exists in APPLICATION_SUBJECT for application type (application-level filtering)
     * - When resultCodes is null: just check APPLICATION_SUBJECT contains application type (offence-level filtering)
     */
    private static List<OffenceResults> getFilteredOffenceResults(final HearingFinancialResultRequest hearingFinancialResultRequest,
                                                                  final List<String> applicationTypes,
                                                                  final List<String> resultCodes) {
        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(offence -> nonNull(offence.getApplicationType()) && applicationTypes.contains(offence.getApplicationType()))
                .filter(offence -> {
                    if (nonNull(resultCodes)) {
                        final boolean isResultCodeInSubject = Optional.ofNullable(NCESDecisionConstants.APPLICATION_SUBJECT.get(offence.getApplicationType()))
                                .map(m -> m.containsKey(offence.getResultCode()))
                                .orElse(false);
                        return isResultCodeInSubject
                                && nonNull(offence.getApplicationId()) 
                                && resultCodes.contains(offence.getResultCode());
                    } else {
                        return NCESDecisionConstants.APPLICATION_SUBJECT.containsKey(offence.getApplicationType());
                    }
                })
                .filter(offence -> isNull(offence.getAmendmentDate()))
                .toList();
    }

    /**
     * Extracts the first OffenceResult from the request that matches common criteria for application notifications.
     * Automatically checks if result code exists in APPLICATION_SUBJECT (preferred), or falls back to checking
     * if application type exists in APPLICATION_SUBJECT if result code is not available or invalid.
     */
    private static OffenceResults getOffenceResultForApplication(final HearingFinancialResultRequest hearingFinancialResultRequest) {
        return hearingFinancialResultRequest.getOffenceResults().stream()
                .filter(result -> nonNull(result.getApplicationId()))
                .filter(result -> isNull(result.getAmendmentDate()))
                .filter(result -> nonNull(result.getApplicationType()))
                .filter(result -> {
                    if (nonNull(result.getResultCode())) {
                        final Map<String, String> subjectMap = NCESDecisionConstants.APPLICATION_SUBJECT.get(result.getApplicationType());
                        if (nonNull(subjectMap) && subjectMap.containsKey(result.getResultCode())) {
                            return true;
                        }
                    }
                    return NCESDecisionConstants.APPLICATION_SUBJECT.containsKey(result.getApplicationType());
                }).findFirst().orElse(null);
    }

    private static boolean areAllApplicationResultsAlreadyFinalised(final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails, final UUID applicationId) {
        final List<OffenceResultsDetails> prevAppResultList = prevApplicationResultsDetails.get(applicationId);
        return isNotEmpty(prevAppResultList) && prevAppResultList.stream()
                .map(OffenceResultsDetails::getApplicationResultsCategory)
                .filter(Objects::nonNull)
                .allMatch(category -> category.equals(FINAL.name()));
    }

    /**
     * This check is to cover https://tools.hmcts.net/jira/browse/DD-35053 AC2,3A
     */
    private static boolean isFinalResultsOnApplicationOrOffences(final List<OffenceResults> offenceResults) {
        if (offenceResults.isEmpty()) {
            return false;
        }

        final boolean hasOffenceResultsCategory = offenceResults.stream()
                .anyMatch(offence -> nonNull(offence.getOffenceResultsCategory()));

        if (hasOffenceResultsCategory) {
            return offenceResults.stream()
                    .filter(offence -> nonNull(offence.getOffenceResultsCategory()))
                    .allMatch(offence -> FINAL.name().equals(offence.getOffenceResultsCategory()));
        } else {
            return offenceResults.stream()
                    .allMatch(offence -> FINAL.name().equals(offence.getApplicationResultsCategory()));
        }
    }

}
