package uk.gov.moj.cpp.results.command.api.accesscontrol;

import static java.util.Arrays.asList;

import java.util.List;

import com.google.common.collect.ImmutableList;

public final class RuleConstants {

    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getUpdateNowsStatusActionGroups() {
        return asList(GROUP_LISTING_OFFICERS, GROUP_COURT_CLERKS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS, GROUP_COURT_ASSOCIATE);
    }

    public static List<String> getPoliceResultsForDefendantGroups() {
        return asList(GROUP_LISTING_OFFICERS, GROUP_COURT_CLERKS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS, GROUP_COURT_ASSOCIATE);
    }

    public static List<String> getCreateResultsActionGroups() {
        return ImmutableList.of(GROUP_SYSTEM_USERS);
    }

    public static List<String> getTrackResultsActionGroups() {
        return ImmutableList.of(GROUP_SYSTEM_USERS);
    }
}

