package uk.gov.moj.cpp.results.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.SharedVariant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.NowsMaterialStatusUpdated;
import uk.gov.moj.cpp.results.persist.HearingResultedDocumentRepository;
import uk.gov.moj.cpp.results.persist.entity.HearingResultedDocument;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.transaction.Transactional;
import java.io.StringReader;

@ServiceComponent(EVENT_LISTENER)
public class UpdateNowsMaterialStatusEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateNowsMaterialStatusEventListener.class);

    @Inject
    private HearingResultedDocumentRepository hearingResultedDocumentRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Transactional
    @Handles("results.event.nows-material-status-updated")
    public void onNowsMaterialStatusUpdated(final JsonEnvelope event) {
        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialStatusUpdated.class);
        final HearingResultedDocument hearingResultedDocument = hearingResultedDocumentRepository.findBy(nowsMaterialStatusUpdated.getHearingId());
        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(jsonFromString(hearingResultedDocument.getPayload()), PublicHearingResulted.class);

        for (final SharedVariant variant : publicHearingResulted.getVariants()) {
            if (variant.getMaterialId().equals(nowsMaterialStatusUpdated.getMaterialId())) {
                variant.setStatus(nowsMaterialStatusUpdated.getStatus());
            }
        }
        hearingResultedDocument.setPayload(objectToJsonObjectConverter.convert(publicHearingResulted).toString());
        hearingResultedDocumentRepository.save(hearingResultedDocument);
        LOGGER.info("Status successfully updated to: {} for material id: {}", nowsMaterialStatusUpdated.getStatus(), nowsMaterialStatusUpdated.getMaterialId());
    }

    private static JsonObject jsonFromString(String payload) {
        final JsonReader jsonReader = Json.createReader(new StringReader(payload));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}