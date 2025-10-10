package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments;

import static java.util.UUID.randomUUID;
import static liquibase.util.Validate.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

class ApplicationOnlyAmendmentAccWriteOffRuleTest {
    private final ApplicationOnlyAmendmentAccWriteOffRule rule = new ApplicationOnlyAmendmentAccWriteOffRule();

    @Test
    void shouldNotApplyToFinToNonFinAmendmentFlow() {
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

    @Test
    void shouldNotApplyToApplicationOnlyAmendmentWithNonFinancialOffences() {
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(randomUUID())
                                .withApplicationTitle("Reopen")
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .withResultCode("RFSD")
                                .build()))
                .build();
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationResultsDetails(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withApplicationId(applicationId)
                                .withApplicationTitle("Reopen")
                                .withIsFinancial(false)
                                .withResultCode("G")
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous non-financial offence details")
                                .build())))
                .withPrevOffenceResultsDetails(Map.of(
                        trackRequest.getOffenceResults().get(0).getOffenceId(),
                        offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(false)
                                .withImpositionOffenceDetails("Previous offence details")
                                .build()))
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(false)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .build())))
                .build();

        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            fail("Expected no Acc Write Off notification to be present for application-only amendment with non-financial offences" +
                    "Rule should not apply for Application Acc Write Off if Application results only amended with offences adj to next hearing\" +\n" +
                    "                \"and application is amended in previous hearing");
        }, () -> {
        });
    }

    @Test
    void shouldApplyToApplicationOnlyAmendmentWithFinancialOffences() {
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        offenceResults()
                                .withApplicationId(applicationId)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withOffenceId(offenceId)
                                .withApplicationTitle("Reopen")
                                .withIsFinancial(false)
                                .withAmendmentDate("2023-01-01")
                                .withResultCode("RFSD")
                                .build()))
                .build();
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationResultsDetails(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(offenceId)
                                .withApplicationId(applicationId)
                                .withApplicationTitle("Reopen")
                                .withIsFinancial(true)
                                .withResultCode("G")
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous non-financial offence details")
                                .build())))
                .withPrevOffenceResultsDetails(Map.of(
                        offenceId,
                        offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withImpositionOffenceDetails("Previous offence details")
                                .build()))
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(offenceId)
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .build())))
                .withCorrelationItemList(
                        List.of(correlationItem()
                                .withAccountCorrelationId(trackRequest.getAccountCorrelationId())
                                .withAccountNumber("AC123456789")
                                .build()))
                .build();

        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.AMEND_AND_RESHARE));
            assertThat("There should not be impostions for Acc Write Off", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Acc Write Off notification to be present"));
    }
}