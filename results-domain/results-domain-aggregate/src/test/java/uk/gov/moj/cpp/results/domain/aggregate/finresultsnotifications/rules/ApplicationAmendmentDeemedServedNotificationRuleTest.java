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
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentDeemedServedNotificationRule;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationAmendmentDeemedServedNotificationRuleTest {
    private ApplicationAmendmentDeemedServedNotificationRule rule;

    static Stream<Arguments> validApplicationTypes() {
        return Stream.of(
                Arguments.of(STAT_DEC, "STAT_DEC"),
                Arguments.of(APPEAL, "APPEAL"),
                Arguments.of(REOPEN, "REOPEN")
        );
    }

    @BeforeEach
    void setUp() {
        rule = new ApplicationAmendmentDeemedServedNotificationRule();
    }

    @Test
    void shouldApplyToAmendmentFlowWithValidApplicationType() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Amendment details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for amendment flow with valid application type");
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for non-amendment flow");
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
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply for invalid application type");
    }

    @Test
    void shouldGenerateDeemedServedAmendmentNotification() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID correlationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(correlationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Amendment details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for deemed served amendment");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served Amendment notification to be present"));
    }

    @Test
    void shouldGenerateDeemedServedAmendmentNotificationWithoutCorrelationId() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Amendment details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for deemed served amendment without correlation ID");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served Amendment notification to be present"));
    }

    @Test
    void shouldGenerateDeemedServedRemovedNotification() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID correlationId = randomUUID();

        // Create previous offence results that were deemed served
        final OffenceResultsDetails prevOffenceResult = OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(offenceId)
                .withIsDeemedServed(true)
                .withIsFinancial(true)
                .build();

        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap =
                Map.of(applicationId, List.of(prevOffenceResult));

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(correlationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(false) // Now not deemed served
                                .withImpositionOffenceDetails("Deemed Served Removed details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .withPrevApplicationOffenceResultsMap(prevApplicationOffenceResultsMap)
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for deemed served removed");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED_REMOVED));
            assertThat("There should be one imposition offence detail", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served Removed notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationForNonFinancialOffences() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Amendment details")
                                .withIsFinancial(false) // Not financial
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification for non-financial offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenNoDeemedServedOffences() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(false) // Not deemed served
                                .withImpositionOffenceDetails("Regular Amendment details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when no deemed served offences");
    }

    @Test
    void shouldNotGenerateNotificationWhenIsParentFlagIsFalse() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(false) // Not parent flag
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Amendment details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when isParentFlag is false");
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
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Amendment details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for " + description + " application type");
    }

    @Test
    void shouldNotGenerateDeemedServedRemovedNotificationWhenNoPreviousDeemedServed() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        // Create previous offence results that were NOT deemed served
        final OffenceResultsDetails prevOffenceResult = OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(offenceId)
                .withIsDeemedServed(false) // Was not deemed served
                .withIsFinancial(true)
                .build();

        final Map<UUID, List<OffenceResultsDetails>> prevApplicationOffenceResultsMap =
                Map.of(applicationId, List.of(prevOffenceResult));

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-10-01")
                                .withIsDeemedServed(false) // Still not deemed served
                                .withImpositionOffenceDetails("Regular Amendment details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .withPrevApplicationOffenceResultsMap(prevApplicationOffenceResultsMap)
                .build();

        var output = rule.apply(input);
        assertFalse(output.isPresent(), "Should not generate notification when previous offence was not deemed served");
    }
}
