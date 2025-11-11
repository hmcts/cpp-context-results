package uk.gov.moj.cpp.results.domain.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.accountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.emptyAccountInfo;
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
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for Case-NonFine-Fine scenarios.
 * This class contains all test cases related to cases where the first offence is non-financial and the second is financial.
 */
class HearingFinancialResultsAggregateForCaseNonFineFineTest {

    /**
     * Test scenarios for Case-NonFine-Fine multi-application scenarios.
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    public static Stream<Arguments> caseNonFineFineMultiApplicationScenarios() {
        return Stream.of(
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-Fine > App2-Granted > Fine-Fine [STE77/28DI3358350]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_stat_decl_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-Fine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated"
                                                , "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_stat_decl_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-Fine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_stat_decl_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-Fine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/2_app1_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked",  "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_stat_decl_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Fine-Fine [STE77/28DI4199070]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-NonFine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-NonFine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > Fine-NonFine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked","NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-Fine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-Fine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-Fine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-Fine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-NonFine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-NonFine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-NonFine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/3_app_appeal_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-Fine > App1-Granted > NonFine-NonFine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-Fine/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/3_app_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                )

        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseNonFineFineMultiApplicationScenarios")
    void shouldCreateNCESNotificationForCaseNonFineFineMultiApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }
}