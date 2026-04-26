package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.G;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.RFSD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationStatusAmendmentNotificationRuleTest {
    private final ApplicationStatusAmendmentNotificationRule rule = new ApplicationStatusAmendmentNotificationRule();

    @Test
    void shouldGenerateApplicationStatusAmendmentNotificationWhenApplicationResultChanges() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationId(applicationId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(RFSD)
                                .withApplicationResultType("Granted")
                                .withIsParentFlag(false)
                                .withIsFinancial(false)
                                .withOffenceTitle("Current offence title")
                                .withAmendmentDate("2023-10-01")
                                .build()))
                .build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId, previousFinancialOffence(offenceId)))
                .withPrevApplicationResultsDetails(Map.of(applicationId, List.of(previousApplicationResult(offenceId, applicationId, G))))
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply when the application result has changed and prior financial impositions exist");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.APPLICATION_SUBJECT.get(STAT_DEC).get(RFSD)));
            assertThat("There should be one original financial offence detail", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected application status amendment notification to be present"));
    }

    @Test
    void shouldNotApplyWhenApplicationResultHasNotChanged() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationId(applicationId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
                                .withIsParentFlag(true)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Current financial details")
                                .withAmendmentDate("2023-10-01")
                                .build()))
                .build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId, previousFinancialOffence(offenceId)))
                .withPrevApplicationResultsDetails(Map.of(applicationId, List.of(previousApplicationResult(offenceId, applicationId, G))))
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when the application result code is unchanged");
    }

    @Test
    void shouldNotApplyWhenCurrentRequestHasAccountCorrelation() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(randomUUID())
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationId(applicationId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
                                .withIsParentFlag(true)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Current financial details")
                                .withAmendmentDate("2023-10-01")
                                .build()))
                .build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId, previousFinancialOffence(offenceId)))
                .withPrevApplicationResultsDetails(Map.of(applicationId, List.of(previousApplicationResult(offenceId, applicationId, RFSD))))
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when the request already has an account correlation");
    }

    @Test
    void shouldNotApplyWhenNoFinancialImpositionExistsPriorToThisApplication() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationId(applicationId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
                                .withIsParentFlag(true)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Current financial details")
                                .withAmendmentDate("2023-10-01")
                                .build()))
                .build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId, previousOffence(offenceId, false, "Previous non-financial details")))
                .withPrevApplicationResultsDetails(Map.of(applicationId, List.of(previousApplicationResult(offenceId, applicationId, RFSD))))
                .build();

        assertFalse(rule.appliesTo(input), "Rule should not apply when no previous financial imposition exists for the offence");
    }

    @Test
    void shouldNotGenerateNotificationForFineToNonFineAmendment() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationId(applicationId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
                                .withApplicationResultType("Granted")
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("Current non-financial details")
                                .withAmendmentDate("2023-10-01")
                                .build()))
                .build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId, previousFinancialOffence(offenceId)))
                .withPrevApplicationResultsDetails(Map.of(applicationId, List.of(previousApplicationResult(offenceId, applicationId, RFSD))))
                .build();

        assertTrue(rule.appliesTo(input), "Rule should still apply before apply() filters out fine-to-non-fine amendments");
        var output = rule.apply(input);

        assertThat("No notification should be generated for fine-to-non-fine amendments", output.isEmpty(), is(true));
    }

    private OffenceResultsDetails previousFinancialOffence(final UUID offenceId) {
        return previousOffence(offenceId, true, "Previous financial details");
    }

    private OffenceResultsDetails previousOffence(final UUID offenceId, final boolean isFinancial, final String impositionOffenceDetails) {
        return offenceResultsDetails()
                .withOffenceId(offenceId)
                .withIsFinancial(isFinancial)
                .withImpositionOffenceDetails(impositionOffenceDetails)
                .withCreatedTime(ZonedDateTime.now().minusDays(1))
                .build();
    }

    private OffenceResultsDetails previousApplicationResult(final UUID offenceId, final UUID applicationId, final String resultCode) {
        return offenceResultsDetails()
                .withOffenceId(offenceId)
                .withApplicationId(applicationId)
                .withApplicationType(STAT_DEC)
                .withResultCode(resultCode)
                .build();
    }

}