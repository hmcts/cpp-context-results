package uk.gov.moj.cpp.results.query.api;

import static org.mockito.Mockito.verify;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.query.api.validator.ProsecutorResultsQueryValidator;
import uk.gov.moj.cpp.results.query.view.ProsecutorResultsQueryView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutorResultsQueryApiTest {

    @Mock
    private ProsecutorResultsQueryValidator prosecutorResultsQueryValidator;

    @Mock
    private ProsecutorResultsQueryView prosecutorResultsQueryView;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private ProsecutorResultsQueryApi prosecutorResultsQueryApi;

    @Test
    public void getProsecutorResults() {
        prosecutorResultsQueryApi.handleProsecutorResults(jsonEnvelope);
        verify(prosecutorResultsQueryValidator).validatePayload(jsonEnvelope);
        verify(prosecutorResultsQueryView).getProsecutorResults(jsonEnvelope);
    }
}