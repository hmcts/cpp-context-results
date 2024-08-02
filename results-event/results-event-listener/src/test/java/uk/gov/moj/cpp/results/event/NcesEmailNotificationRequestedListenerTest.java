package uk.gov.moj.cpp.results.event;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.persist.NcesEmailNotificationDetailsRepository;
import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NcesEmailNotificationRequestedListenerTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private NcesEmailNotificationDetailsRepository ncesEmailNotificationDetailsRepository;

    @InjectMocks
    private NcesEmailNotificationRequestedListener ncesEmailNotificationRequestedListener;

    @Captor
    private ArgumentCaptor<NcesEmailNotificationDetailsEntity> ncesEmailNotificationDetailsEntityArgumentCaptor;

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleEmailToNcesNotificationRequested() {
        NcesEmailNotificationRequested ncesEmailNotificationRequested = createNcesNotificationRequested();
        JsonEnvelope jsonEnvelope = createJsonEnvelope(ncesEmailNotificationRequested);
        ncesEmailNotificationRequestedListener.handleEmailToNcesNotificationRequested(jsonEnvelope);

        verify(ncesEmailNotificationDetailsRepository).save(this.ncesEmailNotificationDetailsEntityArgumentCaptor.capture());

        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues(), is(notNullValue()));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().size(), is(1));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().get(0).getId(), is(notNullValue()));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().get(0).getNotificationId(), is(ncesEmailNotificationRequested.getNotificationId()));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().get(0).getMasterDefendantId(), is(ncesEmailNotificationRequested.getMasterDefendantId()));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().get(0).getMaterialId(), is(ncesEmailNotificationRequested.getMaterialId()));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().get(0).getSubject(), is(ncesEmailNotificationRequested.getSubject()));
        assertThat(ncesEmailNotificationDetailsEntityArgumentCaptor.getAllValues().get(0).getSendTo(), is(ncesEmailNotificationRequested.getSendTo()));
    }

    private NcesEmailNotificationRequested createNcesNotificationRequested() {
        return NcesEmailNotificationRequested.ncesEmailNotificationRequested()
                .withNotificationId(randomUUID())
                .withMasterDefendantId(UUID.randomUUID())
                .withMaterialId(randomUUID())
                .withSubject("Subject")
                .withSendTo("SendTo@gmail.com")
                .withDefendantEmail("email@email.com")
                .withDefendantName("name")
                .build();
    }

    private JsonEnvelope createJsonEnvelope(NcesEmailNotificationRequested ncesEmailNotificationRequested) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("results.event.nces-email-notification-requested")
                .build();

        return envelopeFrom(metadata,
                objectToJsonObjectConverter.convert(ncesEmailNotificationRequested));
    }
}
