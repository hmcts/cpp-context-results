package uk.gov.moj.cpp.results.command.api.accesscontrol;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.results.command.api.accesscontrol.RuleConstants.getUpdateNowsStatusActionGroups;

public class UpdateNowsStatusTest extends BaseDroolsAccessControlTest {

    private static final String RESULTS_COMMAND_UPDATE_NOWS_MATERIAL = "results.update-nows-material-status";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdate() {
        final Action action = createActionFor(RESULTS_COMMAND_UPDATE_NOWS_MATERIAL);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateNowsStatusActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdate() {
        final Action action = createActionFor(RESULTS_COMMAND_UPDATE_NOWS_MATERIAL);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("test"))).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }


    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
