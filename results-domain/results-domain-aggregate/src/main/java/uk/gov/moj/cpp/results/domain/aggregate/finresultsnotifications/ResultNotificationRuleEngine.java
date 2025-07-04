package uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications;

import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ApplicationACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ApplicationAmendmentACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ApplicationAmendmentAccWriteOffNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.ApplicationDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.CaseACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.CaseAmendmentACONNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.CaseAmendmentAccWriteOffNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.CaseAmendmentDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.CaseDeemedServedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.NewApplicationResultedNotificationRule;
import uk.gov.moj.cpp.results.domain.aggregate.finresultsnotifications.rules.NewApplicationUpdatedNotificationRule;
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
        rules.add(new NewApplicationResultedNotificationRule());
        rules.add(new NewApplicationUpdatedNotificationRule());
        rules.add(new ApplicationDeemedServedNotificationRule());
        rules.add(new ApplicationACONNotificationRule());
        rules.add(new ApplicationAmendmentACONNotificationRule());
        rules.add(new ApplicationAmendmentAccWriteOffNotificationRule());
        rules.add(new CaseAmendmentAccWriteOffNotificationRule());
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