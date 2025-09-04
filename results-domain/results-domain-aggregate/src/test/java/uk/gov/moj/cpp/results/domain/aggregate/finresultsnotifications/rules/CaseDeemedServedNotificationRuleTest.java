package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.caseresult.CaseDeemedServedNotificationRule;

import java.util.List;

import org.junit.jupiter.api.Test;

class CaseDeemedServedNotificationRuleTest {

    private final CaseDeemedServedNotificationRule rule = new CaseDeemedServedNotificationRule();

    @Test
    void shouldGenerateDeemedServedNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
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

        assertThat("Rule should apply for Deemed Served", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("Deemed served subject should match", notification.getSubject(), is(WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail for Deemed Served", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotApplyToAmendmentFlow() {
        var trackRequest = hearingFinancialResultRequest()
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

        assertFalse(rule.appliesTo(input), "Rule should not apply for non-amendment Deemed Served");
    }
}