package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CaseAmendmentMixedFinNonFinAccWriteOffRuleTest {

    private final ResultNotificationRule rule = new CaseAmendmentMixedFinNonFinAccWriteOffRule();

    @Test
    void shouldNotApplyToApplicationAmendments() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .withApplicationType("appType")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .withApplicationType("appType")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat(rule.appliesTo(input), is(false));
    }

    @Test
    void shouldApplyToCaseWhenNoPreviousOffencesFoundAndAllCurrentRequestOffencesAreFin() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of())
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat(rule.appliesTo(input), is(true));
    }

    @Test
    void shouldApplyToCase_whenAllCurrentRequestOffencesAreFin_andPreviousOffencesHaveMixedFinancials_andHasNonFinToFinTransition() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build(),
                        offenceResults()
                                .withOffenceId(offenceId2)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId1, offenceResultsDetails().withOffenceId(offenceId1).withIsFinancial(false).build(),
                        offenceId2, offenceResultsDetails().withOffenceId(offenceId2).withIsFinancial(true).build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat(rule.appliesTo(input), is(true));
    }

}