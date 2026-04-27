package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPEAL;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AACD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AW;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AACA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.G;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.ROPENED;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NewAppealAppDeniedNotificationRuleTest {
    private final NewAppealAppDeniedNotificationRule rule = new NewAppealAppDeniedNotificationRule();

    @Test
    void shouldGenerateAppealWithdrawnNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(APPEAL)
                                .withResultCode(AW)
                                .withApplicationId(randomUUID())
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal withdrawn details")
                                .withApplicationResultsCategory("FINAL")
                                .withOffenceResultsCategory("FINAL")
                                .withIsFinancial(true)
                                .build()
                )).withAccountCorrelationId(randomUUID());

        // Create previous offence results details
        OffenceResultsDetails previousOffenceDetails = OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(offenceId)
                .withIsFinancial(true)
                .withImpositionOffenceDetails("Previous imposition details")
                .build();

        Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails = Map.of(offenceId, previousOffenceDetails);

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .withPrevOffenceResultsDetails(prevOffenceResultsDetails)
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withAccountCorrelationId(randomUUID())
                                        .withCreatedTime(ZonedDateTime.now().minusHours(2))
                                        .withAccountNumber("AC123456788")
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).withCreatedTime(ZonedDateTime.now().minusHours(2)).build()))
                                        .build(),
                                correlationItem()
                                        .withAccountCorrelationId(trackRequest.build().getAccountCorrelationId())
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).withCreatedTime(ZonedDateTime.now()).build()))
                                        .withAccountNumber("AC123456789")
                                        .build()))
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for appeal application type with withdrawn result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("APPEAL WITHDRAWN"));
        }, () -> fail("Expected appeal withdrawn notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAppealApplicationType() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode(AACD)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Non-appeal offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for non-appeal application type", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(APPEAL)
                                .withResultCode(AACD)
                                .withAmendmentDate("2023-10-01")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal amendment details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for amendment flow", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAppealAllowedResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(APPEAL)
                                .withResultCode(AACA)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal allowed details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for appeal allowed result", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAppealGrantedResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(APPEAL)
                                .withResultCode(G)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal granted details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for appeal granted result", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAppealReopenedResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(APPEAL)
                                .withResultCode(ROPENED)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal reopened details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for appeal reopened result", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToInvalidResultCode() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(APPEAL)
                                .withResultCode("INVALID_CODE")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Invalid result code details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for invalid result code", rule.appliesTo(input), is(false));
    }


    @Test
    void shouldHandleAppealWithPreviousOffenceResults() {
        UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(APPEAL)
                                .withApplicationId(randomUUID())
                                .withResultCode(AASD)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal with previous results")
                                .withApplicationResultsCategory("FINAL")
                                .withOffenceResultsCategory("FINAL")
                                .withIsFinancial(true)
                                .build()
                )).withAccountCorrelationId(randomUUID());

        // Create previous offence results details
        OffenceResultsDetails previousOffenceDetails = OffenceResultsDetails.offenceResultsDetails()
                .withOffenceId(offenceId)
                .withIsFinancial(true)
                .withImpositionOffenceDetails("Previous imposition details")
                .build();

        Map<UUID, OffenceResultsDetails> prevOffenceResultsDetails = Map.of(offenceId, previousOffenceDetails);

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .withPrevOffenceResultsDetails(prevOffenceResultsDetails)
                .withCorrelationItemList(
                        List.of(correlationItem()
                                        .withAccountCorrelationId(randomUUID())
                                        .withCreatedTime(ZonedDateTime.now().minusHours(2))
                                        .withAccountNumber("AC123456788")
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).withCreatedTime(ZonedDateTime.now().minusHours(2)).build()))
                                        .build(),
                                correlationItem()
                                        .withAccountCorrelationId(trackRequest.build().getAccountCorrelationId())
                                        .withCreatedTime(ZonedDateTime.now())
                                        .withOffenceResultsDetailsList(List.of(offenceResultsDetails().withOffenceId(offenceId).withCreatedTime(ZonedDateTime.now()).build()))
                                        .withAccountNumber("AC123456789")
                                        .build()))
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for appeal with previous offence results");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.APPEAL_DISMISSED));
        }, () -> fail("Expected appeal notification to be present with previous results"));
    }
}
