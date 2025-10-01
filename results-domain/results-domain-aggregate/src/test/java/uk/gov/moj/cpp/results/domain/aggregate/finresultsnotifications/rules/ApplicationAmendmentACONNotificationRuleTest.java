package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.justice.hearing.courts.OffenceResultsDetails.offenceResultsDetails;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.justice.hearing.courts.OffenceResultsDetails;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentACONNotificationRule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApplicationAmendmentACONNotificationRuleTest {
    private final ApplicationAmendmentACONNotificationRule rule = new ApplicationAmendmentACONNotificationRule();

    @Test
    void shouldGenerateACONNotification() {
        final UUID offenceId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID currentCorrelationId = randomUUID();

        var trackRequest = hearingFinancialResultRequest()
                .withAccountCorrelationId(currentCorrelationId)
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(offenceId)
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withAmendmentDate("2023-10-01")
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .withApplicationId(applicationId)
                                .build()))
                .withAccountCorrelationId(randomUUID()).build();

        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .withPrevApplicationOffenceResultsMap(Map.of(
                        applicationId,
                        List.of(offenceResultsDetails()
                                .withOffenceId(trackRequest.getOffenceResults().get(0).getOffenceId())
                                .withIsFinancial(true)
                                .withApplicationType(NCESDecisionConstants.REOPEN)
                                .withImpositionOffenceDetails("Previous Acc Write Off Offence details")
                                .build())))
                .build();

        assertThat("Rule should apply for ACON application type", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> Assertions.fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest();
        trackRequest.withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withImpositionOffenceDetails("ACON Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for non-amendment flow", rule.appliesTo(input), is(false));
    }

}