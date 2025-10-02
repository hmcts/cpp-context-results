package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPEAL;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.ApplicationACONNotificationRule;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationACONNotificationRuleTest {
    private final ApplicationACONNotificationRule rule = new ApplicationACONNotificationRule();

    @Test
    void shouldGenerateACONNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for ACON application type");
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
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for non-amendment flow", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyWhenNoValidApplicationType() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType("INVALID_TYPE")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for invalid application type");
    }

    @Test
    void shouldNotApplyWhenNoApplicationType() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when no application type");
    }

    @ParameterizedTest
    @MethodSource("validApplicationTypes")
    void shouldApplyForValidApplicationTypes(String applicationType, String description) {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(applicationType)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for " + description + " application type");
    }

    static Stream<Arguments> validApplicationTypes() {
        return Stream.of(
                Arguments.of(STAT_DEC, "STAT_DEC"),
                Arguments.of(APPEAL, "APPEAL"),
                Arguments.of(REOPEN, "REOPEN")
        );
    }

    @Test
    void shouldGenerateACONNotificationForAppealApplication() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(APPEAL)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for APPEAL application type");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldGenerateACONNotificationForReopenApplication() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(REOPEN)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for REOPEN application type");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationWhenNoACONOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Regular offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-ACON offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenOffenceIsNotFinancial() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(false)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-financial offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenIsParentFlagIsFalse() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(false)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when isParentFlag is false");
    }

    @Test
    void shouldGenerateNotificationWithMixedOffenceTypesWhenACONIsPresent() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Regular offence details")
                                .withIsFinancial(true)
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
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
                        .withApplicationType(STAT_DEC)
                        .withIsParentFlag(true)
                        .withImpositionOffenceDetails("Regular offence details 1")
                        .withIsFinancial(true)
                        .build(),
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(STAT_DEC)
                        .withIsParentFlag(true)
                        .withImpositionOffenceDetails("Regular offence details 2")
                        .withIsFinancial(false)
                        .build(),
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(STAT_DEC)
                        .withIsParentFlag(true)
                        .withImpositionOffenceDetails("ACON Offence details")
                        .withIsFinancial(true)
                        .withOffenceTitle("ACON Offence")
                        .build(),
                OffenceResults.offenceResults()
                        .withOffenceId(randomUUID())
                        .withApplicationType(STAT_DEC)
                        .withIsParentFlag(true)
                        .withImpositionOffenceDetails("Regular offence details 3")
                        .withIsFinancial(false)
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
    void shouldOnlyProcessApplicationLevelOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Case ACON imposition details")
                                .withIsFinancial(true)
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Application ACON imposition details")
                                .withIsFinancial(true)
                                .withOffenceTitle("Application ACON Offence")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertTrue(output.isPresent(), "Expected ACON notification to be present");
        output.ifPresent(notification -> {
            assertThat("Should have one application-level imposition offence detail", 
                notification.getImpositionOffenceDetails().size(), is(1));
        });
    }

    @Test
    void shouldHandleApplicationWithMultipleProsecutionCaseReferences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1", "CaseId2", "CaseId3"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
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