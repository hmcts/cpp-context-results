package uk.gov.moj.cpp.results.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResultsCommandApiTest {


    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private ResultsCommandApi resultsCommandApi;

    @Mock
    private Function<Object, JsonEnvelope> function;


    @Test
    public void shouldNowsMaterialStatusUpated() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "results.handler.update-nows-material-status"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        resultsCommandApi.updateNowsStatus(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

}
