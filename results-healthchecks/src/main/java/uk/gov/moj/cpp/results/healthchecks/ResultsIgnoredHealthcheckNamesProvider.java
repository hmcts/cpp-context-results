package uk.gov.moj.cpp.results.healthchecks;

import static java.util.List.of;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import uk.gov.justice.services.healthcheck.api.DefaultIgnoredHealthcheckNamesProvider;

import java.util.List;

import javax.enterprise.inject.Specializes;

@Specializes
public class ResultsIgnoredHealthcheckNamesProvider extends DefaultIgnoredHealthcheckNamesProvider {

    public ResultsIgnoredHealthcheckNamesProvider() {
        // Adding a pointless comment to stop sonar marking this as a major violation.
        // This constructor is required by CDI. 
    }

    @Override
    public List<String> getNamesOfIgnoredHealthChecks() {
        return of(JOB_STORE_HEALTHCHECK_NAME);
    }
}