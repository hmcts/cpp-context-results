package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
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

class ApplicationAmendmentFinToNonFinAccWriteOffRuleTest {
    private final ApplicationAmendmentFinToNonFinAccWriteOffRule rule = new ApplicationAmendmentFinToNonFinAccWriteOffRule();

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToNonFinAmendment() {
        final UUID applicationId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(randomUUID())
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToNonFinAmendmentMultiOffences() {
        final UUID applicationId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build(),
                        offenceResults()
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(randomUUID())
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                        .withOffenceId(trackRequest.getOffenceResults().get(1).getOffenceId())
                                        .withIsFinancial(true)
                                        .withApplicationType(NCESDecisionConstants.REOPEN)
                                        .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                        .build(),
                                offenceResultsDetails()
                                        .withOffenceId(trackRequest.getOffenceResults().get(1).getOffenceId())
                                        .withIsFinancial(true)
                                        .withApplicationType(NCESDecisionConstants.REOPEN)
                                        .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                        .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .build()))
                .build();
        final UUID applicationId = randomUUID();
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
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

        assertThat("Rule should not apply for non-amendment flow", !rule.appliesTo(input));
    }
}