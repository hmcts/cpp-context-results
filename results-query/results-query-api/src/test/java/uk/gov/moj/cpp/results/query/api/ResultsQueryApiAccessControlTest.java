package uk.gov.moj.cpp.results.query.api;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.COURT_ADMINISTRATORS;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.COURT_ASSOCIATE;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.COURT_CLERKS;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.CROWN_COURT_ADMINS;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.JUDICIARY;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.LEGAL_ADVISERS;
import static uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType.LISTING_OFFICERS;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.results.query.api.accesscontrol.UserGroupType;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class ResultsQueryApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String ACTION_NAME_GET_PERSON_DETAILS = "results.get-person-details";
    private static final String ACTION_NAME_GET_HEARING_DETAILS = "results.get-hearing-details";
    private static final String ACTION_NAME_GET_RESULTS_DETAILS = "results.get-results-details";
    private static final String ACTION_NAME_GET_RESULTS_SUMMARY = "results.get-results-summary";
    private static final String ACTION_NAME_GET_HEARING_DETAILS_FOR_HEARING_ID = "results.get-hearing-information-details-for-hearing";
    private static final String ACTION_NAME_GET_PROSECUTOR_RESULTS = "results.prosecutor-results";
    private static final String ACTION_NAME_GET_DEFENDANTS_TRACKING_RESULTS = "results.get-defendants-tracking-status";
    private static final String ACTION_NAME_GET_NCES_EMAIL_NOTIFICATION_DETAILS = "results.query.nces-email-notification-details";

    @Mock
    private UserAndGroupProvider mockUserAndGroupProvider;

    public ResultsQueryApiAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToGetPerson() {
        final Action action = createActionFor(ACTION_NAME_GET_PERSON_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName(), COURT_CLERKS.getName(), COURT_ASSOCIATE.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetPerson() {
        final Action action = createActionFor(ACTION_NAME_GET_PERSON_DETAILS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetHearingInformationDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_HEARING_DETAILS_FOR_HEARING_ID);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.CJSE.getName(), LEGAL_ADVISERS.getName(), UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName(), UserGroupType.COURT_CLERKS.getName(), UserGroupType.COURT_ASSOCIATE.getName(), UserGroupType.SYSTEM_USERS.getName(), UserGroupType.MAGISTRATES.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetHearingInformationDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_HEARING_DETAILS_FOR_HEARING_ID);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetHearingDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_HEARING_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName(), COURT_CLERKS.getName(), COURT_ASSOCIATE.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetHearingDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_HEARING_DETAILS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetResultDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.CJSE.getName(), LEGAL_ADVISERS.getName(), UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName(), UserGroupType.COURT_CLERKS.getName(), UserGroupType.COURT_ASSOCIATE.getName(), UserGroupType.SYSTEM_USERS.getName(), UserGroupType.MAGISTRATES.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetResultDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_DETAILS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetResult() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_SUMMARY);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                asList(UserGroupType.PRISON_ADMIN.getName(), UserGroupType.PROBATION_ADMIN.getName(), UserGroupType.POLICE_ADMIN.getName(),
                        UserGroupType.VICTIMS_AND_WITNESS_CARE_ADMIN.getName(), UserGroupType.YOUTH_OFFENDING_SERVICE_ADMIN.getName(),
                        UserGroupType.LEGAL_AID_AGENCY_ADMIN.getName(), COURT_CLERKS.getName(), COURT_ASSOCIATE.getName())))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetResults() {
        final Action action = createActionFor(ACTION_NAME_GET_RESULTS_SUMMARY);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetProsecutorResults() {
        final Action action = createActionFor(ACTION_NAME_GET_PROSECUTOR_RESULTS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, UserGroupType.CPPI_CONSUMERS.getName()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetProsecutorResults() {
        final Action action = createActionFor(ACTION_NAME_GET_PROSECUTOR_RESULTS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetDefendantsTrackingStatusResults() {
        final Action action = createActionFor(ACTION_NAME_GET_DEFENDANTS_TRACKING_RESULTS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldNotAllowUnAuthorisedUserToGetDefendantsTrackingStatusResults() {
        final Action action = createActionFor(ACTION_NAME_GET_DEFENDANTS_TRACKING_RESULTS);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetNcesEmailNotificationDetails() {
        final Action action = createActionFor(ACTION_NAME_GET_NCES_EMAIL_NOTIFICATION_DETAILS);
        given(mockUserAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, UserGroupType.SYSTEM_USERS.getName()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object>  getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, mockUserAndGroupProvider);
    }

}
