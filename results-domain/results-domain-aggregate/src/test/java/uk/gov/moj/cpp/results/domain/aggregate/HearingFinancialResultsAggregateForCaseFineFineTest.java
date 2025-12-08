package uk.gov.moj.cpp.results.domain.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.accountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.emptyAccountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Comparison.comparison;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.CompositeStep.newCompositeStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.NcesEmailForNewApplicationStep.newNcesEmailForNewApplicationStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.ResultTrackedStep.newResultTrackedStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario.newScenario;

import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.CompositeStep;
import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for Case-Fine-Fine scenarios.
 * This class contains all test cases related to cases where both offences are initially financial.
 */
class HearingFinancialResultsAggregateForCaseFineFineTest {

    /**
     * Test scenarios for Case-Fine-Fine multi-application scenarios.
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    public static Stream<Arguments> caseFineFineMultiApplicationScenarios() {
        return Stream.of(
                Arguments.of("Case-Fine-Fine > App1-Granted-> Fine-Fine",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/test/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/test/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/test/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/test/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/test/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),

                Arguments.of("Case-Fine-Fine > App1-Granted-> Non-fine-Non-fine > App2-Granted-> Fine-Non-fine [STE77/28DI5984707 DD-39606]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 appeal resulted - h1")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_appeal_results_tracked.json"
                                                , emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 appeal resulted - h2")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-NonFine/3_app_appeal_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                        )


                ),
                Arguments.of("Case-Fine-Fine > App1-Granted-> Non-fine-Non-fine [STE77/28DI1978850 DD-39125]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/DD-39125/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/DD-39125/2_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/DD-39125/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/DD-39125/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))


                ),
                Arguments.of("Case-Fine-Fine-Fine > App1-Granted-> Fine-Fine-Fine > App2-Granted-> Non-fine-Non-fine-Non-fine [STE77/SJ918778267 DD-39166]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Fine-Fine/App2-Granted-Non-fine-Non-fine-Non-fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Fine-Fine/App2-Granted-Non-fine-Non-fine-Non-fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Fine-Fine/App2-Granted-Non-fine-Non-fine-Non-fine/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Fine-Fine/App2-Granted-Non-fine-Non-fine-Non-fine/3_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Fine-Fine/App2-Granted-Non-fine-Non-fine-Non-fine/3_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))


                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Fine > App2-Granted > Fine-Fine [STE77/28DI3594325]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/2_app1_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app2_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-Fine/3_app2_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Fine > App2-Granted > Fine-Non-fine [STE77/28DI7664890]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app2_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/3_app2_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/4_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/4_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-Fine-NonFine/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Fine > App2-Granted > Non-fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app2_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/3_app2_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/4_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/4_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Fine > App2-Granted > Non-fine-Non-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app2_expected_app_to_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/3_app2_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/4_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-Fine/4_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-Fine/App2-Granted-NonFine-NonFine/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Non-fine-Fine [STE77/28DI7404759]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Fine-Non Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-Fine-NonFine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Non Fine-Fine-1 [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("TBD::::Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Non Fine-Fine-2 [DD-39829: GD61903271]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                /*.newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-1/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))*/
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine-2/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Non Fine-Non Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-NonFine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Non-fine-Fine [STE77/28DI7404759]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/Fine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-Fine-Fine > App2-Granted > Fine-Fine [STE77/28DI9463535]",
                        newScenario()
                                .newStep(caseWithTwoOffencesResultedWithFineFineAndAppResultedWithNonFineFine())
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-Fine-Fine > App2-Granted > Fine-Non Fine [MOCK]",
                        newScenario()
                                .newStep(caseWithTwoOffencesResultedWithFineFineAndAppResultedWithNonFineFine())
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-Fine-NonFine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-Fine-Fine > App2-Granted > Non Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(caseWithTwoOffencesResultedWithFineFineAndAppResultedWithNonFineFine())
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-Fine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-Fine-Fine > App2-Granted > Non Fine-Non Fine [MOCK]",
                        newScenario()
                                .newStep(caseWithTwoOffencesResultedWithFineFineAndAppResultedWithNonFineFine())
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/App2-Granted-NonFine-NonFine/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-fine-Non-fine > App2-Granted > Fine-Fine [STE77/28DI3744431]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/3_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-fine-Non-fine > App2-Granted > Non-fine-Fine [STE77/28DI7629356]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("Case-Fine-Fine > Case-Amend-Non-fine-Non-fine > App1-Granted-> Fine-Fine > App2-Granted-> Fine-Non-fine [STE77/RQ126157841]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/2_case_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/2_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/3_app_stat_dec_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/3_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/4_app_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/4_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/Case-Amend-Non-fine-Non-fine/App1-Granted/Fine-Fine/App2-Granted-Fine-Non-fine/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine > App1-Added [STE77/RQ126168326]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/single-offence/App1-Added/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 appeal received", "json/nces/multi-applications/Case-Fine-Fine/single-offence/App1-Added/2_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/single-offence/App1-Added/3_app1_expected_app_to_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted > Non-fine-Non-fine > App2-Granted > Non-Fine-Non-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked",
                                                "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/2_app1_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/3_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-NonFine/App2-Granted-NonFine-NonFine/3_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Declined > App2-Granted-Fine-Fine [SRE77/28DI6224082]",
                        newScenario()
                                .newStep(caseWithTwoOffencesResultedWithFineFineAndAppDeclined())
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App2-Granted-Fine-Fine/3_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App2-Granted-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App2-Granted-Fine-Fine/3_app2_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Declined > App2-Declined [MOCK]",
                        newScenario()
                                .newStep(caseWithTwoOffencesResultedWithFineFineAndAppDeclined())
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App2-Declined/3_app_reopen_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App2-Declined/3_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App2-Declined/3_app2_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Withdrawn > App1-Amended-Granted-Fine-NonFine > App2-Granted-NonFine-NonFine [SRE77/28DI4859387: DD-39899]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2_1_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/3_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/3_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/3_app_statdec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/3_app2_statdec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Declined-No offence results > App1-Amended-Granted-Fine-Fine > App2-Granted-Fine-Fine > App2-Amended-Declined-Delete offence results > App3-Allowed-Fine-Fine > App3-Decline-Delete offence results [SRE77/28DI3955007: DD-39899]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app_statdec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_1_app_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app2_repoen_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_1_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app2_repoen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app3_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_1_app_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app2_duplicate_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted-Fine-Fine > App1-Amended-Declined-No offence results > App2-Granted-Fine-Fine > App2-Amended-Declined-Delete offence results > App3-Denied-Fine-Fine > App3-Decline-Amend Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_1_app_statdec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app2_repoen_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_1_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app2_repoen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app3_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_1_app_appeal_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app2_duplicate_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Refused > App2-Reopened [SRE77/RL91080687: DD-39911]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec withdrawn")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/2_app_stat_dec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked",
                                                "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/2_app1_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/App1-Refused-App2-added/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                )
        );
    }

    public static Stream<Arguments> caseFinMixedOffencesOnMultiApplication() {
        return Stream.of(
                Arguments.of("Case 2 Offences FP > App1 statdec with FP, DISM > App2 Appeal Granted O1 FP, O2 DISM > App3 Reopen Refused [STE77/28DI4306863]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/2_app1_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/2_app1_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/3_app2_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/3_app2_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/3_app2_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 reopen received", "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/4_app3_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 Reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/4_app3_reopen_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/1_case-2off-fp_app1-statdec-fp-dism_app2-appeal-g-o1-fp-o2-dism_app3-reopen-refused/4_app3_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case > 2 Offences FP > App1 statdec with FP, NonFP > NonRehearing Statdec Granted > App2 Reopen Granted, FP,FP [STE77/28DI3720936]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("id", "materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/2_app_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/3_app2_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("id", "materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/2_case-2off-fp_app1-statdec-fp-nonfp_app2-non-statdec-g_app3-reopen-g-fp-fp/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case o1 & o2 FP, O3 Dism > Case Amended o2 add fo > App1 Reopen o1, o2 Dism & o3 FP > NonRehearing App Refused > App2 statdec o1 & O2 FP, O3 Dism [STE77/28DI9631765]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/2_case_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/2_case_amended_expected_nces_ac_writeoff_notification.json",
                                                comparison()
                                                        .withPathsExcluded("id", "materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app1 reopen received", "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/3_app1_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("id", "materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/3_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 statdec received", "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/4_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/4_app_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("id", "materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/4_app_stat_dec_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/3_case-o1o2fp-o3dism_case-amended-o2-add-fo_app1-reopen-o1o2-dism-o3-fp_app2-non-rehearing-refused_app3-statdec-o1o2-fp-o3-dism/4_app_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
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
                Arguments.of("Case-multi-offences > case amended > App1-Granted > Fine > App2-Granted > Fine > App3-Granted > Fine-Fine [STE77/48DI4492996]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/multi-offences/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/multi-offences/2_case_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/multi-offences/2_case_write-off-nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/multi-offences/3_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/multi-offences/4_app_reopen_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/multi-offences/5_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/multi-offences/5_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine-Fine > App1-Granted > Fine-Non-fine-Fine > App2-Granted > NR-Fine-NR > App3-Granted > NR-NR-Fine > App4-Granted > NR-Fine-Fine [STE77/RQ699722881]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/2_app_stat_dec_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("app3 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/4_app_reopen_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app4 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/5_app_appeal_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/three-offences/Case-Fin-Fin-Fin/App1-Granted/Fine-Non-fine-Fine/App2-Granted-NR-Fine-NR/App3-Granted-NR-NR-Fine/App4-Granted-NR-Fine-Fine/5_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),

                Arguments.of("Case-Fine-Fine > App1-Granted > Fine-Non-fine > App2-Granted > Non Fine-Fine > App3-refused > Fine-Non Fine [STE77/RQ121236318]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/2_app_stat_dec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/2_app1_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/3_app_reopen_1_send_nces_request.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app4 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/4_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40156/4_app_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))

                                )
                ),

                Arguments.of("Case-Fine-Non-fine > Case-amended-o2-Fine > App1-Granted-Fine-Fine > App2-Granted-Fine-Fine [STE77/28DI8919760]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/2_case_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/1_case_expected_write_off_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
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
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/4_app_reopen_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-NonFine/Case-amended-o2-Fine/App1-Granted-Fine-Fine/App2-Granted-Fine-Fine/4_app_reopen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Non-fine > App1-Granted > Fine-Fine [STE77/28DI3316700] DD-39774",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/1_account_consolidated_nces_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-NonFine/App1-Granted/Fine-Fine/2_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
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

                )
        );
    }

    public static Stream<Arguments> caseFinMixedOffencesOnMultiMixedApplicationResults() {
        return Stream.of(
                Arguments.of("Case-Fine+DS-Fine > Case-Fine+DS no change-Fine change > App1-Granted-> Fine+DS-Fine > App1 amended : Fine+DS no change-Fine change - DD-40400",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/3_case_ds_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/2_case_amended_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/3_case_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/3_1_case_ds_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/3_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/3_1_statdec_expected_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/4_statdec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/8_statdec_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/8_statdec_granted_ds_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/5_statdec_amended_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/8_statdec_ar_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400/9_statdec_ds_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Case-Fine+DS-Fine > Case-Fine(change)+DS(no change),Fine(no change) > Case-Fine(no change)+DS(no change),Fine(change) > DD-40400-1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/1_case_ds_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/2_case_amended_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/2_1_case_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/2_case_ds_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended 2")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/3_case_amended_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/3_1_case_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40400-1/3_1_case_ds_nces_email_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Declined-No offence results > App1-Amended-Granted-Fine-Fine > App2-Granted-Fine-Fine > App2-Amended-Declined-Delete offence results > App3-Allowed-Fine-Fine > App3-Decline-Delete offence results [SRE77/28DI3955007: DD-39899]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app_statdec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_1_app_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app2_repoen_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_1_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/3_app2_repoen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app3_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_1_app_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-1/4_app2_duplicate_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted-Fine-Fine > App1-Amended-Declined-No offence results > App2-Granted-Fine-Fine > App2-Amended-Declined-Delete offence results > App3-Denied-Fine-Fine > App3-Decline-Amend Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_1_app_statdec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app2_repoen_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_1_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/3_app2_repoen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app3_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_1_app_appeal_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-2/4_app2_duplicate_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted-Fine-Fine > App1-Amended-Declined-No offence results > App2-Granted-Fine-Fine > App2-Amended-Fine-Fine > App3-Allowed-Fine-Fine > App3-Amend Fine-Fine [STE77/28DI5651663]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/2_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/2_app_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/2_1_app_statdec_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/3_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/3_app2_repoen_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/3_1_app_reopen_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/3_app2_repoen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/4_app_appeal_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/4_app3_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/4_1_app_appeal_results_tracked.json",
                                                accountInfo("66c39541-e8e0-45b3-af99-532b33646b69", "66c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-3/4_app2_duplicate_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "66c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case-Fine-Fine > App1-Granted-Fine-Fine > App1-Amended-Fine-Fine > App2-Granted-Fine-Fine > App2-Granted-Fine-Fine > App3-Allowed-Fine-Fine > App3-Amend Fine-Fine [MOCK]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/2_app_statdec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/2_app1_expected_app_for_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/2_app_statdec_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/2_1_app1_withdrawn_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 statdec amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/2_1_app_statdec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/3_app_reopen_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/3_app2_repoen_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app2 reopen amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/3_1_app_reopen_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/3_app2_repoen_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/4_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/4_app3_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/4_app_appeal_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/4_app3_appeal_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app3 appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/4_1_app_appeal_results_tracked.json",
                                                accountInfo("66c39541-e8e0-45b3-af99-532b33646b69", "66c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/App1-Amended/2-applications/scenario-4/4_app2_duplicate_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "66c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("DD-40172:Enforcement Notification has incorrect GOB when the previous application is WDRN/APA [only for Appeal application ]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 Appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/2_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/2_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 Appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/3_appeal_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/2_1_app1_allowed_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app1 Appeal amended")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/4_appeal_results_tracked.json",
                                               emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked","NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/5_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/3_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/6_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172/3_app2_appeal_allowed_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Case with multi application denials",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 Appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/2_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/2_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 Appeal granted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/3_appeal_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/2_1_app1_allowed_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                               /* .newStep(newResultTrackedStep("app1 Appeal amended to denied")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/3_1_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/2_1_app1_denied_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )*/
                                .newStep(newNcesEmailForNewApplicationStep("app2 Appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/3_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/2_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 Appeal denied")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/4_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked","NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app3 appeal received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/5_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/3_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/6_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/DD-40172-multi-denials/3_app2_appeal_allowed_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                )
        );
    }

    private static CompositeStep caseWithTwoOffencesResultedWithFineFineAndAppResultedWithNonFineFine() {
        return newCompositeStep("Case-Fine-Fine > App1-Granted > Non-Fine-Fine")
                .andThen(newResultTrackedStep("case resulted")
                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/1_case_results_tracked.json",
                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                .andThen(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/2_app_stat_dec_1_send_nces_request.json")
                        .withExpectedEventNames("NcesEmailNotificationRequested")
                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/2_app1_expected_app_for_stat_dec_received_notification.json",
                                comparison()
                                        .withPathsExcluded("materialId", "notificationId")
                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                .andThen(newResultTrackedStep("app1 statdec resulted")
                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/2_app_stat_dec_results_tracked.json",
                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Granted/NonFine-Fine/2_app1_stat_dec_nces_notification_expected.json",
                                comparison()
                                        .withPathsExcluded("materialId", "notificationId")
                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                );
    }

    private static CompositeStep caseWithTwoOffencesResultedWithFineFineAndAppDeclined() {
        return newCompositeStep("Case-Fine-Fine > App1-Declined")
                .andThen(newResultTrackedStep("case resulted")
                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/1_case_results_tracked.json",
                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                .andThen(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/2_app_stat_dec_1_send_nces_request.json")
                        .withExpectedEventNames("NcesEmailNotificationRequested")
                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/2_app1_expected_app_for_statdec_received_notification.json",
                                comparison()
                                        .withPathsExcluded("materialId", "notificationId")
                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                .andThen(newResultTrackedStep("app1 statdec resulted")
                        .withResultTrackedEvent("json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/2_app_stat_dec_results_tracked.json",
                                emptyAccountInfo())
                        .withExpectedEventNames("HearingFinancialResultsTracked",
                                "NcesEmailNotificationRequested")
                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/Case-Fine-Fine/multi-offences/two-offences/App1-Declined/2_app1_granted_nces_notification_expected.json",
                                comparison()
                                        .withPathsExcluded("materialId", "notificationId")
                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseFineFineMultiApplicationScenarios")
    void shouldCreateNCESNotificationForCaseFineFineMultiApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseFinMixedOffencesOnMultiApplication")
    void shouldCreateNCESNotificationForcaseFinMixedOffencesOnMultiApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseFinMixedOffencesOnMultiMixedApplicationResults")
    void shouldCreateNCESNotificationForcaseFinMixedOApplicationResults(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

} 