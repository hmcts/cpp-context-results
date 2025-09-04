package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result.CaseACONNotificationRule;
;

import java.util.List;

import org.junit.jupiter.api.Test;

class CaseACONNotificationRuleTest {
    CaseACONNotificationRule rule = new CaseACONNotificationRule();

    @Test
    void shouldGenerateACONNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();
        assertTrue(rule.appliesTo(input), "Rule should apply for ACON");

        var output = rule.apply(input);
        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.ACON_EMAIL_SUBJECT));
            assertThat("There should be one imposition offence detail for ACON", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected ACON notification to be present"));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest();
        trackRequest.withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withAmendmentDate("2023-10-01")
                                .withResultCode(NCESDecisionConstants.ACON)
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        assertThat("Rule should not apply for ACON in amendment flow", !rule.appliesTo(input));
    }
}