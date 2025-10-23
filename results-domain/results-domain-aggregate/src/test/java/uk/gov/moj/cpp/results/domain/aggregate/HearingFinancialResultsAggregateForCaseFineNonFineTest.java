package uk.gov.moj.cpp.results.domain.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.accountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Comparison.comparison;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.NcesEmailForNewApplicationStep.newNcesEmailForNewApplicationStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.ResultTrackedStep.newResultTrackedStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario.newScenario;

import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for Case-Fine-NonFine scenarios.
 * This class contains all test cases related to cases where the first offence is financial and the second is non-financial.
 */
class HearingFinancialResultsAggregateForCaseFineNonFineTest {

    /**
     * Test scenarios for Case-Fine-NonFine multi-application scenarios.
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    public static Stream<Arguments> caseFineNonFineMultiApplicationScenarios() {
        return Stream.of(
                Arguments.of("Case Fine-Non-fine > App1 STATDEC Granted > Non-fine-Fine",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin,nonfin-case-statdec-nonfin,fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app statdec")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin,nonfin-case-statdec-nonfin,fin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/fin,nonfin-case-statdec-nonfin,fin/application_granted_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Fine-Fine > App2-Granted > Fine [STE77/28DI2179703]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Fine-Fine > App2-Granted > Fine [STE77/28DI3216769]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("TBD-Case-Fine-Non-fine > App1-Granted > Fine-Non-fine > App2-Granted > Fine-Fine [STE77/28DI7582313]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > Fine-Fine [STE77/28DI3216769]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > Fine-NonFine [STE77/28DI3216769]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > NonFine-Fine [STE77/28DI3216769]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > NonFine-NonFine [STE77/28DI3216769]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Non-fine > App2-Granted > Fine-Fine [STE77/28DI7582313]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Non-fine > App2-Granted > Fine-NonFine [STE77/28DI7582313]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Non-fine > App2-Granted > NonFine-Fine [STE77/28DI7582313]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Non-fine-Non-fine > App2-Granted > NonFine-NonFine [STE77/28DI7582313]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Fine-Non-fine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-NonFine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-NonFine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-NonFine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-Fine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))

                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-Fine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))

                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-Fine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))

                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-NonFine > App1-Granted > Fine-Fine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))

                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                )
        );
    }

    /**
     * Test scenarios for Case-Fine-NonFine case amendment scenarios.
     */
    public static Stream<Arguments> caseFineNonFineCaseAmendmentScenarios() {
        return Stream.of(
                Arguments.of("Case-Fine-Non-fine > Case-amended-o2-Fine > App1-Granted-Fine-Fine > App2-Granted-Fine-Fine [STE77/28DI8919760]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_case_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/4_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                )
        );
    }

    /**
     * Test scenarios for Case-Fine-NonFine single application scenarios.
     */
    public static Stream<Arguments> caseFineNonFineSingleApplicationScenarios() {
        return Stream.of(
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Fine-Fine [STE77/28DI3216769]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/1_account_consolidated_nces_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseFineNonFineMultiApplicationScenarios")
    void shouldCreateNCESNotificationForCaseFineNonFineMultiApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseFineNonFineCaseAmendmentScenarios")
    void shouldCreateNCESNotificationForCaseFineNonFineCaseAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseFineNonFineSingleApplicationScenarios")
    void shouldCreateNCESNotificationForCaseFineNonFineSingleApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }
}