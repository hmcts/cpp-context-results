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
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for Case-NonFine-NonFine scenarios.
 * This class contains all test cases related to cases where both offences are initially non-financial.
 */
class HearingFinancialResultsAggregateForCaseNonFineNonFineTest {

    /**
     * Test scenarios for Case-NonFine-NonFine multi-application scenarios.
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    public static Stream<Arguments> caseNonFineNonFineMultiApplicationScenarios() {
        return Stream.of(
                Arguments.of("Case-Non-fine > App1-Granted-> Fine > App2-Granted [STE77/28DI7135464 DD-39432]",
                        newScenario()
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/1_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/1_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Case-Non-fine-Non-fine>App1-Granted-Fine-Non-fine>App2-Granted-Fine-Fine [STE77/28DI1480609]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-NonFine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-NonFine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-NonFine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-NonFine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-NonFine-NonFine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine > App1-Granted-Fine-Fine [STE77/28DI3064249]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/single-application/App1-Granted-Fine-Fine/1_app_appeal_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine > Case-Amended-Fine-Fine > App1-Granted-> Fine-Fine > App2-Granted-> Fine-Fine [STE77/28DI7658121]",
                        newScenario()
                                .newStep(newResultTrackedStep("case amendment resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app_statdec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopem received", "json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app1_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/Case-Amended-Fine-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("Case NonFine > App1 Granted-Fine > App1 Amended Fine > App2 Granted Fine > App2 Declined > App3 received [STE77/28DI9473376/DD-39862]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/1_case_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/2_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/2_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/3_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/3_app_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/4_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested",
                                                "UnmarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/4_app_reopen_granted_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/5_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/5_app_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 reopen received", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/5_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine/App1-Granted-Fine/App1-Amended-Fine/App2-Granted-Fine/App2-Amended-Decline/5_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )

                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted-Fine-Fine [STE77/28DI3064249]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/single-application/App1-Granted-Fine-Fine/1_app_appeal_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted >Fine-Fine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted >Fine-Fine > App2-Granted > Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-Fine-NonFine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted >Fine-Fine > App2-Granted > NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted >Fine-Fine > App2-Granted > NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Granted-NonFine-NonFine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Non-fine-Non-fine>App1-Granted-Fine-NonFine>App2-Granted-Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-NonFine>App1-Granted-Fine-NonFine>App2-Granted-NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-NonFine>App1-Granted-Fine-NonFine>App2-Granted-NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted-NonFine-Fine > App2-Granted-Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-Fine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted-NonFine-Fine > App2-Granted-Fine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-Fine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted-NonFine-Fine > App2-Granted-NonFine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-NonFine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Granted-NonFine-Fine > App2-Granted-NonFine-NonFine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-NonFine-NonFine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Declined > App2-Granted-Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Declined/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Declined/App2-Granted-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-NonFine-NonFine > App1-Declined > App2-Declined [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Declined/App2-Declined/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Declined/App2-Declined/3_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                )
        );
    }

    /**
     * Test scenarios for Case-NonFine-NonFine application amendment scenarios.
     */
    public static Stream<Arguments> caseNonFineNonFineApplicationAmendmentScenarios() {
        return Stream.of(
                Arguments.of("Case-Inactive-Non-fine > App1-Granted-Fine > App1-Amended-Fine-App2-Granted-Fine [STE77/28DI3954642 DD-37270]",
                        newScenario()
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/1_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app1 statdec amend resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/2_app1_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app2_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app2_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )

                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app_appeal_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app3_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app4 appeal received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/5_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app4 appeal amendment resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/5_app_appeal_results_tracked.json", emptyAccountInfo())
                                )
                ),
                Arguments.of("Case-Non-fine > App1-Granted-> Fine > App2-Granted [STE77/28DI7135464 DD-39432]",
                        newScenario()
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/1_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/1_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-Fine-Fine/App2-Recieved/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Case-Non-fine-Non-fine > App1-Granted > Non-fine-Fine > App2-Granted > Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/multi-applications/App1-Granted-NonFine-Fine/App2-Granted-Fine-Fine/2_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Inactive-Non-fine > App1-Granted-Fine > App1-Amended-Fine-App2-Granted-Fine [STE77/28DI3954642 DD-37270]",
                        newScenario()
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/1_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/1_app_stat_dec_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app1 statdec amend resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/2_app1_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app2_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/3_app2_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )

                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app_appeal_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/4_app3_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app4 appeal received", "json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/5_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app4 appeal amendment resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-NonFine-NonFine/App1-Amended-Fine-App2-Granted-Fine/5_app_appeal_results_tracked.json", emptyAccountInfo())
                                )
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseNonFineNonFineMultiApplicationScenarios")
    void shouldCreateNCESNotificationForCaseNonFineNonFineMultiApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseNonFineNonFineApplicationAmendmentScenarios")
    void shouldCreateNCESNotificationForCaseNonFineNonFineApplicationAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }
}