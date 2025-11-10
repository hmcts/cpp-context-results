package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder;
import uk.gov.moj.cpp.results.domain.aggregate.utils.OffenceResultsResolver;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationOnlyAmendmentAccWriteOffRuleTest {
    private final ApplicationOnlyAmendmentAccWriteOffRule rule = new ApplicationOnlyAmendmentAccWriteOffRule();

    @Test
    void shouldApplyOnlyForApplicationResultsOnlyAmendment() {
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withApplicationId(applicationId)
                                .withOffenceId(offenceId)
                                .withIsParentFlag(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2025-01-01")
                                .withResultCode(NCESDecisionConstants.G)
                                .build()))
                .build();

        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationResultsDetails(Map.of(applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(offenceId)
                                // previous imposition was financial to model application-only change
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withResultCode(NCESDecisionConstants.WDRN)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertTrue(input.hasValidApplicationType(), "expected hasValidApplicationType true");
        assertTrue(input.isAmendmentFlow(), "expected isAmendmentFlow true");
        assertTrue(input.prevApplicationResultsDetails().containsKey(applicationId), "expected prevApplicationResultsDetails to contain applicationId");

        final var newOffences = OffenceResultsResolver.getNewOffenceResultsAppAmendment(input.request().getOffenceResults(), input.prevOffenceResultsDetails(), input.prevApplicationOffenceResultsMap(), input.prevApplicationResultsDetails());
        assertTrue(newOffences.isEmpty(), "expected newOffenceResults to be empty");

        assertTrue(rule.appliesTo(input), "Rule should apply for application-only amendment");
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
