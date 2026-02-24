package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.RFSD;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WDRN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.DISM;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NewNonAppealAppsDeniedNotificationRuleTest {
    private final NewNonAppealAppsDeniedNotificationRule rule = new NewNonAppealAppsDeniedNotificationRule();

    @Test
    void shouldGenerateStatutoryDeclarationRefusedNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(RFSD)
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withApplicationId(randomUUID())
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

        assertTrue(rule.appliesTo(input), "Rule should apply for statutory declaration application type with refused result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("STATUTORY DECLARATION REFUSED"));
            assertThat("There should be imposition offence details of previous", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected statutory declaration refused notification to be present"));
    }

    @Test
    void shouldGenerateStatutoryDeclarationWithdrawnNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(WDRN)
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withApplicationId(randomUUID())
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


        assertTrue(rule.appliesTo(input), "Rule should apply for statutory declaration application type with withdrawn result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("STATUTORY DECLARATION WITHDRAWN"));
        }, () -> fail("Expected statutory declaration withdrawn notification to be present"));
    }

    @Test
    void shouldGenerateApplicationToReopenRefusedNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(REOPEN)
                                .withResultCode(RFSD)
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withApplicationId(randomUUID())
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


        assertTrue(rule.appliesTo(input), "Rule should apply for application to reopen with refused result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("APPLICATION TO REOPEN REFUSED"));
        }, () -> fail("Expected application to reopen refused notification to be present"));
    }

    @Test
    void shouldGenerateApplicationToReopenDismissedNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(REOPEN)
                                .withResultCode(DISM)
                                .withIsParentFlag(true)
                                .withIsFinancial(false)
                                .withApplicationId(randomUUID())
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


        assertTrue(rule.appliesTo(input), "Rule should apply for application to reopen with dismissed result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("APPLICATION TO REOPEN DISMISSED"));
        }, () -> fail("Expected application to reopen dismissed notification to be present"));
    }

    @Test
    void shouldNotApplyToGrantedResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode("G")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Granted result details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for granted result", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAppealResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType("APPEAL")
                                .withResultCode("AASD")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal result details")
                                .withIsFinancial(true)
                                .withApplicationId(randomUUID())
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for appeal result", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode(RFSD)
                                .withAmendmentDate("2023-10-01")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Amendment flow details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for amendment flow", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToInvalidApplicationType() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType("INVALID_TYPE")
                                .withResultCode(RFSD)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Invalid application type details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for invalid application type", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToInvalidResultCode() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
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
    void shouldHandleNonFinancialOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode(RFSD)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Non-financial offence details")
                                .withIsFinancial(false)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for non-financial offences");
        var output = rule.apply(input);

        // Should return empty as the rule requires imposition offence details
        assertThat("Should not generate notification for non-financial offences without imposition details", output.isEmpty(), is(true));
    }
}
