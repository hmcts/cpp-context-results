package uk.gov.moj.cpp.results.event.processor;

/**
 * Constants for mapping between original and transformed payload keys
 * when building the migrated inactive NCES document envelope.
 */
public final class MigratedNcesMappingConstants {

    public static final String SUBJECT = "subject";
    public static final String FINE_ACCOUNT_NUMBER = "fineAccountNumber";
    public static final String DIVISION_CODE = "divisionCode";
    public static final String LEGACY_CASE_REFERENCE = "legacyCaseReference";
    public static final String CASE_REFERENCES = "caseReferences";
    public static final String ORIGINAL_DATE_OF_CONVICTION = "originalDateOfConviction";
    public static final String DATE_OF_CONVICTION = "dateOfConviction";
    public static final String LISTED_DATE = "listedDate";
    public static final String HEARING_COURT_CENTRE_NAME = "hearingCourtCentreName";
    public static final String DEFENDANT_NAME = "defendantName";
    public static final String DEFENDANT_DATE_OF_BIRTH = "defendantDateOfBirth";
    public static final String DEFENDANT_ADDRESS = "defendantAddress";
    public static final String DEFENDANT_EMAIL = "defendantEmail";
    public static final String DEFENDANT_CONTACT_NUMBER = "defendantContactNumber";

    private MigratedNcesMappingConstants() {
        // utility class
    }
}
