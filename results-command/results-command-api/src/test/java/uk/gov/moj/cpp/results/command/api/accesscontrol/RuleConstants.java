package uk.gov.moj.cpp.results.command.api.accesscontrol;

import java.util.Arrays;
import java.util.List;

public final class RuleConstants {

    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getUpdateNowsStatusActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_CLERKS);
    }

}