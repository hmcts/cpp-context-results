package uk.gov.moj.cpp.results.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROSECUTION_CASES;
import static uk.gov.moj.cpp.domains.SchemaVariableConstants.PROSECUTION_CASE_IDENTIFIER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.results.event.helper.ApplicationFinalResultsEnricher;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
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
    private static final String IS_RESHARE = "isReshare";
    private static final String HEARING_DAY = "hearingDay";
    private static final String SHADOW_LISTED_OFFENCES = "shadowListedOffences";
    private static final String HEARING_POLICE_CASE_PROSECUTORS = "policeCases";
    private static final String OU_CODE = "prosecutionAuthorityOUCode";
    private static final String PROSECUTOR_CODE = "prosecutionAuthorityId";

    private static final String CPS_PROSECUTOR_IDS = "cpsProsecutorIds";
    private static final String IS_SJP_HEARING = "isSJPHearing";
    private static final String COMMAND_HEARING_ID = "hearingId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_INFORMANT_REGISTER_PUBLISH = "results.command.request-informant-register-publish";

    /**
     * The runtime switch between the legacy informant-register function app and the new informant
     * register service. Evaluated here, before the publish request is recorded, so that toggle-off
     * means no command is sent and nothing downstream changes at all. The guard is fail-closed: no
     * flag row for the environment's label means disabled, so every environment stays on the legacy
     * path until the flag is explicitly created and enabled for its label.
     */
    public static final String INFORMANT_REGISTER_SERVICE_FEATURE = "InformantRegisterService";

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private CacheService cacheService;

    @Inject
    private EventGridService eventGridService;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ApplicationFinalResultsEnricher applicationResultsEnricher;

    @Handles("public.events.hearing.hearing-resulted")
    @SuppressWarnings({"squid:S2221"})
    public void handleHearingResultedPublicEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.events.hearing.hearing-resulted event received {}", envelope.toObfuscatedDebugString());
        }

        final JsonObject hearingPayload = envelope.payloadAsJsonObject();
        final JsonString sharedTime = hearingPayload.getJsonString(SHARED_TIME);
        final String hearingDay = hearingPayload.getString(HEARING_DAY);
        final JsonObject transformedHearing = hearingHelper.transformedHearing(hearingPayload.getJsonObject(HEARING));
        final JsonObject internalHearingPayload = applicationResultsEnricher.enrichIfApplicationResultsMissing(hearingPayload);


        final JsonObject externalPayload = createObjectBuilder()
                .add(HEARING, transformedHearing)
                .add(CPS_PROSECUTOR_IDS, extractCPSProsecutorIds())
                .add(HEARING_POLICE_CASE_PROSECUTORS, extractPoliceCases(transformedHearing))
                .add(SHARED_TIME, sharedTime)
                .build();

        final String hearingId = transformedHearing.getString(HEARING_ID);

        final boolean isSjpHearing = hearingPayload.getJsonObject(HEARING).getBoolean(IS_SJP_HEARING, false);

        if (isSjpHearing) {
            final String cacheKeySjp = CACHE_KEY_SJP_PREFIX + hearingId + "_" + hearingDay + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with sjp key {} to Redis Cache", hearingId, cacheKeySjp);
                cacheService.add(cacheKeySjp, internalHearingPayload.toString());
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
                cacheService.add(cacheKeyInternal, internalHearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: ", e);
            }

            sendEventToGrid(envelope, hearingId, hearingDay, "Hearing_Resulted");
        }

        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder()
                .add(HEARING, internalHearingPayload.getJsonObject(HEARING))
                .add(SHARED_TIME, sharedTime)
                .add(IS_RESHARE, hearingPayload.getBoolean(IS_RESHARE))
                .add(HEARING_DAY, hearingDay);

        if (hearingPayload.containsKey(SHADOW_LISTED_OFFENCES)) {
            commandPayloadBuilder.add(SHADOW_LISTED_OFFENCES, hearingPayload.getJsonArray(SHADOW_LISTED_OFFENCES));
        }

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(commandPayloadBuilder.build())
                .withName("results.command.add-hearing-result-for-day")
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);

        // Last, and only for a regular hearing.
        if (!isSjpHearing) {
            requestInformantRegisterPublish(envelope, hearingId, hearingDay, sharedTime.getString());
        }
    }

    /**
     * Records that this share owes an informant register publish, by sending a command that the
     * command handler turns into a durable event. The Service Bus publish itself now happens on an
     * event processor reading that event, so a broker failure there is redelivered and ultimately
     * dead-lettered instead of being logged and dropped.
     *
     * <p>The command is sent inside this delivery's transaction, so the publish request is recorded
     * only if the resulting it belongs to commits. That is the point of routing through a command
     * rather than publishing inline.
     *
     * <p>What the catch is for, precisely - it does NOT protect the resulting from a rollback, and
     * must not be read as doing so. Three kinds of failure can arrive here:
     * <ul>
     *   <li><b>Caller-side and permanent</b> - a metadata userId that is not a canonical uuid, or a
     *   payload the command's schema rejects. These throw before any JMS work, leaving the
     *   transaction untouched, and are swallowed on purpose: they are defects in this share's
     *   identity, they will fail identically on every redelivery, and they must not cost the
     *   resulting. This mirrors what the Event Grid leg does with the same malformed userId.</li>
     *   <li><b>Caller-side and transient</b> - the feature-store lookup itself fails. With the
     *   caching provider that read is an in-memory map and cannot throw, but with the non-caching
     *   provider it is a remote fetch and the framework does not catch it. It is inside the try for
     *   that reason: the flag read is not worth a resulting. Unlike the permanent case it will
     *   likely succeed on the next share, so the cost is at most this one register.</li>
     *   <li><b>Broker-side</b> - the send itself fails. That marks the transaction rollback-only, so
     *   the resulting command rolls back with it and the whole hearing-resulted event is redelivered
     *   no matter what this catch does. That is the wanted behaviour, and the catch neither causes
     *   nor prevents it.</li>
     * </ul>
     * So the log line below means "this share will have no informant register", never "carrying on
     * regardless" - which is why it is at ERROR.
     */
    @SuppressWarnings({"squid:S2221"})
    private void requestInformantRegisterPublish(final JsonEnvelope envelope, final String hearingId, final String hearingDay, final String sharedTime) {
        try {
            if (!featureControlGuard.isFeatureEnabled(INFORMANT_REGISTER_SERVICE_FEATURE)) {
                LOGGER.info("Feature {} is not enabled - no informant register publish requested for hearing {}, hearingDay {}; the legacy function app remains responsible",
                        INFORMANT_REGISTER_SERVICE_FEATURE, hearingId, hearingDay);
                return;
            }

            final Optional<String> userId = envelope.metadata().userId();
            if (userId.isEmpty()) {
                LOGGER.warn("No userId on the hearing-resulted event - no informant register publish requested for hearing {}, hearingDay {}",
                        hearingId, hearingDay);
                return;
            }

            final JsonObject payload = createObjectBuilder()
                    .add(COMMAND_HEARING_ID, hearingId)
                    .add(HEARING_DAY, hearingDay)
                    .add(SHARED_TIME, sharedTime)
                    .add(USER_ID, UUID.fromString(userId.get()).toString())
                    .build();

            LOGGER.info("Requesting informant register publish for hearing {}, hearingDay {}", hearingId, hearingDay);
            sender.sendAsAdmin(envelop(payload)
                    .withName(REQUEST_INFORMANT_REGISTER_PUBLISH)
                    .withMetadataFrom(envelope));
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to request the informant register publish for hearing {}, hearingDay {}", hearingId, hearingDay, e);
        }
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

    public JsonArray extractCPSProsecutorIds() {
        final JsonArrayBuilder cpsFlagTrueProscutionIds = createArrayBuilder();

        referenceDataService.getProsecutorIdForCPSFlagTrue().forEach(cpsFlagTrueProscutionIds::add);
        return cpsFlagTrueProscutionIds.build();
    }

    /**
     * Method to check hearing contains police case prosecutors, to send it to VEP
     *
     * @param hearing transformedHearing is passed to this method
     * @return JSonArray return policeCases caseids
     */
    public JsonArray extractPoliceCases(JsonObject hearing) {
        LOGGER.info("Results extractPoliceCases hearing {}", hearing.get(ID));
        final JsonArray prosecutionCases = (JsonArray) hearing.get(PROSECUTION_CASES);
        final JsonArrayBuilder policeCases = createArrayBuilder();
        if (null != prosecutionCases && !prosecutionCases.isEmpty()) {
            for (int i = 0; i < prosecutionCases.size(); i++) {
                final JsonObject prosecutionCase = prosecutionCases.getJsonObject(i);
                final JsonObject prosecutionCaseIdentifier = prosecutionCase.getJsonObject(PROSECUTION_CASE_IDENTIFIER);
                final boolean policeFlag = referenceDataService.getPoliceFlag(prosecutionCaseIdentifier.getString(OU_CODE, null), prosecutionCaseIdentifier.getString(PROSECUTOR_CODE, null));
                LOGGER.info("Results prosecutionCase policeFlag {}", policeFlag);
                if (policeFlag) {
                    LOGGER.info("Results prosecutionCase id {}", prosecutionCase.get(ID));
                    policeCases.add(prosecutionCase.get(ID));

                }
            }
        }
        return policeCases.build();
    }
}