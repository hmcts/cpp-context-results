package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static liquibase.util.Validate.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentACONNotificationRule;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseAmendmentACONNotificationRuleTest {

    private CaseAmendmentACONNotificationRule rule;

    @BeforeEach
    void setUp() {
        rule = new CaseAmendmentACONNotificationRule();
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
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("imposition details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for ACON amendment");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertEquals(NCESDecisionConstants.ACON_EMAIL_SUBJECT, notification.getSubject());
            assertEquals(1, notification.getImpositionOffenceDetails().size());
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for non-amendment ACON");
    }

    @Test
    void shouldNotApplyWhenNoCaseResult() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType("APPLICATION") // This makes it an application offence, not case
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when no case result");
    }

    @Test
    void shouldApplyForAmendmentProcessWithCaseResult() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for amendment process with case result");
    }

    @Test
    void shouldNotGenerateNotificationWhenNoACONOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode("NON-ACON")
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Regular offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when no ACON offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenOffenceIsNotFinancial() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-financial offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenNoAmendmentDate() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when no amendment date");
    }

    @Test
    void shouldGenerateACONNotificationWithValidData() {
        final UUID offenceId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withOffenceTitle("Test Offence Title")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail", notification.getImpositionOffenceDetails().size(), is(1));

            var impositionDetail = notification.getImpositionOffenceDetails().get(0);
            assertThat("offenceId should match", impositionDetail.getOffenceId(), is(offenceId));
            assertThat("details should match", impositionDetail.getDetails(), is("ACON Offence details"));
            assertThat("title should match", impositionDetail.getTitle(), is("Test Offence Title"));
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldHandleMultipleOffenceResultsWithMixedACONAndNonACON() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId1)
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(null)
                                .withResultCode("NON-ACON")
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Regular offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail", notification.getImpositionOffenceDetails().size(), is(2));

            var impositionDetail = notification.getImpositionOffenceDetails().get(0);
            assertThat("offenceId should match", impositionDetail.getOffenceId(), is(offenceId1));
        }, () -> fail("Expected ACON notification to be present"));
    }
}