package uk.gov.moj.cpp.results.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.CacheService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class ResultsEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventProcessor.class);
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "id";
    private static final String HEARING = "hearing";
    private static final String RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED = "results.case-or-application-ejected";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private CacheService cacheService;

    @Inject
    private EventGridService eventGridService;

    @Handles("public.hearing.resulted")
    @SuppressWarnings({"squid:S2221"})
    public void hearingResulted(final JsonEnvelope envelope) {

        LOGGER.info("Hearing Resulted Event Received");

        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        final JsonObject transformedHearing = hearingHelper.transformedHearing(hearingResultPayload.getJsonObject(HEARING));

        final String hearingId = transformedHearing.getString(HEARING_ID);

        try {
            LOGGER.info("Adding hearing {} to Redis Cache", hearingId);
            cacheService.add(hearingId, transformedHearing.toString());
        } catch (Exception e) {
            LOGGER.error("Exception caught while connection to cache service: {}", e);
        }

        LOGGER.info("Adding Hearing Resulted for hearing {} to EventGrid", hearingId);
        eventGridService.sendHearingResultedEvent(hearingId);

        sender.sendAsAdmin(enveloper.withMetadataFrom(envelope, "results.command.add-hearing-result").apply(hearingResultPayload));
    }

    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                    .build();
            if(payload.containsKey(PROSECUTION_CASE_ID)) {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                    final JsonObject caseEjectedCommandPayload = Json.createObjectBuilder()
                            .add(HEARING_IDS,hearingIds)
                            .add(CASE_ID, caseId)
                            .build();
                    sender.sendAsAdmin(JsonEnvelope.envelopeFrom(metadata, caseEjectedCommandPayload));
            } else  {
                final String applicationId = payload.getString(APPLICATION_ID);
                    final JsonObject applicationEjectedCommandPayload = Json.createObjectBuilder()
                            .add(HEARING_IDS, hearingIds)
                            .add(APPLICATION_ID, applicationId)
                            .build();
                    sender.sendAsAdmin(JsonEnvelope.envelopeFrom(metadata, applicationEjectedCommandPayload));
            }

        } else  {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload has been ignored as it does not contain hearing ids : {}" ,  envelope.toObfuscatedDebugString());
            }
        }
    }
}
