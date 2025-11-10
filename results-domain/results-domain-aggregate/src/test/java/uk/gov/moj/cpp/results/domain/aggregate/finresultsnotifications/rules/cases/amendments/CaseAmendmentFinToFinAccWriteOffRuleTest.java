package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments;

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

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.ResultNotificationRule;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class CaseAmendmentFinToFinAccWriteOffRuleTest {
    private final ResultNotificationRule rule = new CaseAmendmentFinToFinAccWriteOffRule();

    @Test
    @DisplayName("Should generate Acc Write Off notification for Fin to Fin amendement")
    void shouldGenerateAccWriteOffNotificationForFinToFinAmendment() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID hearingId = randomUUID();

        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withAccountCorrelationId(accountCorrelationId)
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("financial amendments1")
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
                                        .withHearingId(hearingId)
                                        .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
                                        .withAccountNumber("AC123456789_OLD")
                                        .build(),
                                correlationItem()
                                        .withHearingId(hearingId)
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
            assertThat("There should be one latest GOB for Acc Write Off", notification.getOldGobAccountNumber(), is("AC123456789"));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    @DisplayName("Should not generate Acc Write Off notification for initial financial result")
    void shouldNotApplyToNonAmendmentFlow() {
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(randomUUID())
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("financial impostions")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .build();

        assertThat("Rule should not apply for initial results flow", rule.appliesTo(input), is(false));
    }

    @Test
    @DisplayName("Should not generate Acc Write Off notification for application amendment flow")
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

    @Test
    void shouldGenerateAccWriteOffNotificationForFinTransitionFineOffence() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
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
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        offenceId1,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId1)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous Acc fin Offence details")
                                .build(),
                        offenceId2,
                        offenceResultsDetails()
                                .withIsFinancial(false)
                                .withOffenceId(offenceId2)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous Acc nonFin Offence details")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                offenceResultsDetails().withOffenceId(offenceId2).build()))
                                        .withAccountNumber("AC123456789_OLD")
                                        .build(),
                                correlationItem()
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountCorrelationId(randomUUID())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                offenceResultsDetails().withOffenceId(offenceId2).build()))
                                        .withAccountNumber("AC123456789")
                                        .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldGenerateAccWriteOffNotificationForFinTransitionNonFineOffence() {
        final UUID accountCorrelationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
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
                        offenceId1,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId1)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous nonFin fin Offence details")
                                .build(),
                        offenceId2,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId2)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous Acc nonFin Offence details")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                offenceResultsDetails().withOffenceId(offenceId2).build()))
                                        .withAccountNumber("AC123456789_OLD")
                                        .build(),
                                correlationItem()
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountCorrelationId(randomUUID())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                offenceResultsDetails().withOffenceId(offenceId2).build()))
                                        .withAccountNumber("AC123456789")
                                        .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    @DisplayName("Should not generate Acc Write Off notification for case if amendment is missing" +
            "[if offence1 is resulted out 2 offences and later offence2 is resulted]")
        // CCT-2357:DD-39223 AC7 discussion (aka: DD-40920 defect) - Need change based on the discussion outcome
    void shouldHandleMissingAmendmentDate() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID accountCorrelationId1 = randomUUID();
        final UUID accountCorrelationId2 = randomUUID();

        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withHearingId(hearingId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withAccountCorrelationId(accountCorrelationId1)
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("financial impostions1")
                                .build(),
                        offenceResults()
                                .withOffenceId(offenceId2)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("financial impostions2")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(offenceId1,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withHearingId(hearingId)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withAccountCorrelationId(accountCorrelationId2)
                                .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId1).build()))
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should not apply for financial to non-financial transition", rule.appliesTo(input), is(false));
    }

    @Test
    @DisplayName("Should not generate A&R for non-financial to financial transition - Post CCT-2390")
    void shouldHandleNonFinancialToFinancialTransition() {
        final UUID offenceId = randomUUID();
        final UUID hearingId = randomUUID();
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(randomUUID())
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withHearingId(hearingId)
                .withAccountCorrelationId(randomUUID())
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("fines amended")
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        offenceId,
                        offenceResultsDetails()
                                .withIsFinancial(false)
                                .withOffenceId(offenceId)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous non-financial offence details")
                                .build()))
                .build();

        assertThat("Rule should not apply for financial to non-financial transition", rule.appliesTo(input), is(false));

        final var output = rule.apply(input);
        assertThat("Rule should not generate A&R for non-financial to financial transition", output.isEmpty(), is(false));
    }

    @Test
    @DisplayName("Should handle multiple offences with mixed financial states")
        //TBD
    void shouldHandleMultipleOffencesWithMixedFinancialStates() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID accountCorrelationId = randomUUID();

        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withHearingId(hearingId)
                .withAccountCorrelationId(accountCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("fine impositions")
                                .withAmendmentDate("2023-01-01")
                                .build(),
                        offenceResults()
                                .withOffenceId(offenceId2)
                                .withImpositionOffenceDetails("non fine impositions")
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build(),
                        offenceResults()
                                .withOffenceId(offenceId3)
                                .withImpositionOffenceDetails("fine impositions")
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        offenceId1,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId1)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous financial offence 1")
                                .build(),
                        offenceId2,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId2)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous financial offence 2")
                                .build(),
                        offenceId3,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId3)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous financial offence 3")
                                .build()))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withHearingId(hearingId)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withAccountCorrelationId(randomUUID())
                                .withOffenceResultsDetailsList(List.of(
                                        offenceResultsDetails().withOffenceId(offenceId1).build(),
                                        offenceResultsDetails().withOffenceId(offenceId2).build(),
                                        offenceResultsDetails().withOffenceId(offenceId3).build()))
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        assertThat("Rule should not apply for financial to non-financial transition", rule.appliesTo(input), is(true));

        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(3));
            assertThat("There should be one latest GOB for Acc Write Off", notification.getOldGobAccountNumber(), is("AC123456789"));
        }, () -> fail("Expected Acc Write Off notification to be present"));

    }

    @Test
    @DisplayName("Should not generate A&R for missing previous non offence results details pre CCT-2390")
    void shouldHandleMissingPreviousOffenceResultsDetails() {
        final UUID offenceId = randomUUID();
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .build();

        assertThat("Rule should not apply when previous offence details are missing", rule.appliesTo(input), is(false));
    }

    @Test
    @DisplayName("Should generate A&R case with single offence fin to fin transition")
    void shouldHandleBoundaryCaseWithSingleOffence() {
        final UUID offenceId = randomUUID();
        final UUID accountCorrelationId = randomUUID();
        final UUID accountCorrelationId1 = randomUUID();
        final UUID hearingId = randomUUID();

        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withHearingId(hearingId)
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("financial offence details")
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .withAccountCorrelationId(accountCorrelationId1)
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        offenceId,
                        offenceResultsDetails()
                                .withIsFinancial(true)
                                .withOffenceId(offenceId)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous financial offence details")
                                .build()))
                .withCorrelationItemList(List.of(
                        correlationItem()
                                .withHearingId(hearingId)
                                .withAccountCorrelationId(accountCorrelationId)
                                .withAccountNumber("AC123456789")
                                .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).build()))
                                .build()))
                .build();

        assertThat("Rule should apply for single offence", rule.appliesTo(input), is(true));
        final var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
            assertThat("There should be one latest GOB for Acc Write Off", notification.getOldGobAccountNumber(), is("AC123456789"));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    @DisplayName("Should not generate A&R for case with non financial amendments - post 2390")
    void shouldHandleCaseWithNoFinancialAmendments() {
        final UUID offenceId = randomUUID();
        HearingFinancialResultRequest trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withOffenceId(offenceId)
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .build();
        final ResultNotificationRule.RuleInput input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevOffenceResultsDetails(Map.of(
                        offenceId,
                        offenceResultsDetails()
                                .withIsFinancial(false)
                                .withOffenceId(offenceId)
                                .withCreatedTime(ZonedDateTime.now().minusHours(1))
                                .withImpositionOffenceDetails("Previous non-financial offence details")
                                .build()))
                .build();

        assertThat("Rule should not apply for non-financial amendments", rule.appliesTo(input), is(false));
    }
}