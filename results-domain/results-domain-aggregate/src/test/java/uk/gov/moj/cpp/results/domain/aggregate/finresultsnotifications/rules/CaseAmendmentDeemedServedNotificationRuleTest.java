package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;

import uk.gov.justice.hearing.courts.HearingFinancialResultRequest;
import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentDeemedServedNotificationRule;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CaseAmendmentDeemedServedNotificationRuleTest {

    private final CaseAmendmentDeemedServedNotificationRule rule = new CaseAmendmentDeemedServedNotificationRule();

    @Test
    void shouldGenerateDeemedServedNotification() {
        var trackRequest = HearingFinancialResultRequest.hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .withAmendmentDate("2023-10-01")
                                .build()
                ));
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        Assertions.assertTrue(rule.appliesTo(input), "Rule should apply for Deemed Served amendment");
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            Assertions.assertEquals(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED, notification.getSubject());
            Assertions.assertEquals(1, notification.getImpositionOffenceDetails().size());
        }, () -> Assertions.fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotApplyToNonAmendmentFlow() {
        var trackRequest = HearingFinancialResultRequest.hearingFinancialResultRequest();
        trackRequest.withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(null)
                                .withIsDeemedServed(true)
                                .withIsFinancial(true)
                                .build()
                ));
        var input = ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder()
                .withRequest(trackRequest.build())
                .build();

        Assertions.assertFalse(rule.appliesTo(input), "Rule should not apply for non-amendment Deemed Served");
    }
}
