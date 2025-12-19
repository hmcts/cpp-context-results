package uk.gov.moj.cpp.results.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.NCESDecisionHelper.previousUpdateNotificationSent;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.FINAL;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.ResultCategoryType.INTERMEDIARY;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class NCESDecisionHelperTest {

    @Test
    public void givenFinancialResultRequest_whenNoPreviousApplicationResults_previousUpdateNotificationSentReturnsFalse() {
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(emptyList()).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = emptyMap();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(randomUUID(), List.of(offenceResultsDetails().build()));

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(false));
    }

    @Test
    public void givenFinancialResultRequest_whenNoPreviousApplicationOffenceResults_previousUpdateNotificationSentReturnsFalse() {
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(emptyList()).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(randomUUID(), List.of(offenceResultsDetails().build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = emptyMap();

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(false));
    }

    @Test
    public void givenFinancialResultRequest_whenNoPreviousApplicationResultsForTheApplication_previousUpdateNotificationSentReturnsFalse() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(STAT_DEC).build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(randomUUID(), List.of(offenceResultsDetails().withApplicationId(randomUUID()).build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(randomUUID(), List.of(offenceResultsDetails().withApplicationId(randomUUID()).build()));

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(false));
    }

    @Test
    public void givenFinancialResultRequestForStatdec_whenPreviousApplicationResultedGranted_andPreviousApplicationOffencesResultedAdj_previousUpdateNotificationSentReturnsFalse() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(STAT_DEC).build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withApplicationResultsCategory(FINAL.name()).build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withOffenceResultsCategory(INTERMEDIARY.name()).build()));

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(false));
    }

    @Test
    public void givenFinancialResultRequestForStatdec_whenPreviousApplicationResultedAdj_andPreviousApplicationOffencesResultedAdj_previousUpdateNotificationSentReturnsTrue() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(STAT_DEC).build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withApplicationResultsCategory(INTERMEDIARY.name()).build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withOffenceResultsCategory(INTERMEDIARY.name()).build()));

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(true));
    }

    @Test
    public void givenFinancialResultRequestForReopen_whenPreviousApplicationResultedGranted_andPreviousApplicationOffencesResultedAdj_previousUpdateNotificationSentReturnsTrue() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(REOPEN).build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withResultCode("G").withApplicationResultsCategory(FINAL.name()).build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withOffenceResultsCategory(INTERMEDIARY.name()).build()));

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(true));
    }

    @Test
    public void givenFinancialResultRequestForReopen_whenPreviousApplicationResultedAdj_andPreviousApplicationOffencesResultedAdj_previousUpdateNotificationSentReturnsFalse() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(REOPEN).build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withApplicationResultsCategory(INTERMEDIARY.name()).build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withOffenceResultsCategory(INTERMEDIARY.name()).build()));

        final boolean previousUpdateNotificationSent = previousUpdateNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(false));
    }

    @Test
    public void givenFinancialResultRequestForStatdec_whenPreviousApplicationResultedGranted_andPreviousApplicationOffencesResultedAdj_isNewApplicationGrantedFalse() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(STAT_DEC).withResultCode("G").build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withResultCode("G").build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withOffenceResultsCategory(INTERMEDIARY.name()).build()));

        final boolean previousUpdateNotificationSent = NCESDecisionHelper.previousNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(false));
    }

    @Test
    public void givenFinancialResultRequestForStatdec_whenPreviousApplicationResultedGranted_andPreviousApplicationOffencesResultedFine_isNewApplicationGrantedFalse() {
        final UUID applicationId = randomUUID();
        final HearingFinancialResultRequest hearingFinancialResultRequest = hearingFinancialResultRequest().withOffenceResults(List.of(offenceResults().withApplicationId(applicationId).withApplicationType(STAT_DEC).withResultCode("G").build())).build();
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationResultsDetails = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withResultCode("G").build()));
        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap = Map.of(applicationId, List.of(offenceResultsDetails().withApplicationId(applicationId).withOffenceResultsCategory(FINAL.name()).build()));

        final boolean previousUpdateNotificationSent = NCESDecisionHelper.previousNotificationSent(hearingFinancialResultRequest, prevApplicationResultsDetails, prevApplicationOffenceResultsMap);
        assertThat(previousUpdateNotificationSent, is(true));
    }

}