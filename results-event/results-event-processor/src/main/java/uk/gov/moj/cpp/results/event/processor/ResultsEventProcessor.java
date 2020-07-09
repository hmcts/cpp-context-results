package uk.gov.moj.cpp.results.event.processor;

import static java.util.Comparator.comparing;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.BaseStructure;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.sjp.results.PublicSjpResulted;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.notification.Notification;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.event.helper.BaseSessionStructureConverterForSjp;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CaseDetailsConverterForSjp;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String CACHE_KEY_SUFFIX = "_result_";
    private static final String CACHE_KEY_EXTERNAL_PREFIX = "EXT_";
    private static final String CACHE_KEY_INTERNAL_PREFIX = "INT_";
    private static final String SHARED_TIME = "sharedTime";
    private static final String URN = "URN";
    private static final String COMMON_PLATFORM_URL = "common_platform_url";

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

    @Handles("public.hearing.resulted")
    @SuppressWarnings({"squid:S2221"})
    public void hearingResulted(final JsonEnvelope envelope) {

        LOGGER.info("Hearing Resulted Event Received");

        final JsonObject internalPayload = envelope.payloadAsJsonObject();

        final JsonString sharedTime = internalPayload.getJsonString(SHARED_TIME);

        final JsonObject transformedHearing = hearingHelper.transformedHearing(internalPayload.getJsonObject(HEARING));

        final JsonObject externalPayload = Json.createObjectBuilder()
                .add(HEARING, transformedHearing)
                .add(SHARED_TIME, sharedTime)
                .build();

        final String hearingId = transformedHearing.getString(HEARING_ID);

        final String cacheKeyExternal = CACHE_KEY_EXTERNAL_PREFIX + hearingId + CACHE_KEY_SUFFIX;
        final String cacheKeyInternal = CACHE_KEY_INTERNAL_PREFIX + hearingId + CACHE_KEY_SUFFIX;

        final Optional<String> userId = envelope.metadata().userId();

        try {
            LOGGER.info("Adding external JSON document for hearing {} to Redis Cache", hearingId);
            cacheService.add(cacheKeyExternal, externalPayload.toString());

            LOGGER.info("Adding internal JSON document for hearing {} to Redis Cache", hearingId);
            cacheService.add(cacheKeyInternal, internalPayload.toString());
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to cache service: ", e);
        }

        try {
            LOGGER.info("Adding Hearing Resulted for hearing {} to EventGrid", hearingId);
            userId.ifPresent(s -> eventGridService.sendHearingResultedEvent(UUID.fromString(s), hearingId));
        } catch (Exception e) {
            LOGGER.error("Exception caught while attempting to connect to EventGrid: ", e);
        }

        final Envelope<JsonObject> jsonObjectEnvelope = envelop(internalPayload)
                .withName("results.command.add-hearing-result")
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }

    @Handles("results.hearing-results-added")
    public void hearingResultAdded(final JsonEnvelope envelope) {

        final JsonObject hearingResultPayload = envelope.payloadAsJsonObject();

        final PublicHearingResulted publicHearingResulted = jsonObjectToObjectConverter.convert(hearingResultPayload, PublicHearingResulted.class);

        if (isNotEmpty(publicHearingResulted.getHearing().getProsecutionCases())) {
            publicHearingResulted.getHearing().getHearingDays().sort(comparing(HearingDay::getSittingDay).reversed());
            final BaseStructure baseStructure = new BaseStructureConverter(referenceDataService).convert(publicHearingResulted);
            final List<CaseDetails> caseDetails = new CasesConverter(referenceCache).convert(publicHearingResulted);
            final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
            caseDetails.stream().forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
            final JsonObjectBuilder resultJsonPayload = Json.createObjectBuilder();
            baseStructure.setSourceType("CC");
            resultJsonPayload.add("session", objectToJsonObjectConverter.convert(baseStructure));
            resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());
            resultJsonPayload.add("jurisdictionType", publicHearingResulted.getHearing().getJurisdictionType().toString());

            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName("results.create-results")
                    .build();

            sender.sendAsAdmin(envelopeFrom(metadata, resultJsonPayload.build()));

        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("No Prosecution Cases present for hearing id : {} ", publicHearingResulted.getHearing().getId());
        }
    }

    @Handles("results.event.police-result-generated")
    public void createResult(final JsonEnvelope envelope) {
        LOGGER.debug("results.event.police-result-generated {}", envelope.payload());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("public.results.police-result-generated")
                .build();


        sender.sendAsAdmin(envelopeFrom(metadata, envelope.payloadAsJsonObject()));

    }

    @Handles("public.sjp.case-resulted")
    public void sjpCaseResulted(final JsonEnvelope envelope) {

        final JsonObject sjpResultedPayload = envelope.payloadAsJsonObject();

        LOGGER.debug("public.sjp.case-resulted event received {}", sjpResultedPayload);
        final PublicSjpResulted publicSjpCaseResulted = jsonObjectToObjectConverter.convert(sjpResultedPayload, PublicSjpResulted.class);

        final BaseStructure baseStructure = new BaseSessionStructureConverterForSjp().convert(publicSjpCaseResulted);
        final List<CaseDetails> caseDetails = new CaseDetailsConverterForSjp(referenceCache).convert(publicSjpCaseResulted);
        final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
        caseDetails.stream().forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
        final JsonObjectBuilder resultJsonPayload = Json.createObjectBuilder();
        baseStructure.setSourceType("SJP");
        resultJsonPayload.add("session", objectToJsonObjectConverter.convert(baseStructure));
        resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("results.create-results")
                .build();
        sender.sendAsAdmin(envelopeFrom(metadata, resultJsonPayload.build()));
    }

    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            if (payload.containsKey(PROSECUTION_CASE_ID)) {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                final JsonObject caseEjectedCommandPayload = Json.createObjectBuilder()
                        .add(HEARING_IDS, hearingIds)
                        .add(CASE_ID, caseId)
                        .build();

                final Envelope<JsonObject> jsonObjectEnvelope = envelop(caseEjectedCommandPayload)
                        .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                        .withMetadataFrom(envelope);
                sender.sendAsAdmin(jsonObjectEnvelope);

            } else {
                final String applicationId = payload.getString(APPLICATION_ID);
                final JsonObject applicationEjectedCommandPayload = Json.createObjectBuilder()
                        .add(HEARING_IDS, hearingIds)
                        .add(APPLICATION_ID, applicationId)
                        .build();

                final Envelope<JsonObject> jsonObjectEnvelope = envelop(applicationEjectedCommandPayload)
                        .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                        .withMetadataFrom(envelope);
                sender.sendAsAdmin(jsonObjectEnvelope);
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


    private Notification buildNotification(final PoliceNotificationRequested policeNotificationRequested) {
        final Map<String, String> personalisationProperties = new HashMap();
        personalisationProperties.put(URN, policeNotificationRequested.getUrn());
        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());
        return new Notification(policeNotificationRequested.getNotificationId(), fromString(applicationParameters.getEmailTemplateId()), policeNotificationRequested.getPoliceEmailAddress(), personalisationProperties);
    }
}
