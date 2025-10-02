package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result.CaseACONNotificationRule;
;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseACONNotificationRuleTest {
    private CaseACONNotificationRule rule;

    @BeforeEach
    void setUp() {
        rule = new CaseACONNotificationRule();
    }

    @Test
    void shouldGenerateACONNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Impositions details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();
        assertTrue(rule.appliesTo(input), "Rule should apply for ACON");

        var output = rule.apply(input);
        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest();
        trackRequest.withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withAmendmentDate("2023-10-01")
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for ACON in amendment flow", !rule.appliesTo(input));
    }

    @Test
    void shouldNotGenerateNotificationWhenNoACONOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode("OTHER")
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Other imposition details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-ACON offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenACONOffenceIsNotFinancial() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("ACON imposition details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-financial ACON offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenOffenceHasApplicationType() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType("APPLICATION")
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("ACON imposition details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for application offences");
    }

    @Test
    void shouldGenerateNotificationWithMixedOffenceTypesWhenACONIsPresent() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode("OTHER")
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Other imposition details")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("ACON imposition details")
                                .withOffenceTitle("ACON Offence")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertTrue(output.isPresent(), "Expected ACON notification to be present");
        output.ifPresent(notification -> {
            assertThat("Subject should match ACON email subject", 
                notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("Should have two imposition offence details (both financial)", 
                notification.getImpositionOffenceDetails().size(), is(2));
        });
    }

    @Test
    void shouldHandleLargeNumberOfMixedOffencesWithSingleACON() {
        var offences = List.of(
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(null)
                        .withResultCode("FINE")
                        .withIsFinancial(true)
                        .withImpositionOffenceDetails("Fine imposition details")
                        .build(),
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(null)
                        .withResultCode("CUSTODIAL")
                        .withIsFinancial(false)
                        .withImpositionOffenceDetails("Custodial imposition details")
                        .build(),
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(null)
                        .withResultCode(NCESDecisionConstants.ACON)
                        .withIsFinancial(true)
                        .withImpositionOffenceDetails("ACON imposition details")
                        .withOffenceTitle("ACON Offence")
                        .build(),
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(null)
                        .withResultCode("COMMUNITY")
                        .withIsFinancial(false)
                        .withImpositionOffenceDetails("Community imposition details")
                        .build()
        );

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(offences);
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertTrue(output.isPresent(), "Expected ACON notification to be present");
        output.ifPresent(notification -> {
            assertThat("Subject should match ACON email subject", 
                notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("Should have two financial imposition offence details", 
                notification.getImpositionOffenceDetails().size(), is(2));
        });
    }

    @Test
    void shouldOnlyProcessCaseLevelOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType("APPLICATION")
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Application ACON imposition details")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Case ACON imposition details")
                                .withOffenceTitle("Case ACON Offence")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertTrue(output.isPresent(), "Expected ACON notification to be present");
        output.ifPresent(notification -> {
            assertThat("Should have one case-level imposition offence detail", 
                notification.getImpositionOffenceDetails().size(), is(1));
        });
    }

    @Test
    void shouldHandleCaseWithMultipleProsecutionCaseReferences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1", "CaseId2", "CaseId3"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("ACON imposition details")
                                .withOffenceTitle("ACON Offence")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertTrue(output.isPresent(), "Expected ACON notification to be present");
        output.ifPresent(notification -> {
            assertThat("Subject should match ACON email subject", 
                notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("Should have one imposition offence detail", 
                notification.getImpositionOffenceDetails().size(), is(1));
        });
    }
}