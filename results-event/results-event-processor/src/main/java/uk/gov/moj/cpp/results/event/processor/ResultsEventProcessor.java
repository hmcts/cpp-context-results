package uk.gov.moj.cpp.results.event.processor;

import static java.util.Comparator.comparing;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.notification.Notification;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.DocumentGeneratorService;
import uk.gov.moj.cpp.results.event.service.EmailNotification;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@SuppressWarnings({"squid:S2221"})
public class ResultsEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventProcessor.class);
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "id";
    private static final String HEARING = "hearing";
    private static final String RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED = "results.case-or-application-ejected";
    private static final String CACHE_KEY_SUFFIX = "_result_";
    private static final String CACHE_KEY_SJP_PREFIX = "SJP_";
    private static final String CACHE_KEY_EXTERNAL_PREFIX = "EXT_";
    private static final String CACHE_KEY_INTERNAL_PREFIX = "INT_";
    private static final String SHARED_TIME = "sharedTime";
    private static final String URN = "URN";
    private static final String COMMON_PLATFORM_URL = "common_platform_url";

    @Inject
    @Value(key = "ncesEmailNotificationTemplateId")
    private String ncesEmailNotificationTemplateId;

    @Inject
    ReferenceDataService referenceDataService;

    @Inject
    ReferenceCache referenceCache;

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private CacheService cacheService;

    @Inject
    private EventGridService eventGridService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    private static final String RESULTS_NCES_SEND_EMAIL_NOT_FOUND = "results.event.send-nces-email-not-found";

    @Handles("public.hearing.resulted")
    public void hearingResulted(final JsonEnvelope envelope) {

        LOGGER.info("Hearing Resulted Event Received");

        final JsonObject hearingPayload = envelope.payloadAsJsonObject();
        final JsonString sharedTime = hearingPayload.getJsonString(SHARED_TIME);
        final JsonObject transformedHearing = hearingHelper.transformedHearing(hearingPayload.getJsonObject(HEARING));
        final JsonObject externalPayload = createObjectBuilder()
                .add(HEARING, transformedHearing)
                .add(SHARED_TIME, sharedTime)
                .build();
        final String hearingId = transformedHearing.getString(HEARING_ID);

        if (hearingPayload.getJsonObject(HEARING).getBoolean("isSJPHearing", false)) {

            final String cacheKeySjp = CACHE_KEY_SJP_PREFIX + hearingId + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with sjp key {} to Redis Cache", hearingId, cacheKeySjp);
                cacheService.add(cacheKeySjp, hearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: {} with sjp key {}", e, cacheKeySjp);
            }

            sendEventToGrid(envelope, hearingId, "SJP_Hearing_Resulted");
        } else {
            final String cacheKeyExternal = CACHE_KEY_EXTERNAL_PREFIX + hearingId + CACHE_KEY_SUFFIX;
            final String cacheKeyInternal = CACHE_KEY_INTERNAL_PREFIX + hearingId + CACHE_KEY_SUFFIX;

            try {
                LOGGER.info("Adding external JSON document for hearing {} with external key {} to Redis Cache", hearingId, cacheKeyExternal);
                cacheService.add(cacheKeyExternal, externalPayload.toString());

                LOGGER.info("Adding internal JSON document for hearing {} with internal key {} to Redis Cache", hearingId, cacheKeyInternal);
                cacheService.add(cacheKeyInternal, hearingPayload.toString());
            } catch (Exception e) {
                LOGGER.error("Exception caught while attempting to connect to cache service: {} with LAA key {} {}", e, cacheKeyExternal, cacheKeyInternal);
            }

            sendEventToGrid(envelope, hearingId, "Hearing_Resulted");
        }

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(hearingPayload)
                .withName("results.command.add-hearing-result")
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }

    @Handles("results.hearing-results-added")
    public void hearingResultAdded(final JsonEnvelope resultsAddedEvent) {
        processResultsAdded(resultsAddedEvent, "results.create-results", Optional.empty());
    }

    @Handles("results.events.hearing-results-added-for-day")
    public void hearingResultAddedForDay(final JsonEnvelope resultsAddedForDayEvent) {
        final LocalDate hearingDay = LocalDate.parse(resultsAddedForDayEvent.payloadAsJsonObject().getString("hearingDay"), DateTimeFormatter.ISO_LOCAL_DATE);
        processResultsAdded(resultsAddedForDayEvent, "results.command.create-results-for-day", Optional.of(hearingDay));
    }

    @Handles("results.event.police-result-generated")
    public void createResult(final JsonEnvelope envelope) {
        LOGGER.debug("results.event.police-result-generated {}", envelope.payload());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("public.results.police-result-generated")
                .build();

        sender.sendAsAdmin(envelopeFrom(metadata, envelope.payloadAsJsonObject()));
    }

    @Handles("results.event.nces-email-notification-requested")
    public void handleEmailToNcesNotificationRequested(final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));
        final UUID materialId = UUID.randomUUID();

        final JsonObject requestJson = envelope.payloadAsJsonObject();
        final NcesEmailNotificationRequested ncesEmailNotificationRequested = jsonObjectToObjectConverter.convert(requestJson, NcesEmailNotificationRequested.class);
        final String sendToAddress = ncesEmailNotificationRequested.getSendTo();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Nces notification requested payload - {}", requestJson);
        }

        //generate and upload pdf
        documentGeneratorService.generateNcesDocument(sender, envelope, userId , materialId);

        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(ncesEmailNotificationRequested.getNotificationId())
                .withTemplateId(fromString(ncesEmailNotificationTemplateId))
                .withSendToAddress(ncesEmailNotificationRequested.getSendTo())
                .withSubject(ncesEmailNotificationRequested.getSubject())
                .withMaterialUrl(materialUrl)
                .build();

        //Send Email
        if (isEmpty(sendToAddress)) {
            final Envelope<JsonObject> jsonObjectEnvelope = envelop(envelope.payloadAsJsonObject())
                    .withName(RESULTS_NCES_SEND_EMAIL_NOT_FOUND)
                    .withMetadataFrom(envelope);
            sender.sendAsAdmin(jsonObjectEnvelope);
        } else {
            notificationNotifyService.sendNcesEmail(emailNotification, envelope);
        }
    }


    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            if (payload.containsKey(PROSECUTION_CASE_ID)) {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                final JsonObject caseEjectedCommandPayload = createObjectBuilder()
                        .add(HEARING_IDS, hearingIds)
                        .add(CASE_ID, caseId)
                        .build();

                raiseHandlerEvent(envelope, caseEjectedCommandPayload);
            } else {
                final String applicationId = payload.getString(APPLICATION_ID);
                final JsonObject applicationEjectedCommandPayload = createObjectBuilder()
                        .add(HEARING_IDS, hearingIds)
                        .add(APPLICATION_ID, applicationId)
                        .build();

                raiseHandlerEvent(envelope, applicationEjectedCommandPayload);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload has been ignored as it does not contain hearing ids : {}", envelope.toObfuscatedDebugString());
            }
        }
    }

    @Handles("results.event.police-notification-requested")
    public void handlePoliceNotificationRequested(final JsonEnvelope envelope) {
        final PoliceNotificationRequested policeNotificationRequested = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PoliceNotificationRequested.class);
        final JsonObject notificationPayload = this.objectToJsonObjectConverter.convert(buildNotification(policeNotificationRequested));
        notificationNotifyService.sendEmailNotification(envelope, notificationPayload);
    }

    private void sendEventToGrid(final JsonEnvelope envelope, final String hearingId, final String eventType) {
        final Optional<String> userId = envelope.metadata().userId();
        try {
            LOGGER.info("Adding Hearing Resulted for hearing {} and eventType {} to EventGrid", hearingId, eventType);
            userId.ifPresent(s -> eventGridService.sendHearingResultedEvent(UUID.fromString(s), hearingId, eventType));
        }
        catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to EventGrid: {} for eventType {}", e, eventType);
        }
    }

    /**
     * Processes Results Added, original command without hearingDay or the new command (multi-day).
     *
     * @param envelope    - the event being processed.
     * @param commandName - the name of the command to hit (v1 or v2, where v2 supports multi-day)
     * @param hearingDay  - optional hearingDay, used in the latest command that supports multi day
     *                    hearings
     */
    private void processResultsAdded(final JsonEnvelope envelope, final String commandName, final Optional<LocalDate> hearingDay) {
        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(hearingResultPayload, PublicHearingResulted.class);

        final List<CaseDetails> caseDetails = new CasesConverter(referenceCache, referenceDataService).convert(publicHearingResulted);

        if (isNotEmpty(caseDetails)) {
            publicHearingResulted.getHearing().getHearingDays().sort(comparing(HearingDay::getSittingDay).reversed());
            final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
            caseDetails.forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));

            final JsonObjectBuilder resultJsonPayload = createObjectBuilder();
            resultJsonPayload.add("session", objectToJsonObjectConverter.convert(new BaseStructureConverter(referenceDataService).convert(publicHearingResulted)));
            resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());
            resultJsonPayload.add("jurisdictionType", publicHearingResulted.getHearing().getJurisdictionType().toString());

            if (hearingDay.isPresent()) {
                resultJsonPayload.add("hearingDay", hearingDay.get().toString());
            }

            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName(commandName)
                    .build();
            final JsonObject payload = resultJsonPayload.build();
            sender.sendAsAdmin(envelopeFrom(metadata, payload));
        }
    }

    private Notification buildNotification(final PoliceNotificationRequested policeNotificationRequested) {
        final Map<String, String> personalisationProperties = new HashMap<>();
        personalisationProperties.put(URN, policeNotificationRequested.getUrn());
        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());
        return new Notification(policeNotificationRequested.getNotificationId(), fromString(applicationParameters.getEmailTemplateId()), policeNotificationRequested.getPoliceEmailAddress(), personalisationProperties);
    }

    private void raiseHandlerEvent(final JsonEnvelope envelope, final JsonObject commandPayload) {
        final Envelope<JsonObject> jsonObjectEnvelope = envelop(commandPayload)
                .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }
}
