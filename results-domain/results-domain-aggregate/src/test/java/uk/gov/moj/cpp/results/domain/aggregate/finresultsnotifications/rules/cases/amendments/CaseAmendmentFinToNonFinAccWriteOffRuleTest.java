package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CaseAmendmentFinToNonFinAccWriteOffRuleTest {
    private final ResultNotificationRule rule = new CaseAmendmentFinToNonFinAccWriteOffRule();

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
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
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
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID accountCorrelationId = randomUUID();
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .build(),
                        offenceResults()
                                .withOffenceId(offenceId2)
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build(),
                        trackRequest.getOffenceResults().get(1).getOffenceId(),
                        offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(1).getOffenceId())
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()
                ))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                        .withAccountCorrelationId(randomUUID())
                                        .withAccountNumber("AC123456789OLD")
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                offenceResultsDetails().withOffenceId(offenceId2).withCreatedTime(ZonedDateTime.now().minusHours(1)).build()))
                                        .build(),
                                correlationItem()
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountCorrelationId(accountCorrelationId)
                                        .withAccountNumber("AC123456789")
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build()))
                                        .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be two imposition offence details for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }
}