package uk.gov.moj.cpp.results.event.processor;

import static java.lang.Boolean.TRUE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.results.event.service.TemplateIdentifier.POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE;

import uk.gov.justice.core.courts.CaseDefendant;
import uk.gov.justice.core.courts.CaseDetails;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.IndividualDefendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.domains.HearingHelper;
import uk.gov.moj.cpp.domains.results.notification.Notification;
import uk.gov.moj.cpp.domains.results.shareresults.PublicHearingResulted;
import uk.gov.moj.cpp.results.domain.event.AppealUpdateNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.CaseResultDetails;
import uk.gov.moj.cpp.results.domain.event.NcesEmailNotification;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequested;
import uk.gov.moj.cpp.results.domain.event.PoliceNotificationRequestedV2;
import uk.gov.moj.cpp.results.event.helper.BaseStructureConverter;
import uk.gov.moj.cpp.results.event.helper.CasesConverter;
import uk.gov.moj.cpp.results.event.helper.DcsCaseHelper;
import uk.gov.moj.cpp.results.event.helper.FixedListComparator;
import uk.gov.moj.cpp.results.event.helper.PoliceEmailHelper;
import uk.gov.moj.cpp.results.event.helper.ReferenceCache;
import uk.gov.moj.cpp.results.event.service.ApplicationParameters;
import uk.gov.moj.cpp.results.event.service.CacheService;
import uk.gov.moj.cpp.results.event.service.ConversionFormat;
import uk.gov.moj.cpp.results.event.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.results.event.service.DocumentGeneratorService;
import uk.gov.moj.cpp.results.event.service.EmailNotification;
import uk.gov.moj.cpp.results.event.service.EventGridService;
import uk.gov.moj.cpp.results.event.service.FileService;
import uk.gov.moj.cpp.results.event.service.FileParams;
import uk.gov.moj.cpp.results.event.service.NotificationNotifyService;
import uk.gov.moj.cpp.results.event.service.ProgressionService;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;
import uk.gov.moj.cpp.results.event.service.SystemDocGenerator;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.results.event.service.SjpService;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:S2221", "squid:S1132"})
public class ResultsEventProcessor {
    public static final String DELIMITER = ",";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final String AMEND_RESHARE1 = "Amend & Reshare";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsEventProcessor.class);
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String HEARING_IDS = "hearingIds";
    private static final String CASE_ID = "caseId";
    private static final String SJP_CASE_ID = "id";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "id";
    private static final String HEARING = "hearing";
    private static final String IS_SJP_HEARING = "isSJPHearing";

    private static final String RESULTS_COMMAND_UPDATE_DEFENDANT_TRACKING_STATUS = "results.command.update-defendant-tracking-status";
    private static final String RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED = "results.case-or-application-ejected";
    private static final String CACHE_KEY_SUFFIX = "_result_";
    private static final String CACHE_KEY_SJP_PREFIX = "SJP_";
    private static final String CACHE_KEY_EXTERNAL_PREFIX = "EXT_";
    private static final String CACHE_KEY_INTERNAL_PREFIX = "INT_";
    private static final String SHARED_TIME = "sharedTime";
    private static final String URN = "URN";
    private static final String DEFENDANTS = "Defendants";
    private static final String SUBJECT = "Subject";
    private static final String APPLICATIONS = "Applications";
    private static final String APPLICATION = "Application";
    private static final String AMEND_RESHARE = "Amend_Reshare";
    private static final String COMMON_PLATFORM_URL = "common_platform_url";
    private static final String COMMON_PLATFORM_URL_CAAG = "common_platform_url_caag";
    private static final String COMMON_PLATFORM_URL_AAAG = "common_platform_url_aaag";
    public static final String PROSECUTION_CASEFILE_CASE_AT_A_GLANCE = "prosecution-casefile/case-at-a-glance/";
    public static final String PROSECUTION_CASEFILE_APPLICATION_AT_A_GLANCE = "prosecution-casefile/application-at-a-glance/";
    public static final String SPC = " ";
    private static final String MATERIAL_ID = "materialId";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String CASE_REFERENCES = "caseReferences";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications";
    private static final String SJP_DOCUMENT_TYPE_OTHER = "OTHER";
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String SJP_UPLOAD_CASE_DOCUMENT = "sjp.upload-case-document";
    public static final String IS_RESHARE = "isReshare";

    private static final String POLICE_NOTIFICATION_HEARING_RESULTS = "POLICE_NOTIFICATION_HEARING_RESULTS";
    private static final String APPLICATIONS_POLICE_NOTIFICATION_TEMPLATE = "applications";
    private static final String DEFENDANTS_POLICE_NOTIFICATION_TEMPLATE = "defendants";
    private static final String AMEND_RESHARE_POLICE_NOTIFICATION_TEMPLATE = "amendReshare";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String EMAIL_TEMPLATE_ID = "emailTemplateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    public static final String HEARING_DAY = "hearingDay";

    @Inject
    ReferenceDataService referenceDataService;

    @Inject
    ReferenceCache referenceCache;

    @Inject
    private Sender sender;

    @Inject
    private HearingHelper hearingHelper;

    @Inject
    private DcsCaseHelper dcsCaseHelper;

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
    private ProgressionService progressionService;
    @Inject
    private SjpService sjpService;

    @Inject
    private PoliceEmailHelper policeEmailHelper;

    @Inject
    private UtcClock utcClock;

    @Inject
    private FileService fileService;

    @Inject
    private SystemDocGenerator systemDocGenerator;

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

        if (hearingPayload.getJsonObject(HEARING).getBoolean(IS_SJP_HEARING, false)) {

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

        processDefendantTrackingStatus(resultsAddedEvent);
    }

    @Handles("results.events.hearing-results-added-for-day")
    public void hearingResultAddedForDay(final JsonEnvelope resultsAddedForDayEvent) {
        final LocalDate hearingDay = LocalDate.parse(resultsAddedForDayEvent.payloadAsJsonObject().getString(HEARING_DAY), ISO_LOCAL_DATE);
        processResultsAdded(resultsAddedForDayEvent, "results.command.create-results-for-day", of(hearingDay));

        processDefendantTrackingStatus(resultsAddedForDayEvent);
    }

    @Handles("results.event.publish-to-dcs")
    public void publishToDcs(final JsonEnvelope inputEvent) {
        dcsCaseHelper.prepareAndSendToDCSIfEligible(inputEvent);
    }

    @Handles("results.event.police-result-generated")
    public void createResult(final JsonEnvelope envelope) {
        LOGGER.debug("results.event.police-result-generated {}", envelope.toObfuscatedDebugString());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("public.results.police-result-generated")
                .build();

        sender.sendAsAdmin(envelopeFrom(metadata, envelope.payloadAsJsonObject()));
    }

    @Handles("results.event.nces-email-notification-requested")
    public void handleNcesEmailNotificationRequested(final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));
        boolean isSJPHearing = false;

        if (envelope.payloadAsJsonObject().containsKey(IS_SJP_HEARING)) {
            isSJPHearing = envelope.payloadAsJsonObject().getBoolean(IS_SJP_HEARING);
        }

        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString(MATERIAL_ID));
        final List<String> caseUrns = extractCaseUrns(envelope.payloadAsJsonObject().getString(CASE_REFERENCES));
        final FileParams fileParams = documentGeneratorService.generateNcesDocument(sender, envelope, userId, materialId);

        for (final String caseUrn : caseUrns) {
            if (isSJPHearing) {
                getSjpCaseUUID(caseUrn).ifPresentOrElse(
                    caseUUID -> {
                        LOGGER.info("In SJP case Nces notification requested payload for add court document- caseUrn {}  fileid {} case UUID {}", caseUrn, fileParams.getFileId(), caseUUID);
                        addCourtDocumentForSjpCase(envelope, caseUUID, fileParams.getFilename(), fileParams.getFileId());
                    },
                    () -> LOGGER.warn("No SJP case UUID found for caseUrn: {}", caseUrn)
                );
            } else {
                getCcCaseUUID(caseUrn).ifPresentOrElse(
                    caseUUID -> {
                        LOGGER.info("In CC case Nces notification requested payload for add court document- caseUrn {}  fileid {} case UUID {}", caseUrn, fileParams.getFileId(), caseUUID);
                        addCourtDocumentForCCCase(envelope, caseUUID, materialId, fileParams.getFilename());
                    },
                    () -> LOGGER.warn("No CC case UUID found for caseUrn: {}", caseUrn)
                );
            }
        }
    }

    private static List<String> extractCaseUrns(final String caseReferences) {
        return Stream.of(caseReferences.split(DELIMITER))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .toList();
    }

    private void addCourtDocumentForSjpCase(final JsonEnvelope envelope, final UUID caseUUID, final String fileName, final UUID fileId) {
        LOGGER.info("addCourtDocumentForSjpCase caseUUID {} , fileName {} , fileId {}" , caseUUID , fileName , fileId);
        final JsonObject uploadCaseDocumentPayload = createObjectBuilder()
                .add(CASE_ID, caseUUID.toString())
                .add("caseDocumentType", SJP_DOCUMENT_TYPE_OTHER + "-" + fileName)
                .add("caseDocument", fileId.toString())
                .build();

        final Envelope<JsonObject> sjpEenvelope = envelopeFrom(
                JsonEnvelope.metadataFrom(envelope.metadata()).withName(SJP_UPLOAD_CASE_DOCUMENT),
                uploadCaseDocumentPayload);

        sender.send(sjpEenvelope);
    }

    private void addCourtDocumentForCCCase(final JsonEnvelope envelope, final UUID caseUUID, final UUID materialId, final String fileName) {
        LOGGER.info("addCourtDocumentForCCCase caseUUID {} , fileName {} , materialId {}" , caseUUID , fileName , materialId);
        final CourtDocument courtDocument = buildCourtDocument(caseUUID, materialId, fileName);
        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter.convert(courtDocument))
                .build();
        final Envelope<JsonObject> data = envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(PROGRESSION_ADD_COURT_DOCUMENT), jsonObject);
        sender.send(data);
    }

    private CourtDocument buildCourtDocument(UUID caseUUID, UUID materialId, String fileName) {
        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument()
                        .withProsecutionCaseId(caseUUID)
                        .build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withReceivedDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(fileName)
                .withMaterials(Collections.singletonList(material))
                .withSendToCps(false)
                .withContainsFinancialMeans(false)
                .build();
    }

    private Optional<UUID> getCcCaseUUID(final String caseUrn) {
        final Optional<JsonObject> caseIdJsonObject = progressionService.caseExistsByCaseUrn(caseUrn);
        if(caseIdJsonObject.isPresent() && caseIdJsonObject.get().containsKey(CASE_ID)){
            return of(fromString(caseIdJsonObject.get().getString(CASE_ID)));
        }
        return Optional.empty() ;
    }

    private Optional<UUID> getSjpCaseUUID(final String caseUrn) {
        final Optional<JsonObject> caseIdJsonObject = sjpService.caseExistsByCaseUrn(caseUrn);
        if(caseIdJsonObject.isPresent() && caseIdJsonObject.get().containsKey(SJP_CASE_ID)){
            return of(fromString(caseIdJsonObject.get().getString(SJP_CASE_ID)));
        }
        return Optional.empty() ;
    }

    @Handles("results.event.nces-email-notification")
    public void handleSendNcesEmailNotification(final JsonEnvelope envelope) {
        final JsonObject requestJson = envelope.payloadAsJsonObject();
        final NcesEmailNotification ncesEmailNotification = jsonObjectToObjectConverter.convert(requestJson, NcesEmailNotification.class);
        LOGGER.info("Nces email notification event - {}", envelope.toObfuscatedDebugString());

        final EmailNotification emailNotification = EmailNotification.emailNotification()
                .withNotificationId(ncesEmailNotification.getNotificationId())
                .withTemplateId(ncesEmailNotification.getTemplateId())
                .withSendToAddress(ncesEmailNotification.getSendToAddress())
                .withSubject(ncesEmailNotification.getSubject())
                .withMaterialUrl(ncesEmailNotification.getMaterialUrl())
                .build();

        //Send Email
        LOGGER.info("send email notification - {}", emailNotification.getNotificationId());
        if (isEmpty(emailNotification.getSendToAddress())) {
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

    @Handles("results.event.police-notification-requested-v2")
    public void handlePoliceNotificationRequestedV2(final JsonEnvelope envelope) {
        final PoliceNotificationRequestedV2 policeNotificationRequestedV2 = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PoliceNotificationRequestedV2.class);
        final Map<String, String> additionalInformation = buildPoliceNotificationV2(policeNotificationRequestedV2);
        generatePoliceNotificationHearingResult(envelope, policeNotificationRequestedV2, additionalInformation);
    }

    private void generatePoliceNotificationHearingResult(final JsonEnvelope envelope, final PoliceNotificationRequestedV2 policeNotificationRequestedV2, final Map<String, String> additionalInformation) {
        if (additionalInformation.get(AMEND_RESHARE).equalsIgnoreCase(NO)) {
            final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
            final Notification notification = new Notification(notificationId,
                    fromString(additionalInformation.get(EMAIL_TEMPLATE_ID)),
                    policeNotificationRequestedV2.getPoliceEmailAddress(),
                    additionalInformation);
            final JsonObject notificationPayload = this.objectToJsonObjectConverter.convert(notification);
            LOGGER.info("Sending initial police email notification for hearing result share, notificationId: {}", notificationId);
            notificationNotifyService.sendEmailNotification(envelope, notificationPayload);
        } else {
            final JsonObjectBuilder payload = createObjectBuilder()
                    .add(URN, additionalInformation.get(URN))
                    .add(DEFENDANTS_POLICE_NOTIFICATION_TEMPLATE, additionalInformation.get(DEFENDANTS))
                    .add(AMEND_RESHARE_POLICE_NOTIFICATION_TEMPLATE, additionalInformation.get(AMEND_RESHARE));
            if (additionalInformation.containsKey(APPLICATIONS)) {
                payload.add(APPLICATIONS_POLICE_NOTIFICATION_TEMPLATE, additionalInformation.get(APPLICATIONS));
            }

            final UUID notificationId = policeNotificationRequestedV2.getNotificationId();
            final String fileName = POLICE_NOTIFICATION_HEARING_RESULTS + notificationId + ".html";
            final UUID fileId = fileService.storePayload(payload.build(), fileName, POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE.getValue(), ConversionFormat.THYMELEAF);
            final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                    POLICE_NOTIFICATION_HEARING_RESULTS,
                    POLICE_NOTIFICATION_HEARING_RESULTS_TEMPLATE,
                    ConversionFormat.THYMELEAF,
                    notificationId.toString(),
                    fileId,
                    additionalInformation);

            LOGGER.info("Generating document for police notification of hearing amend and re-share result. Notification ID: {}, File ID: {}", notificationId, fileId);
            systemDocGenerator.generateDocument(documentGenerationRequest, envelope);
        }
    }

    @Handles("results.event.appeal-update-notification-requested")
    public void handleAppealUpdateNotificationRequested(final JsonEnvelope envelope) {
        final AppealUpdateNotificationRequested appealUpdateNotificationRequested = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), AppealUpdateNotificationRequested.class);
        final JsonObject notificationPayload = this.objectToJsonObjectConverter.convert(buildAppealUpdateNotification(appealUpdateNotificationRequested));
        notificationNotifyService.sendEmailNotification(envelope, notificationPayload);
    }

    private void sendEventToGrid(final JsonEnvelope envelope, final String hearingId, final String eventType) {
        final Optional<String> userId = envelope.metadata().userId();
        try {
            LOGGER.info("Adding Hearing Resulted for hearing {} and eventType {} to EventGrid", hearingId, eventType);
            userId.ifPresent(s -> eventGridService.sendHearingResultedEvent(fromString(s), hearingId, eventType));
        } catch (Exception e) {
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

        final List<CourtApplication> courtApplications = publicHearingResulted.getHearing().getCourtApplications();
        final List<CaseDetails> caseDetails = new CasesConverter(referenceCache, referenceDataService, progressionService).convert(publicHearingResulted);

        publicHearingResulted.getHearing().getHearingDays().sort(comparing(HearingDay::getSittingDay).reversed());
        final JsonArrayBuilder caseDetailsJsonArrayBuilder = createArrayBuilder();
        if (isNotEmpty(caseDetails)) {
            caseDetails.forEach(c -> caseDetailsJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
        }
        final JsonObjectBuilder resultJsonPayload = createObjectBuilder();
        resultJsonPayload.add("session", objectToJsonObjectConverter.convert(new BaseStructureConverter(referenceDataService).convert(publicHearingResulted)));
        resultJsonPayload.add("cases", caseDetailsJsonArrayBuilder.build());
        resultJsonPayload.add("jurisdictionType", publicHearingResulted.getHearing().getJurisdictionType().toString());

        hearingDay.ifPresent(localDate -> resultJsonPayload.add(HEARING_DAY, localDate.toString()));
        if (isNotEmpty(courtApplications)) {
            final JsonArrayBuilder courtApplicationJsonArrayBuilder = createArrayBuilder();
            courtApplications.forEach(c -> courtApplicationJsonArrayBuilder.add(objectToJsonObjectConverter.convert(c)));
            resultJsonPayload.add("courtApplications", courtApplicationJsonArrayBuilder.build());
        }

        if (hearingResultPayload.containsKey(IS_RESHARE)) {
            resultJsonPayload.add(IS_RESHARE, hearingResultPayload.getBoolean(IS_RESHARE));
        }

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(commandName)
                .build();
        final JsonObject payload = resultJsonPayload.build();
        sender.sendAsAdmin(envelopeFrom(metadata, payload));
    }

    /**
     * Processes Defendant Tracking Status, to track the Electronic Monitoring or Warrant of Arrest for the defendant.
     * Calls "results.command.update-defendant-tracking-status" per defendant
     *
     * @param envelope - the event being processed.
     */
    @SuppressWarnings("squid:S1481")
    private void processDefendantTrackingStatus(final JsonEnvelope envelope) {

        final Hearing resultedHearing = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject().getJsonObject(HEARING),
                Hearing.class);


        ofNullable(resultedHearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).collect(toList()).forEach(prosecutionCase ->

                prosecutionCase.getDefendants().forEach(defendant -> {
                    final JsonObjectBuilder defendantBuilder = createObjectBuilder().add("defendantId", defendant.getId().toString());
                    final JsonArrayBuilder offencesBuilder = createArrayBuilder();
                    defendant.getOffences().forEach(offence ->
                            offencesBuilder.add(objectToJsonObjectConverter.convert(offence)));
                    defendantBuilder.add("offences", offencesBuilder);

                    final Metadata metadata = metadataFrom(envelope.metadata())
                            .withName(RESULTS_COMMAND_UPDATE_DEFENDANT_TRACKING_STATUS)
                            .build();
                    final JsonObject payload = defendantBuilder.build();
                    sender.sendAsAdmin(envelopeFrom(metadata, payload));

                })
        );

    }

    private Notification buildNotification(final PoliceNotificationRequested policeNotificationRequested) {
        final Map<String, String> personalisationProperties = new HashMap<>();
        personalisationProperties.put(URN, policeNotificationRequested.getUrn());
        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());
        return new Notification(policeNotificationRequested.getNotificationId(), fromString(applicationParameters.getEmailTemplateId()), policeNotificationRequested.getPoliceEmailAddress(), personalisationProperties);
    }


    private Map<String, String> buildPoliceNotificationV2(final PoliceNotificationRequestedV2 policeNotificationRequestedV2) {
        final Map<String, String> personalisationProperties = new HashMap<>();
        String emailTemplateId = applicationParameters.getEmailTemplateId();
        personalisationProperties.put(URN, policeNotificationRequestedV2.getUrn());
        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());
        personalisationProperties.put(AMEND_RESHARE, NO);

        if (isNotEmpty(policeNotificationRequestedV2.getCaseDefendants())) {
            final boolean resultsAmended = !policeNotificationRequestedV2.getAmendReshare().isEmpty();
            final CaseResultDetails caseResultDetails = policeNotificationRequestedV2.getCaseResultDetails();

            if (resultsAmended && nonNull(caseResultDetails) && isNotEmpty(caseResultDetails.getDefendantResultDetails())) {
                personalisationProperties.put(DEFENDANTS, policeEmailHelper.buildDefendantAmendmentDetails(policeNotificationRequestedV2.getCaseResultDetails()));
            } else {
                personalisationProperties.put(DEFENDANTS, getCaseDefendants(policeNotificationRequestedV2.getCaseDefendants(), resultsAmended));
            }

            personalisationProperties.put(AMEND_RESHARE, resultsAmended ? YES : NO);

            final String sortedSubject = FixedListComparator.sortBasedOnFixedList(getCaseSubject(policeNotificationRequestedV2.getCaseDefendants()));
            final String caseApplication = policeNotificationRequestedV2.getApplicationTypeForCase();

            final String applicationPropValue = buildApplicationProp(policeNotificationRequestedV2);
            if (isNotEmpty(applicationPropValue)) {
                personalisationProperties.put(APPLICATIONS, applicationPropValue);
            }

            emailTemplateId = getPoliceEmailTemplate(isNotEmpty(applicationPropValue), resultsAmended);

            final boolean isApplicationAmended = nonNull(caseResultDetails) && isNotEmpty(caseResultDetails.getApplicationResultDetails()) && caseResultDetails.getApplicationResultDetails().stream()
                    .anyMatch(applicationResultDetails -> isNotEmpty(applicationResultDetails.getJudicialResultDetails()));

            personalisationProperties.put(SUBJECT, buildEmailSubject(resultsAmended, policeNotificationRequestedV2.getUrn(), policeNotificationRequestedV2.getDateOfHearing(), caseApplication, sortedSubject, policeNotificationRequestedV2.getCourtCentre(), isApplicationAmended));
            personalisationProperties.put(COMMON_PLATFORM_URL_CAAG, applicationParameters.getCommonPlatformUrl().concat(PROSECUTION_CASEFILE_CASE_AT_A_GLANCE).concat(policeNotificationRequestedV2.getCaseId()));
        }

        personalisationProperties.put(NOTIFICATION_ID, policeNotificationRequestedV2.getNotificationId().toString());
        personalisationProperties.put(EMAIL_TEMPLATE_ID, emailTemplateId);
        personalisationProperties.put(SEND_TO_ADDRESS, policeNotificationRequestedV2.getPoliceEmailAddress());
        return personalisationProperties;
    }

    private Notification buildAppealUpdateNotification(final AppealUpdateNotificationRequested appealUpdateNotificationRequested){
        final Map<String, String> personalisationProperties = new HashMap<>();
        String commonPlatformUrl = applicationParameters.getCommonPlatformUrl();

        if (!commonPlatformUrl.endsWith("/")) {
            commonPlatformUrl = commonPlatformUrl + "/";
        }

        personalisationProperties.put(COMMON_PLATFORM_URL, applicationParameters.getCommonPlatformUrl());
        personalisationProperties.put(COMMON_PLATFORM_URL_AAAG, commonPlatformUrl
                .concat(PROSECUTION_CASEFILE_APPLICATION_AT_A_GLANCE).concat(appealUpdateNotificationRequested.getApplicationId()));

        personalisationProperties.put(SUBJECT, appealUpdateNotificationRequested.getSubject());
        personalisationProperties.put(URN, appealUpdateNotificationRequested.getUrn());
        personalisationProperties.put(DEFENDANTS, appealUpdateNotificationRequested.getDefendant());

        return new Notification(appealUpdateNotificationRequested.getNotificationId(),
                fromString(applicationParameters.getAppealUpdateNotificationTemplateId()),
                appealUpdateNotificationRequested.getEmailAddress(), personalisationProperties);
    }

    private String buildApplicationProp(final PoliceNotificationRequestedV2 policeNotificationRequestedV2) {
        final String caseApplication = policeNotificationRequestedV2.getApplicationTypeForCase();
        final boolean resultsAmended = !policeNotificationRequestedV2.getAmendReshare().isEmpty();
        final CaseResultDetails caseResultDetails = policeNotificationRequestedV2.getCaseResultDetails();

        if (isEmpty(caseApplication)) {
            return null;
        }

        if (!resultsAmended) {
            return caseApplication;
        }

        if (nonNull(caseResultDetails) && nonNull(caseResultDetails.getApplicationResultDetails())) {
            return policeEmailHelper.buildApplicationAmendmentDetails(caseResultDetails.getApplicationResultDetails());
        }

        return null;
    }

    protected String getCaseSubject(final List<CaseDefendant> caseDefendants) {
        final String caseSubject = caseDefendants.stream()
                .flatMap(caseDefendant -> caseDefendant.getOffences().stream())
                .flatMap(offenceDetails -> {
                    if (offenceDetails.getJudicialResults() != null) {
                        return offenceDetails.getJudicialResults().stream();
                    } else {
                        return Stream.empty();
                    }
                })
                .filter(judicialResult -> nonNull(judicialResult.getIsNewAmendment()) && judicialResult.getIsNewAmendment())
                .filter(judicialResult -> {
                    String policeSubjectLineTitle = judicialResult.getPoliceSubjectLineTitle();
                    String resultText = judicialResult.getResultText();
                    return !(policeSubjectLineTitle != null && policeSubjectLineTitle.equals("Bail Conditions Cancelled")
                            && (resultText != null && (resultText.contains("Domestic Violence case") || resultText.contains("Vulnerable or Intimidated Victim"))));
                })
                .map(JudicialResult::getPoliceSubjectLineTitle)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.joining(DELIMITER));
        return caseSubject.isEmpty() ? "" : caseSubject;
    }


    private String buildEmailSubject(final Boolean resultsAmended, final String urn, final String hearingDate, final String caseApplication, final String subject, final String courtCentre, final boolean isApplicationAmended) {
        final String dateTimeFormat = dateTimeFormat(hearingDate);
        final String formattedSubject = subject.replace(DELIMITER, " / ").trim();

        final StringBuilder sb = new StringBuilder();
        if (TRUE.equals(resultsAmended)) {
            sb.append(AMEND_RESHARE1).append(SPC);
        }

        sb.append(urn).append(SPC);
        sb.append(dateTimeFormat).append(SPC);

        if (TRUE.equals(resultsAmended) && nonNull(courtCentre)) {
            sb.append(courtCentre).append(SPC);
        }

        if (printApplicationText(resultsAmended, caseApplication, isApplicationAmended)) {
            sb.append(APPLICATION).append(SPC);
            if (!formattedSubject.isEmpty()) {
                sb.append("/").append(SPC);
            }
        }

        if (!formattedSubject.isEmpty()) {
            sb.append(formattedSubject);
        }

        return sb.toString().trim();

    }

    private boolean printApplicationText(final boolean resultsAmended, final String caseApplication, final boolean isApplicationAmended) {
        if (isEmpty(caseApplication)) {
            return false;
        }

        if (!TRUE.equals(resultsAmended)) {
            return true;
        }

        return isApplicationAmended;
    }

    private String getPoliceEmailTemplate(final Boolean includeApplications, final Boolean includeResultsAmended) {
        if (TRUE.equals(includeApplications)) {
            return TRUE.equals(includeResultsAmended)
                    ? applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()
                    : applicationParameters.getPoliceEmailHearingResultsWithApplicationTemplateId();
        }
        return TRUE.equals(includeResultsAmended)
                ? applicationParameters.getPoliceNotificationHearingResultsAmendedTemplateId()
                : applicationParameters.getPoliceEmailHearingResultsTemplateId();
    }

    private String dateTimeFormat(final String hearingDate) {
        final DateTimeFormatter dateTimeFormatter = ofPattern("yyyy-MM-dd");
        final LocalDate date = LocalDate.parse(hearingDate.isEmpty() ? LocalDate.now().toString() : hearingDate, dateTimeFormatter);
        final DateTimeFormatter formatter = ofPattern("dd-MM-yyyy");
        return date.format(formatter);
    }

    private String getCaseDefendants(final List<CaseDefendant> caseDefendants, final boolean amended) {
        return caseDefendants.stream()
                .map(CaseDefendant::getIndividualDefendant)
                .map(IndividualDefendant::getPerson)
                .map(individual -> individual.getFirstName().concat(" ")
                        .concat(individual.getLastName()))
                .collect(Collectors.joining(", "));
    }

    private void raiseHandlerEvent(final JsonEnvelope envelope, final JsonObject commandPayload) {
        final Envelope<JsonObject> jsonObjectEnvelope = envelop(commandPayload)
                .withName(RESULTS_COMMAND_HANDLER_CASE_OR_APPLICATION_EJECTED)
                .withMetadataFrom(envelope);
        sender.sendAsAdmin(jsonObjectEnvelope);
    }
}
