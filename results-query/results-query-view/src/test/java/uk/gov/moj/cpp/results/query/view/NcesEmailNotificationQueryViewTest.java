package uk.gov.moj.cpp.results.query.view;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.results.persist.NcesEmailNotificationDetailsRepository;
import uk.gov.moj.cpp.results.persist.entity.NcesEmailNotificationDetailsEntity;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NcesEmailNotificationQueryViewTest {

    private static final String MATERIAL_ID = "materialId";
    @Mock
    NcesEmailNotificationDetailsRepository ncesEmailNotificationDetailsRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private NcesEmailNotificationQueryView ncesEmailNotificationQueryView;

    @Test
    public void getNcesEmailNotificationDetails() {
        final UUID materialId = randomUUID();
        final JsonEnvelope requestEnvelope = createJsonEnvelope(materialId);
        final NcesEmailNotificationDetailsEntity entity = createNcesEmailNotificationDetailsEntity(materialId);
        when(ncesEmailNotificationDetailsRepository.findByMaterialId(materialId))
                .thenReturn(entity);

        final JsonEnvelope ncesEmailNotificationDetails = ncesEmailNotificationQueryView.getNcesEmailNotificationDetails(requestEnvelope);

        verify(ncesEmailNotificationDetailsRepository).findByMaterialId(materialId);
        assertThat(ncesEmailNotificationDetails.payloadAsJsonObject().getString(MATERIAL_ID), is(materialId.toString()));
        assertThat(ncesEmailNotificationDetails.payloadAsJsonObject().getString("id"), is(entity.getId().toString()));
        assertThat(ncesEmailNotificationDetails.payloadAsJsonObject().getString("masterDefendantId"), is(entity.getMasterDefendantId().toString()));
        assertThat(ncesEmailNotificationDetails.payloadAsJsonObject().getString("notificationId"), is(entity.getNotificationId().toString()));
        assertThat(ncesEmailNotificationDetails.payloadAsJsonObject().getString("subject"), is(entity.getSubject()));
        assertThat(ncesEmailNotificationDetails.payloadAsJsonObject().getString("sendTo"), is(entity.getSendTo()));
    }

    private JsonEnvelope createJsonEnvelope(final UUID materialId) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("results.query.nces-email-notification-details")
                .build();

        return JsonEnvelope.envelopeFrom(metadata, createObjectBuilder().add(MATERIAL_ID, materialId.toString()).build());
    }

    private NcesEmailNotificationDetailsEntity createNcesEmailNotificationDetailsEntity(final UUID materialId) {
        NcesEmailNotificationDetailsEntity ncesEmailNotificationDetailsEntity = new NcesEmailNotificationDetailsEntity();
        ncesEmailNotificationDetailsEntity.setId(randomUUID());
        ncesEmailNotificationDetailsEntity.setMasterDefendantId(randomUUID());
        ncesEmailNotificationDetailsEntity.setMaterialId(materialId);
        ncesEmailNotificationDetailsEntity.setNotificationId(randomUUID());
        ncesEmailNotificationDetailsEntity.setSubject("subject");
        ncesEmailNotificationDetailsEntity.setSendTo("mail@email.com");

        return ncesEmailNotificationDetailsEntity;
    }
}
