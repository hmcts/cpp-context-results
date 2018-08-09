package uk.gov.moj.cpp.results.query.api;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class ResultsQueryApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_NAME_GET_PERSON_DETAILS = "results.get-person-details";
    private static final String ACTION_NAME_GET_HEARING_DETAILS = "results.get-hearing-details";
    private static final String ACTION_NAME_GET_RESULTS_DETAILS = "results.get-results-details";
    private static final String ACTION_NAME_GET_RESULTS_SUMMARY = "results.get-results-summary";

    @Mock
    private UserAndGroupProvider mockUserAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToGetPerson() {
        final Action action = createActionFor(ACTION_NAME_GET_PERSON_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetPerson() {
        final Action action = createActionFor(ACTION_NAME_GET_PERSON_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetHearing() {
        final Action action = createActionFor(ACTION_NAME_GET_HEARING_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetHearing() {
        final Action action = createActionFor(ACTION_NAME_GET_HEARING_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetResultDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetResultDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetResult() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_SUMMARY);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetResults() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_SUMMARY);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName())))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, mockUserAndGroupProvider).build();
    }

}
