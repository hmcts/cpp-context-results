package uk.gov.moj.cpp.results.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;
import uk.gov.moj.cpp.results.persist.VariantDirectoryRepository;

@RunWith(MockitoJUnitRunner.class)
public class UpdateNowsMaterialStatusEventListenerTest {

    private static final String GENERATED = "GENERATED";

    @Mock
    private VariantDirectoryRepository nowsMaterialRepository;

    @InjectMocks
    private UpdateNowsMaterialStatusEventListener nowsGeneratedEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldUpdateNowsMaterialStatusToGenerated() throws Exception {

        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = new NowsMaterialStatusUpdated(UUID.randomUUID(), UUID.randomUUID(), "generated");

        when(nowsMaterialRepository.updateStatus(nowsMaterialStatusUpdated.getMaterialId(), GENERATED)).thenReturn(1);

        nowsGeneratedEventListener.onNowsMaterialStatusUpdated(envelopeFrom(metadataWithRandomUUID("results.event.nows-material-status-updated"),
                objectToJsonObjectConverter.convert(nowsMaterialStatusUpdated)));

        final ArgumentCaptor<UUID> materialIdArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<String> nowsMaterialStatusArgumentCaptor = ArgumentCaptor.forClass(String.class);
 
        verify(this.nowsMaterialRepository).updateStatus(materialIdArgumentCaptor.capture(), nowsMaterialStatusArgumentCaptor.capture());

        assertThat(materialIdArgumentCaptor.getValue(), is(nowsMaterialStatusUpdated.getMaterialId()));
        assertThat(nowsMaterialStatusArgumentCaptor.getValue(), is(GENERATED));
    }

    @SuppressWarnings("deprecation")
    @Test(expected=RuntimeException.class)
    public void shouldFailureToUpdateNowsMaterialStatusToGenerated() throws Exception {

        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = new NowsMaterialStatusUpdated(UUID.randomUUID(), UUID.randomUUID(), "generated");

        when(nowsMaterialRepository.updateStatus( nowsMaterialStatusUpdated.getMaterialId(), GENERATED)).thenReturn(0);

        nowsGeneratedEventListener.onNowsMaterialStatusUpdated(envelopeFrom(metadataWithRandomUUID("results.event.nows-material-status-updated"),
                objectToJsonObjectConverter.convert(nowsMaterialStatusUpdated)));

        final ArgumentCaptor<UUID> materialIdArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<String> nowsMaterialStatusArgumentCaptor = ArgumentCaptor.forClass(String.class);
 
        verify(this.nowsMaterialRepository).updateStatus(materialIdArgumentCaptor.capture(), nowsMaterialStatusArgumentCaptor.capture());

        assertThat(materialIdArgumentCaptor.getValue(), is(nowsMaterialStatusUpdated.getMaterialId()));
        assertThat(nowsMaterialStatusArgumentCaptor.getValue(), is(GENERATED));
    }
}