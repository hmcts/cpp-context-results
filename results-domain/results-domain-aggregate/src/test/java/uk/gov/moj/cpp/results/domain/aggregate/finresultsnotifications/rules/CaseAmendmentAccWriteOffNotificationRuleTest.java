package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static liquibase.util.Validate.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentAccWriteOffNotificationRule;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CaseAmendmentAccWriteOffNotificationRuleTest {
    private final CaseAmendmentAccWriteOffNotificationRule rule = new CaseAmendmentAccWriteOffNotificationRule();

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToFinAmendment() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceId = randomUUID();
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withAccountCorrelationId(accountCorrelationId)
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
                                        .withAccountNumber("AC123456789_OLD")
                                        .build(),
                                correlationItem()
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountCorrelationId(randomUUID())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
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
                .withAccountCorrelationId(accountCorrelationId)
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