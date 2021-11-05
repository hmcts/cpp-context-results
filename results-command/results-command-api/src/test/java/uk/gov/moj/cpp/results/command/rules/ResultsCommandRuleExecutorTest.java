package uk.gov.moj.cpp.results.command.rules;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsCommandRuleExecutorTest extends BaseDroolsAccessControlTest {

    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";
    private static final String GROUP_MAGISTRATES = "Magistrates";

    protected Action action;

    @Mock
    protected UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder()
                .put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups_thenSuccessfullyAllowUpload() {
        stream(ResultsRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(true);

            final ExecutionResults executionResults = executeRulesWith(action);

            assertSuccessfulOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }

    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailUpload() {
        stream(ResultsRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(false);

            final ExecutionResults executionResults = executeRulesWith(action);

            assertFailureOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }

    public enum ResultsRules {
        ADD_HEARING_RESULT_RULE("results.add-hearing-result", asList(GROUP_LISTING_OFFICERS, GROUP_COURT_CLERKS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS, GROUP_COURT_ASSOCIATE, GROUP_MAGISTRATES)),
        CREATE_RESULTS_RULE("results.api.create-results", ImmutableList.of(GROUP_SYSTEM_USERS));

        private final String actionName;
        private final List<String> allowedUserGroups;

        ResultsRules(final String actionName, final List<String> allowedUserGroups) {
            this.actionName = actionName;
            this.allowedUserGroups = allowedUserGroups;
        }
    }
}
