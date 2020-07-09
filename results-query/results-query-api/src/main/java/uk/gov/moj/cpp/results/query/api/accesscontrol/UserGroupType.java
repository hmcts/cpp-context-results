package uk.gov.moj.cpp.results.query.api.accesscontrol;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

public enum UserGroupType {

    CJSE("CJSE"),
    LEGAL_ADVISERS("Legal Advisers"),
    PRISON_ADMIN("Prison Admin"),
    PROBATION_ADMIN("Probation Admin"),
    POLICE_ADMIN("Police Admin"),
    VICTIMS_AND_WITNESS_CARE_ADMIN("Victims & Witness Care Admin"),
    YOUTH_OFFENDING_SERVICE_ADMIN("Youth Offending Service Admin"),
    LEGAL_AID_AGENCY_ADMIN("Legal Aid Agency Admin"),
    COURT_CLERKS("Court Clerks");

    private final String name;

    UserGroupType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return this.name;
    }

    public static List<String> personDetailsGroups() {
        return Stream.of(PRISON_ADMIN, PROBATION_ADMIN, POLICE_ADMIN, VICTIMS_AND_WITNESS_CARE_ADMIN, YOUTH_OFFENDING_SERVICE_ADMIN, LEGAL_AID_AGENCY_ADMIN, COURT_CLERKS)
                .map(UserGroupType::getName).collect(toList());
    }

    public static List<String> hearingDetailsGroups() {
        return Stream.of(PRISON_ADMIN, PROBATION_ADMIN, POLICE_ADMIN, VICTIMS_AND_WITNESS_CARE_ADMIN, YOUTH_OFFENDING_SERVICE_ADMIN, LEGAL_AID_AGENCY_ADMIN, COURT_CLERKS)
                .map(UserGroupType::getName).collect(toList());
    }

    public static List<String> resultsDetailsGroups() {
        return Stream.of(CJSE, LEGAL_ADVISERS, PRISON_ADMIN, PROBATION_ADMIN, POLICE_ADMIN, VICTIMS_AND_WITNESS_CARE_ADMIN, YOUTH_OFFENDING_SERVICE_ADMIN, LEGAL_AID_AGENCY_ADMIN, COURT_CLERKS)
                .map(UserGroupType::getName).collect(toList());
    }

    public static List<String> resultsSummaryGroups() {
        return Stream.of(PRISON_ADMIN, PROBATION_ADMIN, POLICE_ADMIN, VICTIMS_AND_WITNESS_CARE_ADMIN, YOUTH_OFFENDING_SERVICE_ADMIN, LEGAL_AID_AGENCY_ADMIN, COURT_CLERKS)
                .map(UserGroupType::getName).collect(toList());
    }
}
