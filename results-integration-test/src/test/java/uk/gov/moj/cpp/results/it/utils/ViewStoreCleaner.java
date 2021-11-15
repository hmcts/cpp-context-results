package uk.gov.moj.cpp.results.it.utils;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

public class ViewStoreCleaner {

    public static void cleanViewStoreTables() {
        final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
        databaseCleaner.cleanViewStoreTables("results",
                "defendant_tracking_status");
    }

    public static void cleanEventStoreTables() {
        final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
        databaseCleaner.cleanEventStoreTables("results");
    }
}
