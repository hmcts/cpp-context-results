package uk.gov.moj.cpp.results.domain.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.accountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.AccountInfo.emptyAccountInfo;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Comparison.comparison;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.ResultTrackedStep.newResultTrackedStep;
import static uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario.newScenario;

import uk.gov.moj.cpp.results.domain.aggregate.HearingFinancialResultAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link HearingFinancialResultsAggregate} that verify the creation of NCES notifications for sjp case .
 */
class HearingFinancialResultsAggregateForSjpNCESTest {

    public static Stream<Arguments> sjpReopenApplicationScenarios() {
        return Stream.of(
                Arguments.of("dd-40300: AC1 resulted SJP case refer to CC with REOPEN application. All offences has resulted with financial result in CC",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac1-dd-40300/sjp-case-resulted.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked","HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("reopen application")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac1-dd-40300/sjp-case-reopened.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked","NcesEmailNotificationRequested")// updated notification
                                )
                                .newStep(newResultTrackedStep("case adj in cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac1-dd-40300/cc-case-adj.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                                .newStep(newResultTrackedStep("case resulted in cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac1-dd-40300/cc-case-resulted.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked","HearingFinancialResultsUpdated","MarkedAggregateSendEmailWhenAccountReceived","UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/sjp/reopen-application/ac1-dd-40300/cc-case-resulted-expected-event.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("subject", "APPLICATION TO REOPEN GRANTED"))
                                )
                ),
                Arguments.of("dd-40300: AC3 resulted SJP case refer to CC with REOPEN application. All offences has resulted with financial result in CC and OATS",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac3-dd-40300/sjp-case-resulted_3.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("reopen application")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac3-dd-40300/sjp-case-reopened_3.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked","NcesEmailNotificationRequested")
                                )
                                .newStep(newResultTrackedStep("case resulted in cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac3-dd-40300/cc-case-resulted_3.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked","HearingFinancialResultsUpdated","MarkedAggregateSendEmailWhenAccountReceived","UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/sjp/reopen-application/ac3-dd-40300/cc-case-resulted-expected-event_3.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("subject", "APPLICATION TO REOPEN GRANTED"))
                                )
                ),
                Arguments.of("dd-40300: AC4 resulted SJP case and then adjourn to sjp then refer to CC with REOPEN application. All offences has resulted with financial result in CC and OATS",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac4-dd-40300/sjp-case-resulted_4.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("sjp adjourn")
                                    .withResultTrackedEvent("json/nces/sjp/reopen-application/ac4-dd-40300/sjp-case-reopen-adj-to-sjp_4.json",
                                            emptyAccountInfo())
                                .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("case adj to cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac4-dd-40300/sjp-case-reopen-adj-to-sjp_4.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                                .newStep(newResultTrackedStep("case resulted in cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac4-dd-40300/cc-case-resulted_4.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked","HearingFinancialResultsUpdated","MarkedAggregateSendEmailWhenAccountReceived","UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/sjp/reopen-application/ac4-dd-40300/cc-case-resulted-expected-event_4.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("subject", "APPLICATION TO REOPEN GRANTED"))
                                )
                ),
                Arguments.of("dd-40300: AC5 resulted SJP case and then adjourn to sjp then refer to CC with REOPEN application. All offence has not resulted with financial result in CC ",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac5-dd-40300/sjp-case-resulted_5.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("sjp adjourn")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac5-dd-40300/sjp-case-reopen-adj-to-sjp_5.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("sjp adjourn")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac5-dd-40300/sjp-case-reopen-adj-to-sjp2_5.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newResultTrackedStep("case resulted in cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac5-dd-40300/cc-case-resulted_5.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked")
                                )
                ),
                Arguments.of("dd-40300: AC6 resulted SJP case then refer to CC with REOPEN application. Resulted with matched case in CC",
                        newScenario()
                                .newStep(newResultTrackedStep("case resulted")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac6-dd-40300/sjp-case-resulted_6.json",
                                                accountInfo("11c39541-e8e0-45b3-af99-532b33646b69", "11c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "HearingFinancialResultsUpdated"))
                                .newStep(newResultTrackedStep("sjp adjourn")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac6-dd-40300/sjp-case-reopened_6.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked", "NcesEmailNotificationRequested"))
                                .newStep(newResultTrackedStep("sjp adjourn")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac6-dd-40300/cc-case-reopen-adj-to-cc_6.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                                .newStep(newResultTrackedStep("case resulted in cc")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac6-dd-40300/cc-case-resulted_6.json",
                                                accountInfo("22c39541-e8e0-45b3-af99-532b33646b69", "22c39541-e8e0-45b3-af99-532b33646b69ACCOUNT"))
                                        .withExpectedEventNames("HearingFinancialResultsTracked","HearingFinancialResultsUpdated","MarkedAggregateSendEmailWhenAccountReceived","UnmarkedAggregateSendEmailWhenAccountReceived", "NcesEmailNotificationRequested")
                                        .withExpectedEventPayloadEquals("NcesEmailNotificationRequested", "json/nces/sjp/reopen-application/ac6-dd-40300/cc-case-resulted-expected-event_6.json",
                                                comparison()
                                                        .withPathsExcluded("materialId", "notificationId")
                                                        .withParam("subject", "APPLICATION TO REOPEN GRANTED"))
                                )
                                .newStep(newResultTrackedStep("matched cc case resulted")
                                        .withResultTrackedEvent("json/nces/sjp/reopen-application/ac6-dd-40300/cc-matched-case-resulted_6.json",
                                                emptyAccountInfo())
                                        .withExpectedEventNames("HearingFinancialResultsTracked"))
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("sjpReopenApplicationScenarios")
    void shouldCreateNCESNotificationForSjpReferralApplications(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new HearingFinancialResultsAggregate()));
    }


}
