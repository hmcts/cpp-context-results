package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static liquibase.util.Validate.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result.CaseDeemedServedNotificationRule;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseDeemedServedNotificationRuleTest {

    private CaseDeemedServedNotificationRule rule;

    @BeforeEach
    void setUp() {
        rule = new CaseDeemedServedNotificationRule();
    }

    @Test
    void shouldGenerateDeemedServedNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Imposition details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for Deemed Served");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("Deemed served subject should match", notification.getSubject(), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail for Deemed Served", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for amendment flow");
    }

    @Test
    void shouldApplyForCaseResultWithDeemedServed() {
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

        assertTrue(rule.appliesTo(input), "Rule should apply for case result with deemed served");
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
                                .withImpositionOffenceDetails("Regular offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when no deemed served offences");
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
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(null)
                                .withIsDeemedServed(false)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Regular offence details")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be two imposition offence details (deemed served and regular)", notification.getImpositionOffenceDetails().size(), is(2));
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
                                .withImpositionOffenceDetails("Deemed Served Offence details 1")
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details 2")
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be two imposition offence details", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }
}