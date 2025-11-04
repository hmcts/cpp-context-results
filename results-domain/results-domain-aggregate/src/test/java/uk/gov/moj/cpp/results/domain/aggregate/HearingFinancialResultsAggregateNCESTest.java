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
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for case and application amendments.
 */
class HearingFinancialResultsAggregateNCESTest {

    /**
     * Test scenarios for case amendments.& Application amendments
     * Each scenario consists of a sequence of result tracked steps, a step is started by newResultTrackedStep and finished by finishStep.
     */
    public static Stream<Arguments> caseAmendmentScenarios() {
        return Stream.of(
                Arguments.of("single offence(fin > fin) DD-36847 AC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin/case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin/nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("DLQ: nonfin > nonfine DD-40322",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/nonfin-to-nonfin/case_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newResultTrackedStep("application adj")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/nonfin-to-nonfin/app_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/nonfin-to-nonfin/app_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                ),
                Arguments.of("FAIL: nonfin > fin DD-36847 AC2)",
                        newScenario()
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/nonfin-to-fin/case_amended.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                ),
                Arguments.of("fin > nonfin (FO -> FO:deleted + CD:Added) (One GOB A/C) (DD-36847 AC3)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin/1_case_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-nonfin/1_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Regression > fin > nonfin (FO -> Fine :No Change + Non Fine:amended)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("PRE:CCT-2390::case non fine amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_case_amended-pre2390.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("POST:CCT-2390::case nonfine amended2")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_case_amended-post2390.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                                .newStep(newResultTrackedStep("POST:CCT-2390::case fine amended3")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/2_case_amended-post2390.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-nonfin-regression/2_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("Regression > fin > nonfin (FO -> Fine :No Change + Non Fine:amended) > Statdec",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("PRE:CCT-2390::case non fine amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_case_amended-pre2390.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("POST:CCT-2390::case nonfine amended2")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin-regression/1_case_amended-post2390.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                                .newStep(newNcesEmailForNewApplicationStep("app1 appeal received", "json/nces/case-amendments/single-offence/fin-to-nonfin-regression/2_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/single-offence/fin-to-nonfin-regression/2_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Regression3 > fin > nonfin (O1:FO -> Fine :No Change + Non Fine:amended" +
                                "O2, O3: Fine to NonFine )",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/regression/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("POST:CCT-2390::case nonfine amended1")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/regression/1_case_amended-post2390.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/multi-offences/regression/1_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("1-REG1 - pre-2390 > DD-40587 Financial changes with Nonfine to Fine change",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/reg-1-pre-2390/1_case_resulted-pre-2390.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended-PRE-2390")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/reg-1-pre-2390/2_case_amended-pre-2390.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/multi-offences/reg-1-pre-2390/2a_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("2-REG1 - post-2390 > DD-40587 Financial changes with Nonfine to Fine change",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/reg-1-post-2390/1_case_resulted-post-2390.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended-PRE-2390")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/reg-1-post-2390/2_case_amended-post-2390.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/multi-offences/reg-1-post-2390/2a_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),

                Arguments.of("DD-40592 scenario1 - post-2390 > mixed Financial changes with Nonfine to Fine change",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-1/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-1/2_case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived",  "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/multi-offences/mixed-financial/scenario-1/2a_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),

                Arguments.of("DD-40592 scenario2 - pre-2390[nonFine offence are not tracked], then post-2390 > mixed Financial changes with Nonfine to Fine change",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-2/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-2/2_case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived",  "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/multi-offences/mixed-financial/scenario-2/2a_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),

                Arguments.of("DD-40592 scenario3 - pre-2390[nonFine offence are not tracked] case resulted and amended, then post-2390 > mixed Financial changes",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-3/1_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended 1")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-3/2_case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("case amended 2")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/mixed-financial/scenario-3/3_case_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/case-amendments/multi-offences/mixed-financial/scenario-3/2a_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),

                Arguments.of("TBD::CCT-2357::fin > nonfin (FO+CD -> FO:deleted + CD:NoChange) (One GOB A/C) (DD-36847 AC3)(CCT-2357, see https://tools.hmcts.net/confluence/display/CROWN/CCT-2266+Gaps)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-nonfin/2_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                ),
                Arguments.of("fin > fin-FIDICI* (FO -> FO:Changed + FIDICI:Added) (One GOB A/C) (DD-36847 AC4)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin_fidici/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin_fidici/case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin_fidici/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin_fidici/nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("fin > fin-FIDICI* (FO -> FO:Changed + FIDICI:Added) > fin (FO -> FO:Changed + FIDICI:Removed) (One GOB A/C) (DD-36847 AC4)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin_fidici/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended 1")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin_fidici/case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin_fidici/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin_fidici/nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("case amended 2")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin-to-fin_fidici/2_case_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin_fidici/2_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin-to-fin_fidici/2_nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("fin_fidici > fin (FO+FIDICI -> FO:Changed+FIDICI:Deleted) (2 GOB A/C) (DD-36847 AC5)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici-to-fin/2_case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-fin/1_nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici-to-fin/2_case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-fin/2_nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-fin/2_nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("TBD::CCT-2357:DS-REMOVED:SHOWING NONFINE details & amendmentdetails are popping in the notification::fin_fidici > nonfin (FO+FIDICI -> FO:Deleted+FIDICI:Deleted+CD:Added) (One GOB A/C) (DD-36847 AC6)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici-to-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-nonfin/nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici-to-nonfin/case_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-nonfin/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-nonfin/2_nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("fin-fidici > fin*-fidici* (DD-37163 DD-36847:AC7)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici-to-fin_fidici/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-fin_fidici/nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici-to-fin_fidici/case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-fin_fidici/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici-to-fin_fidici/nces_deemed_serve2_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("fin-fcost-fidici > fin*-fidici*-fcost (DD-36847 AC7)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fine_fcost_fidici-to-fine_fcost-fidici/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fine_fcost_fidici-to-fine_fcost-fidici/nces_deemed_serve1_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fine_fcost_fidici-to-fine_fcost-fidici/case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fine_fcost_fidici-to-fine_fcost-fidici/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fine_fcost_fidici-to-fine_fcost-fidici/nces_deemed_serve2_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("fin_fidici_acon-to-fin_fidici_acon (FO+FIDICI+ACON -> FO:Amended+FIDICI:NoChange+ACON:NoChange) (2 GOB A/Cs)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici_acon-to-fin_fidici_acon/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici_acon-to-fin_fidici_acon/nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/single-offence/fin_fidici_acon-to-fin_fidici_acon/case_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici_acon-to-fin_fidici_acon/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici_acon-to-fin_fidici_acon/nces_account_consolidated_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/single-offence/fin_fidici_acon-to-fin_fidici_acon/2_nces_deemed_serve_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("multi offence(fin > amend ds, acon) DD-40249",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/fine-amend-ds-acon/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/fine-amend-ds-acon/case_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/multi-offences/fine-amend-ds-acon/nces_notification_expected-acon.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/multi-offences/fine-amend-ds-acon/nces_notification_expected-ds.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))))
                ,
                Arguments.of("offence is adjourned to next hearing and resulted with ACON > DD-40639",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/acon/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case amended")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/acon/case_amended-1.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/multi-offences/acon/nces_acon_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/multi-offences/acon/nces_ds_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("case amended 2")
                                        .withResultTrackedEvent("json/nces/case-amendments/multi-offences/acon/case_amended-2.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/multi-offences/acon/nces_ar_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/case-amendments/multi-offences/acon/nces_ds_notification_removed_expected-1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                )
        );
    }

    public static Stream<Arguments> finCaseSingleOffenceAppScenarios() {
        return Stream.of(
                Arguments.of("1. Appeal > asv > offence > FO,SV DD-35053 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/single-offence/fin-sv-appeal-withdrawn-asv/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app withdrawn")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/single-offence/fin-sv-appeal-withdrawn-asv/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/single-offence/fin-sv-appeal-withdrawn-asv/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("2. Appeal > aasd > offence > FO, SV DD-35053 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/single-offence/fin-sv-appeal-dismissed-aasd/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app dismissed")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/single-offence/fin-sv-appeal-dismissed-aasd/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/single-offence/fin-sv-appeal-dismissed-aasd/appeal_dismissed_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("3. Appeal > aasd  DD-35053 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-appeal-dismissed-aasd/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app dismissed")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-appeal-dismissed-aasd/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-only-results/single-offence/fin-appeal-dismissed-aasd/appeal_dismissed_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("4. Appeal > asv  DD-35053 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-appeal-withdrawn-asv/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app dismissed")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-appeal-withdrawn-asv/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-only-results/single-offence/fin-appeal-withdrawn-asv/appeal_dismissed_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("5. Statdec > wdrn,  DD-35053 SC3",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-statdec-withdrawn/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app withdrawn")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-statdec-withdrawn/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-only-results/single-offence/fin-statdec-withdrawn/statdec_withdrawn_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("6. reopen > rfsd,  DD-35053 SC3",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-reopen-refused/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app refused")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/single-offence/fin-reopen-refused/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-only-results/single-offence/fin-reopen-refused/reopen_refused_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("7.Reopen > mutlioffence >  > Fine,FVS,FIDICI+Fine,ACON",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/nces-acon-case.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/nces-deemedserve-case.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app granted")
                                        .withResultTrackedEvent("json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/application-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/nces-app-reopen-granted.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/nces-app-acon.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/o1-fin+fvs+fidici-o2-fin+acon/nces-app-deemedserve.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("8. Statdec > Granted,  Offence : Next Hearing, Result Offence in NEXH with FO",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/app-granted-off-next-result-offence-with-fo/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app granted")
                                        .withResultTrackedEvent("json/nces/application/app-granted-off-next-result-offence-with-fo/application-resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                                .newStep(newResultTrackedStep("app granted with offence fine which is in next hearing ")
                                        .withResultTrackedEvent("json/nces/application/app-granted-off-next-result-offence-with-fo/offence-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/app-granted-off-next-result-offence-with-fo/application-granted-1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )

                                )
                ),
                Arguments.of("10. Reopen > Granted, resulted FO and amened FO > Application 2 statdec > Adjourned",
                        newScenario()
                                .newStep(newResultTrackedStep("application 1 resulted")
                                        .withResultTrackedEvent("json/nces/application/app1-grantedAmended-fo-app2-nextHearing/application1-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application 1 ameneded")
                                        .withResultTrackedEvent("json/nces/application/app1-grantedAmended-fo-app2-nextHearing/application1-amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/app1-grantedAmended-fo-app2-nextHearing/application1-duplicate-writeoff.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application 2 Adjourned ")
                                        .withResultTrackedEvent("json/nces/application/app1-grantedAmended-fo-app2-nextHearing/application2-resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/app1-grantedAmended-fo-app2-nextHearing/application2-updated.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("11. Case > offence > FO,IMP > Statdec > offence non fin (DD-39431)",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/single-offence/fin-case-statdec-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app granted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/single-offence/fin-case-statdec-nonfin/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/single-offence/fin-case-statdec-nonfin/application_granted_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                )
        );
    }

    public static Stream<Arguments> finCaseMultiOffenceAppScenarios() {
        return Stream.of(
                Arguments.of("Appeal > apa > offences > FO,SV DD-35053 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-apa/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app abandoned")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-apa/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-apa/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Appeal > asv > offences > FO,SV DD-35053 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app1 appeal received", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/2_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/2_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app1 abandoned")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/application_resulted1.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("app2 appeal received", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/3_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/3_app1_expected_app_for_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app 2 resulted ")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/application_resulted2.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/appeal_sv_dismissed_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app 2 amended")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/application-amended-2.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-abandoned-asv/duplicate_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Appeal > aw > offences > FO, SV DD-35053 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-withdrawn-aw/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app withdrawn")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-withdrawn-aw/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/fin-sv-appeal-withdrawn-aw/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Appeal > apa,  DD-35053 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/multi-offence/fin-appeal-abandoned-apa/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app abandoned")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/multi-offence/fin-appeal-abandoned-apa/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-only-results/multi-offence/fin-appeal-abandoned-apa/appeal_abandoned_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Statdec > dismissed,  DD-35053 SC3",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/multi-offence/fin-statdec-dismissed/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app dismissed")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-only-results/multi-offence/fin-statdec-dismissed/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-only-results/multi-offence/fin-statdec-dismissed/statdec_dismissed_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("TBD:CCT-2357:DD-40592 : Legacy unit test case 1 : APPEAL > asv, o1:NF, O2:Fine > O1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-1/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("appeal asv")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-1/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-1/appeal_dismissed_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("appeal amended")
                                                .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-1/application_amended.json",
                                                        accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                                .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                        //Need to add A&R after DD-40592 fix
                                )
                ),
                Arguments.of("Legacy unit test case 2 : APPEAL > asv, o1:F, O2:F+DS > O1:F",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("appeal asv")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/appeal_dismissed_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/appeal_ds_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("appeal amended")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/appeal_ds_notification-1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-2/appeal_ar_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("TBD:CCT-2357:DD-40592 :Legacy unit test case 3 : APPEAL > asv, o1:NF, O2:F+ACON > O1:F",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-3/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("appeal asv")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-3/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-3/appeal_dismissed_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/fin-case-application-offence-results/multi-offence/legacy-3/appeal_acon_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("appeal amended")
                                        .withResultTrackedEvent("json/nces/application/fin-case-application-offence-results/multi-offence/legacy-3/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                )

        );
    }

    public static Stream<Arguments> finCaseSingleOffenceAppAmendmentScenarios() {
        return Stream.of(
                Arguments.of("single offence > case fin > app nonfin > fin",
                        newScenario()
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin-1/application_amended.json")
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                ),
                Arguments.of("single offence > case fin > app nonfin > fin",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/nonfin-amended-nonfine/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/nonfin-amended-nonfine/application_resulted.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/nonfin-amended-nonfine/nces_granted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/nonfin-amended-nonfine/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                ),
                Arguments.of("single offence > case fin > app fin > nonfin DD-36848 SC3",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-nonfin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-nonfin/application_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-nonfin/nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("single offence > case fin > app fin > fin,ds DD-36848 SC4",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin-ds/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin-ds/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin-ds/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-fin-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-fin-ds/nces_notification_deemed_served_writeoff_expected.json"))
                ),
                //In this case clicked on Amend on different Result but added new result without amending the original one
                //In turn causing extra NCES for ACON & DeemedServed Writeoffs due to amendement information being passed to down
                Arguments.of("single offence > case fin > app fin,ds > fin DD-36848 SC5",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/nces_notification_reopen_granted_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/nces_notification_account_consolidated_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fin/nces_notification_deemed_served_writeoff_expected1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("TBD:CCT-2390::single offence > case fin > app fin,ds > nonfin DD-36848 SC6",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-nonfin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-nonfin/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-nonfin/application_amended.json", emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-nonfin/nces_notification_deemed_served_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-nonfin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )

                ),
                Arguments.of("single offence > case fin > app fin,ds > fin,ds DD-36848 SC7",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fine-ds/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fine-ds/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fine-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fine-ds/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fine-ds/nces_notification_deemed_served_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-ds-amended-fine-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("fin > app(fin/granted) -> app refused(all results deleted) DD-37035 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-fin-to-refused-and-delete-results/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-fin-to-refused-and-delete-results/app_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-to-fin-to-refused-and-delete-results/app_granted_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app refused(offence results deleted)")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-fin-to-refused-and-delete-results/app_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-to-fin-to-refused-and-delete-results/acc_writeoff_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("fin > appeal dismissed -> refused DD-37035 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-statdec_dismissed-to-refused/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("appeal dismissed")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-statdec_dismissed-to-refused/appeal_dismissed.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-to-statdec_dismissed-to-refused/appeal_dismissed_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("appeal refused")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-statdec_dismissed-to-refused/appeal_dismissed_to_refused.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-to-statdec_dismissed-to-refused/2_app1_write_off_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("fin > app refused -> withdrawn DD-37035 SC3",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-app_refused-to-withdrawn/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app refused")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-app_refused-to-withdrawn/app_refused.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-to-app_refused-to-withdrawn/app_refused_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app amended withdrawn")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-to-app_refused-to-withdrawn/app_refused_to_withdrawn.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-to-app_refused-to-withdrawn/nces_acc_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("application result amendments for fin-case(adjourned data amended) DD-37035 SC4",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-adjourned_app-amend-adjrn_date/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app adjourned")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-adjourned_app-amend-adjrn_date/app_adjrned.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-adjourned_app-amend-adjrn_date/app_updated_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app adjournment amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-adjourned_app-amend-adjrn_date/app_adjrn-data_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                        .withNotExpectedEventNames("NcesEmailNotificationRequested"))

                ),
                Arguments.of("Appeal > aasd > offence > FO, SV > fin DD-35053 SC3",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-sv-appeal-dismissed-aasd-amend-fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app dismissed")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-sv-appeal-dismissed-aasd-amend-fin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-sv-appeal-dismissed-aasd-amend-fin/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-sv-appeal-dismissed-aasd-amend-fin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-sv-appeal-dismissed-aasd-amend-fin/writeoff_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("single offence > case fin > app fin > add fidici",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici/application-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici/nces_granted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici/application-amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici/nces_deemed.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("single offence > case fin > app fin > add cd",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-cd/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-cd/application-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-cd/nces_granted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-cd/application-amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-cd/nces_duplicate_writeoff.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("single offence > case fin > app fin+ds+acon",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici-acon-extra-offencetitle-issue/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici-acon-extra-offencetitle-issue/application-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested",
                                                "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici-acon-extra-offencetitle-issue/nces_granted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici-acon-extra-offencetitle-issue/nces_deemed.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amend-add-fidici-acon-extra-offencetitle-issue/nces_acon.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                )
        );
    }

    public static Stream<Arguments> finCaseMultiOffenceAppAmendmentScenarios() {
        return Stream.of(

                Arguments.of("multi results > case fin > app fin,ds > nonfin DD-35049 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_application_granted.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("multi results > case fin > app fin,ds > app amendment fine,ds DD-34580 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_granted.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_deemed_served_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("multi offences > case fin > app fin, ds > app amendment nonfin DD-35049 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_application_granted.json")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("multi offences > case fin > app fin,ds > app amendment nonfin DD-35049 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_application_granted.json")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-ds-amended-nonfin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("Appeal > apa > offences > Fine,SV > Fine DD-35053 SC4",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-abandoned-apa-amend-fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app abandoned")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-abandoned-apa-amend-fin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-abandoned-apa-amend-fin/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-abandoned-apa-amend-fin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-abandoned-apa-amend-fin/duplicate_writeoff_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Appeal > aw > offences > Fine,SV > Fine DD-35053 SC4",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-withdrawn-aw-amend-fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app withdrawn")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-withdrawn-aw-amend-fin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-withdrawn-aw-amend-fin/appeal_sv_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-withdrawn-aw-amend-fin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin-sv-appeal-withdrawn-aw-amend-fin/duplicate_writeoff_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("Reopen > mutlioffence >  > Fine,FVS,ACON+Fine,FIDICI > Fine changed",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-deemedserve-case.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-acon-case.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app granted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/application-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-app-reopen-granted.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-app-deemedserve.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-app-acon.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/application-amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-duplicate-writeoff.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-app-deemedserve1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/o1-fin+fvs+acon-o2-fin+fidici-amend-o1-fin/nces-app-acon1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                )
        );
    }

    public static Stream<Arguments> nonFinCaseSingleOffenceAppAmendmentScenarios() {
        return Stream.of(
                Arguments.of("single result- non finance case (DD-34575 SC1)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/single-result/application_resulted.json",
                                                accountInfo("75c39541-e8e0-45b3-af99-532b33646b69", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/single-result/application_amended.json",
                                                accountInfo("a0eb0229-487b-4002-9761-83bd4a8efd6c", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/single-offence/single-result/nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),

                Arguments.of("multi application - non finance case",
                        newScenario()
                                .newStep(newResultTrackedStep("1. application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-applications/1.0_app_resulted.json",
                                                accountInfo("75c39541-e8e0-45b3-af99-532b33646b69", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("2. application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-applications/2.0_app_amended.json",
                                                accountInfo("a0eb0229-487b-4002-9761-83bd4a8efd6c", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/multi-applications/2.1_expected_nces_duplicate_acc_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newNcesEmailForNewApplicationStep("3. application to reopen received", "json/nces/application-amendments/non-fin-case/multi-applications/2.2_app_reopen_received.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/multi-applications/2.2_expected_app_to_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")))
                                .newStep(newResultTrackedStep("3. new application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-applications/3.0_new_app_resulted.json",
                                                accountInfo("b0eb0229-487b-4002-9761-83bd4a8efd6c", "b0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/multi-applications/3.1_expected_app_granted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("gobAccountNumber", "b0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")))
                                .newStep(newResultTrackedStep("4. new application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-applications/4.0_new_app_amended.json",
                                                accountInfo("c0eb0229-487b-4002-9761-83bd4a8efd6c", "c0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/multi-applications/4.1_expected_nces_duplicate_acc_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "c0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "b0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")))
                ),
                Arguments.of("single offence > case non fin >  app fin > app result to refused and remove offence results(DD-34575 SC4)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-amended-app-result-remove-offesults/application_resulted.json",
                                                accountInfo("75c39541-e8e0-45b3-af99-532b33646b69", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-amended-app-result-remove-offesults/application_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/single-offence/fin-amended-app-result-remove-offesults/nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("single offence > nonfin >app fin > fin)",
                        newScenario()
                                .newStep(newResultTrackedStep("application amended(nonfin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/nonfin-to-fin-result/nonfin-to-fin-app-resulted.json",
                                                accountInfo("75c39541-e8e0-45b3-af99-532b33646b69", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                        .withNotExpectedEventNames("NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("application amended(fin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/nonfin-to-fin-result/fin-to-fin-app-amended.json",
                                                accountInfo("a0eb0229-487b-4002-9761-83bd4a8efd6c", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/nonfin-to-fin-result/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )),
                Arguments.of("single result- case fine > app fin > fin (DD-36848 SC1)",//TODO move to case fine scenarios
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b78", "22c39541-e8e0-45b3-af99-532b33646b78ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-fin/first_nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b78ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-fin/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b98", "33c39541-e8e0-45b3-af99-532b33646b98ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-fin/nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b98ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b78ACCOUNT")))
                ),
                Arguments.of("single result- case non fine > app fine,ds > fine,ds(DD-34580 SC1)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-ds/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-ds/nces_notification_deemed_served_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("single result- case non fin > app fine, ds > fine(DD-34579 SC1)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/nces_notification_deemed_served_writeoff_expected_1.json"))
                                .newStep(newResultTrackedStep("application amended1")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/application_amended1.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-ds-amended-fin-amended-fin-removed-ds/nces_notification_account_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("single result- case non fin > app fin > fin, ds(DD-34578 SC1)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-amended-fin-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-amended-fin-ds/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-amended-fin-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-amended-fin-ds/nces_notification_deemed_served_writeoff_expected.json"))
                ),
                Arguments.of("single result- case non fin >  app fine, acon > fin",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin/nces_account_consolidated.json"))

                ),
                Arguments.of("single result- case non fin > app fin, acon > fin, acon",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin-acon/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin-acon/nces_notification_account_consolidated.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin-acon/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin-acon/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-fin-acon/nces_notification_account_consolidated_1.json"))
                ),
                Arguments.of("single result- case non fin > app fin, acon > acon",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-acon/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-acon/nces_notification_account_consolidated.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-acon/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-acon/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/single-offence/fin-acon-amended-acon/nces_notification_account_consolidated_1.json"))
                ),
                Arguments.of("single offence > case fin > app fin > fin,ds DD-34584 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/application_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("single offence > case fin > app fin > fin,ds > fin,ds removed DD-34584 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/application_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                ).newStep(newResultTrackedStep("application amended 2")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/2_application_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/single-offence/fin-amended-add-ds/2_nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                )
        );
    }

    public static Stream<Arguments> nonFinCaseMultiOffenceAppAmendmentScenarios() {
        return Stream.of(
                Arguments.of("multiple result- case nonfine application offences resulted to fine amended offence1 to different fine and offence 2 with diff amount(DD-34575 SC2)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/multiple-results-case-nonfine-application-fine-amended-diff-fine/application_resulted.json",
                                                accountInfo("75c39541-e8e0-45b3-af99-532b33646b69", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/multiple-results-case-nonfine-application-fine-amended-diff-fine/application_amended.json",
                                                accountInfo("a0eb0229-487b-4002-9761-83bd4a8efd6c", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application-amendments/non-fin-case/multi-offence/multiple-results-case-nonfine-application-fine-amended-diff-fine/nces_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("multi offence amended same time(nonfin>fin>fin)",
                        newScenario()
                                .newStep(newResultTrackedStep("application amended(nonfin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-sametime/nonfin-to-fin-app-resulted.json",
                                                accountInfo("75c39541-e8e0-45b3-af99-532b33646b69", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                        .withNotExpectedEventNames("NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("application amended(fin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-sametime/fin-to-fin-app-amended.json",
                                                accountInfo("a0eb0229-487b-4002-9761-83bd4a8efd6c", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-sametime/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "a0eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "75c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )),
                Arguments.of("multi offence amended different times(nonfin>fin>fin)",
                        newScenario()
                                .newStep(newResultTrackedStep("application amended(nonfin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-differenttime/nonfin-to-fin-app-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                        .withNotExpectedEventNames("NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("application off1 amended(fin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-differenttime/fin-to-fin-off1-amended.json",
                                                accountInfo("22eb0229-487b-4002-9761-83bd4a8efd6c", "22eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-differenttime/nces_notification_account_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                ).newStep(newResultTrackedStep("application off2 amended(fin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-differenttime/fin-to-fin-off2-amended.json",
                                                accountInfo("33eb0229-487b-4002-9761-83bd4a8efd6c", "33eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-differenttime/nces_notification_account_writeoff_expected_2.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                )),
                Arguments.of("multi offence amended (nonfin/nonfin > fin/fin > fin/nonfin)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted(nonfin to fin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin_nonfin-results-amended/nonfin-to-fin-app-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                        .withNotExpectedEventNames("NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("application amended(fin/fin to fin/nonfin)")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin_nonfin-results-amended/fin-to-fin_nonfin-amended.json",
                                                accountInfo("22eb0229-487b-4002-9761-83bd4a8efd6c", "22eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin_nonfin-results-amended/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22eb0229-487b-4002-9761-83bd4a8efd6cACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )),
                Arguments.of("multi result- case non fine>application fine,ds>amendment fine,ds(DD-34580 SC2))",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fine-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fine-ds/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_deemed_served_writeoff_expected_1.json")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fine-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                ),
                Arguments.of("multiple result- case non fine application fine deemed served amendment fine(DD-34579 SC2)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fin-amended-fin/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fin-amended-fin/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fin-amended-fin/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-ds-amended-fin-amended-fin/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                ),
                Arguments.of("multiple result- case non fine application fine amendment fine deemed serve(DD-34578 SC2)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds/nces_notification_deemed_served_writeoff_expected.json"))
                ),
                Arguments.of("multiple result- case non fine application fine amendment fine deemed serve many changes(DD-34578 SC3)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds-many-changes/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds-many-changes/application_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds-many-changes/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-amended-fin-ds-many-changes/nces_notification_deemed_served_writeoff_expected.json"))
                ),
                Arguments.of("multiple result- case non fine application fine amendment fine deemed serve amended at different times(DD-34578 SC4)",
                        newScenario()
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/application_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application amended1")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/fin-to-fin-ds-off1-amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/nces_notification_account_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/nces_notification_deemed_served_writeoff_expected_1.json"))
                                .newStep(newResultTrackedStep("application amended2")
                                        .withResultTrackedEvent("json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/fin-to-fin-ds-off2-amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/nces_notification_account_writeoff_expected_2.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/non-fin-case/multi-offence/fin-to-fin-results-amended-fin-ds-differenttime/nces_notification_deemed_served_writeoff_expected_2.json"))
                ),
                Arguments.of("multi results > case fin > app fin,ds > app amendment fine,ds > next hearing > DD-34586 SC2",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("application resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived",
                                                "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/nces_notification_granted.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/nces_notification_deemed_served_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("application amended")
                                        .withResultTrackedEvent("json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/application_amended.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/nces_notification_deemed_served_writeoff_expected_1.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments/fin-case/multi-offence/fin+ds-amended-fin+ds/nces_notification_account_writeoff_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))

                )
        );
    }

    public static Stream<Arguments> matchedDefsfinCaseMultiOffenceAppScenarios() {
        return Stream.of(
                Arguments.of("Matched Def > fin case > multi offences , FO DD-40638",
                        newScenario()
                                .newStep(newResultTrackedStep("case1 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_1_GD25910423_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case2 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_2_GD19416603_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("case2 amended")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_3_GD19416603_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/DD-40638/case_3_GD19416603_nces-duplicate-writeoff.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                                .newStep(newResultTrackedStep("case1 amended to adj")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_4_GD25910423_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/DD-40638/case_4_GD25910423_nces-duplicate-writeoff.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                                .newStep(newResultTrackedStep("case1 adj resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_5_GD25910423_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated")
                                )
                                .newStep(newResultTrackedStep("case1 adj amended")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_6_GD25910423_results_tracked.json",
                                                accountInfo("55c39541-e8e0-45b3-af99-532b33646b69", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/DD-40638/case_6_GD25910423_nces-duplicate-write-off.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                                .newStep(newResultTrackedStep("statdec created reulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/DD-40638/case_7_GD19416603_results_tracked.json",
                                                accountInfo("66c39541-e8e0-45b3-af99-532b33646b69", "66c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/DD-40638/case_7_GD19416603_nces-granted.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "66c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "55c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Matched Def > fin case > multi offences > Grnated, FO DD-38815 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/application-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/granted_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Matched Def 2 cases > case1 fine with multi offences > case2 fine with multi offences > app Granted, DD-40357 SC1",
                        newScenario()
                                .newStep(newResultTrackedStep("case1 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-fine-application-added/28DI2654240_1_case_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case2 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-fine-application-added/28DI4256692_1_case_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newNcesEmailForNewApplicationStep("app STAT_DEC received", "json/nces/application/matched-defendants/2-cases-resulted-fine-application-added/2_app_stat_dec_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION FOR A STATUTORY DECLARATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application/matched-defendants/2-cases-resulted-fine-application-added/2_app_expected_statdec_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app STAT_DEC resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-fine-application-added/2_app_stat_dec_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/2-cases-resulted-fine-application-added/2_app_granted_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Matched Def 2 INACTIVE cases > case1 fine, non-fine with multi offences > case2 fine, non-fine with multi offences > app allowed, DD-40488",
                        newScenario()
                                .newStep(newResultTrackedStep("case1 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-inactive-application-added/28DI975229_1_case_1_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case2 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-inactive-application-added/28DI975227_1_case_2_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))

                                .newStep(newNcesEmailForNewApplicationStep("app1 appeal received", "json/nces/application/matched-defendants/2-cases-resulted-inactive-application-added/2_app_appeal_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPEAL APPLICATION RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application/matched-defendants/2-cases-resulted-inactive-application-added/2_app_expected_appeal_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app APPEAL resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-inactive-application-added/2_app_appeal_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/2-cases-resulted-inactive-application-added/2_app_allowed_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Matched Def 2 INACTIVE cases > case1 fine, non-fine with multi offences > case2 fine, non-fine with multi offences > app refused, DD-40488",
                        newScenario()
                                .newStep(newResultTrackedStep("case1 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-inactive-application-refused/28DI975223_1_case_1_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newResultTrackedStep("case2 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-inactive-application-refused/28DI975224_1_case_1_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))

                                .newStep(newNcesEmailForNewApplicationStep("app1 REOPEN received", "json/nces/application/matched-defendants/2-cases-resulted-inactive-application-refused/2_app_reopen_1_send_nces_request.json")
                                        .withExpectedEventNames("NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadRegEx("NcesEmailNotificationRequested", ".*\"subject\":\"APPLICATION TO REOPEN RECEIVED\".*")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested",
                                                "json/nces/application/matched-defendants/2-cases-resulted-inactive-application-refused/2_app_expected_reopen_received_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                                .newStep(newResultTrackedStep("app REOPEN resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-cases-resulted-inactive-application-refused/2_app_reopen_results_tracked.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/2-cases-resulted-inactive-application-refused/2_app_refused_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT,11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                ),
                Arguments.of("Matched Def 2 cases > case1 fine with multi offences exits > case2 fine with multi offences  o1 FCOST > case2 amended o2 fine updated + DS, DD-40391",
                        newScenario()
                                .newStep(newResultTrackedStep("case1 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-related-cases-resulted-have-own-account-nos/2_case_1_results_tracked.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case1 amended")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-related-cases-resulted-have-own-account-nos/4_case_1_amend_results_tracked.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived"))
                                .newStep(newResultTrackedStep("case2 resulted")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-related-cases-resulted-have-own-account-nos/6_case_2_results_tracked.json",
                                                accountInfo("33c39541-e8e0-45b3-af99-532b33646b69", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("case2 amended")
                                        .withResultTrackedEvent("json/nces/application/matched-defendants/2-related-cases-resulted-have-own-account-nos/8_case_2_amend_results_tracked.json",
                                                accountInfo("44c39541-e8e0-45b3-af99-532b33646b69", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated",
                                                "NcesEmailNotificationRequested", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application/matched-defendants/2-related-cases-resulted-have-own-account-nos/9_dup_write_off_notification_expected.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "33c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "44c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                        )
                                )
                )
        );
    }

    public static Stream<Arguments> appAmendmentsNextHearingScenarios() {
        return Stream.of(
                Arguments.of("single offence > fin case > app > Granted, FO > app amended NEXH DD-37035",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/fin-case/single-offence/casefin-appfingranted-amended-nexh/case_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/fin-case/single-offence/casefin-appfingranted-amended-nexh/application_resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested", "HearingFinancialResultsUpdated", "MarkedAggregateSendEmailWhenAccountReceived", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments-nexh/fin-case/single-offence/casefin-appfingranted-amended-nexh/nces_granted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                                .newStep(newResultTrackedStep("app amended with NEXH")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/fin-case/single-offence/casefin-appfingranted-amended-nexh/application_amended.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                ),
//TODO add scenarios for app and offence adjournment to next hearing > next hearing finalise the application with Fine impositions
                Arguments.of("Next Hearing>> non fin case > app > finalised, offence adj > NextHearing : offence Fine & amended fine(tracking starts from adjourned)",
                        newScenario()
                                .newStep(newResultTrackedStep("adjourned app resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/nonfin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/adjourned_app_resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("adjourned app amended")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/nonfin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/adjourned_app_amended.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments-nexh/nonfin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/nces_ac_writeoff.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")))
                ),
                Arguments.of("TBD-CCT-2389:Need to change ** app resulted: Granted and offence adjourned payload***:: Next Hearing>> fin case > app > finalised, offence adj > NextHearing : offence Fine",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/fin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("app resulted: Granted and offence adjourned")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/fin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/app-resulted-hearing1.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                                .newStep(newResultTrackedStep("adj app resulted: fine")
                                        .withResultTrackedEvent("json/nces/application-amendments-nexh/fin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/app-resulted-hearing2.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "MarkedAggregateSendEmailWhenAccountReceived", "HearingFinancialResultsUpdated", "NcesEmailNotificationRequested", "UnmarkedAggregateSendEmailWhenAccountReceived")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/application-amendments-nexh/fin-case/single-offence/appfinal-off-adj-nexh-off-fin-amended-fin/nces_granted_offence_resulted_notification.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("oldGobAccountNumber", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT")
                                                        .withParam("gobAccountNumber", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                )
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("appAmendmentsNextHearingScenarios")
    void shouldCreateNCESNotificationForAppAmendmentsNexh(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("matchedDefsfinCaseMultiOffenceAppScenarios")
    void shouldCreateNCESNotificationForMatchedDefsCaseAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("caseAmendmentScenarios")
    void shouldCreateNCESNotificationForCaseAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("finCaseSingleOffenceAppScenarios")
    void shouldCreateNCESNotificationForAppSingleOffenceResults(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("finCaseMultiOffenceAppScenarios")
    void shouldCreateNCESNotificationForAppMultiOffenceResults(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("finCaseSingleOffenceAppAmendmentScenarios")
    void shouldCreateNCESNotificationForFinCaseSingleOffenceAppAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("finCaseMultiOffenceAppAmendmentScenarios")
    void shouldCreateNCESNotificationForFinCaseMultiOffenceAppAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("nonFinCaseSingleOffenceAppAmendmentScenarios")
    void shouldCreateNCESNotificationForNonFinCaseSingleOffenceAppAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("nonFinCaseMultiOffenceAppAmendmentScenarios")
    void shouldCreateNCESNotificationForNonFinCaseMultiOffenceAppAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }
}