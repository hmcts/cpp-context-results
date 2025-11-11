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
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.ApplicationDeemedServedNotificationRule;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationDeemedServedNotificationRuleTest {
    private ApplicationDeemedServedNotificationRule rule;

    static Stream<Arguments> validApplicationTypes() {
        return Stream.of(
                Arguments.of(STAT_DEC, "STAT_DEC"),
                Arguments.of(APPEAL, "APPEAL"),
                Arguments.of(REOPEN, "REOPEN")
        );
    }

    @BeforeEach
    void setUp() {
        rule = new ApplicationDeemedServedNotificationRule();
    }

    @Test
    void shouldGenerateDeemedServedNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder().withRequest(trackRequest.build()).build();

        assertThat("Rule should apply for Deemed Served application type", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail for Deemed Served", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationForNonDeemedServedFlow() {
        var trackRequest = hearingFinancialResultRequest().withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(false)
                                .withImpositionOffenceDetails("Non-Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                )).build();
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .build();

        var output = rule.apply(input);
        assertThat("notification should not be generated for non-Deemed Served flow", output.isEmpty());
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
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for " + description + " application type");
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
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
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
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when no application type");
    }

    @Test
    void shouldNotApplyForAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for amendment flow");
    }

    @ParameterizedTest
    @MethodSource("validApplicationTypes")
    void shouldGenerateDeemedServedNotificationForApplicationType(String applicationType, String description) {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(applicationType)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for " + description + " application type");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail for Deemed Served", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationWhenNoDeemedServedOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(false)
                                .withImpositionOffenceDetails("Regular offence details")
                                .withIsFinancial(true)
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
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
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
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
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
    void shouldHandleMultipleOffenceResultsWithMixedDeemedServedAndNonDeemedServed() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId1)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(false)
                                .withImpositionOffenceDetails("Regular offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for valid application type");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be two imposition offence details", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }
}