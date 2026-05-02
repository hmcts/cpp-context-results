package uk.gov.moj.cpp.results.event.processor;

/**
 * Constants for migration-related JSON keys and command names,
 * grouped by structure from inactive-migrated-cases payload.
 */
public final class MigrationConstants {

    private MigrationConstants() {}

    public static final class Defendant {
        private Defendant() {}

        public static final String NAME = "defendantName";
        public static final String ADDRESS = "defendantAddress";
        public static final String EMAIL = "defendantEmail";
        public static final String DATE_OF_BIRTH = "defendantDateOfBirth";
        public static final String CONTACT_NUMBER = "defendantContactNumber";
        public static final String ID = "defendantId";
        public static final String DEFENDANTS = "defendants";
        public static final String ORIGINAL_DATE_OF_CONVICTION = "originalDateOfConviction";
    }

    public static final class Case {
        private Case() {}

        public static final String URN = "caseURN";
        public static final String ID = "caseId";
        public static final String CASE_IDS = "caseIds";
        public static final String COURT_EMAIL = "courtEmail";
        public static final String DIVISION = "division";
    }

    public static final class FineAccount {
        private FineAccount() {}

        public static final String FINE_ACCOUNT_NUMBER = "fineAccountNumber";
    }

    public static final class InactiveMigratedCase {
        private InactiveMigratedCase() {}

        public static final String INACTIVE_MIGRATED_CASE_SUMMARIES = "inactiveMigratedCaseSummaries";
        public static final String INACTIVE_CASE_SUMMARY = "inactiveCaseSummary";
        public static final String ID = "id";
        public static final String MIGRATION_SOURCE_SYSTEM = "migrationSourceSystem";
        public static final String DEFENDANT_FINE_ACCOUNT_NUMBERS = "defendantFineAccountNumbers";
        public static final String MIGRATION_SOURCE_SYSTEM_CASE_IDENTIFIER = "migrationSourceSystemCaseIdentifier";
    }

    public static final class PersonDetails {
        private PersonDetails() {}

        public static final String PERSON_DEFENDANT = "personDefendant";
        public static final String PERSON_DETAILS = "personDetails";
        public static final String FIRST_NAME = "firstName";
        public static final String LAST_NAME = "lastName";
        public static final String DATE_OF_BIRTH = "dateOfBirth";
        public static final String ADDRESS = "address";
        public static final String ADDRESS_1 = "address1";
        public static final String ADDRESS_2 = "address2";
        public static final String ADDRESS_3 = "address3";
        public static final String ADDRESS_4 = "address4";
        public static final String ADDRESS_5 = "address5";
        public static final String POSTCODE = "postcode";
        public static final String CONTACT = "contact";
        public static final String PRIMARY_EMAIL = "primaryEmail";
        public static final String WORK = "work";
        public static final String MOBILE = "mobile";
        public static final String HOME = "home";
    }

    public static final class Offence {
        private Offence() {}

        public static final String OFFENCES = "offences";
        public static final String CONVICTION_DATE = "convictionDate";
    }

    /** Reference data / organisation unit keys (e.g. hearing court centre, enforcement area). */
    public static final class ReferenceData {
        private ReferenceData() {}

        public static final String HEARING_COURT_CENTRE_ID = "hearingCourtCentreId";
        public static final String ENFORCEMENT_AREA = "enforcementArea";
        public static final String NCES_NOTIFICATION_EMAIL = "ncesNotificationEmail";
        public static final String DIVISION_CODE = "divisionCode";
    }

    /** Command payload keys and command names. */
    public static final String MIGRATED_MASTER_DEFENDANT_COURT_EMAIL_AND_FINE_ACCOUNT = "migratedMasterDefendantCourtEmailAndFineAccount";
    public static final String RESULT_COMMAND_SEND_MIGRATED_INACTIVE_NCES_EMAIL_FOR_APPLICATION = "result.command.send-migrated-inactive-nces-email-for-application";
}
