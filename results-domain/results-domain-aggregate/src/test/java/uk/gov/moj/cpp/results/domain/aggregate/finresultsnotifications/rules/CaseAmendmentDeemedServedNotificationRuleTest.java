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
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentDeemedServedNotificationRule;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseAmendmentDeemedServedNotificationRuleTest {

    private CaseAmendmentDeemedServedNotificationRule rule;

    @BeforeEach
    void setUp() {
        rule = new CaseAmendmentDeemedServedNotificationRule();
    }

    @Test
    void shouldGenerateDeemedServedNotification() {
        var trackRequest = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Imposition Details")
                                .build()
                ));
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for Deemed Served amendment");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertEquals(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED, notification.getSubject());
            assertEquals(1, notification.getImpositionOffenceDetails().size());
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = HearingFinancialResultRequest.hearingFinancialResultRequest();
        trackRequest.withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .build()
                ));
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for non-amendment Deemed Served");
    }

    @Test
    void shouldNotApplyWhenNotAmendmentProcess() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when not amendment process");
    }

    @Test
    void shouldApplyForAmendmentProcessWithCaseResult() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for amendment process with case result");
    }

    @Test
    void shouldGenerateDeemedServedAmendmentNotification() {
        final UUID offenceId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withOffenceTitle("Test Offence Title")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for amendment process");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail", notification.getImpositionOffenceDetails().size(), is(1));

            var impositionDetail = notification.getImpositionOffenceDetails().get(0);
            assertThat("offenceId should match", impositionDetail.getOffenceId(), is(offenceId));
            assertThat("details should match", impositionDetail.getDetails(), is("Deemed Served Offence details"));
            assertThat("title should match", impositionDetail.getTitle(), is("Test Offence Title"));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationWhenNoDeemedServedOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(false)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Regular offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-deemed served offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenOffenceIsNotFinancial() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-financial offences");
    }

    @Test
    void shouldHandleMultipleOffenceResultsWithMixedDeemedServedAndNonDeemedServed() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId1)
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(null)
                                .withIsDeemedServed(false)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Regular offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for amendment process");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be two imposition offence details", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldHandleMultipleDeemedServedOffenceResults() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId1)
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details 1")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details 2")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for amendment process");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be two imposition offence details", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

}
