package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static liquibase.util.Validate.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.core.courts.CorrelationIdHistoryItem.correlationIdHistoryItem;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CaseAmendmentAccWriteOffNotificationRuleTest {
    private final CaseAmendmentAccWriteOffNotificationRule rule = new CaseAmendmentAccWriteOffNotificationRule();

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToFinAmendment() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .withAccountCorrelationId(randomUUID())
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()))
                .withCorrelationIdHistoryItemList(
                        List.of(correlationIdHistoryItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToNonFinAmendment() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .withAccountCorrelationId(randomUUID())
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()))
                .withCorrelationIdHistoryItemList(
                        List.of(correlationIdHistoryItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToNonFinAmendmentMultiOffences() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .build(),
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build(),
                        trackRequest.getOffenceResults().get(1).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()
                ))
                .withCorrelationIdHistoryItemList(
                        List.of(correlationIdHistoryItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .build();

        assertThat("Rule should not apply for non-amendment flow", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToApplicationAmendmentFlow() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .build();

        assertThat("Rule should not apply for application amendment flow", rule.appliesTo(input), is(false));
    }
}