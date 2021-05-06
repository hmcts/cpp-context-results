package uk.gov.moj.cpp.results.event.service;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.results.event.helper.Originator.assembleEnvelopeWithPayloadAndMetaDetails;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaterialService {
    protected static final String UPLOAD_MATERIAL = "material.command.upload-file";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialService.class.getCanonicalName());
    private static final String FIELD_MATERIAL_ID = "materialId";
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final JsonEnvelope envelope) {
        LOGGER.info("material being uploaded '{}' file service id '{}'", materialId, fileServiceId);
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));
        final JsonObject uploadMaterialPayload = Json.createObjectBuilder()
                .add(FIELD_MATERIAL_ID, materialId.toString())
                .add("fileServiceId", fileServiceId.toString())
                .build();

        LOGGER.info("requesting material service to upload file id {} for material {}", fileServiceId, materialId);

        sender.send(assembleEnvelopeWithPayloadAndMetaDetails(uploadMaterialPayload, UPLOAD_MATERIAL, userId.toString()));
    }
}
