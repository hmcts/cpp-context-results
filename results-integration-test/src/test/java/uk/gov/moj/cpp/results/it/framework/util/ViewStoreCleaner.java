package uk.gov.moj.cpp.results.it.framework.util;

import static uk.gov.moj.cpp.results.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

public class ViewStoreCleaner {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    public void cleanViewstoreTables() {
        // deliberately only clearing result table entries as other tests
        // rely on data populated in those tables by liquibase

        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME,
                "hearing_resulted_document",
                "processed_event");
    }
}
