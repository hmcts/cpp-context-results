package uk.gov.moj.cpp.results.healthchecks;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ResultsIgnoredHealthcheckNamesProviderTest {


    @InjectMocks
    private ResultsIgnoredHealthcheckNamesProvider resultsIgnoredHealthcheckNamesProvider;

    @Test
    public void shouldIgnoreFileStoreAndJobStoreHealthchecks() throws Exception {

        final List<String> namesOfIgnoredHealthChecks = resultsIgnoredHealthcheckNamesProvider.getNamesOfIgnoredHealthChecks();

        assertThat(namesOfIgnoredHealthChecks.size(), is(1));
        assertThat(namesOfIgnoredHealthChecks, hasItem(JOB_STORE_HEALTHCHECK_NAME));
    }

}