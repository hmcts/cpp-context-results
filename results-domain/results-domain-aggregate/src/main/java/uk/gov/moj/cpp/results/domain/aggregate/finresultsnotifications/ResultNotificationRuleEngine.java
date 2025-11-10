package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications;

import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationOnlyAmendmentAccWriteOffRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentFinToFinAccWriteOffRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.amendments.ApplicationAmendmentFinToNonFinAccWriteOffRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.ApplicationACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.ApplicationDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.NewApplicationAcceptedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.NewAppealAppDeniedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.NewApplicationUpdatedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.applications.result.NewNonAppealAppsDeniedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentFinToFinAccWriteOffRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.amendments.CaseAmendmentFinToNonFinAccWriteOffRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result.CaseACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.cases.result.CaseDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.event.MarkedAggregateSendEmailWhenAccountReceived;

import java.util.ArrayList;
import java.util.List;

/**
 * The ResultNotificationRuleEngine is responsible for evaluating a set of notification rules
 * based on the provided input. It applies each rule to the input and collects the results.
 */
public class ResultNotificationRuleEngine {

    private final List<ResultNotificationRule> rules;

    private ResultNotificationRuleEngine() {
        this.rules = new ArrayList<>();
        rules.add(new NewAppealAppDeniedNotificationRule());
        rules.add(new NewApplicationAcceptedNotificationRule());
        rules.add(new NewNonAppealAppsDeniedNotificationRule());
        rules.add(new NewApplicationUpdatedNotificationRule());
        rules.add(new ApplicationDeemedServedNotificationRule());
        rules.add(new ApplicationAmendmentDeemedServedNotificationRule());
        rules.add(new ApplicationACONNotificationRule());
        rules.add(new ApplicationAmendmentACONNotificationRule());
        rules.add(new ApplicationAmendmentFinToFinAccWriteOffRule());
        rules.add(new ApplicationAmendmentFinToNonFinAccWriteOffRule());
        rules.add(new ApplicationOnlyAmendmentAccWriteOffRule());
        rules.add(new CaseAmendmentFinToFinAccWriteOffRule());
        rules.add(new CaseAmendmentFinToNonFinAccWriteOffRule());
        rules.add(new CaseAmendmentACONNotificationRule());
        rules.add(new CaseACONNotificationRule());
        rules.add(new CaseAmendmentDeemedServedNotificationRule());
        rules.add(new CaseDeemedServedNotificationRule());
    }

    public static ResultNotificationRuleEngine resultNotificationRuleEngine() {
        return new ResultNotificationRuleEngine();
    }

    /**
     * Evaluates the input against all registered rules and returns a list of MarkedAggregateSendEmailWhenAccountReceived
     * events that match the rules.
     *
     * @param input the input to evaluate against the rules
     * @return a list of MarkedAggregateSendEmailWhenAccountReceived events
     */
    public List<MarkedAggregateSendEmailWhenAccountReceived> evaluate(ResultNotificationRule.RuleInput input) {
        List<MarkedAggregateSendEmailWhenAccountReceived> results = new ArrayList<>();
        for (ResultNotificationRule rule : rules) {
            if (rule.appliesTo(input)) {
                rule.apply(input).ifPresent(results::add);
            }
        }
        return results;
    }
}