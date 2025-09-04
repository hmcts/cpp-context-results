package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.AASA;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.REOPEN;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.APPEAL;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.G;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STDEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;
import static uk.gov.moj.cpp.results.domain.aggregate.utils.CorrelationItem.correlationItem;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NewAppGrantedNotificationRuleTest {
    private final NewAppGrantedNotificationRule rule = new NewAppGrantedNotificationRule();

    @Test
    void shouldGenerateStatutoryDeclarationGrantedNotification() {

        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(STDEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("stat dec details")
                                .withIsFinancial(true)
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
                .build();;

        assertTrue(rule.appliesTo(input), "Rule should apply for appeal application type with allowed result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("STATUTORY DECLARATION GRANTED"));
        }, () -> fail("Expected statdec stdec notification to be present"));
    }

    @Test
    void shouldGenerateApplicationToReopenGrantedNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(REOPEN)
                                .withResultCode(G)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Application to reopen granted details")
                                .withIsFinancial(true)
                                .withApplicationId(randomUUID())
                                .build()
                ))
                .withAccountCorrelationId(randomUUID());
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

        assertTrue(rule.appliesTo(input), "Rule should apply for application to reopen with granted result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("APPLICATION TO REOPEN GRANTED"));
        }, () -> fail("Expected application to reopen granted notification to be present"));
    }

    @Test
    void shouldGenerateAppealAllowedNotification() {
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(APPEAL)
                                .withResultCode(AASA)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Appeal allowed details")
                                .withIsFinancial(true)
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
                .build();;

        assertTrue(rule.appliesTo(input), "Rule should apply for appeal application type with allowed result");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("APPEAL ALLOWED"));
        }, () -> fail("Expected appeal allowed notification to be present"));
    }

    @Test
    void shouldNotApplyToNonGrantedResults() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode("RFSD")
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Non-granted result details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for non-granted result", rule.appliesTo(input), is(false));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
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
                                .withResultCode(G)
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
    void shouldHandleGrantedApplicationWithPreviousOffenceResults() {
        UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("Granted application with previous results")
                                .withIsFinancial(true)
                                .withApplicationId(randomUUID())
                                .build()
                ))
                .withAccountCorrelationId(randomUUID());
        
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

        assertTrue(rule.appliesTo(input), "Rule should apply for granted application with previous offence results");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is("STATUTORY DECLARATION GRANTED"));
        }, () -> fail("Expected granted notification to be present with previous results"));
    }

    @Test
    void shouldNotApplyToNonFinancialOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
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

        // Should return empty as the rule checks for financial offences with parent flag
        assertThat("Should not generate notification for non-financial offences", output.isEmpty(), is(true));
    }

    @Test
    void shouldNotApplyToNonParentFlagOffences() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withResultCode(G)
                                .withIsParentFlag(false)
                                .withImpositionOffenceDetails("Non-parent flag offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertTrue(rule.appliesTo(input), "Rule should apply for non-parent flag offences");
        var output = rule.apply(input);

        // Should return empty as the rule checks for offences with parent flag
        assertThat("Should not generate notification for non-parent flag offences", output.isEmpty(), is(true));
    }
}

