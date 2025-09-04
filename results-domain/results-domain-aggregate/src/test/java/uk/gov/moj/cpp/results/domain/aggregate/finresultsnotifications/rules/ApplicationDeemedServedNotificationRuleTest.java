package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules;

import static java.util.UUID.randomUUID;
import static liquibase.util.Validate.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.hearing.courts.HearingFinancialResultRequest.hearingFinancialResultRequest;
import static uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants.STAT_DEC;
import static uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ResultNotificationRuleInputBuilder.resultNotificationRuleInputBuilder;

import uk.gov.justice.hearing.courts.OffenceResults;
import uk.gov.moj.cpp.results.domain.aggregate.application.NCESDecisionConstants;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.ApplicationDeemedServedNotificationRule;

import java.util.List;

import org.junit.jupiter.api.Test;

class ApplicationDeemedServedNotificationRuleTest {
    private final ApplicationDeemedServedNotificationRule rule = new ApplicationDeemedServedNotificationRule();

    @Test
    void shouldGenerateDeemedServedNotification() {
        var trackRequest = hearingFinancialResultRequest()
                .withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(true)
                                .withImpositionOffenceDetails("Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                ));
        var input = resultNotificationRuleInputBuilder().withRequest(trackRequest.build()).build();

        assertThat("Rule should apply for Deemed Served application type", rule.appliesTo(input));
        var output = rule.apply(input);

        output.ifPresentOrElse(notification -> {
            assertThat("subject should match", notification.getSubject(), is(NCESDecisionConstants.WRITE_OFF_ONE_DAY_DEEMED_SERVED));
            assertThat("There should be one imposition offence detail for Deemed Served", notification.getImpositionOffenceDetails().size(), is(1));
        }, () -> fail("Expected Deemed Served notification to be present"));
    }

    @Test
    void shouldNotGenerateNotificationForNonDeemedServedFlow() {
        var trackRequest = hearingFinancialResultRequest().withProsecutionCaseReferences(List.of("CaseId1"))
                .withOffenceResults(List.of(
                        OffenceResults.offenceResults()
                                .withOffenceId(randomUUID())
                                .withApplicationType(STAT_DEC)
                                .withIsParentFlag(true)
                                .withIsDeemedServed(false)
                                .withImpositionOffenceDetails("Non-Deemed Served Offence details")
                                .withIsFinancial(true)
                                .build()
                )).build();
        var input = resultNotificationRuleInputBuilder()
                .withRequest(trackRequest)
                .build();

        var output = rule.apply(input);
        assertThat("notification should not be generated for non-Deemed Served flow", output.isEmpty());
    }
}