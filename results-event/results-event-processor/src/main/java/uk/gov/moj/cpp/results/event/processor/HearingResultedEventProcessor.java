package uk.gov.moj.cpp.results.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.FeatureControl;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventProcessor.class);

    private static final String HEARING_ID = "id";
    private static final String HEARING = "hearing";
    private static final String CACHE_KEY_SUFFIX = "_result_";
    private static final String CACHE_KEY_SJP_PREFIX = "SJP_";
    private static final String CACHE_KEY_EXTERNAL_PREFIX = "EXT_";
    private static final String CACHE_KEY_INTERNAL_PREFIX = "INT_";
    private static final String SHARED_TIME = "sharedTime";
    private static final String HEARING_DAY = "hearingDay";
    private static final String SHADOW_LISTED_OFFENCES = "shadowListedOffences";

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private CacheService cacheService;

    @Inject
    private EventGridService eventGridService;

    @Handles("public.events.hearing.hearing-resulted")
    @SuppressWarnings({"squid:S2221"})
    @FeatureControl("amendReshare")
    public void handleHearingResultedPublicEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.events.hearing.hearing-resulted event received {}", envelope.toObfuscatedDebugString());
        }

        final JsonObject hearingPayload = envelope.payloadAsJsonObject();
        final JsonString sharedTime = hearingPayload.getJsonString(SHARED_TIME);
        final String hearingDay = hearingPayload.getString(HEARING_DAY);
        final JsonObject incomingHearing = hearingPayload.getJsonObject(HEARING);
        final JsonObject transformedHearing = hearingHelper.transformedHearing(incomingHearing);

        final JsonObject externalPayload = createObjectBuilder()
                .add(HEARING, transformedHearing)
                .add(SHARED_TIME, sharedTime)
                .build();

        final String hearingId = transformedHearing.getString(HEARING_ID);

        if (hearingPayload.getJsonObject(HEARING).getBoolean("isSJPHearing", false)) {
            final String cacheKeySjp = CACHE_KEY_SJP_PREFIX + hearingId + "_" + hearingDay + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with sjp key {} to Redis Cache", hearingId, cacheKeySjp);
                cacheService.add(cacheKeySjp, hearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: {} with sjp key {}", e, cacheKeySjp);
            }

            sendEventToGrid(envelope, hearingId, hearingDay, "SJP_Hearing_Resulted");
        } else {

            try {
                LOGGER.info("Adding external JSON document for hearing {}, hearingDay {} to Redis Cache", hearingId, hearingDay);
                final String cacheKeyExternal = CACHE_KEY_EXTERNAL_PREFIX + hearingId + "_" + hearingDay + CACHE_KEY_SUFFIX;
                cacheService.add(cacheKeyExternal, externalPayload.toString());

                LOGGER.info("Adding internal JSON document for hearing {}, hearingDay {} to Redis Cache", hearingId, hearingDay);
                final String cacheKeyInternal = CACHE_KEY_INTERNAL_PREFIX + hearingId + "_" + hearingDay + CACHE_KEY_SUFFIX;
                cacheService.add(cacheKeyInternal, hearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: ", e);
            }

            sendEventToGrid(envelope, hearingId, hearingDay, "Hearing_Resulted");
        }

        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder()
                .add(HEARING, incomingHearing)
                .add(SHARED_TIME, sharedTime)
                .add(HEARING_DAY, hearingDay);

        if (hearingPayload.containsKey(SHADOW_LISTED_OFFENCES)) {
            commandPayloadBuilder.add(SHADOW_LISTED_OFFENCES, hearingPayload.getJsonArray(SHADOW_LISTED_OFFENCES));
        }

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(commandPayloadBuilder.build())
                .withName("results.command.add-hearing-result-for-day")
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }

    @SuppressWarnings({"squid:S2221"})
    private void sendEventToGrid(final JsonEnvelope envelope, final String hearingId, final String hearingDay, final String eventType) {
        final Optional<String> userId = envelope.metadata().userId();
        try {
            LOGGER.info("Adding Hearing Resulted for hearing {}, hearingDay {} and eventType {} to EventGrid", hearingId, hearingDay, eventType);
            userId.ifPresent(s -> eventGridService.sendHearingResultedForDayEvent(UUID.fromString(s), hearingId, hearingDay, eventType));
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to EventGrid: {} for eventType {}", e, eventType);
        }
    }
}
