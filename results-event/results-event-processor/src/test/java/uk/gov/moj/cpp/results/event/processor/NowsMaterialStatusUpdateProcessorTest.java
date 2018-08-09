package uk.gov.moj.cpp.results.event.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NowsMaterialStatusUpdateProcessorTest {

    public static final String MATERIAL_ID = "materialId";
    static final String PUBLIC_RESULTS_EVENT_NOWS_MATERIAL_STATUS_UPDATED = "public.results.event.nows-material-status-updated";
    @InjectMocks
    private NowsMaterialStatusUpdateProcessor listener;

    @Mock
    private Sender sender;
    @Mock
    private Enveloper enveloper;
    @Mock
    private JsonEnvelope jsonEnvelope;
    @Mock
    private JsonObject payload;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;


    @Test
    public void shouldHandleNowsMaterialStatusUpatedEventMessage() throws Exception {
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString(MATERIAL_ID)).thenReturn(UUID.randomUUID().toString());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_RESULTS_EVENT_NOWS_MATERIAL_STATUS_UPDATED)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        listener.handleNowsMaterialStatusUpdated(jsonEnvelope);
        verify(sender).send(finalEnvelope);
        verify(payload).getString(MATERIAL_ID);
    }


}