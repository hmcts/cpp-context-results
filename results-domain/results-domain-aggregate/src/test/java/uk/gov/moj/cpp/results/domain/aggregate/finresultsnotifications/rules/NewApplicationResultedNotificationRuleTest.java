package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.core.courts.CorrelationIdHistoryItem.correlationIdHistoryItem;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NewApplicationResultedNotificationRuleTest {
    private final NewApplicationResultedNotificationRule rule = new NewApplicationResultedNotificationRule();

    @Test
    void shouldGenerateNotificationForNewApplicationWithFinancialImpositions() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(REOPEN)
                                .withApplicationId(randomUUID())
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withAmendmentDate(null)
                                .withResultCode(NCESDecisionConstants.G)
                                .build()))
                .withAccountCorrelationId(randomUUID())
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
                .withCorrelationIdHistoryItemList(
                        List.of(correlationIdHistoryItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply to new application", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("APPLICATION TO REOPEN GRANTED"));
        }, () -> fail("Expected notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationForAdjournedApplicationWhichIsAlreadyResulted() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(REOPEN)
                                .withApplicationId(randomUUID())
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withAmendmentDate(null)
                                .withResultCode(NCESDecisionConstants.G)
                                .build()))
                .withAccountCorrelationId(randomUUID())
                .build();
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Offence details")
                                .build()))
                .withPrevApplicationResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getApplicationId(),
                        List.of(offenceResultsDetails()
                                .withApplicationType(REOPEN)
                                .withResultCode(NCESDecisionConstants.G)
                                .build()
                        )))
                .withCorrelationIdHistoryItemList(
                        List.of(correlationIdHistoryItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should not apply to application already resulted", !rule.appliesTo(input));
    }
}