package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResults.offenceResults;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplicationAmendmentFinToFinAccWriteOffRuleTest {
    private final ApplicationAmendmentFinToFinAccWriteOffRule rule = new ApplicationAmendmentFinToFinAccWriteOffRule();

    @Test
    void shouldGenerateAccWriteOffNotificationForFinToFinAmendment() {
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .withAccountCorrelationId(randomUUID())
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
                                        .withAccountCorrelationId(randomUUID())
                                        .withCreatedTime(ZonedDateTime.now().minusHours(2))
                                        .withAccountNumber("AC123456789OLD")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId).build()))
                                        .build(),
                                correlationItem()
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountNumber("AC123456789")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId).build()))
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
    void shouldGenerateAccWriteOffNotificationForSentenceVariedAmendment() {
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();
        final UUID offenceId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withAccountCorrelationId(currentCorrelationId)
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withImpositionOffenceDetails(NCESDecisionConstants.SV_SENTENCE_VARIED)
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withIsParentFlag(true)
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
                                .withApplicationType(NCESDecisionConstants.APPEAL)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withAccountCorrelationId(randomUUID())
                                        .withCreatedTime(ZonedDateTime.now().minusHours(2))
                                        .withAccountNumber("AC123456789OLD")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId).build()))
                                        .build(),
                                correlationItem()
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountNumber("AC123456789NEW")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId).build()))
                                        .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
            assertThat("Imposition offence details should contain SV_SENTENCE_VARIED",
                    notification.getNewOffenceByResult().get(0).getTitle(),
                    containsString(NCESDecisionConstants.SENTENCE_VARIED));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldGenerateAccWriteOffNotificationForFinTransitionFineOffence() {
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .build(),
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(offenceId2)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .withAccountCorrelationId(randomUUID())
                .build();

        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence1 details")
                                .build(),
                                offenceResultsDetails()
                                        .withOffenceId(offenceId2)
                                        .withIsFinancial(false)
                                        .withApplicationType(NCESDecisionConstants.REOPEN)
                                        .withImpositionOffenceDetails("Previous Acc Write Off Offence2 details")
                                        .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withAccountCorrelationId(randomUUID())
                                        .withCreatedTime(ZonedDateTime.now().minusHours(2))
                                        .withAccountNumber("AC123456789OLD")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId1).build()))
                                        .build(),
                                correlationItem()
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountNumber("AC123456789")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId1).build()))
                                        .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }

    @Test
    void shouldGenerateAccWriteOffNotificationForFinTransitionNonFineOffence() {
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .build(),
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(offenceId2)
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .build()))
                .withAccountCorrelationId(randomUUID())
                .build();

        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(offenceId1)
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence1 details")
                                .build(),
                                offenceResultsDetails()
                                        .withOffenceId(offenceId2)
                                        .withIsFinancial(true)
                                        .withApplicationType(NCESDecisionConstants.REOPEN)
                                        .withImpositionOffenceDetails("Previous Acc Write Off Offence2 details")
                                        .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withAccountCorrelationId(randomUUID())
                                        .withCreatedTime(ZonedDateTime.now().minusHours(2))
                                        .withAccountNumber("AC123456789OLD")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId2).build()))
                                        .build(),
                                correlationItem()
                                        .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withAccountNumber("AC123456789")
                                        .withOffenceResultsDetailsList(List.of(OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId1).build(),
                                                OffenceResultsDetails.offenceResultsDetails().withOffenceId(offenceId2).build()))
                                        .build()))
                .build();

        assertThat("Rule should apply for Acc Write Off", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should be one imposition offence detail for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(2));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }
}