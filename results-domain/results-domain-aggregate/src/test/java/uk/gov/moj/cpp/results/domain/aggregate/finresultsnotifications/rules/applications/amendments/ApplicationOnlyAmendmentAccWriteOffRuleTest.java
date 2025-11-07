package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationOnlyAmendmentAccWriteOffRuleTest {
    private final ApplicationOnlyAmendmentAccWriteOffRule rule = new ApplicationOnlyAmendmentAccWriteOffRule();

    @Test
    void shouldApplyOnlyForApplicationResultsOnlyAmendment() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withApplicationId(randomUUID())
                                .withOffenceId(randomUUID())
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withAmendmentDate("2025-01-01")
                                .withResultCode(NCESDecisionConstants.G)
                                .build()))
                .build();

        final UUID applicationId = randomUUID();
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getApplicationId(),
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply for application-only amendment", rule.appliesTo(input));
    }

    @Test
    void shouldNotApplyWhenNonFineToNonFine() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withApplicationId(randomUUID())
                                .withOffenceId(randomUUID())
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withAmendmentDate("2025-01-01")
                                .build()))
                .build();

        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        randomUUID(),
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(false)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should not apply for non-fine to non-fine amendment", !rule.appliesTo(input));
    }
}
