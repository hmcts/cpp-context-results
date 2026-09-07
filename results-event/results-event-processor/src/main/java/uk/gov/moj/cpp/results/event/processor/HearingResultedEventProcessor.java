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

/**
 * Fans a {@code public.events.hearing.hearing-resulted} event out to Redis, the results command
 * handler, the informant register and Event Grid.
 *
 * <p>The order of those operations is load-bearing. This processor runs inside the container
 * transaction that also acknowledges the inbound event, and the steps fall into two kinds:
 * <ul>
 *   <li><b>Transactional</b> - the JMS command sends. They go through the XA-enlisted connection
 *   factory, so the commands become visible only when the delivery commits, and a send failure
 *   rolls the whole delivery back and the event is redelivered.</li>
 *   <li><b>Not transactional</b> - the Redis writes and the Event Grid publish. Both are plain
 *   network calls that a rollback cannot undo.</li>
 * </ul>
 * So the handler runs the steps as: Redis, then the command sends, then Event Grid, last.
 * <ol>
 *   <li>Redis goes first because the Event Grid subscribers read the cached documents on receipt,
 *   so the documents must exist before the pointer event does. The keys are fixed per hearing and
 *   day, so a redelivery re-writes the same documents and nothing is duplicated.</li>
 *   <li>The command sends go next, before anything that cannot be rolled back. If either fails,
 *   the delivery rolls back with no external side effect and the redelivery starts clean.</li>
 *   <li>Event Grid goes last precisely because it is fire-and-forget: had it run before a send
 *   that then failed, every redelivery would publish another {@code Hearing_Resulted} for the same
 *   share. The one window left is a commit failure after the Event Grid POST, which is accepted.</li>
 * </ol>
 * Moving a step out of this order reintroduces one of those failure modes.
 */
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

        // Step 1 - Redis. Not transactional, but idempotent: the keys are fixed per hearing and day,
        // so a redelivery re-writes the same documents. Must precede the Event Grid publish because
        // the subscribers read these documents when the pointer event arrives.
        if (isSjpHearing) {
            final String cacheKeySjp = CACHE_KEY_SJP_PREFIX + hearingId + "_" + hearingDay + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with sjp key {} to Redis Cache", hearingId, cacheKeySjp);
                cacheService.add(cacheKeySjp, internalHearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: {} with sjp key {}", e, cacheKeySjp);
            }
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
        }

        // Step 2 - the command sends. Transactional: both are XA-enlisted with this delivery, so
        // they are visible only on commit, and a failure here rolls the delivery back before
        // anything irreversible has happened. Neither send is caught for that reason.
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

        // Only for a regular hearing; SJP hearings are out of scope for the informant register.
        if (!isSjpHearing) {
            requestInformantRegisterPublish(envelope, hearingId, hearingDay, sharedTime.getString());
        }

        // Step 3 - Event Grid, last. Fire-and-forget over HTTP: it cannot be rolled back and never
        // throws, so it must run only once everything that can fail has succeeded. Otherwise a
        // failed send would roll the delivery back and every redelivery would publish another
        // Hearing_Resulted for the same share.
        sendEventToGrid(envelope, hearingId, hearingDay, isSjpHearing ? "SJP_Hearing_Resulted" : "Hearing_Resulted");
    }

    /**
     * Records that this share owes an informant register publish, by sending a command that the
     * command handler turns into a durable event. The Service Bus publish itself happens on an
     * event processor reading that event, so a broker failure there is redelivered and ultimately
     * dead-lettered instead of being logged and dropped.
     *
     * <p>The command is sent inside this delivery's transaction, so the publish request is recorded
     * only if the resulting it belongs to commits. That is the point of routing through a command
     * rather than publishing inline.
     *
     * <p>The send is deliberately not caught. If it fails, the exception propagates, the container
     * rolls the delivery back - the resulting command sent just before it included - and the
     * hearing-resulted event is redelivered. Catching it here would let the resulting commit while
     * the informant register request is silently lost, which is the failure this chain exists to
     * remove. Because the send runs before the Event Grid publish, the rollback has no external
     * side effect to undo.
     *
     * <p>What is caught, in {@link #informantRegisterPublishPayload}, is everything that decides
     * whether there is anything to send: the feature-flag read and the assembly of the payload from
     * the event's metadata. Those failures are either permanent for this share (a userId that is not
     * a canonical uuid) or not worth a resulting (the flag read itself), and none of them involves
     * JMS, so skipping the register and letting the resulting proceed is the right outcome.
     */
    private void requestInformantRegisterPublish(final JsonEnvelope envelope, final String hearingId, final String hearingDay, final String sharedTime) {
        final Optional<JsonObject> payload = informantRegisterPublishPayload(envelope, hearingId, hearingDay, sharedTime);
        if (payload.isEmpty()) {
            return;
        }

        LOGGER.info("Requesting informant register publish for hearing {}, hearingDay {}", hearingId, hearingDay);
        // THROWAWAY - simulated Artemis failure for InformantRegisterSendFailureThrowawayIT. Never merge.
        if (hearingId.endsWith("dead")) {
            throw new RuntimeException("THROWAWAY simulated Artemis failure for hearing " + hearingId);
        }
        sender.sendAsAdmin(envelop(payload.get())
                .withName(REQUEST_INFORMANT_REGISTER_PUBLISH)
                .withMetadataFrom(envelope));
    }

    /**
     * Decides whether this share owes an informant register publish and, if so, builds the command
     * payload. Returns empty when the feature is off, when the event carries no userId, or when
     * anything in here throws - each of those means "this share will have no informant register",
     * never "carrying on regardless", which is why the exception case is logged at ERROR. A
     * malformed userId is rejected on this side rather than dead-lettered on arrival at the
     * consumer, mirroring what the Event Grid leg does with the same value.
     */
    @SuppressWarnings({"squid:S2221"})
    private Optional<JsonObject> informantRegisterPublishPayload(final JsonEnvelope envelope, final String hearingId, final String hearingDay, final String sharedTime) {
        try {
            if (!featureControlGuard.isFeatureEnabled(INFORMANT_REGISTER_SERVICE_FEATURE)) {
                LOGGER.info("Feature {} is not enabled - no informant register publish requested for hearing {}, hearingDay {}; the legacy function app remains responsible",
                        INFORMANT_REGISTER_SERVICE_FEATURE, hearingId, hearingDay);
                return Optional.empty();
            }

            final Optional<String> userId = envelope.metadata().userId();
            if (userId.isEmpty()) {
                LOGGER.warn("No userId on the hearing-resulted event - no informant register publish requested for hearing {}, hearingDay {}",
                        hearingId, hearingDay);
                return Optional.empty();
            }

            return Optional.of(createObjectBuilder()
                    .add(COMMAND_HEARING_ID, hearingId)
                    .add(HEARING_DAY, hearingDay)
                    .add(SHARED_TIME, sharedTime)
                    .add(USER_ID, UUID.fromString(userId.get()).toString())
                    .build());
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to request the informant register publish for hearing {}, hearingDay {}", hearingId, hearingDay, e);
            return Optional.empty();
        }
    }

    @SuppressWarnings({"squid:S2221"})
    private void sendEventToGrid(final JsonEnvelope envelope, final String hearingId, final String hearingDay, final String eventType) {
        // THROWAWAY - simulated Event Grid failure that escapes the catch below, so the delivery rolls
        // back and step 2 (both command sends) is undone. Logs the same attempt line as the real path
        // so the WildFly log counts attempts identically. Never merge.
        if (hearingId.endsWith("e6bad")) {
            LOGGER.info("Adding Hearing Resulted for hearing {}, hearingDay {} and eventType {} to EventGrid", hearingId, hearingDay, eventType);
            throw new RuntimeException("THROWAWAY simulated Event Grid failure for hearing " + hearingId);
        }
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