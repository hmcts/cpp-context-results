package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.NewApplicationUpdatedNotificationRule;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NewApplicationUpdatedNotificationRuleTest {
    private final NewApplicationUpdatedNotificationRule rule = new NewApplicationUpdatedNotificationRule();

    @Test
    void shouldGenerateUpdateNotificationForAdjournedApplication() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(STAT_DEC)
                                .withApplicationResultType("Adjournment")
                                .withResultCode(null)
                                .withAmendmentDate(null)
                                .withApplicationId(randomUUID())
                                .withOffenceId(randomUUID())
                                .build()))
                .build();
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Offence details")
                                .build()))
                .withPrevApplicationResultsDetails(Map.of())
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply to new application", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("STATUTORY DECLARATION UPDATED"));
        }, () -> fail("Expected notification to be present"));
    }

    @Test
    void shouldNotGenerateUpdateNotificationForAdjournedApplicationWithoutAnyFinancialImpositions() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(STAT_DEC)
                                .withApplicationResultType("Adjournment")
                                .withResultCode(null)
                                .withAmendmentDate(null)
                                .withApplicationId(randomUUID())
                                .withOffenceId(randomUUID())
                                .build()))
                .build();
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("Previous Offence details")
                                .build()))
                .withPrevApplicationResultsDetails(Map.of())
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply to new application", rule.appliesTo(input));
        var output = rule.apply(input);
        assertThat("No notification should be generated", output.isEmpty());
    }
}