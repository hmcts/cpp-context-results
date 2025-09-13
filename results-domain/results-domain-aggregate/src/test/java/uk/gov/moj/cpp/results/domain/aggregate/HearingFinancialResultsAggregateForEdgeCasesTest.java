package uk.gov.moj.cpp.results.domain.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.accountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.emptyAccountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Comparison.comparison;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.GobAccountUpdateStep.newGobAccountUpdateStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.NcesEmailForNewApplicationStep.newNcesEmailForNewApplicationStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.ResultTrackedStep.newResultTrackedStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario.newScenario;

import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for Case-Fine-Fine scenarios.
 * This class contains all test cases related to cases where both offences are initially financial.
 */
class HearingFinancialResultsAggregateForEdgeCasesTest {

    /**
     * Test scenarios for Case-Fine-Fine multi-application scenarios.
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    public static Stream<Arguments> complexMultiApplicationScenarios() {
        return Stream.of(
                Arguments.of("Case-o1Fine-o2Fine-o3Fine-o4Fine > App1-Granted-o1Non-fine-o2Non-fine > App2-Granted-o2Fine-o3Fine > App3-Granted-o1Fine-o4Fine > App4-Granted-o3Non-fine-o4Non-fine > App5-Granted-o1Non-fine-o2Fine-o3Non-fine-o4Fine [STE77/48DI6925261]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", ""))
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newGobAccountUpdateStep("GOB export")
                                        .withAccountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        .withExpectedEventNames("HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/2_app_stat_dec_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/3_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/3_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                                .newStep(newNcesEmailForNewApplicationStep("app3 reopen received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/4_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/4_app_reopen_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/4_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                                .newStep(newNcesEmailForNewApplicationStep("app4 appeal received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/5_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/5_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app4 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/5_app_appeal_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/5_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app5 appeal received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/6_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/6_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app5 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/6_app_appeal_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-1/6_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Case-o1Fine-o2Fine-o3Fine-o4Fine > App1-Granted-o1Non-fine-o2Non-fine > App2-Granted-o2Non-fine-o3Fine > App3-Granted-o1Non-fine-o4Non-fine > App4-Granted-o2Non-fine-o3Non-fine > App5-Granted-o1Fine-o2Fine-o3Fine-o4Fine [STE77/48DI9761991]",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 statdec received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/2_app1_expected_app_for_stat_dec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 statdec resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/2_app_stat_dec_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/2_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                                .newStep(newNcesEmailForNewApplicationStep("app2 reopen received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/3_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/3_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app2 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/3_app_reopen_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/3_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                                .newStep(newNcesEmailForNewApplicationStep("app3 reopen received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/4_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/4_app1_expected_app_for_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app3 reopen resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/4_app_reopen_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/4_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                                .newStep(newNcesEmailForNewApplicationStep("app4 appeal received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/5_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/5_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app4 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/5_app_appeal_results_tracked.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/5_app_granted_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app5 appeal received", "json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/6_app_appeal_1_send_nces_request.json"))
                                .newStep(newResultTrackedStep("app5 appeal resulted")
                                        .withResultTrackedEvent("json/nces/multi-applications/applications-mixed-offences/Case-4Offences-5Applications/scenario-2/6_app_appeal_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                )
        );
    }


    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("complexMultiApplicationScenarios")
    void shouldCreateNCESNotificationForComplexMultiApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }
} 