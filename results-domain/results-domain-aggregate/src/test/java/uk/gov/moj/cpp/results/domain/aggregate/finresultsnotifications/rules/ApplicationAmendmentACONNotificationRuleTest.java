package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPEAL;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentACONNotificationRule;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationAmendmentACONNotificationRuleTest {
    private ApplicationAmendmentACONNotificationRule rule;

    static Stream<Arguments> validApplicationTypes() {
        return Stream.of(
                Arguments.of(STAT_DEC, "STAT_DEC"),
                Arguments.of(APPEAL, "APPEAL"),
                Arguments.of(REOPEN, "REOPEN")
        );
    }

    @BeforeEach
    void setUp() {
        rule = new ApplicationAmendmentACONNotificationRule();
    }

    @Test
    void shouldGenerateACONNotification() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build())))
                .build();

        assertThat("Rule should apply for ACON application type", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> Assertions.fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest();
        trackRequest.withProsecutionCaseReferences(List.of("CaseId1"))
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

        assertThat("Rule should not apply for non-amendment flow", rule.appliesTo(input), is(false));
    }

    @ParameterizedTest
    @MethodSource("validApplicationTypes")
    void shouldGenerateACONNotificationForApplicationAmendment(String applicationType, String description) {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(applicationType)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(applicationType)
                                .withImpositionOffenceDetails("Previous ACON Offence details")
                                .build())))
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for " + description + " application type");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationWhenNoACONOffences() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Regular offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(STAT_DEC)
                                .withImpositionOffenceDetails("Previous Regular Offence details")
                                .build())))
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-ACON offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenOffenceIsNotFinancial() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(false)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(false)
                                .withApplicationType(STAT_DEC)
                                .withImpositionOffenceDetails("Previous ACON Offence details")
                                .build())))
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-financial offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenIsParentFlagIsFalse() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(false)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(STAT_DEC)
                                .withImpositionOffenceDetails("Previous ACON Offence details")
                                .build())))
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when isParentFlag is false");
    }

    @Test
    void shouldHandleMultipleOffenceResultsWithMixedACONAndNonACON() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId1)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build(),
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId2)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("Regular offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(
                                offenceResultsDetails()
                                        .withOffenceId(offenceId1)
                                        .withIsFinancial(true)
                                        .withApplicationType(STAT_DEC)
                                        .withImpositionOffenceDetails("Previous ACON Offence details")
                                        .build(),
                                offenceResultsDetails()
                                        .withOffenceId(offenceId2)
                                        .withIsFinancial(true)
                                        .withApplicationType(STAT_DEC)
                                        .withImpositionOffenceDetails("Previous Regular Offence details")
                                        .build())))
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for valid application type");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be two imposition offence details", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected ACON notification to be present"));
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
                                .withAmendmentDate("2023-10-01")
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
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when no application type");
    }

    @Test
    void shouldNotApplyWhenEmptyOffenceResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of());
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when no offence results");
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
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for " + description + " application type");
    }
}